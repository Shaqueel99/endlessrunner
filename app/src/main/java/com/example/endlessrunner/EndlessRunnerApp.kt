package com.example.endlessrunner

import android.app.Application
import androidx.room.Room

class EndlessRunnerApp : Application() {
    lateinit var leaderboardDatabase: LeaderboardDatabase
        private set
    lateinit var leaderboardRepository: LeaderboardRepository
        private set

    override fun onCreate() {
        super.onCreate()
        leaderboardDatabase = Room.databaseBuilder(
            applicationContext,
            LeaderboardDatabase::class.java,
            "leaderboard_db"
        ).build()

        leaderboardRepository = LeaderboardRepository(leaderboardDatabase.leaderboardDao())
    }
}
