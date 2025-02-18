package com.example.endlessrunner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile)

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUser(username: String): UserProfile?
}
