package com.example.endlessrunner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val score: Int
)
