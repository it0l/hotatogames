package com.example.batataquente.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.batataquente.data.remote.JogadorOnline
import com.example.batataquente.data.remote.RecordeRemoto
import com.example.batataquente.data.repository.RankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RankingOnlineState(
    val recordes: List<RecordeRemoto> = emptyList(),
    val jogadores: List<JogadorOnline> = emptyList(),
    val carregando: Boolean = true,
    val erro: String? = null
)

class RankingViewModel(private val repository: RankingRepository) : ViewModel() {
    private val _state = MutableStateFlow(RankingOnlineState())
    val state: StateFlow<RankingOnlineState> = _state

    init {
        viewModelScope.launch {
            repository.observarRecordes()
                .catch { e -> _state.update { it.copy(carregando = false, erro = "Ranking online indisponível: ${e.message}") } }
                .collect { lista -> _state.update { it.copy(recordes = lista, carregando = false, erro = null) } }
        }
        viewModelScope.launch {
            repository.observarJogadores()
                .catch { e -> _state.update { it.copy(carregando = false, erro = "Ranking online indisponível: ${e.message}") } }
                .collect { lista -> _state.update { it.copy(jogadores = lista, carregando = false) } }
        }
    }
}

class RankingViewModelFactory(private val repository: RankingRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RankingViewModel(repository) as T
    }
}
