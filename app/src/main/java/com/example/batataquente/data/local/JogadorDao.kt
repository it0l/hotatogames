package com.example.batataquente.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JogadorDao {
    @Query("SELECT * FROM jogadores ORDER BY nome COLLATE NOCASE")
    fun observarTodos(): Flow<List<JogadorEntity>>

    @Insert suspend fun inserir(jogador: JogadorEntity): Long
    @Update suspend fun atualizar(jogador: JogadorEntity)
    @Delete suspend fun excluir(jogador: JogadorEntity)
    @Query("SELECT * FROM jogadores WHERE id = :id LIMIT 1")
    suspend fun buscar(id: Int): JogadorEntity?
}
