package com.example.batataquente.ui.telas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.batataquente.data.local.QuestaoEntity
import com.example.batataquente.data.repository.QuestaoRepository
import com.example.batataquente.ui.admin.AdminState
import com.example.batataquente.ui.admin.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelaAdmin(vm: AdminViewModel, voltar: () -> Unit) {
    val s by vm.state.collectAsState()

    // Campos do formulário: estado da TELA (texto sendo digitado).
    // rememberSaveable mantém o que foi digitado se o celular girar.
    var pergunta by rememberSaveable { mutableStateOf("") }
    var resposta by rememberSaveable { mutableStateOf("") }
    var dificuldade by rememberSaveable { mutableStateOf("Fácil") }
    var paraExcluir by remember { mutableStateOf<QuestaoEntity?>(null) }

    val limparFormulario = { pergunta = ""; resposta = ""; dificuldade = "Fácil" }

    // O botão "voltar" do Android também sai do modo administrador.
    BackHandler(onBack = voltar)

    if (!s.autenticado) {
        Moldura("Acesso restrito") { Button(onClick = voltar) { Text("Voltar ao início") } }
        return
    }

    Moldura("🛠 Questões — Admin") {
        StatusSincronizacao(s, sincronizar = vm::sincronizar)

        // ---------- Formulário ----------
        val numero = resposta.trim().toIntOrNull()
        val formularioValido = pergunta.isNotBlank() && numero != null
        OutlinedTextField(
            value = pergunta, onValueChange = { pergunta = it },
            label = { Text("Pergunta (ex: 7 × 8)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = resposta, onValueChange = { resposta = it },
            label = { Text("Resposta numérica") },
            isError = resposta.isNotBlank() && numero == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuestaoRepository.DIFICULDADES.forEach { d ->
                FilterChip(selected = dificuldade == d, onClick = { dificuldade = d }, label = { Text(d) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    vm.salvar(
                        QuestaoEntity(pergunta = pergunta.trim(), resposta = numero ?: 0, dificuldade = dificuldade),
                        aoSalvar = limparFormulario
                    )
                },
                enabled = formularioValido && !s.carregando,
                modifier = Modifier.weight(1f)
            ) { Text(if (s.editando == null) "ADICIONAR" else "SALVAR EDIÇÃO") }
            if (s.editando != null) {
                OutlinedButton(onClick = { vm.cancelarEdicao(); limparFormulario() }, enabled = !s.carregando) {
                    Text("CANCELAR")
                }
            }
        }

        // ---------- Mensagens ----------
        s.erro?.let { erro ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                    Text(erro, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::limparMensagem) { Text("OK") }
                }
            }
        }
        s.mensagem?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        // ---------- Filtro ----------
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            (listOf("Todas") + QuestaoRepository.DIFICULDADES).forEach { d ->
                val total = if (d == "Todas") s.questoes.size else s.questoes.count { it.dificuldade == d }
                FilterChip(selected = s.filtro == d, onClick = { vm.filtrar(d) }, label = { Text("$d ($total)") })
            }
        }

        // ---------- Lista ----------
        val lista = s.questoesFiltradas
        if (lista.isEmpty()) {
            Text(
                if (s.carregando) "Carregando questões..." else "Nenhuma questão nesta dificuldade.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lista, key = { it.id }) { q ->
                    CartaoQuestao(
                        questao = q,
                        emEdicao = s.editando?.id == q.id,
                        habilitado = !s.carregando,
                        editar = {
                            vm.iniciarEdicao(q)
                            pergunta = q.pergunta; resposta = q.resposta.toString(); dificuldade = q.dificuldade
                        },
                        excluir = { paraExcluir = q }
                    )
                }
            }
        }
        OutlinedButton(onClick = voltar, modifier = Modifier.fillMaxWidth()) { Text("SAIR DO ADMIN") }
    }

    // Confirmação antes de excluir: a exclusão no Firebase não tem "desfazer".
    paraExcluir?.let { q ->
        AlertDialog(
            onDismissRequest = { paraExcluir = null },
            title = { Text("Excluir questão?") },
            text = { Text("\"${q.pergunta} = ${q.resposta}\" será removida do Firebase e de todos os celulares na próxima sincronização.") },
            confirmButton = {
                TextButton(onClick = {
                    if (s.editando?.id == q.id) limparFormulario()
                    vm.excluir(q); paraExcluir = null
                }) { Text("EXCLUIR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { paraExcluir = null }) { Text("CANCELAR") } }
        )
    }
}

@Composable
private fun StatusSincronizacao(s: AdminState, sincronizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            val texto = when {
                s.carregando -> "Comunicando com o Firebase..."
                s.ultimaSincronizacao != null -> "☁ Sincronizado às ${formatarHora(s.ultimaSincronizacao)}"
                else -> "☁ Ainda não sincronizado"
            }
            Text(texto, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = sincronizar, enabled = !s.carregando) { Text("SINCRONIZAR") }
        }
        if (s.carregando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CartaoQuestao(
    questao: QuestaoEntity,
    emEdicao: Boolean,
    habilitado: Boolean,
    editar: () -> Unit,
    excluir: () -> Unit
) {
    val cores = if (emEdicao) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    else CardDefaults.cardColors()
    Card(colors = cores, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("${questao.pergunta} = ${questao.resposta}", style = MaterialTheme.typography.titleMedium)
            Text(
                "${questao.dificuldade} • ${if (questao.origem == QuestaoRepository.ORIGEM_FIREBASE) "☁ Firebase" else "📱 só neste celular"}",
                style = MaterialTheme.typography.bodySmall
            )
            Row {
                TextButton(onClick = editar, enabled = habilitado) { Text(if (emEdicao) "Editando..." else "Editar") }
                TextButton(onClick = excluir, enabled = habilitado) { Text("Excluir") }
            }
        }
    }
}

private fun formatarHora(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(millis))
