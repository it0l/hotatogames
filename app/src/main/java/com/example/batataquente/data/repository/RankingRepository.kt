package com.example.batataquente.data.repository

import com.example.batataquente.data.remote.FirebaseRankingDataSource
import com.example.batataquente.data.remote.JogadorOnline
import com.example.batataquente.data.remote.PartidaRemota
import com.example.batataquente.data.remote.RecordeRemoto
import android.util.Log
import kotlinx.coroutines.flow.Flow

class RankingRepository(
    private val remoto: FirebaseRankingDataSource = FirebaseRankingDataSource()
) {
    fun observarRecordes(): Flow<List<RecordeRemoto>> = remoto.observarRecordes()
    fun observarJogadores(): Flow<List<JogadorOnline>> = remoto.observarJogadores()

    suspend fun entrar(): Result<Unit> = try {
        remoto.garantirLogin()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.w(TAG, "Login anônimo falhou", e)
        Result.failure(e)
    }

    /** Sucesso com true = confirmado no servidor; false = na fila offline. */
    suspend fun registrarPartida(partida: PartidaRemota): Result<Boolean> = try {
        Result.success(remoto.registrarPartida(partida))
    } catch (e: Exception) {
        Log.w(TAG, "Envio da partida falhou", e)
        Result.failure(e)
    }

    private companion object {
        const val TAG = "RankingOnline"
    }
}
