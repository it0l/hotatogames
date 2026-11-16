package com.example.batataquente.logic

import com.example.batataquente.data.remote.FirebaseRankingDataSource
import org.junit.Assert.assertEquals
import org.junit.Test

class PontuacaoTest {
    @Test
    fun respostaRapidaDobraABase() {
        assertEquals(200, Pontuacao.acerto("Fácil", 1_000L, combo = 1))
        assertEquals(600, Pontuacao.acerto("Difícil", 3_000L, combo = 1))
    }

    @Test
    fun respostaLentaValeSoABase() {
        assertEquals(200, Pontuacao.acerto("Médio", 15_000L, combo = 1))
    }

    @Test
    fun bonusDeVelocidadeCaiAosPoucos() {
        assertEquals(50, Pontuacao.bonusVelocidade(100, 6_500L))
    }

    @Test
    fun comboMultiplicaOsPontos() {
        assertEquals(1, Pontuacao.multiplicador(2))
        assertEquals(2, Pontuacao.multiplicador(3))
        assertEquals(3, Pontuacao.multiplicador(7))
        assertEquals(600, Pontuacao.acerto("Fácil", 0L, combo = 5))
    }

    @Test
    fun vencedorGanhaBonusPorVida() {
        assertEquals(700, Pontuacao.bonusFinal(2))
    }

    @Test
    fun mesmoNomeGeraMesmaChave() {
        assertEquals("joao-pedro", FirebaseRankingDataSource.chave("  João Pedro "))
        assertEquals("joao-pedro", FirebaseRankingDataSource.chave("joao pedro"))
        assertEquals("jogador", FirebaseRankingDataSource.chave("!!!"))
    }
}
