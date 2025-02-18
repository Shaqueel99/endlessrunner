package com.example.endlessrunner

import android.app.Application
import androidx.room.Room

class EndlessRunnerApp : Application() {
    lateinit var leaderboardDatabase: LeaderboardDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Room database
        leaderboardDatabase = Room.databaseBuilder(
            applicationContext,
            LeaderboardDatabase::class.java,
            "leaderboard_db"
        ).fallbackToDestructiveMigration()  // Clears DB on schema changes
            .build()
    }
}
