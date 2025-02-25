package com.example.endlessrunner

import java.security.MessageDigest
/**
 * Utility object for hashing strings using the SHA-256 algorithm.
 */
object HashUtil {
    /**
     * Hashes the provided password using the SHA-256 algorithm.
     *
     * @param password The input password string.
     * @return The hexadecimal string representation of the hashed password.
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
