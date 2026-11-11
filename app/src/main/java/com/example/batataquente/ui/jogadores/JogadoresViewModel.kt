package com.example.batataquente.ui.jogadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.batataquente.data.local.JogadorEntity
import com.example.batataquente.data.repository.JogadorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JogadoresState(
    val jogadores: List<JogadorEntity> = emptyList(),
    val selecionados: Set<Int> = emptySet(),
    val erro: String? = null
)

class JogadoresViewModel(private val repository: JogadorRepository) : ViewModel() {
    private val _state = MutableStateFlow(JogadoresState())
    val state: StateFlow<JogadoresState> = _state

    init {
        viewModelScope.launch {
            repository.observar().collect { lista ->
                _state.update { it.copy(jogadores = lista, selecionados = it.selecionados.intersect(lista.map { j -> j.id }.toSet())) }
            }
        }
    }

    fun salvar(nome: String, original: JogadorEntity? = null) = viewModelScope.launch {
        val resultado = repository.salvar(original?.copy(nome = nome.trim()) ?: JogadorEntity(nome = nome.trim()))
        _state.update { it.copy(erro = resultado.exceptionOrNull()?.message) }
    }
    fun excluir(jogador: JogadorEntity) = viewModelScope.launch {
        val resultado = repository.excluir(jogador)
        _state.update { it.copy(erro = resultado.exceptionOrNull()?.message) }
    }
    fun selecionar(id: Int) {
        _state.update { s ->
            s.copy(selecionados = if (id in s.selecionados) s.selecionados - id else s.selecionados + id)
        }
    }
    val ranking: List<JogadorEntity>
        get() = _state.value.jogadores.filter { it.partidas > 0 }.sortedByDescending { it.pontos }

    fun limparErro() { _state.update { it.copy(erro = null) } }
}

class JogadoresViewModelFactory(private val repository: JogadorRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = JogadoresViewModel(repository) as T
}
