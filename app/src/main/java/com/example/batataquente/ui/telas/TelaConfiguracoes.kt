package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.batataquente.data.local.JogadorEntity
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.ui.jogadores.JogadoresViewModel
import com.example.batataquente.ui.jogo.JogoViewModel
import com.example.batataquente.ui.admin.AdminViewModel

@Composable
fun TelaConfiguracoes(jogadoresVm: JogadoresViewModel, jogoVm: JogoViewModel, iniciar: () -> Unit) {
    val s by jogadoresVm.state.collectAsState()
    var dificuldade by remember { mutableStateOf("Fácil") }
    var tempo by remember { mutableIntStateOf(30) }
    var vidas by remember { mutableIntStateOf(2) }
    val participantes = s.jogadores.filter { it.id in s.selecionados }
    Moldura("⚙ Configurações") {
        Text("Participantes: ${participantes.joinToString { it.nome }}")
        Text("Dificuldade", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Fácil", "Médio", "Difícil").forEach { nivel ->
                FilterChip(selected = nivel == dificuldade, onClick = { dificuldade = nivel }, label = { Text(nivel) })
            }
        }
        Text("Tempo por rodada: ${tempo}s", fontWeight = FontWeight.Bold)
        Slider(value = tempo.toFloat(), onValueChange = { tempo = (it / 10f).toInt() * 10 }, valueRange = 20f..60f, steps = 3)
        Text("Vidas por jogador: $vidas", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { numero ->
                FilterChip(selected = numero == vidas, onClick = { vidas = numero }, label = { Text("$numero") })
            }
        }
        Text("A batata passa ao acertar. Quem ficar com ela quando o relógio zerar perde uma vida.")
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { jogoVm.iniciar(participantes, dificuldade, tempo, vidas); iniciar() },
            enabled = participantes.size in 2..6,
            modifier = Modifier.fillMaxWidth()
        ) { Text("INICIAR JOGO") }
    }
}
