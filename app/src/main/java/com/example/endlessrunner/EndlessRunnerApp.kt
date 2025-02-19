package com.example.endlessrunner

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore

class EndlessRunnerApp : Application() {
    lateinit var firestore: FirebaseFirestore
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance()
    }
}
