package com.example.llistadelacompraJH.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.llistadelacompraJH.ui.theme.data.LlistaRepository
import com.example.llistadelacompraJH.ui.theme.data.Llista
import com.example.llistadelacompraJH.ui.theme.model.Categoria
import com.example.llistadelacompraJH.ui.theme.model.Producte
import kotlinx.coroutines.flow.catch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    listId: String,
    listName: String,
    userId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { LlistaRepository() }
    var list by remember(listId) { mutableStateOf<Llista?>(null) }
    var showListMenu by remember(listId) { mutableStateOf(false) }
    var listDialog by rememberSaveable(listId) { mutableStateOf<ListDialog?>(null) }
    var products by remember(listId) { mutableStateOf(emptyList<Producte>()) }
    var productName by rememberSaveable(listId) { mutableStateOf("") }
    var loadAttempt by remember(listId) { mutableStateOf(0) }
    var isLoading by remember(listId) { mutableStateOf(true) }
    var isAdding by remember(listId) { mutableStateOf(false) }
    var loadError by remember(listId) { mutableStateOf<String?>(null) }
    var writeError by remember(listId) { mutableStateOf<String?>(null) }
    var categories by remember(listId) { mutableStateOf(emptyList<Categoria>()) }
    var categoriesError by remember(listId) { mutableStateOf<String?>(null) }
    var categoriesAttempt by remember(listId) { mutableStateOf(0) }
    var selectedCategoryId by rememberSaveable(listId) { mutableStateOf("") }

    LaunchedEffect(repository, listId, loadAttempt) {
        repository.getLlista(listId)
            .catch { failure ->
                list = null
                writeError = firestoreError(failure, "No se han podido cargar los datos de la lista.")
            }
            .collect { list = it }
    }

    LaunchedEffect(repository, listId, categoriesAttempt) {
        categoriesError = null
        repository.getCategories(listId)
            .catch { error ->
                categoriesError = firestoreError(error, "No se han podido cargar las categorías.")
            }
            .collect { categories = it.sortedBy { category -> category.nom.lowercase() } }
    }

    LaunchedEffect(repository, listId, loadAttempt) {
        isLoading = true
        loadError = null
        repository.getProductes(listId)
            .catch { error ->
                loadError = firestoreError(error, "No se han podido cargar los productos.")
                isLoading = false
            }
            .collect {
                products = it
                isLoading = false
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text((list?.nombre ?: listName).ifBlank { "Lista de la compra" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { listDialog = ListDialog.CATEGORY }) {
                        Text("Nueva categoría")
                    }
                    val currentList = list
                    if (currentList != null) {
                        if (currentList.ownerId == userId) {
                            IconButton(onClick = { listDialog = ListDialog.SHARE }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir lista")
                            }
                        }
                        Box {
                            IconButton(onClick = { showListMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones de la lista")
                            }
                            DropdownMenu(
                                expanded = showListMenu,
                                onDismissRequest = { showListMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text("Cambiar nombre") }, onClick = {
                                    showListMenu = false
                                    listDialog = ListDialog.RENAME
                                })
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nuevo producto") },
                    singleLine = true,
                    enabled = !isAdding,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = productName.isNotBlank() && !isAdding && !isLoading && loadError == null,
                    onClick = {
                        isAdding = true
                        writeError = null
                        repository.afegirProducte(
                            listId = listId,
                            nom = productName.trim(),
                            categoryId = selectedCategoryId,
                            onSuccess = {
                                productName = ""
                                isAdding = false
                            },
                            onFailure = { error ->
                                writeError = firestoreError(error, "No se ha podido añadir el producto.")
                                isAdding = false
                            }
                        )
                    }
                ) {
                    Text(if (isAdding) "Guardando…" else "Añadir")
                }
            }

            if (categories.isNotEmpty()) {
                val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.nom
                CategoryMenu(categories, onSelect = { selectedCategoryId = it }) { open ->
                    TextButton(onClick = open, enabled = !isAdding) {
                        Text(selectedName?.let { "Categoría: $it" } ?: "Elegir categoría")
                    }
                }
            }
            categoriesError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { categoriesAttempt++ }) { Text("Reintentar categorías") }
            }
            writeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val error = loadError
            when {
                isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { loadAttempt++ }) { Text("Reintentar") }
                }
                products.isEmpty() && categories.isEmpty() ->
                    Text("Todavía no hay productos en esta lista.")
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    val categoryIds = categories.mapTo(mutableSetOf()) { it.id }
                    val grouped = products.groupBy { product ->
                        product.categoryId.takeIf { it in categoryIds }.orEmpty()
                    }
                    val sections = categories + Categoria(nom = "Sin categoría")
                    for (category in sections) {
                        val sectionProducts = grouped[category.id].orEmpty()
                        if (category.id.isEmpty() && sectionProducts.isEmpty()) continue
                        item(key = "category:${category.id}") {
                            Text(
                                text = category.nom,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(sectionProducts, key = { "product:${it.id}" }) { product ->
                            ProductRow(
                                product = product,
                                categories = categories,
                                onChecked = { checked ->
                                    writeError = null
                                    repository.marcarProducte(listId, product.id, checked) { failure ->
                                        writeError = firestoreError(failure, "No se ha podido actualizar el producto.")
                                    }
                                },
                                onCategorySelected = { categoryId ->
                                    if (product.categoryId != categoryId) {
                                        writeError = null
                                        repository.canviarCategoria(listId, product.id, categoryId) { failure ->
                                            writeError = firestoreError(failure, "No se ha podido mover el producto.")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val closeDialog = { listDialog = null }
    when (listDialog) {
        ListDialog.CATEGORY -> ListTextDialog(
            title = "Nueva categoría",
            label = "Nombre",
            confirmLabel = "Crear",
            failureMessage = "No se ha podido crear la categoría.",
            onDismiss = closeDialog
        ) { name, onSuccess, onFailure ->
            repository.afegirCategoria(listId, name, { categoryId ->
                selectedCategoryId = categoryId
                onSuccess()
            }, onFailure)
        }
        ListDialog.SHARE -> ListTextDialog(
            title = "Compartir lista",
            label = "Código de usuario",
            confirmLabel = "Dar acceso",
            description = "Pídele a la otra persona que copie su código desde Perfil. Podrá abrir y editar esta lista.",
            failureMessage = "No se ha podido compartir la lista.",
            validate = { code ->
                when {
                    code.length > 128 || code.any { it.isWhitespace() } -> "Introduce un código de usuario válido."
                    list?.members?.contains(code) == true -> "Esta persona ya tiene acceso."
                    else -> null
                }
            },
            onDismiss = closeDialog
        ) { code, onSuccess, onFailure ->
            repository.compartirLlista(listId, code, {
                onSuccess()
                Toast.makeText(context, "Acceso concedido al código indicado", Toast.LENGTH_SHORT).show()
            }, onFailure)
        }
        ListDialog.RENAME -> ListTextDialog(
            title = "Cambiar nombre",
            label = "Nombre de la lista",
            initialValue = list?.nombre ?: listName,
            confirmLabel = "Guardar",
            failureMessage = "No se ha podido cambiar el nombre.",
            onDismiss = closeDialog
        ) { name, onSuccess, onFailure ->
            repository.canviarNomLlista(listId, name, onSuccess, onFailure)
        }
        null -> Unit
    }
}

private enum class ListDialog { CATEGORY, SHARE, RENAME }

@Composable
private fun ProductRow(
    product: Producte,
    categories: List<Categoria>,
    onChecked: (Boolean) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .toggleable(value = product.comprat, role = Role.Checkbox, onValueChange = onChecked)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(checked = product.comprat, onCheckedChange = null)
            Text(
                text = product.nom,
                textDecoration = if (product.comprat) TextDecoration.LineThrough else null
            )
        }
        if (categories.isNotEmpty()) {
            CategoryMenu(categories, onCategorySelected) { open ->
                IconButton(onClick = open) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Cambiar categoría de ${product.nom}")
                }
            }
        }
    }
}

@Composable
private fun CategoryMenu(
    categories: List<Categoria>,
    onSelect: (String) -> Unit,
    anchor: @Composable (open: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        anchor { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (category in listOf(Categoria(nom = "Sin categoría")) + categories) {
                DropdownMenuItem(text = { Text(category.nom) }, onClick = {
                    expanded = false
                    onSelect(category.id)
                })
            }
        }
    }
}

@Composable
private fun ListTextDialog(
    title: String,
    label: String,
    confirmLabel: String,
    failureMessage: String,
    initialValue: String = "",
    description: String? = null,
    validate: (String) -> String? = { null },
    onDismiss: () -> Unit,
    onSave: (String, () -> Unit, (Exception) -> Unit) -> Unit
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                description?.let { Text(it) }
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        error = null
                    },
                    label = { Text(label) },
                    singleLine = true,
                    enabled = !isSaving,
                    supportingText = error?.let { message -> { Text(message) } },
                    isError = error != null
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank() && !isSaving,
                onClick = {
                    val trimmed = value.trim()
                    error = validate(trimmed)
                    if (error == null) {
                        isSaving = true
                        onSave(trimmed, onDismiss) { failure ->
                            isSaving = false
                            error = firestoreError(failure, failureMessage)
                        }
                    }
                }
            ) { Text(if (isSaving) "Guardando…" else confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
        }
    )
}
