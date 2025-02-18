package com.example.endlessrunner

import androidx.room.Database
import androidx.room.RoomDatabase
import java.security.MessageDigest

@Database(entities = [LeaderboardEntry::class, UserProfile::class], version = 2, exportSchema = false)
abstract class LeaderboardDatabase : RoomDatabase() {
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun userDao(): UserDao
}



object HashUtil {
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }  // Convert bytes to hex
    }
}
