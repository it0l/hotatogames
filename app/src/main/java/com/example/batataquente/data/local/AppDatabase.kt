package com.example.batataquente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [JogadorEntity::class, QuestaoEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jogadorDao(): JogadorDao
    abstract fun questaoDao(): QuestaoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun obter(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "batata_quente.db")
                .build().also { INSTANCE = it }
        }
    }
}
