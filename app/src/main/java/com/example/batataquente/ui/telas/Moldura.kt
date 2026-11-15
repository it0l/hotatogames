package com.example.batataquente.ui.telas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun Moldura(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        conteudo()
    }
}

@Composable
internal fun MolduraLista(titulo: String, itens: LazyListScope.() -> Unit) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "titulo") {
            Text(titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        itens()
    }
}
