package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.batataquente.ui.jogadores.JogadoresViewModel
import com.example.batataquente.ui.jogo.JogoViewModel

@Composable
fun TelaConfiguracoes(jogadoresVm: JogadoresViewModel, jogoVm: JogoViewModel, iniciar: () -> Unit) {
    val s by jogadoresVm.state.collectAsStateWithLifecycle()
    var dificuldade by rememberSaveable { mutableStateOf("Fácil") }
    var tempo by rememberSaveable { mutableIntStateOf(30) }
    var vidas by rememberSaveable { mutableIntStateOf(2) }
    val participantes = s.jogadores.filter { it.id in s.selecionados }
    Moldura("⚙ Configurações") {
        Text("Participantes: ${participantes.joinToString { it.nome }}")
        Text("Dificuldade", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Fácil", "Médio", "Difícil").forEach { nivel ->
                FilterChip(selected = nivel == dificuldade, onClick = { dificuldade = nivel }, label = { Text(nivel) })
            }
        }
        Text("Tempo por rodada: ${tempo}s", fontWeight = FontWeight.Bold)
        Slider(value = tempo.toFloat(), onValueChange = { tempo = (it / 10f).toInt().coerceIn(2, 6) * 10 }, valueRange = 20f..60f, steps = 3)
        Text("Vidas por jogador: $vidas", fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { numero ->
                FilterChip(selected = numero == vidas, onClick = { vidas = numero }, label = { Text("$numero") })
            }
        }
        Text("A batata passa quando a resposta está correta. Quando o tempo termina, o jogador atual perde uma vida.")
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { jogoVm.iniciar(participantes, dificuldade, tempo, vidas); iniciar() },
            enabled = participantes.size in 2..6, modifier = Modifier.fillMaxWidth()
        ) { Text("INICIAR JOGO") }
    }
}
