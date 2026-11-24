package com.example.llistadelacompraJH.ui.theme.data

import com.example.llistadelacompraJH.ui.theme.model.Categoria
import com.example.llistadelacompraJH.ui.theme.model.Producte
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LlistaRepository {
    // Obtenim una instància de la base de dades Firestore
    private val db = FirebaseManager.db

    fun crearLlista(
        nombre: String,
        usuariId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        // Creem l'objecte Llista
        val llista = Llista(
            nombre = nombre,
            ownerId = usuariId,
            members = listOf(usuariId),
            createdAt = Timestamp.now()
        )

        // Afegim la llista a la base de dades
        db.collection("lists")
            .add(llista)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getLlistesUsuari(usuariId: String): Flow<List<Llista>> =
        db.collection("lists")
            .whereArrayContains("members", usuariId)
            .observeDocuments { it.toObject(Llista::class.java) }

    fun getLlista(listId: String): Flow<Llista?> = callbackFlow {
        val listener = listDocument(listId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            try {
                trySend(snapshot?.toObject(Llista::class.java))
            } catch (failure: RuntimeException) {
                close(failure)
            }
        }
        awaitClose { listener.remove() }
    }

    fun compartirLlista(
        listId: String,
        memberId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        listDocument(listId).update("members", FieldValue.arrayUnion(memberId))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun canviarNomLlista(
        listId: String,
        nom: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        listDocument(listId).update("nombre", nom)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getProductes(listId: String): Flow<List<Producte>> =
        items(listId).observeDocuments { document ->
            document.toObject(Producte::class.java)?.copy(id = document.id)
        }

    fun afegirProducte(
        listId: String,
        nom: String,
        categoryId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        items(listId)
            .add(mapOf("nom" to nom, "comprat" to false, "categoryId" to categoryId))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun marcarProducte(
        listId: String,
        productId: String,
        comprat: Boolean,
        onFailure: (Exception) -> Unit
    ) {
        items(listId).document(productId)
            .update("comprat", comprat)
            .addOnFailureListener { onFailure(it) }
    }

    fun getCategories(listId: String): Flow<List<Categoria>> =
        categories(listId).observeDocuments { document ->
            document.toObject(Categoria::class.java)?.copy(id = document.id)
        }

    fun afegirCategoria(
        listId: String,
        nom: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        categories(listId)
            .add(mapOf("nom" to nom))
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onFailure(it) }
    }

    fun canviarCategoria(
        listId: String,
        productId: String,
        categoryId: String,
        onFailure: (Exception) -> Unit
    ) {
        items(listId).document(productId)
            .update("categoryId", categoryId)
            .addOnFailureListener { onFailure(it) }
    }

    private fun listDocument(listId: String) = db.collection("lists").document(listId)

    private fun items(listId: String) = listDocument(listId).collection("items")

    private fun categories(listId: String) = listDocument(listId).collection("categories")

    private fun <T> Query.observeDocuments(
        fromDocument: (DocumentSnapshot) -> T?
    ): Flow<List<T>> = callbackFlow {
        val listenerRegistration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val documents = snapshot?.documents ?: return@addSnapshotListener
            val values = try {
                documents.mapNotNull(fromDocument)
            } catch (error: RuntimeException) {
                close(error)
                return@addSnapshotListener
            }
            trySend(values)
        }
        awaitClose { listenerRegistration.remove() }
    }
}
