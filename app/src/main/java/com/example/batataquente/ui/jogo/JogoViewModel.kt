package com.example.batataquente.ui.jogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.batataquente.data.local.JogadorEntity
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.data.repository.JogadorRepository
import com.example.batataquente.data.repository.QuestaoRepository
import com.example.batataquente.logic.BaralhoQuestoes
import com.example.batataquente.logic.Participante
import com.example.batataquente.logic.RegrasJogo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JogoState(
    val jogadores: List<Participante> = emptyList(),
    val jogadorAtual: Int = 0,
    val questao: QuestaoEntity? = null,
    val tempoRestante: Int = 30,
    val duracao: Int = 30,
    val dificuldade: String = "Fácil",
    val rodada: Int = 1,
    val fase: String = "aguardando",
    val mensagem: String? = null,
    val vencedor: Participante? = null
)

class JogoViewModel(
    private val questaoRepository: QuestaoRepository,
    private val jogadorRepository: JogadorRepository
) : ViewModel() {
    private val _state = MutableStateFlow(JogoState())
    val state: StateFlow<JogoState> = _state

    private var relogio: Job? = null
    private var baralho: BaralhoQuestoes? = null
    private var prazoEmMillis = 0L

    fun iniciar(jogadores: List<JogadorEntity>, dificuldade: String, segundos: Int, vidas: Int) {
        if (jogadores.size !in 2..6) {
            _state.update { it.copy(mensagem = "Selecione de 2 a 6 jogadores.") }
            return
        }
        relogio?.cancel()
        _state.update { it.copy(fase = "aguardando", mensagem = "Preparando partida...") }
        viewModelScope.launch {
            try {
                val questoes = questaoRepository.observar().first()
                baralho = BaralhoQuestoes(questoes, dificuldade)
                prazoEmMillis = System.currentTimeMillis() + segundos * 1000L
                _state.value = JogoState(
                    jogadores = jogadores.map { Participante(it.id, it.nome, vidas) },
                    questao = proximaQuestao(),
                    tempoRestante = segundos,
                    duracao = segundos,
                    dificuldade = dificuldade,
                    fase = "jogando"
                )
                iniciarRelogio()
            } catch (e: Exception) {
                _state.update { it.copy(mensagem = "Não foi possível iniciar: ${e.message}") }
            }
        }
    }

    private fun proximaQuestao(): QuestaoEntity {
        val cartas = baralho ?: error("As questões ainda não foram carregadas.")
        return cartas.proxima()
    }

    fun responder(resposta: String): Boolean {
        val atual = _state.value
        if (atual.fase != "jogando" || atual.questao == null || baralho == null) return false
        if (System.currentTimeMillis() >= prazoEmMillis) {
            explodir()
            return false
        }
        val numero = resposta.trim().toIntOrNull()
        if (numero == null) {
            _state.update { it.copy(mensagem = "Digite um número.") }
            return false
        }
        if (numero != atual.questao.resposta) {
            _state.update { it.copy(mensagem = "Resposta incorreta! Continue com a batata.") }
            return false
        }

        val jogadores = atual.jogadores.toMutableList()
        val posicao = atual.jogadorAtual
        jogadores[posicao] = jogadores[posicao].copy(acertos = jogadores[posicao].acertos + 1)
        val proxima = proximaQuestao()
        _state.update {
            it.copy(
                jogadores = jogadores,
                jogadorAtual = RegrasJogo.proximoAtivo(jogadores, posicao),
                questao = proxima,
                mensagem = "Correto! Passe a batata para o próximo jogador."
            )
        }
        return true
    }

    private fun iniciarRelogio() {
        relogio?.cancel()
        relogio = viewModelScope.launch {
            while (_state.value.fase == "jogando") {
                val faltam = ((prazoEmMillis - System.currentTimeMillis() + 999L) / 1000L)
                    .coerceAtLeast(0L).toInt()
                _state.update { it.copy(tempoRestante = faltam) }
                if (faltam == 0) {
                    explodir()
                    break
                }
                delay(250L)
            }
        }
    }

    private fun explodir() {
        val atual = _state.value
        if (atual.fase != "jogando") return
        val jogadores = atual.jogadores.toMutableList()
        val punido = jogadores[atual.jogadorAtual]
        jogadores[atual.jogadorAtual] = punido.copy(vidas = (punido.vidas - 1).coerceAtLeast(0))
        val vencedor = RegrasJogo.vencedor(jogadores)
        _state.update {
            it.copy(
                jogadores = jogadores,
                tempoRestante = 0,
                fase = if (vencedor == null) "explosao" else "fim",
                vencedor = vencedor,
                mensagem = if (vencedor == null) "A batata explodiu com ${punido.nome}!" else "${vencedor.nome} venceu!"
            )
        }
        if (vencedor != null) {
            viewModelScope.launch {
                val pontos = jogadores.associate { it.id to it.acertos * 10 }
                val resultado = jogadorRepository.registrarResultado(jogadores.map { it.id }, vencedor.id, pontos)
                if (resultado.isFailure) {
                    _state.update { it.copy(mensagem = "Erro ao salvar ranking: ${resultado.exceptionOrNull()?.message}") }
                }
            }
        }
    }

    fun proximaRodada() {
        val atual = _state.value
        if (atual.fase != "explosao" || baralho == null) return
        prazoEmMillis = System.currentTimeMillis() + atual.duracao * 1000L
        val proxima = proximaQuestao()
        _state.update {
            it.copy(
                fase = "jogando",
                rodada = it.rodada + 1,
                tempoRestante = it.duracao,
                jogadorAtual = RegrasJogo.proximoAtivo(it.jogadores, it.jogadorAtual),
                questao = proxima,
                mensagem = null
            )
        }
        iniciarRelogio()
    }

    fun sair() {
        relogio?.cancel()
        baralho = null
        prazoEmMillis = 0L
        _state.value = JogoState()
    }
}

class JogoViewModelFactory(
    private val questaoRepository: QuestaoRepository,
    private val jogadorRepository: JogadorRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return JogoViewModel(questaoRepository, jogadorRepository) as T
    }
}
