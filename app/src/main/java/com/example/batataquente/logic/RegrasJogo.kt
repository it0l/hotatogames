package com.example.batataquente.logic

/** Regras independentes da interface, fáceis de estudar e testar. */
data class Participante(
    val id: Int,
    val nome: String,
    val vidas: Int,
    val acertos: Int = 0,
    val pontos: Int = 0,
    /** Acertos seguidos sem errar nem deixar a batata explodir. */
    val combo: Int = 0
)

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

/**
 * Pontuação estilo fliperama:
 * - cada acerto vale uma base que depende da dificuldade;
 * - responder rápido dá bônus (até dobrar a base);
 * - acertos seguidos multiplicam os pontos (combo x2 e x3);
 * - quem vence ganha um bônus, mais um extra por vida que sobrou.
 */
object Pontuacao {
    const val BONUS_VITORIA = 500
    const val BONUS_POR_VIDA = 100
    private const val RESPOSTA_RAPIDA_MS = 3_000L
    private const val RESPOSTA_LENTA_MS = 10_000L

    fun base(dificuldade: String): Int = when (dificuldade) {
        "Difícil" -> 300
        "Médio" -> 200
        else -> 100
    }

    /** Até 3 s ganha a base inteira de bônus; depois cai até zero em 10 s. */
    fun bonusVelocidade(base: Int, tempoRespostaMs: Long): Int {
        val tempo = tempoRespostaMs.coerceAtLeast(0L)
        if (tempo <= RESPOSTA_RAPIDA_MS) return base
        if (tempo >= RESPOSTA_LENTA_MS) return 0
        val restante = RESPOSTA_LENTA_MS - tempo
        return (base * restante / (RESPOSTA_LENTA_MS - RESPOSTA_RAPIDA_MS)).toInt()
    }

    /** [combo] já conta o acerto atual: 1–2 → x1, 3–4 → x2, 5 ou mais → x3. */
    fun multiplicador(combo: Int): Int = when {
        combo >= 5 -> 3
        combo >= 3 -> 2
        else -> 1
    }

    fun acerto(dificuldade: String, tempoRespostaMs: Long, combo: Int): Int {
        val base = base(dificuldade)
        return (base + bonusVelocidade(base, tempoRespostaMs)) * multiplicador(combo)
    }

    fun bonusFinal(vidasRestantes: Int): Int = BONUS_VITORIA + BONUS_POR_VIDA * vidasRestantes.coerceAtLeast(0)
}
