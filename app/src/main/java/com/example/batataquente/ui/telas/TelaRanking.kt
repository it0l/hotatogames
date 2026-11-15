package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.batataquente.ui.jogadores.JogadoresViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TelaRanking(vm: JogadoresViewModel) {
    val estado by vm.state.collectAsStateWithLifecycle()
    val ranking = vm.ranking
    MolduraLista("🏆 Ranking") {
        item(key = "descricao") { Text("Ranking salvo no Room, atualizado automaticamente pelo Flow.") }
        if (estado.erro != null) item(key = "erro") { Text(estado.erro ?: "", color = MaterialTheme.colorScheme.error) }
        if (ranking.isEmpty()) item(key = "vazio") { Text("Ainda não há partidas finalizadas.") }
        itemsIndexed(ranking, key = { _, j -> j.id }) { indice, jogador ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("#${indice + 1} ${jogador.nome}", style = MaterialTheme.typography.titleMedium)
                    Text("${jogador.pontos} pontos • ${jogador.vitorias} vitórias • ${jogador.partidas} partidas")
                }
            }
        }
    }
}
