package com.example.batataquente.data.repository

import com.example.batataquente.data.local.QuestaoDao
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.logic.BaralhoQuestoes
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fonte única das questões para o resto do app.
 *
 * - Firestore (nuvem) é a fonte OFICIAL: cadastro, edição e exclusão acontecem lá primeiro.
 * - Room (celular) é uma CÓPIA local: o jogo lê daqui, então funciona mesmo sem internet.
 * - Sincronizar = baixar tudo do Firestore e substituir a cópia do Room.
 *
 * Cada questão vira um documento na coleção "questoes". O ID do documento é a
 * "chave" da pergunta (7 × 8 e 8 × 7 geram a mesma chave), o que impede duplicatas.
 */
class QuestaoRepository(
    private val dao: QuestaoDao,
    // Sem argumento, getInstance() usa o banco "(default)". Nosso banco tem nome próprio.
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(BANCO)
) {
    private val colecao get() = firestore.collection(COLECAO)

    fun observar(): Flow<List<QuestaoEntity>> = dao.observarTodas().catch { emit(emptyList()) }

    /**
     * Cria (id == 0) ou edita uma questão. Para edição, [original] é a versão antes
     * da alteração; se não for informada, é buscada no Room pelo id.
     */
    suspend fun salvar(questao: QuestaoEntity, original: QuestaoEntity? = null): Result<Unit> = executar {
        val pergunta = questao.pergunta.trim()
        require(pergunta.isNotBlank()) { "Escreva a pergunta." }
        require(questao.dificuldade in DIFICULDADES) { "Dificuldade inválida." }

        val anterior = original ?: if (questao.id != 0) dao.buscar(questao.id) else null
        val idNovo = idRemoto(pergunta)
        val idAntigo = anterior?.let { idRemoto(it.pergunta) }

        // Se a pergunta é nova (ou mudou), confere no servidor se ela já existe.
        if (idNovo != idAntigo) {
            val existente = comLimite { colecao.document(idNovo).get(Source.SERVER).await() }
            require(!existente.exists()) { "A questão \"$pergunta\" já está cadastrada." }
        }

        // Batch = várias escritas enviadas juntas: ou todas funcionam, ou nenhuma.
        val final = questao.copy(pergunta = pergunta, origem = ORIGEM_FIREBASE)
        val batch = firestore.batch()
        if (idAntigo != null && idAntigo != idNovo) batch.delete(colecao.document(idAntigo))
        batch.set(colecao.document(idNovo), paraMapa(final))
        comLimite { batch.commit().await(); Unit }

        // Só depois que a nuvem confirmou, atualiza a cópia local.
        if (anterior != null) dao.atualizar(final.copy(id = anterior.id)) else dao.inserir(final.copy(id = 0))
        Unit
    }

    suspend fun excluir(questao: QuestaoEntity): Result<Unit> = executar {
        comLimite { colecao.document(idRemoto(questao.pergunta)).delete().await(); Unit }
        dao.excluir(questao)
    }

    /** Baixa todas as questões do Firestore e substitui as do Room. Retorna quantas vieram. */
    suspend fun sincronizar(): Result<Int> = executar {
        val snapshot = comLimite { colecao.get(Source.SERVER).await() }
        val remotas = snapshot.documents.mapNotNull(::paraEntidade)
        dao.substituirTodas(remotas)
        remotas.size
    }

    /**
     * Chamado ao abrir o app. Se o Firestore estiver vazio, envia as questões de exemplo;
     * depois sincroniza. Sem internet no primeiro uso, grava os exemplos só no Room.
     */
    suspend fun iniciarQuestoes(): Result<Int> {
        val preparo = executar {
            val vazia = comLimite { colecao.limit(1).get(Source.SERVER).await() }.isEmpty
            if (vazia) {
                val batch = firestore.batch()
                EXEMPLOS.forEach { batch.set(colecao.document(idRemoto(it.pergunta)), paraMapa(it)) }
                comLimite { batch.commit().await(); Unit }
            }
        }
        val resultado = preparo.fold(onSuccess = { sincronizar() }, onFailure = { Result.failure(it) })
        if (resultado.isFailure) {
            runCatching { if (dao.contar() == 0) EXEMPLOS.forEach { dao.inserir(it) } }
        }
        return resultado
    }

    // ---------- Conversões entre Room e Firestore ----------

    private fun paraMapa(q: QuestaoEntity): Map<String, Any> = mapOf(
        "pergunta" to q.pergunta,
        "resposta" to q.resposta,
        "dificuldade" to q.dificuldade,
        "atualizadoEm" to FieldValue.serverTimestamp()
    )

    /** Documentos incompletos (ex.: editados à mão no console) são ignorados. */
    private fun paraEntidade(doc: DocumentSnapshot): QuestaoEntity? {
        val pergunta = doc.getString("pergunta")?.takeIf { it.isNotBlank() } ?: return null
        val resposta = doc.getLong("resposta")?.toInt() ?: return null
        val dificuldade = doc.getString("dificuldade")?.takeIf { it in DIFICULDADES } ?: return null
        return QuestaoEntity(pergunta = pergunta, resposta = resposta, dificuldade = dificuldade, origem = ORIGEM_FIREBASE)
    }

    private fun idRemoto(pergunta: String) = BaralhoQuestoes.chave(pergunta)

    // ---------- Tratamento de erros ----------

    /**
     * Sem internet, algumas operações do Firebase ficam esperando para sempre.
     * Aqui limitamos a espera e transformamos isso em um erro claro.
     */
    private suspend fun <T : Any> comLimite(bloco: suspend () -> T): T =
        withTimeoutOrNull(TEMPO_LIMITE_MS) { bloco() }
            ?: throw SemRespostaException()

    /** Executa o bloco e converte qualquer exceção em Result.failure com mensagem em português. */
    private suspend fun <T> executar(bloco: suspend () -> T): Result<T> = try {
        Result.success(bloco())
    } catch (e: CancellationException) {
        throw e // cancelamento de corrotina não é erro: precisa continuar se propagando
    } catch (e: Exception) {
        Result.failure(Exception(mensagemAmigavel(e), e))
    }

    private fun mensagemAmigavel(e: Exception): String = when (e) {
        is SemRespostaException -> e.message!!
        is IllegalArgumentException -> e.message ?: "Dados inválidos."
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Sem conexão com o Firebase. Verifique a internet."
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "O Firebase recusou o acesso. Verifique as regras do Firestore."
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Faça login novamente para alterar as questões."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "O Firebase demorou demais para responder."
            FirebaseFirestoreException.Code.NOT_FOUND -> "Banco Firestore não encontrado. Crie o banco no console do Firebase."
            else -> "Erro no Firebase (${e.code})."
        }
        else -> e.message ?: "Erro inesperado."
    }

    private class SemRespostaException : Exception("O Firebase não respondeu. Verifique a internet e tente de novo.")

    companion object {
        /** ID do banco criado no console do Firebase (Firestore Database). */
        const val BANCO = "batataquente"
        const val COLECAO = "questoes"
        const val ORIGEM_FIREBASE = "FIREBASE"
        private const val TEMPO_LIMITE_MS = 10_000L
        val DIFICULDADES = listOf("Fácil", "Médio", "Difícil")

        private val EXEMPLOS = listOf(
            Triple("5 + 7", 12, "Fácil"), Triple("9 + 8", 17, "Fácil"),
            Triple("14 - 6", 8, "Fácil"), Triple("3 × 4", 12, "Fácil"),
            Triple("25 + 17", 42, "Médio"), Triple("7 × 8", 56, "Médio"),
            Triple("72 ÷ 8", 9, "Médio"), Triple("43 - 19", 24, "Médio"),
            Triple("15 × 6", 90, "Difícil"), Triple("144 ÷ 12", 12, "Difícil"),
            Triple("125 - 78", 47, "Difícil"), Triple("17 × 9", 153, "Difícil")
        ).map { (pergunta, resposta, dificuldade) ->
            QuestaoEntity(pergunta = pergunta, resposta = resposta, dificuldade = dificuldade)
        }
    }
}
