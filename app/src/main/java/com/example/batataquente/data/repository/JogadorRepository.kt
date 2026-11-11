package com.example.batataquente.data.repository

import com.example.batataquente.data.local.JogadorDao
import com.example.batataquente.data.local.JogadorEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class JogadorRepository(private val dao: JogadorDao) {
    fun observar(): Flow<List<JogadorEntity>> = dao.observarTodos().catch { emit(emptyList()) }

    suspend fun salvar(jogador: JogadorEntity): Result<Unit> = runCatching {
        require(jogador.nome.isNotBlank()) { "Informe o nome do jogador." }
        if (jogador.id == 0) dao.inserir(jogador) else dao.atualizar(jogador)
        Unit
    }

    suspend fun excluir(jogador: JogadorEntity): Result<Unit> = runCatching { dao.excluir(jogador) }

    suspend fun registrarResultado(ids: List<Int>, vencedorId: Int, pontos: Map<Int, Int>): Result<Unit> = runCatching {
        ids.forEach { id ->
            val jogador = dao.buscar(id) ?: return@forEach
            dao.atualizar(jogador.copy(
                partidas = jogador.partidas + 1,
                vitorias = jogador.vitorias + if (id == vencedorId) 1 else 0,
                pontos = jogador.pontos + (pontos[id] ?: 0)
            ))
        }
    }
}
