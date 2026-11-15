package com.example.batataquente.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaralhoQuestoesTest {
    @Test
    fun naoRepetePerguntasNaMesmaPartida() {
        listOf("Fácil", "Médio", "Difícil").forEach { nivel ->
            val baralho = BaralhoQuestoes(emptyList(), nivel)
            val usadas = mutableSetOf<String>()
            repeat(300) {
                val questao = baralho.proxima()
                assertTrue("Pergunta repetida: ${questao.pergunta}", usadas.add(BaralhoQuestoes.chave(questao.pergunta)))
            }
            assertEquals(300, usadas.size)
        }
    }

    @Test
    fun normalizaComutatividade() {
        assertEquals(BaralhoQuestoes.chave("7 × 8"), BaralhoQuestoes.chave("8 × 7"))
        assertEquals(BaralhoQuestoes.chave("4 + 6"), BaralhoQuestoes.chave("6 + 4"))
    }

    @Test
    fun selecionaSomenteJogadoresVivos() {
        val jogadores = listOf(Participante(1, "Lucas", 1), Participante(2, "Rafael", 0), Participante(3, "Pedro", 1))
        assertEquals(2, RegrasJogo.proximoAtivo(jogadores, 0))
        assertEquals(0, RegrasJogo.proximoAtivo(jogadores, 2))
    }
}
