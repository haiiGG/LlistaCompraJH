package com.example.llistadelacompraJH.ui.theme.model

data class Llista(
    val id: String = "",
    val nom: String = "",
    val ownerId: String = "",
    val membres: List<String> = emptyList()
)