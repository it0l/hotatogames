package com.example.batataquente.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.data.repository.QuestaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tudo o que as telas de administração precisam desenhar, num único objeto imutável.
 * A tela nunca altera o estado diretamente: ela chama funções do ViewModel.
 */
data class AdminState(
    val autenticado: Boolean = false,
    val questoes: List<QuestaoEntity> = emptyList(),
    val filtro: String = "Todas",
    /** Questão aberta para edição (null = formulário de nova questão). */
    val editando: QuestaoEntity? = null,
    /** true enquanto uma operação com o Firebase está em andamento. */
    val carregando: Boolean = false,
    /** Mensagem de sucesso (ex.: "Questão salva."). */
    val mensagem: String? = null,
    /** Mensagem de erro, mostrada em vermelho na tela. */
    val erro: String? = null,
    /** Horário (em milissegundos) da última sincronização bem-sucedida. */
    val ultimaSincronizacao: Long? = null
) {
    /** Calculado a partir do próprio estado: sempre coerente com lista e filtro. */
    val questoesFiltradas: List<QuestaoEntity>
        get() = questoes
            .filter { filtro == "Todas" || it.dificuldade == filtro }
            .sortedBy { it.pergunta }
}

class AdminViewModel(private val repository: QuestaoRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    init {
        // O Room avisa sempre que a tabela muda (inclusive após sincronizar).
        viewModelScope.launch {
            repository.observar().collect { lista -> _state.update { it.copy(questoes = lista) } }
        }
    }

    // ---------- Login ----------

    /** [aoEntrar] só é chamado se o login deu certo: a tela não precisa conhecer a senha. */
    fun login(senha: String, aoEntrar: () -> Unit = {}) {
        if (senha == SENHA_DEMO) {
            _state.update { it.copy(autenticado = true, erro = null, mensagem = null) }
            aoEntrar()
            sincronizar() // ao entrar no painel, traz a versão mais recente da nuvem
        } else {
            _state.update { it.copy(erro = "Senha de demonstração incorreta.") }
        }
    }

    fun sair() {
        _state.update { it.copy(autenticado = false, editando = null, mensagem = null, erro = null) }
    }

    // ---------- Formulário e filtro ----------

    fun filtrar(filtro: String) = _state.update { it.copy(filtro = filtro) }

    fun iniciarEdicao(questao: QuestaoEntity) =
        _state.update { it.copy(editando = questao, mensagem = null, erro = null) }

    fun cancelarEdicao() = _state.update { it.copy(editando = null) }

    fun limparMensagem() = _state.update { it.copy(mensagem = null, erro = null) }

    // ---------- Operações com o Firebase ----------

    /**
     * Cria ou edita uma questão. [aoSalvar] só é chamado se deu certo:
     * assim a tela limpa o formulário apenas no sucesso, e o usuário
     * não perde o que digitou quando falta internet.
     */
    fun salvar(questao: QuestaoEntity, aoSalvar: () -> Unit = {}) {
        val original = _state.value.editando
        val paraSalvar = if (original != null) questao.copy(id = original.id) else questao.copy(id = 0)
        executar(sucesso = if (original != null) "Questão atualizada no Firebase." else "Questão cadastrada no Firebase.") {
            repository.salvar(paraSalvar, original).onSuccess {
                _state.update { it.copy(editando = null) }
                aoSalvar()
            }
        }
    }

    fun excluir(questao: QuestaoEntity) {
        executar(sucesso = "Questão excluída do Firebase.") {
            repository.excluir(questao).onSuccess {
                // Se a questão excluída estava aberta no formulário, fecha a edição.
                if (_state.value.editando?.id == questao.id) _state.update { it.copy(editando = null) }
            }
        }
    }

    fun sincronizar() {
        executar(sucesso = null) {
            repository.sincronizar().onSuccess { total ->
                _state.update {
                    it.copy(
                        mensagem = "$total questões sincronizadas com o Firebase.",
                        ultimaSincronizacao = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    /**
     * Padrão comum a todas as operações remotas:
     * 1. ignora cliques enquanto outra operação está rodando;
     * 2. liga o "carregando" e limpa mensagens antigas;
     * 3. executa e mostra sucesso ou erro;
     * 4. desliga o "carregando".
     */
    private fun executar(sucesso: String?, operacao: suspend () -> Result<*>) {
        if (_state.value.carregando) return
        _state.update { it.copy(carregando = true, mensagem = null, erro = null) }
        viewModelScope.launch {
            val resultado = operacao()
            _state.update {
                it.copy(
                    carregando = false,
                    erro = resultado.exceptionOrNull()?.message,
                    mensagem = if (resultado.isSuccess) sucesso ?: it.mensagem else null
                )
            }
        }
    }

    private companion object {
        const val SENHA_DEMO = "1234"
    }
}

/**
 * O Android é quem cria os ViewModels (para que sobrevivam à rotação da tela).
 * A Factory ensina a ele como criar um AdminViewModel que precisa de um repositório.
 */
class AdminViewModelFactory(private val repository: QuestaoRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            "AdminViewModelFactory só cria AdminViewModel, não ${modelClass.name}."
        }
        return AdminViewModel(repository) as T
    }
}
