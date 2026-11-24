package com.example.llistadelacompraJH.ui.theme.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Llista(
    @DocumentId
    val id: String = "",
    val nombre: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)
