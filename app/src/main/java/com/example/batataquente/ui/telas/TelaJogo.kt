package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.batataquente.ui.jogo.JogoViewModel

@Composable
fun TelaJogo(vm: JogoViewModel, verResultado: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var resposta by rememberSaveable(s.questao?.pergunta, s.jogadorAtual, s.rodada) {
        mutableStateOf("")
    }
    val atual = s.jogadores.getOrNull(s.jogadorAtual)
    Moldura("🥔 Batata Quente") {
        when (s.fase) {
            "aguardando" -> { Text(s.mensagem ?: "Preparando partida...") }
            "jogando" -> {
                Text("Rodada ${s.rodada}", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(
                    progress = { s.tempoRestante.toFloat() / s.duracao.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${s.tempoRestante} segundos", style = MaterialTheme.typography.headlineLarge)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔥 🥔 🔥", style = MaterialTheme.typography.displayMedium)
                        Text("Vez de ${atual?.nome ?: "..."}", style = MaterialTheme.typography.titleLarge)
                        Text("${s.questao?.pergunta ?: "..."} = ?", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                OutlinedTextField(
                    value = resposta, onValueChange = { resposta = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Resposta") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Button(
                    onClick = { if (vm.responder(resposta)) resposta = "" },
                    enabled = resposta.isNotBlank(), modifier = Modifier.fillMaxWidth()
                ) { Text("RESPONDER E PASSAR") }
                s.mensagem?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Text("Vidas", fontWeight = FontWeight.Bold)
                s.jogadores.forEach { j ->
                    Text("${j.nome}: ${"❤️".repeat(j.vidas)} (${j.acertos} acertos)")
                }
            }
            "explosao" -> {
                Text("💥", style = MaterialTheme.typography.displayLarge)
                Text(s.mensagem ?: "A batata explodiu!", style = MaterialTheme.typography.headlineSmall)
                s.jogadores.forEach { j -> Text("${j.nome}: ${j.vidas} vidas") }
                Button(onClick = vm::proximaRodada, modifier = Modifier.fillMaxWidth()) { Text("PRÓXIMA RODADA") }
            }
            "fim" -> {
                Text("🏆 ${s.vencedor?.nome} venceu!", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = verResultado, modifier = Modifier.fillMaxWidth()) { Text("VER RESULTADO") }
            }
        }
    }
}
