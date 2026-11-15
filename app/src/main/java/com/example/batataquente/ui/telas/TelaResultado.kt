package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.batataquente.ui.jogo.JogoViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TelaResultado(vm: JogoViewModel, verRanking: () -> Unit, jogarNovamente: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    Moldura("🏆 Resultado") {
        Text("Vencedor: ${s.vencedor?.nome ?: "Partida não concluída"}", style = MaterialTheme.typography.headlineSmall)
        Text("Rodadas: ${s.rodada}")
        Text("Pontuação da partida:")
        s.jogadores.sortedByDescending { it.acertos }.forEach { j ->
            Text("${j.nome} • ${j.acertos} acertos • ${j.acertos * 10} pontos")
        }
        Button(onClick = verRanking, modifier = Modifier.fillMaxWidth()) { Text("VER RANKING") }
        OutlinedButton(onClick = jogarNovamente, modifier = Modifier.fillMaxWidth()) { Text("JOGAR NOVAMENTE") }
    }
}
