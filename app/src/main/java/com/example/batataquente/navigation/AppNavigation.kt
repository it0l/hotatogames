package com.example.batataquente.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.batataquente.ui.jogadores.JogadoresViewModel
import com.example.batataquente.ui.jogo.JogoViewModel
import com.example.batataquente.ui.admin.AdminViewModel
import com.example.batataquente.ui.telas.*

@Composable
fun AppNavigation(jogadoresVm: JogadoresViewModel, jogoVm: JogoViewModel, adminVm: AdminViewModel) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rota = entrada?.destination?.route ?: "inicio"
    val bottomVisivel = rota in setOf("jogadores", "configuracoes", "ranking")

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            bottomBar = {
                if (bottomVisivel) NavigationBar {
                    listOf("jogadores" to "Jogadores", "configuracoes" to "Configurar", "ranking" to "Ranking").forEach { (destino, nome) ->
                        NavigationBarItem(
                            selected = rota == destino,
                            onClick = { if (rota != destino) nav.navigate(destino) { launchSingleTop = true } },
                            icon = { Text(if (destino == "ranking") "🏆" else if (destino == "configuracoes") "⚙" else "👥") },
                            label = { Text(nome) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(navController = nav, startDestination = "inicio", modifier = Modifier.padding(padding)) {
                composable("inicio") {
                    TelaInicial(
                        jogar = { nav.navigate("jogadores") },
                        administrar = { nav.navigate("login") }
                    )
                }
                composable("jogadores") { TelaJogadores(jogadoresVm, avancar = { nav.navigate("configuracoes") }) }
                composable("configuracoes") {
                    TelaConfiguracoes(jogadoresVm, jogoVm, iniciar = { nav.navigate("jogo") })
                }
                composable("jogo") {
                    TelaJogo(jogoVm, verResultado = { nav.navigate("resultado") })
                }
                composable("resultado") {
                    TelaResultado(jogoVm, verRanking = { nav.navigate("ranking") }, jogarNovamente = {
                        jogoVm.sair(); nav.navigate("jogadores")
                    })
                }
                composable("ranking") { TelaRanking(jogadoresVm) }
                composable("login") {
                    TelaLoginAdmin(adminVm, entrar = { nav.navigate("admin") }, voltar = { nav.popBackStack() })
                }
                composable("admin") {
                    TelaAdmin(adminVm, voltar = { adminVm.sair(); nav.navigate("inicio") })
                }
            }
        }
    }
}
