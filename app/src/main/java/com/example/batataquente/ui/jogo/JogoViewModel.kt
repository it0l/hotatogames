package com.example.batataquente.ui.jogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.batataquente.data.local.JogadorEntity
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.data.repository.JogadorRepository
import com.example.batataquente.data.repository.QuestaoRepository
import com.example.batataquente.data.repository.RankingRepository
import com.example.batataquente.data.remote.PartidaRemota
import com.example.batataquente.data.remote.ResultadoJogador
import com.example.batataquente.logic.BaralhoQuestoes
import com.example.batataquente.logic.Participante
import com.example.batataquente.logic.Pontuacao
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
    val vencedor: Participante? = null,
    /** Situação do envio da partida para o ranking online (Firebase). */
    val envioOnline: String? = null
)

class JogoViewModel(
    private val questaoRepository: QuestaoRepository,
    private val jogadorRepository: JogadorRepository,
    private val rankingRepository: RankingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(JogoState())
    val state: StateFlow<JogoState> = _state

    private var relogio: Job? = null
    private var baralho: BaralhoQuestoes? = null
    private var prazoEmMillis = 0L
    private var questaoMostradaEm = 0L

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
                questaoMostradaEm = System.currentTimeMillis()
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
        val jogadores = atual.jogadores.toMutableList()
        val posicao = atual.jogadorAtual
        if (numero != atual.questao.resposta) {
            jogadores[posicao] = jogadores[posicao].copy(combo = 0)
            _state.update { it.copy(jogadores = jogadores, mensagem = "Resposta incorreta! Combo perdido, continue com a batata.") }
            return false
        }

        val agora = System.currentTimeMillis()
        val jogador = jogadores[posicao]
        val combo = jogador.combo + 1
        val ganhou = Pontuacao.acerto(atual.dificuldade, agora - questaoMostradaEm, combo)
        jogadores[posicao] = jogador.copy(acertos = jogador.acertos + 1, pontos = jogador.pontos + ganhou, combo = combo)
        val proxima = proximaQuestao()
        questaoMostradaEm = agora
        val multiplicador = Pontuacao.multiplicador(combo)
        val textoCombo = if (multiplicador > 1) " COMBO x$multiplicador!" else ""
        _state.update {
            it.copy(
                jogadores = jogadores,
                jogadorAtual = RegrasJogo.proximoAtivo(jogadores, posicao),
                questao = proxima,
                mensagem = "Correto! +$ganhou pontos.$textoCombo Passe a batata."
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
        jogadores[atual.jogadorAtual] = punido.copy(vidas = (punido.vidas - 1).coerceAtLeast(0), combo = 0)
        val vencedor = RegrasJogo.vencedor(jogadores)?.let { sobrevivente ->
            val premiado = sobrevivente.copy(pontos = sobrevivente.pontos + Pontuacao.bonusFinal(sobrevivente.vidas))
            jogadores[jogadores.indexOf(sobrevivente)] = premiado
            premiado
        }
        _state.update {
            it.copy(
                jogadores = jogadores,
                tempoRestante = 0,
                fase = if (vencedor == null) "explosao" else "fim",
                vencedor = vencedor,
                envioOnline = if (vencedor != null) "Enviando para o ranking online..." else null,
                mensagem = if (vencedor == null) "A batata explodiu com ${punido.nome}!" else "${vencedor.nome} venceu!"
            )
        }
        if (vencedor != null) salvarPartida(jogadores, vencedor, atual)
    }

    private fun salvarPartida(jogadores: List<Participante>, vencedor: Participante, atual: JogoState) {
        viewModelScope.launch {
            val pontos = jogadores.associate { it.id to it.pontos }
            val resultado = jogadorRepository.registrarResultado(jogadores.map { it.id }, vencedor.id, pontos)
            if (resultado.isFailure) {
                _state.update { it.copy(mensagem = "Erro ao salvar ranking: ${resultado.exceptionOrNull()?.message}") }
            }
        }
        viewModelScope.launch {
            val partida = PartidaRemota(
                dificuldade = atual.dificuldade,
                duracao = atual.duracao,
                rodadas = atual.rodada,
                jogadores = jogadores.map {
                    ResultadoJogador(it.nome, it.pontos, it.acertos, it.vidas, venceu = it.id == vencedor.id)
                }
            )
            val envio = rankingRepository.registrarPartida(partida)
            val texto = envio.fold(
                onSuccess = { confirmado ->
                    if (confirmado) "Partida enviada para o ranking online! 🕹️"
                    else "Sem internet: a partida será enviada quando a conexão voltar."
                },
                onFailure = { "Não foi possível enviar ao ranking online: ${it.message}" }
            )
            _state.update { it.copy(envioOnline = texto) }
        }
    }

    fun proximaRodada() {
        val atual = _state.value
        if (atual.fase != "explosao" || baralho == null) return
        prazoEmMillis = System.currentTimeMillis() + atual.duracao * 1000L
        questaoMostradaEm = System.currentTimeMillis()
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
    private val jogadorRepository: JogadorRepository,
    private val rankingRepository: RankingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return JogoViewModel(questaoRepository, jogadorRepository, rankingRepository) as T
    }
}
