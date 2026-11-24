package com.example.llistadelacompraJH.ui.theme.data

import com.google.firebase.Timestamp
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

    fun getLlistesUsuari(usuariId: String): Flow<List<Llista>> = callbackFlow {
        val listenerRegistration = db.collection("lists")
            .whereArrayContains("members", usuariId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.documents ?: return@addSnapshotListener
                val llistes = try {
                    documents.mapNotNull { it.toObject(Llista::class.java) }
                } catch (error: RuntimeException) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(llistes)
            }
        awaitClose { listenerRegistration.remove() }
    }
}
