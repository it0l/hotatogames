package com.example.batataquente.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestaoDao {
    @Query("SELECT * FROM questoes ORDER BY id DESC")
    fun observarTodas(): Flow<List<QuestaoEntity>>

    @Query("SELECT COUNT(*) FROM questoes")
    suspend fun contar(): Int

    @Query("SELECT * FROM questoes WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun buscarFirebase(firebaseId: String): QuestaoEntity?

    @Query("SELECT * FROM questoes WHERE firebaseId IS NOT NULL")
    suspend fun listarFirebase(): List<QuestaoEntity>

    @Insert suspend fun inserir(questao: QuestaoEntity): Long
    @Update suspend fun atualizar(questao: QuestaoEntity)
    @Delete suspend fun excluir(questao: QuestaoEntity)
}
