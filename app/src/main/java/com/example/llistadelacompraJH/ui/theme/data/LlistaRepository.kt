package com.example.llistadelacompraJH.ui.theme.data

class LlistaRepository {
    // Obtenim una instància de la base de dades Firestore
    private val db = FirebaseManager.db
    // Creem la col·lecció de llistes

    fun crearLlista(nombre: String, usuariId: String) {
        // Afegim les dades
        val dadesLlista = hashMapOf(
            "nombre" to nombre,
            "ownerId" to usuariId,
            "members" to listOf(usuariId),
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        // Afegim la llista a la base de dades
        db.collection("lists")
            .add(dadesLlista)
    }
}