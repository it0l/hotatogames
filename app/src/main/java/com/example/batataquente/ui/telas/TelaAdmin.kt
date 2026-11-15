package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.ui.admin.AdminViewModel

@Composable
fun TelaAdmin(vm: AdminViewModel, voltar: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var pergunta by rememberSaveable { mutableStateOf("") }
    var resposta by rememberSaveable { mutableStateOf("") }
    var dificuldade by rememberSaveable { mutableStateOf("Fácil") }
    var editandoId by rememberSaveable { mutableIntStateOf(0) }
    val original = s.questoes.firstOrNull { it.id == editandoId }

    if (!s.autenticado) {
        Moldura("Acesso restrito") { OutlinedButton(onClick = voltar) { Text("Voltar ao início") } }
        return
    }
    MolduraLista("🛠 Questões — Admin") {
        item(key = "controles") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (s.cloudConectado) "Modo Firebase + Room" else "Modo local (Room). Faça login no Firebase para publicar na nuvem.")
                OutlinedTextField(value = pergunta, onValueChange = { pergunta = it },
                    label = { Text("Pergunta (ex: 7 × 8)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = resposta, onValueChange = { resposta = it },
                    label = { Text("Resposta numérica") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Fácil", "Médio", "Difícil").forEach { d ->
                        FilterChip(selected = dificuldade == d, onClick = { dificuldade = d }, label = { Text(d) })
                    }
                }
                Button(
                    onClick = {
                        val numero = resposta.toIntOrNull()
                        if (numero != null && pergunta.isNotBlank()) {
                            vm.salvar(QuestaoEntity(id = original?.id ?: 0, pergunta = pergunta.trim(),
                                resposta = numero, dificuldade = dificuldade,
                                origem = original?.origem ?: "LOCAL", firebaseId = original?.firebaseId))
                            pergunta = ""; resposta = ""; dificuldade = "Fácil"; editandoId = 0
                        }
                    }, modifier = Modifier.fillMaxWidth(),
                    enabled = !s.carregando && pergunta.isNotBlank() && resposta.toIntOrNull() != null
                ) { Text(if (original == null) "ADICIONAR QUESTÃO" else "SALVAR EDIÇÃO") }
                if (original != null) TextButton(onClick = {
                    editandoId = 0; pergunta = ""; resposta = ""; dificuldade = "Fácil"
                }) { Text("Cancelar edição") }
                OutlinedButton(onClick = { vm.sincronizar() }, modifier = Modifier.fillMaxWidth(), enabled = !s.carregando) {
                    Text("SINCRONIZAR QUESTÕES DO FIREBASE")
                }
                if (s.carregando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                s.mensagem?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Text("Filtrar questões:")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Todas", "Fácil", "Médio", "Difícil").forEach { d ->
                        FilterChip(selected = s.filtro == d, onClick = { vm.filtrar(d) }, label = { Text(d) })
                    }
                }
            }
        }
        items(vm.questoesFiltradas, key = { "questao_${it.id}" }) { q ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("${q.pergunta} = ${q.resposta}", style = MaterialTheme.typography.titleMedium)
                    Text("${q.dificuldade} • ${if (q.firebaseId == null) "Room" else "Firebase"}", style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { editandoId = q.id; pergunta = q.pergunta; resposta = q.resposta.toString(); dificuldade = q.dificuldade }) { Text("Editar") }
                        TextButton(onClick = { vm.excluir(q) }, enabled = !s.carregando) { Text("Excluir") }
                    }
                }
            }
        }
        item(key = "sair") { OutlinedButton(onClick = voltar, modifier = Modifier.fillMaxWidth()) { Text("SAIR DO ADMIN") } }
    }
}
