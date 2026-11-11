package com.example.batataquente.logic

/** Regras independentes da interface, fáceis de estudar e testar. */
data class Participante(val id: Int, val nome: String, val vidas: Int, val acertos: Int = 0)

object RegrasJogo {
    fun proximoAtivo(jogadores: List<Participante>, atual: Int): Int {
        if (jogadores.none { it.vidas > 0 }) return -1
        for (passo in 1..jogadores.size) {
            val index = (atual + passo) % jogadores.size
            if (jogadores[index].vidas > 0) return index
        }
        return -1
    }

    fun vencedor(jogadores: List<Participante>): Participante? =
        jogadores.singleOrNull { it.vidas > 0 }
}
