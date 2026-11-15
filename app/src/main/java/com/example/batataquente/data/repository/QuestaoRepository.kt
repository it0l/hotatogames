package com.example.batataquente.data.repository

import com.example.batataquente.data.local.QuestaoDao
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.data.remote.FirebaseQuestaoDataSource
import kotlinx.coroutines.flow.Flow

class QuestaoRepository(
    private val dao: QuestaoDao,
    private val remoto: FirebaseQuestaoDataSource = FirebaseQuestaoDataSource()
) {
    fun observar(): Flow<List<QuestaoEntity>> = dao.observarTodas()
    fun cloudConectado(): Boolean = remoto.conectado()
    fun desconectarCloud() = remoto.desconectar()

    suspend fun autenticar(email: String, senha: String): Result<Unit> {
        return try {
            remoto.autenticar(email, senha)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun salvar(questao: QuestaoEntity, publicar: Boolean = false): Result<Unit> {
        return try {
            if (questao.pergunta.isBlank()) {
                return Result.failure(IllegalArgumentException("Escreva a pergunta."))
            }
            if (questao.dificuldade !in listOf("Fácil", "Médio", "Difícil")) {
                return Result.failure(IllegalArgumentException("Dificuldade inválida."))
            }
            if (publicar) {
                val id = remoto.salvar(questao.firebaseId, questao.pergunta, questao.resposta, questao.dificuldade)
                val local = questao.copy(firebaseId = id, origem = "FIREBASE")
                if (local.id == 0) dao.inserir(local) else dao.atualizar(local)
            } else {
                if (questao.firebaseId != null) {
                    return Result.failure(IllegalArgumentException("Conecte ao Firebase para editar esta questão."))
                }
                if (questao.id == 0) dao.inserir(questao) else dao.atualizar(questao)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun excluir(questao: QuestaoEntity, publicar: Boolean = false): Result<Unit> {
        return try {
            if (questao.firebaseId != null) {
                if (!publicar) {
                    return Result.failure(IllegalArgumentException("Conecte ao Firebase para excluir esta questão."))
                }
                remoto.excluir(questao.firebaseId)
            }
            dao.excluir(questao)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizar(): Result<Int> {
        return try {
            val lista = remoto.baixar()
            val idsRemotos = lista.map { it.id }.toSet()
            for (q in lista) {
                val antiga = dao.buscarFirebase(q.id)
                val local = QuestaoEntity(
                    id = antiga?.id ?: 0, pergunta = q.pergunta, resposta = q.resposta,
                    dificuldade = q.dificuldade, origem = "FIREBASE", firebaseId = q.id
                )
                if (antiga == null) dao.inserir(local) else dao.atualizar(local)
            }
            for (q in dao.listarFirebase()) {
                if (q.firebaseId !in idsRemotos) dao.excluir(q)
            }
            Result.success(lista.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun iniciarQuestoes(): Result<Unit> {
        return try {
            if (dao.contar() == 0) {
                val exemplos = listOf(
                    Triple("5 + 7", 12, "Fácil"), Triple("9 + 8", 17, "Fácil"),
                    Triple("14 - 6", 8, "Fácil"), Triple("3 × 4", 12, "Fácil"),
                    Triple("25 + 17", 42, "Médio"), Triple("7 × 8", 56, "Médio"),
                    Triple("72 ÷ 8", 9, "Médio"), Triple("43 - 19", 24, "Médio"),
                    Triple("15 × 6", 90, "Difícil"), Triple("144 ÷ 12", 12, "Difícil"),
                    Triple("125 - 78", 47, "Difícil"), Triple("17 × 9", 153, "Difícil")
                )
                for ((pergunta, resposta, dificuldade) in exemplos) {
                    dao.inserir(QuestaoEntity(pergunta = pergunta, resposta = resposta, dificuldade = dificuldade))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
