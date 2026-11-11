package com.example.batataquente.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jogadores")
data class JogadorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val pontos: Int = 0,
    val vitorias: Int = 0,
    val partidas: Int = 0
)
