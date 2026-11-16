package com.example.batataquente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.batataquente.data.local.AppDatabase
import com.example.batataquente.data.repository.JogadorRepository
import com.example.batataquente.data.repository.QuestaoRepository
import com.example.batataquente.data.repository.RankingRepository
import com.example.batataquente.navigation.AppNavigation
import com.example.batataquente.ui.admin.AdminViewModel
import com.example.batataquente.ui.admin.AdminViewModelFactory
import com.example.batataquente.ui.jogadores.JogadoresViewModel
import com.example.batataquente.ui.jogadores.JogadoresViewModelFactory
import com.example.batataquente.ui.jogo.JogoViewModel
import com.example.batataquente.ui.jogo.JogoViewModelFactory
import com.example.batataquente.ui.ranking.RankingViewModel
import com.example.batataquente.ui.ranking.RankingViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val db by lazy { AppDatabase.obter(applicationContext) }
    private val jogadoresRepository by lazy { JogadorRepository(db.jogadorDao()) }
    private val questoesRepository by lazy { QuestaoRepository(db.questaoDao()) }
    private val rankingRepository by lazy { RankingRepository() }

    private val jogadoresVm: JogadoresViewModel by viewModels { JogadoresViewModelFactory(jogadoresRepository) }
    private val jogoVm: JogoViewModel by viewModels { JogoViewModelFactory(questoesRepository, jogadoresRepository, rankingRepository) }
    private val adminVm: AdminViewModel by viewModels { AdminViewModelFactory(questoesRepository) }
    private val rankingVm: RankingViewModel by viewModels { RankingViewModelFactory(rankingRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                questoesRepository.iniciarQuestoes()
                questoesRepository.sincronizar()
                rankingRepository.entrar() // login anônimo para o ranking online
            }
        }
        setContent { AppNavigation(jogadoresVm, jogoVm, adminVm, rankingVm) }
    }
}
