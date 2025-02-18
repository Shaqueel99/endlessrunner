package com.example.endlessrunner

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val username: String,  // Unique identifier
    val hashedPassword: String,        // Store hashed password
    val profileImagePath: String?,     // Local file path or URL
    val coinsCollected: Int = 0        // Track player's coins
)
