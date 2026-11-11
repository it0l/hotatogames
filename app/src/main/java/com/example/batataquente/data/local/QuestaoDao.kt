package com.example.batataquente.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestaoDao {
    @Query("SELECT * FROM questoes ORDER BY id DESC")
    fun observarTodas(): Flow<List<QuestaoEntity>>

    @Query("SELECT COUNT(*) FROM questoes")
    suspend fun contar(): Int
    @Insert suspend fun inserir(questao: QuestaoEntity): Long
    @Update suspend fun atualizar(questao: QuestaoEntity)
    @Delete suspend fun excluir(questao: QuestaoEntity)

    @Query("SELECT * FROM questoes WHERE id = :id LIMIT 1")
    suspend fun buscar(id: Int): QuestaoEntity?

    @Query("DELETE FROM questoes")
    suspend fun excluirTodas()

    /**
     * Usado na sincronização com o Firebase: troca todas as questões locais pelas da nuvem.
     * @Transaction garante que as duas etapas acontecem juntas (ou nenhuma acontece).
     */
    @Transaction
    suspend fun substituirTodas(questoes: List<QuestaoEntity>) {
        excluirTodas()
        questoes.forEach { inserir(it) }
    }
}
