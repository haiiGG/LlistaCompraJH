package com.example.llistadelacompraJH.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.llistadelacompraJH.ui.theme.LlistaDeLaCompraJHTheme
import com.example.llistadelacompraJH.ui.theme.data.Llista
import com.example.llistadelacompraJH.ui.theme.data.LlistaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.catch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: String,
    modifier: Modifier = Modifier,
    onLlistaClick: ((Llista) -> Unit)? = null,
    repository: LlistaRepository = remember { LlistaRepository() }
) {
    var llistes by remember { mutableStateOf<List<Llista>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableStateOf(0) }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, userId, loadAttempt) {
        isLoading = true
        loadError = null
        repository.getLlistesUsuari(userId)
            .catch { error ->
                Log.w("ShoppingLists", "List loading failed", error)
                loadError = firestoreError(error, "No se han podido cargar las listas.")
                isLoading = false
            }
            .collect { llistesObtingudes ->
                llistes = llistesObtingudes
                isLoading = false
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Listas de la Compra",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear lista"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val screenError = loadError
            when {
                screenError != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(screenError)
                        TextButton(onClick = { loadAttempt++ }) {
                            Text("Reintentar")
                        }
                    }
                }

                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                llistes.isEmpty() -> {
                    EmptyStateContent(
                        onCrearClick = { showCreateDialog = true }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(llistes, key = { it.id }) { llista ->
                            LlistaCard(
                                llista = llista,
                                onClick = onLlistaClick?.let { callback -> { callback(llista) } }
                            )
                        }
                    }
                }
            }

            if (showCreateDialog) {
                CrearLlistaDialog(
                    isCreating = isCreating,
                    createError = createError,
                    onDismiss = {
                        showCreateDialog = false
                        createError = null
                    },
                    onConfirm = { nomLlista ->
                        isCreating = true
                        createError = null
                        repository.crearLlista(
                            nombre = nomLlista,
                            usuariId = userId,
                            onSuccess = {
                                isCreating = false
                                showCreateDialog = false
                            },
                            onFailure = { error ->
                                Log.w("ShoppingLists", "List creation failed", error)
                                isCreating = false
                                createError = firestoreError(
                                    error,
                                    "No se ha podido guardar la lista. Inténtalo de nuevo."
                                )
                            }
                        )
                    }
                )
            }
        }
    }
}

internal fun firestoreError(error: Throwable, fallback: String): String {
    return if ((error as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        "Firestore ha denegado el acceso. Revisa los permisos del proyecto."
    } else {
        fallback
    }
}

@Composable
fun LlistaCard(
    llista: Llista,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = llista.nombre.ifBlank { "Sin nombre" },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${llista.members.size} miembro(s)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyStateContent(
    onCrearClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tienes ninguna lista",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crea tu primera lista de la compra",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCrearClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Crear nueva lista")
        }
    }
}

@Composable
fun CrearLlistaDialog(
    isCreating: Boolean,
    createError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nomLlista by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Text(text = "Nueva lista de la compra")
        },
        text = {
            Column {
                Text(
                    text = "Introduce el nombre de la nueva lista:",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = nomLlista,
                    onValueChange = { nomLlista = it },
                    label = { Text("Nombre de la lista") },
                    enabled = !isCreating,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (createError != null) {
                    Text(
                        text = createError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nomLlista.trim()) },
                enabled = !isCreating && nomLlista.isNotBlank()
            ) {
                Text(if (isCreating) "Guardando…" else "Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LlistaDeLaCompraJHTheme {
        LlistaCard(
            llista = Llista(
                id = "1",
                nombre = "Compra de la semana",
                members = listOf("user1", "user2")
            )
        )
    }
}
