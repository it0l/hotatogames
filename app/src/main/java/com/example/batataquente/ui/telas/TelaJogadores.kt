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
fun TelaJogadores(vm: JogadoresViewModel, avancar: () -> Unit) {
    val s by vm.state.collectAsState()
    var nome by remember { mutableStateOf("") }
    var editando by remember { mutableStateOf<JogadorEntity?>(null) }
    Moldura("👥 Jogadores") {
        Text("Cadastre participantes e marque de 2 a 6 para começar.")
        OutlinedTextField(
            value = nome, onValueChange = { nome = it },
            label = { Text(if (editando == null) "Nome" else "Editar jogador") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Button(onClick = {
            if (nome.isNotBlank()) {
                vm.salvar(nome, editando)
                nome = ""; editando = null
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (editando == null) "Adicionar" else "Salvar edição") }
        s.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Selecionados: ${s.selecionados.size}/6", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(s.jogadores, key = { it.id }) { j ->
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
                        TextButton(onClick = { editando = j; nome = j.nome }) { Text("Editar") }
                        TextButton(onClick = { vm.excluir(j) }) { Text("Excluir") }
                    }
                }
            }
        }
        Button(
            onClick = avancar, enabled = s.selecionados.size in 2..6,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Configurar partida") }
    }
}
