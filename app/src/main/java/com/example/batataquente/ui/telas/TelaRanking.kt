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
fun TelaRanking(vm: JogadoresViewModel) {
    val s by vm.state.collectAsState()
    val ranking = vm.ranking
    Moldura("🏆 Ranking") {
        Text("Ranking local salvo no Room, atualizado por Flow.")
        if (ranking.isEmpty()) Text("Ainda não há partidas finalizadas.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ranking, key = { it.id }) { jogador ->
                val posicao = ranking.indexOfFirst { it.id == jogador.id } + 1
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("#$posicao ${jogador.nome}", style = MaterialTheme.typography.titleMedium)
                        Text("${jogador.pontos} pontos • ${jogador.vitorias} vitórias • ${jogador.partidas} partidas")
                    }
                }
            }
        }
    }
}
