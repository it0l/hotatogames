package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.batataquente.ui.jogadores.JogadoresViewModel

@Composable
fun TelaJogadores(vm: JogadoresViewModel, avancar: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var nome by rememberSaveable { mutableStateOf("") }
    var editandoId by rememberSaveable { mutableIntStateOf(0) }
    val original = s.jogadores.firstOrNull { it.id == editandoId }
    MolduraLista("👥 Jogadores") {
        item(key = "formulario") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cadastre participantes e selecione de 2 a 6.")
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text(if (original == null) "Nome" else "Editar jogador") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Button(
                    onClick = { vm.salvar(nome, original); nome = ""; editandoId = 0 },
                    enabled = nome.isNotBlank(), modifier = Modifier.fillMaxWidth()
                ) { Text(if (original == null) "Adicionar" else "Salvar edição") }
                if (original != null) TextButton(onClick = { nome = ""; editandoId = 0 }) { Text("Cancelar edição") }
                s.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Selecionados: ${s.selecionados.size}/6", fontWeight = FontWeight.Bold)
            }
        }
        items(s.jogadores, key = { "jogador_${it.id}" }) { j ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = j.id in s.selecionados,
                        onCheckedChange = { vm.selecionar(j.id) },
                        enabled = j.id in s.selecionados || s.selecionados.size < 6
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(j.nome, fontWeight = FontWeight.SemiBold)
                        Text("${j.vitorias} vitórias • ${j.pontos} pontos", style = MaterialTheme.typography.bodySmall)
                    }
                    Column {
                        TextButton(onClick = { editandoId = j.id; nome = j.nome }) { Text("Editar") }
                        TextButton(onClick = { vm.excluir(j); if (editandoId == j.id) { editandoId = 0; nome = "" } }) { Text("Excluir") }
                    }
                }
            }
        }
        item(key = "avancar") {
            Button(
                onClick = avancar, enabled = s.selecionados.size in 2..6,
                modifier = Modifier.fillMaxWidth()
            ) { Text("CONFIGURAR PARTIDA") }
        }
    }
}
