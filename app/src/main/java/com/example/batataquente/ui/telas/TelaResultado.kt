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
fun TelaResultado(vm: JogoViewModel, verRanking: () -> Unit, jogarNovamente: () -> Unit) {
    val s by vm.state.collectAsState()
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
