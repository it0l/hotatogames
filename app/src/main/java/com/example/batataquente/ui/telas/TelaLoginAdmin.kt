package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.batataquente.ui.admin.AdminViewModel

@Composable
fun TelaLoginAdmin(vm: AdminViewModel, entrar: () -> Unit, voltar: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var senhaLocal by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senhaFirebase by rememberSaveable { mutableStateOf("") }
    Moldura("🔒 Administrador") {
        Text("Modo local: gerenciamento offline das questões. Senha de demonstração: 1234.")
        OutlinedTextField(value = senhaLocal, onValueChange = { senhaLocal = it }, label = { Text("Senha local") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { if (vm.loginLocal(senhaLocal)) entrar() }, modifier = Modifier.fillMaxWidth()) {
            Text("ENTRAR NO MODO LOCAL")
        }
        HorizontalDivider()
        Text("Firebase: use e-mail e senha de um usuário autorizado no console.")
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail Firebase") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = senhaFirebase, onValueChange = { senhaFirebase = it },
            label = { Text("Senha Firebase") }, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(
            onClick = { vm.loginFirebase(email, senhaFirebase, entrar) },
            enabled = !s.carregando && email.isNotBlank() && senhaFirebase.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (s.carregando) "CONECTANDO..." else "ENTRAR COM FIREBASE") }
        s.mensagem?.let { Text(it, color = if (s.autenticado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = voltar, modifier = Modifier.fillMaxWidth()) { Text("VOLTAR") }
    }
}
