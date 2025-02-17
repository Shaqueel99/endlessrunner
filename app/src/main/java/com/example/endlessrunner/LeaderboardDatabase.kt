package com.example.endlessrunner

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LeaderboardEntry::class], version = 2, exportSchema = false)
abstract class LeaderboardDatabase : RoomDatabase() {
    abstract fun leaderboardDao(): LeaderboardDao
}
