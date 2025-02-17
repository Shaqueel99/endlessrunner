package com.example.endlessrunner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LeaderboardEntry)

    @Query("SELECT * FROM leaderboard_entries ORDER BY score DESC")
    fun getAllEntries(): Flow<List<LeaderboardEntry>>
}
