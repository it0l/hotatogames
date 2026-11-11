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

/** Um celular é passado entre os jogadores. A rodada não para ao responder. */
data class JogoState(
    val jogadores: List<Participante> = emptyList(),
    val jogadorAtual: Int = 0,
    val questao: QuestaoEntity? = null,
    val tempoRestante: Int = 30,
    val duracao: Int = 30,
    val vidasIniciais: Int = 2,
    val dificuldade: String = "Fácil",
    val rodada: Int = 1,
    val fase: String = "aguardando", // jogando, explosao, fim
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

    fun iniciar(jogadores: List<JogadorEntity>, dificuldade: String, segundos: Int, vidas: Int) {
        if (jogadores.size !in 2..6) {
            _state.update { it.copy(mensagem = "Selecione de 2 a 6 jogadores.") }
            return
        }
        relogio?.cancel()
        viewModelScope.launch {
            try {
                val todas = questaoRepository.observar().first()
                // Baralho misto: questões do Room + novas contas geradas automaticamente.
                // A mesma pergunta não se repete durante toda esta partida.
                baralho = BaralhoQuestoes(todas, dificuldade)
                _state.value = JogoState(
                    jogadores = jogadores.map { Participante(it.id, it.nome, vidas) },
                    questao = proximaQuestao(), tempoRestante = segundos, duracao = segundos,
                    vidasIniciais = vidas, dificuldade = dificuldade, fase = "jogando"
                )
                iniciarRelogio()
            } catch (e: Exception) {
                _state.update { it.copy(mensagem = e.message ?: "Não foi possível iniciar a partida.") }
            }
        }
    }

    private fun proximaQuestao(): QuestaoEntity =
        checkNotNull(baralho) { "A partida ainda não foi iniciada." }.proxima()

    fun responder(resposta: String) {
        val atual = _state.value
        if (atual.fase != "jogando" || atual.questao == null) return
        val numero = resposta.trim().toIntOrNull()
        if (numero == null) {
            _state.update { it.copy(mensagem = "Digite um número.") }
            return
        }
        if (numero != atual.questao.resposta) {
            _state.update { it.copy(mensagem = "Resposta errada! A batata continua com você.") }
            return
        }
        val novos = atual.jogadores.toMutableList()
        novos[atual.jogadorAtual] = novos[atual.jogadorAtual].copy(acertos = novos[atual.jogadorAtual].acertos + 1)
        _state.update { it.copy(
            jogadores = novos, jogadorAtual = RegrasJogo.proximoAtivo(novos, atual.jogadorAtual),
            questao = proximaQuestao(), mensagem = "Acertou! Passe o celular."
        ) }
    }

    private fun iniciarRelogio() {
        relogio?.cancel()
        relogio = viewModelScope.launch {
            while (_state.value.fase == "jogando" && _state.value.tempoRestante > 0) {
                delay(1000)
                if (_state.value.fase == "jogando") {
                    _state.update { it.copy(tempoRestante = (it.tempoRestante - 1).coerceAtLeast(0)) }
                }
            }
            if (_state.value.fase == "jogando" && _state.value.tempoRestante == 0) explodir()
        }
    }

    private fun explodir() {
        val antes = _state.value
        val novos = antes.jogadores.toMutableList()
        val punido = novos[antes.jogadorAtual]
        novos[antes.jogadorAtual] = punido.copy(vidas = (punido.vidas - 1).coerceAtLeast(0))
        val vencedor = RegrasJogo.vencedor(novos)
        _state.update { it.copy(
            jogadores = novos, fase = if (vencedor == null) "explosao" else "fim",
            vencedor = vencedor,
            mensagem = if (vencedor == null) "A batata explodiu com ${punido.nome}!" else "${vencedor.nome} venceu!"
        ) }
        if (vencedor != null) {
            viewModelScope.launch {
                val pontos = novos.associate { it.id to it.acertos * 10 }
                val resultado = jogadorRepository.registrarResultado(novos.map { it.id }, vencedor.id, pontos)
                if (resultado.isFailure) _state.update { it.copy(mensagem = "Vitória! Falha ao salvar ranking: ${resultado.exceptionOrNull()?.message}") }
            }
        }
    }

    fun proximaRodada() {
        val s = _state.value
        if (s.fase != "explosao") return
        val proximo = RegrasJogo.proximoAtivo(s.jogadores, s.jogadorAtual)
        _state.update { it.copy(
            fase = "jogando", rodada = it.rodada + 1, tempoRestante = it.duracao,
            jogadorAtual = proximo, questao = proximaQuestao(), mensagem = null
        ) }
        iniciarRelogio()
    }

    fun sair() {
        relogio?.cancel()
        baralho = null
        _state.value = JogoState()
    }
}

class JogoViewModelFactory(
    private val questaoRepository: QuestaoRepository,
    private val jogadorRepository: JogadorRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = JogoViewModel(questaoRepository, jogadorRepository) as T
}
