package com.example.batataquente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.batataquente.data.local.AppDatabase
import com.example.batataquente.data.repository.JogadorRepository
import com.example.batataquente.data.repository.QuestaoRepository
import com.example.batataquente.navigation.AppNavigation
import com.example.batataquente.ui.admin.AdminViewModel
import com.example.batataquente.ui.admin.AdminViewModelFactory
import com.example.batataquente.ui.jogadores.JogadoresViewModel
import com.example.batataquente.ui.jogadores.JogadoresViewModelFactory
import com.example.batataquente.ui.jogo.JogoViewModel
import com.example.batataquente.ui.jogo.JogoViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val db by lazy { AppDatabase.obter(applicationContext) }
    private val jogadoresRepository by lazy { JogadorRepository(db.jogadorDao()) }
    private val questoesRepository by lazy { QuestaoRepository(db.questaoDao()) }

    private val jogadoresVm: JogadoresViewModel by viewModels { JogadoresViewModelFactory(jogadoresRepository) }
    private val jogoVm: JogoViewModel by viewModels { JogoViewModelFactory(questoesRepository, jogadoresRepository) }
    private val adminVm: AdminViewModel by viewModels { AdminViewModelFactory(questoesRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { questoesRepository.iniciarQuestoes() }
        setContent { AppNavigation(jogadoresVm, jogoVm, adminVm) }
    }
}
