package com.example.batataquente.data.remote

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseConfig {
    /** O projeto usa um banco nomeado, não o "(default)". */
    const val DATABASE_ID = "batataquente"

    val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance(DATABASE_ID)
}
