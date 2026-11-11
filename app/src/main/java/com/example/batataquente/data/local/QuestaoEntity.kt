package com.example.batataquente.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questoes")
data class QuestaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pergunta: String,
    val resposta: Int,
    val dificuldade: String = "Fácil",
    val origem: String = "LOCAL"
)
