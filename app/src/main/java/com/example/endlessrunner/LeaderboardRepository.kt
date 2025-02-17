package com.example.endlessrunner

import kotlinx.coroutines.flow.Flow

class LeaderboardRepository(private val dao: LeaderboardDao) {
    val allEntries: Flow<List<LeaderboardEntry>> = dao.getAllEntries()

    suspend fun insert(entry: LeaderboardEntry) {
        dao.insert(entry)
    }
}
