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
fun TelaInicial(jogar: () -> Unit, administrar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🥔🔥", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text("BATATA QUENTE", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text("MATEMÁTICA", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        Text("Acerte as contas e passe a batata antes que ela exploda!", textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = jogar, modifier = Modifier.fillMaxWidth()) { Text("JOGAR") }
        OutlinedButton(onClick = administrar, modifier = Modifier.fillMaxWidth()) { Text("ADMINISTRADOR") }
    }
}
