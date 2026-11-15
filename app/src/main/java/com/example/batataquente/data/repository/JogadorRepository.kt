package com.example.batataquente.data.repository

import com.example.batataquente.data.local.JogadorDao
import com.example.batataquente.data.local.JogadorEntity
import kotlinx.coroutines.flow.Flow

class JogadorRepository(private val dao: JogadorDao) {
    fun observar(): Flow<List<JogadorEntity>> = dao.observarTodos()

    suspend fun salvar(jogador: JogadorEntity): Result<Unit> {
        return try {
            if (jogador.nome.isBlank()) {
                Result.failure(IllegalArgumentException("Informe o nome do jogador."))
            } else {
                if (jogador.id == 0) dao.inserir(jogador) else dao.atualizar(jogador)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun excluir(jogador: JogadorEntity): Result<Unit> {
        return try {
            dao.excluir(jogador)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrarResultado(ids: List<Int>, vencedorId: Int, pontos: Map<Int, Int>): Result<Unit> {
        return try {
            for (id in ids) {
                val jogador = dao.buscar(id)
                if (jogador != null) {
                    dao.atualizar(jogador.copy(
                        partidas = jogador.partidas + 1,
                        vitorias = jogador.vitorias + if (id == vencedorId) 1 else 0,
                        pontos = jogador.pontos + (pontos[id] ?: 0)
                    ))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
