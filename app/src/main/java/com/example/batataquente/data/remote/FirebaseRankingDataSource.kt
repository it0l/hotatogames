package com.example.batataquente.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer
import java.util.Date

data class ResultadoJogador(
    val nome: String,
    val pontos: Int,
    val acertos: Int,
    val vidas: Int,
    val venceu: Boolean
)

data class PartidaRemota(
    val dificuldade: String,
    val duracao: Int,
    val rodadas: Int,
    val jogadores: List<ResultadoJogador>
)

/** Uma linha da tabela de recordes: a pontuação de um jogador em uma partida. */
data class RecordeRemoto(
    val id: String,
    val nome: String,
    val pontos: Int,
    val dificuldade: String,
    val data: Date?
)

/** Totais de um jogador somando todas as partidas, de qualquer aparelho. */
data class JogadorOnline(
    val chave: String,
    val nome: String,
    val pontos: Int,
    val vitorias: Int,
    val partidas: Int
)

/**
 * Ranking online no estilo fliperama: não há cadastro, o jogador é identificado
 * só pelo nome. O mesmo nome em aparelhos diferentes soma na mesma ficha.
 *
 * Coleções:
 * - partidas/{auto}: histórico completo de cada partida;
 * - recordes/{auto}: uma entrada por jogador por partida (tabela de high score);
 * - jogadores/{chaveDoNome}: totais acumulados (pontos, vitórias, partidas).
 */
class FirebaseRankingDataSource(
    private val firestore: FirebaseFirestore = FirebaseConfig.firestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val partidas get() = firestore.collection("partidas")
    private val recordes get() = firestore.collection("recordes")
    private val jogadores get() = firestore.collection("jogadores")

    /** Login anônimo: o jogador não vê nada, mas as regras do Firestore exigem um usuário. */
    suspend fun garantirLogin() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    /**
     * Grava partida, recordes e totais de uma vez (batch).
     * Retorna true se o servidor confirmou, false se ficou na fila offline do Firestore.
     */
    suspend fun registrarPartida(partida: PartidaRemota): Boolean {
        garantirLogin()
        val uid = auth.currentUser?.uid ?: error("Não foi possível entrar no Firebase.")
        val lote = firestore.batch()
        val partidaRef = partidas.document()

        lote.set(partidaRef, mapOf(
            "uid" to uid,
            "criadaEm" to FieldValue.serverTimestamp(),
            "dificuldade" to partida.dificuldade,
            "duracao" to partida.duracao,
            "rodadas" to partida.rodadas,
            "vencedor" to partida.jogadores.first { it.venceu }.nome.trim(),
            "jogadores" to partida.jogadores.map {
                mapOf(
                    "nome" to it.nome.trim(),
                    "chave" to chave(it.nome),
                    "pontos" to it.pontos,
                    "acertos" to it.acertos,
                    "vidas" to it.vidas,
                    "venceu" to it.venceu
                )
            }
        ))

        for (jogador in partida.jogadores) {
            val chave = chave(jogador.nome)
            lote.set(recordes.document(), mapOf(
                "nome" to jogador.nome.trim(),
                "chave" to chave,
                "pontos" to jogador.pontos,
                "dificuldade" to partida.dificuldade,
                "partidaId" to partidaRef.id,
                "criadoEm" to FieldValue.serverTimestamp()
            ))
            // increment funciona offline e não precisa ler o documento antes.
            lote.set(jogadores.document(chave), mapOf(
                "nome" to jogador.nome.trim(),
                "pontos" to FieldValue.increment(jogador.pontos.toLong()),
                "vitorias" to FieldValue.increment(if (jogador.venceu) 1L else 0L),
                "partidas" to FieldValue.increment(1L),
                "atualizadoEm" to FieldValue.serverTimestamp()
            ), SetOptions.merge())
        }

        val envio = lote.commit()
        return withTimeoutOrNull(8_000L) { envio.await(); true } ?: false
    }

    fun observarRecordes(limite: Long = 10): Flow<List<RecordeRemoto>> = callbackFlow {
        val registro = recordes.orderBy("pontos", Query.Direction.DESCENDING).limit(limite)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val nome = doc.getString("nome") ?: return@mapNotNull null
                    val pontos = doc.getLong("pontos")?.toInt() ?: return@mapNotNull null
                    RecordeRemoto(doc.id, nome, pontos, doc.getString("dificuldade") ?: "", doc.getDate("criadoEm"))
                }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    fun observarJogadores(limite: Long = 20): Flow<List<JogadorOnline>> = callbackFlow {
        val registro = jogadores.orderBy("pontos", Query.Direction.DESCENDING).limit(limite)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val nome = doc.getString("nome") ?: return@mapNotNull null
                    JogadorOnline(
                        chave = doc.id,
                        nome = nome,
                        pontos = doc.getLong("pontos")?.toInt() ?: 0,
                        vitorias = doc.getLong("vitorias")?.toInt() ?: 0,
                        partidas = doc.getLong("partidas")?.toInt() ?: 0
                    )
                }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    companion object {
        /** "  João Pedro " e "joao pedro" viram a mesma chave: "joao-pedro". */
        fun chave(nome: String): String {
            val semAcento = Normalizer.normalize(nome.trim().lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
            val chave = semAcento.replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40)
            return chave.ifEmpty { "jogador" }
        }
    }
}
