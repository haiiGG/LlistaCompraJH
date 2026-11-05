package com.example.llistadelacompraJH.ui.theme.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

object FirebaseManager {
    // Ens connectem a la base de dades Firestore
    val db = Firebase.firestore
}