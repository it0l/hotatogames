package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.batataquente.ui.jogadores.JogadoresViewModel
import com.example.batataquente.ui.ranking.RankingViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val abas = listOf("🕹️ Recordes", "🌎 Geral", "📱 Este aparelho")

@Composable
fun TelaRanking(vm: JogadoresViewModel, rankingVm: RankingViewModel) {
    val estado by vm.state.collectAsStateWithLifecycle()
    val online by rankingVm.state.collectAsStateWithLifecycle()
    val ranking = vm.ranking
    var aba by rememberSaveable { mutableIntStateOf(0) }

    MolduraLista("🏆 Ranking") {
        item(key = "abas") {
            TabRow(selectedTabIndex = aba) {
                abas.forEachIndexed { indice, nome ->
                    Tab(selected = aba == indice, onClick = { aba = indice }, text = { Text(nome) })
                }
            }
        }
        when (aba) {
            0 -> {
                item(key = "desc-recordes") { Text("Top 10 pontuações de todos os aparelhos, como num fliperama.") }
                avisosOnline(online.carregando, online.erro, online.recordes.isEmpty())
                itemsIndexed(online.recordes, key = { _, r -> r.id }) { indice, r ->
                    LinhaFliperama(indice, r.nome, r.pontos, r.dificuldade)
                }
            }
            1 -> {
                item(key = "desc-geral") { Text("Pontos somados de todas as partidas online. Mesmo nome = mesmo jogador.") }
                avisosOnline(online.carregando, online.erro, online.jogadores.isEmpty())
                itemsIndexed(online.jogadores, key = { _, j -> j.chave }) { indice, j ->
                    CartaoJogador(indice, j.nome, j.pontos, j.vitorias, j.partidas)
                }
            }
            else -> {
                item(key = "descricao") { Text("Ranking salvo no Room, atualizado automaticamente pelo Flow.") }
                if (estado.erro != null) item(key = "erro") { Text(estado.erro ?: "", color = MaterialTheme.colorScheme.error) }
                if (ranking.isEmpty()) item(key = "vazio") { Text("Ainda não há partidas finalizadas.") }
                itemsIndexed(ranking, key = { _, j -> j.id }) { indice, j ->
                    CartaoJogador(indice, j.nome, j.pontos, j.vitorias, j.partidas)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.avisosOnline(carregando: Boolean, erro: String?, vazio: Boolean) {
    when {
        erro != null -> item(key = "erro-online") { Text(erro, color = MaterialTheme.colorScheme.error) }
        carregando -> item(key = "carregando") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        vazio -> item(key = "vazio-online") { Text("Ninguém pontuou ainda. Seja o primeiro!") }
    }
}

@Composable
private fun LinhaFliperama(indice: Int, nome: String, pontos: Int, dificuldade: String) {
    val cor = when (indice) {
        0 -> Color(0xFFFFD54F)
        1 -> Color(0xFFE0E0E0)
        2 -> Color(0xFFFFAB91)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${indice + 1}º".padStart(3), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = cor)
        Text(nome.uppercase().take(12), fontFamily = FontFamily.Monospace, color = cor, modifier = Modifier.weight(1f))
        Text(dificuldade.take(3).uppercase(), fontFamily = FontFamily.Monospace, color = cor)
        Text(pontos.toString().padStart(7, '0'), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = cor)
    }
}

@Composable
private fun CartaoJogador(indice: Int, nome: String, pontos: Int, vitorias: Int, partidas: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("#${indice + 1} $nome", style = MaterialTheme.typography.titleMedium)
            Text("$pontos pontos • $vitorias vitórias • $partidas partidas")
        }
    }
}
