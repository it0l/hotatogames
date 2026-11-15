package com.example.batataquente.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.data.repository.QuestaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminState(
    val autenticado: Boolean = false,
    val cloudConectado: Boolean = false,
    val carregando: Boolean = false,
    val questoes: List<QuestaoEntity> = emptyList(),
    val filtro: String = "Todas",
    val mensagem: String? = null
)

class AdminViewModel(private val repository: QuestaoRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state

    init {
        viewModelScope.launch {
            try {
                repository.observar().collect { lista -> _state.update { it.copy(questoes = lista) } }
            } catch (e: Exception) {
                _state.update { it.copy(mensagem = "Não foi possível ler Room: ${e.message}") }
            }
        }
    }

    /** A senha de demonstração acessa SOMENTE o banco local, nunca libera escrita no Firestore. */
    fun loginLocal(senha: String): Boolean {
        val certo = senha == "1234"
        _state.update { it.copy(
            autenticado = certo, cloudConectado = false,
            mensagem = if (certo) "Modo local. Alterações não são enviadas para a nuvem." else "Senha local incorreta."
        ) }
        return certo
    }

    fun loginFirebase(email: String, senha: String, sucesso: () -> Unit) {
        _state.update { it.copy(carregando = true, mensagem = null) }
        viewModelScope.launch {
            val resultado = repository.autenticar(email, senha)
            _state.update { it.copy(
                carregando = false, autenticado = resultado.isSuccess, cloudConectado = resultado.isSuccess,
                mensagem = resultado.exceptionOrNull()?.localizedMessage ?: "Conectado ao Firebase."
            ) }
            if (resultado.isSuccess) sucesso()
        }
    }

    fun sair() {
        if (_state.value.cloudConectado) repository.desconectarCloud()
        _state.update { it.copy(autenticado = false, cloudConectado = false, mensagem = null) }
    }

    fun filtrar(filtro: String) { _state.update { it.copy(filtro = filtro) } }

    fun salvar(questao: QuestaoEntity) = viewModelScope.launch {
        _state.update { it.copy(carregando = true) }
        val resultado = repository.salvar(questao, publicar = _state.value.cloudConectado)
        _state.update { it.copy(carregando = false,
            mensagem = resultado.exceptionOrNull()?.localizedMessage
                ?: if (it.cloudConectado) "Questão salva no Firebase e Room." else "Questão salva no Room (local).") }
    }

    fun excluir(questao: QuestaoEntity) = viewModelScope.launch {
        _state.update { it.copy(carregando = true) }
        val resultado = repository.excluir(questao, publicar = _state.value.cloudConectado)
        _state.update { it.copy(carregando = false,
            mensagem = resultado.exceptionOrNull()?.localizedMessage ?: "Questão excluída.") }
    }

    fun sincronizar() = viewModelScope.launch {
        _state.update { it.copy(carregando = true, mensagem = "Buscando questões do Firebase...") }
        val resultado = repository.sincronizar()
        _state.update { it.copy(carregando = false,
            mensagem = resultado.fold(
                onSuccess = { n -> "$n questões sincronizadas do Firebase para Room." },
                onFailure = { erro -> "Falha de sincronização: ${erro.localizedMessage}. O jogo continua offline." }
            )) }
    }

    val questoesFiltradas: List<QuestaoEntity>
        get() = _state.value.questoes
            .filter { _state.value.filtro == "Todas" || it.dificuldade == _state.value.filtro }
            .sortedBy { it.pergunta }
}

class AdminViewModelFactory(private val repository: QuestaoRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminViewModel(repository) as T
}
