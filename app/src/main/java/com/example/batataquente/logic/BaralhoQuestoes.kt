package com.example.batataquente.logic

import com.example.batataquente.data.local.QuestaoEntity
import kotlin.random.Random

/**
 * Em cada partida, cada pergunta só aparece uma vez, mesmo que esteja duplicada no Room.
 * Quando as questões cadastradas acabam, novas contas são geradas automaticamente.
 * As contas geradas são temporárias: não poluem o painel administrativo nem o Room.
 */
class BaralhoQuestoes(
    questoesSalvas: List<QuestaoEntity>,
    private val dificuldade: String,
    private val sorteio: Random = Random.Default
) {
    private val apresentadas = mutableSetOf<String>()
    private val pendentes = mutableListOf<QuestaoEntity>()
    private var idGerado = -1

    init {
        // Mistura as contas já cadastradas com outras geradas; não limita o jogo
        // às poucas perguntas de exemplo existentes no banco local.
        pendentes.addAll(questoesSalvas.filter { it.dificuldade == dificuldade })
        completarBaralho()
    }

    fun proxima(): QuestaoEntity {
        while (true) {
            if (pendentes.isEmpty()) completarBaralho()
            if (pendentes.isEmpty()) {
                error("Não foi possível gerar novas questões sem repetir nesta partida.")
            }
            val indice = sorteio.nextInt(pendentes.size)
            val questao = pendentes.removeAt(indice)
            if (apresentadas.add(chave(questao.pergunta))) return questao
        }
    }

    private fun completarBaralho() {
        val pendentesAgora = pendentes.map { chave(it.pergunta) }.toMutableSet()
        val bloquedas = apresentadas + pendentesAgora
        val novas = GeradorQuestoes.gerarLote(dificuldade, bloquedas, 100, sorteio)
        novas.forEach { conta ->
            pendentes += QuestaoEntity(
                id = idGerado--,
                pergunta = conta.pergunta,
                resposta = conta.resposta,
                dificuldade = dificuldade,
                origem = "GERADA"
            )
        }
    }

    companion object {
        /** Considera 7×8 e 8×7 a mesma conta, assim como 5+3 e 3+5. */
        fun chave(pergunta: String): String {
            val limpa = pergunta.trim().lowercase()
                .replace(Regex("\\s+"), "")
                .replace('x', '×')
                .replace('*', '×')
                .replace('−', '-')
                .replace('/', '÷')
            val match = Regex("^(\\d+)([+×÷-])(\\d+)$").matchEntire(limpa) ?: return limpa
            val (primeiro, operador, segundo) = match.destructured
            return if (operador == "+" || operador == "×") {
                val a = primeiro.toInt()
                val b = segundo.toInt()
                "${minOf(a, b)}$operador${maxOf(a, b)}"
            } else {
                "${primeiro.toInt()}$operador${segundo.toInt()}"
            }
        }
    }
}

/** Gera contas com respostas inteiras e dificuldade compatível com a partida. */
object GeradorQuestoes {
    data class Conta(val pergunta: String, val resposta: Int)

    fun gerarLote(
        dificuldade: String,
        indisponiveis: Set<String>,
        quantidade: Int,
        sorteio: Random = Random.Default
    ): List<Conta> {
        val chaves = indisponiveis.toMutableSet()
        val resultado = mutableListOf<Conta>()
        var tentativas = 0
        while (resultado.size < quantidade && tentativas < quantidade * 250) {
            tentativas++
            val conta = gerarUma(dificuldade, sorteio)
            if (chaves.add(BaralhoQuestoes.chave(conta.pergunta))) resultado += conta
        }
        return resultado
    }

    private fun gerarUma(dificuldade: String, sorteio: Random): Conta {
        val operacao = sorteio.nextInt(4)
        val (a, b) = when (dificuldade) {
            "Médio" -> when (operacao) {
                0 -> sorteio.nextInt(20, 201) to sorteio.nextInt(15, 151)
                1 -> sorteio.nextInt(65, 251) to sorteio.nextInt(15, 65)
                2 -> sorteio.nextInt(6, 26) to sorteio.nextInt(3, 17)
                else -> sorteio.nextInt(3, 17) to sorteio.nextInt(5, 31)
            }
            "Difícil" -> when (operacao) {
                0 -> sorteio.nextInt(180, 1001) to sorteio.nextInt(170, 901)
                1 -> sorteio.nextInt(500, 1601) to sorteio.nextInt(100, 500)
                2 -> sorteio.nextInt(13, 71) to sorteio.nextInt(7, 41)
                else -> sorteio.nextInt(7, 33) to sorteio.nextInt(13, 101)
            }
            else -> when (operacao) {
                0 -> sorteio.nextInt(1, 31) to sorteio.nextInt(1, 21)
                1 -> sorteio.nextInt(12, 51) to sorteio.nextInt(1, 12)
                2 -> sorteio.nextInt(2, 11) to sorteio.nextInt(2, 11)
                else -> sorteio.nextInt(2, 11) to sorteio.nextInt(2, 11)
            }
        }
        return when (operacao) {
            0 -> Conta("$a + $b", a + b)
            1 -> Conta("$a - $b", a - b)
            2 -> Conta("$a × $b", a * b)
            // Em divisão, a é o divisor e b é o quociente: resultado sempre exato.
            else -> Conta("${a * b} ÷ $a", b)
        }
    }
}
