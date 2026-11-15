package com.example.batataquente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [JogadorEntity::class, QuestaoEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jogadorDao(): JogadorDao
    abstract fun questaoDao(): QuestaoDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE questoes ADD COLUMN firebaseId TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_questoes_firebaseId ON questoes (firebaseId)")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun obter(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "batata_quente.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
        }
    }
}
