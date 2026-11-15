package com.example.batataquente.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "questoes", indices = [Index(value = ["firebaseId"], unique = true)])
data class QuestaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pergunta: String,
    val resposta: Int,
    val dificuldade: String = "Fácil",
    val origem: String = "LOCAL",
    val firebaseId: String? = null
)
