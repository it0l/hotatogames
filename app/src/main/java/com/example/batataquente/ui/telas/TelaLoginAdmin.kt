package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.batataquente.ui.admin.AdminViewModel

@Composable
fun TelaLoginAdmin(vm: AdminViewModel, entrar: () -> Unit, voltar: () -> Unit) {
    val s by vm.state.collectAsState()
    var senha by rememberSaveable { mutableStateOf("") }
    // Quem decide se a senha está certa é o ViewModel; a tela só reage.
    val tentarEntrar = { vm.login(senha, aoEntrar = { senha = ""; entrar() }) }

    Moldura("🔒 Administrador") {
        Text("Área para cadastrar, editar e excluir as questões salvas no Firebase.")
        Text("Senha de demonstração: 1234", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it; if (s.erro != null) vm.limparMensagem() },
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { tentarEntrar() }),
            isError = s.erro != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        s.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = tentarEntrar, enabled = senha.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("ENTRAR") }
        OutlinedButton(onClick = { vm.limparMensagem(); voltar() }, modifier = Modifier.fillMaxWidth()) { Text("VOLTAR") }
    }
}
