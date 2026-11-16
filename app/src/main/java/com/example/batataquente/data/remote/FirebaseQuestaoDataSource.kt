package com.example.batataquente.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

data class QuestaoRemota(
    val id: String,
    val pergunta: String,
    val resposta: Int,
    val dificuldade: String
)

class FirebaseQuestaoDataSource(
    private val firestore: FirebaseFirestore = FirebaseConfig.firestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val colecao get() = firestore.collection("questoes")

    // O login anônimo dos jogadores (ranking online) não conta como administrador.
    fun conectado(): Boolean = auth.currentUser?.isAnonymous == false

    suspend fun autenticar(email: String, senha: String) {
        require(email.isNotBlank() && senha.isNotBlank()) { "Informe e-mail e senha do Firebase." }
        auth.signInWithEmailAndPassword(email.trim(), senha).await()
    }

    fun desconectar() = auth.signOut()

    suspend fun baixar(): List<QuestaoRemota> {
        val documentos = colecao.get(Source.SERVER).await().documents
        val questoes = mutableListOf<QuestaoRemota>()
        for (documento in documentos) {
            val questao = converter(documento)
            if (questao != null) questoes.add(questao)
        }
        return questoes
    }

    private fun converter(doc: DocumentSnapshot): QuestaoRemota? {
        val pergunta = doc.getString("pergunta") ?: return null
        val numero = doc.getLong("resposta")?.toInt() ?: return null
        val nivel = doc.getString("dificuldade") ?: "Fácil"
        return QuestaoRemota(doc.id, pergunta, numero, nivel)
    }

    suspend fun salvar(id: String?, pergunta: String, resposta: Int, dificuldade: String): String {
        check(conectado()) { "Entre com a conta Firebase de administrador para publicar questões." }
        val doc = if (id == null) colecao.document() else colecao.document(id)
        doc.set(mapOf("pergunta" to pergunta, "resposta" to resposta, "dificuldade" to dificuldade)).await()
        return doc.id
    }

    suspend fun excluir(id: String) {
        check(conectado()) { "Entre com a conta Firebase de administrador para excluir questões remotas." }
        colecao.document(id).delete().await()
    }
}
