package com.example.endlessrunner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class GameOverActivity : AppCompatActivity() {

    // Firebase Firestore instance
    private lateinit var firestore: FirebaseFirestore
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        // Initialize Firestore instance.
        firestore = FirebaseFirestore.getInstance()

        // Get the score passed via Intent.
        val score = intent.getIntExtra("score", 0)

        val scoreTextView = findViewById<TextView>(R.id.scoreTextView)
        scoreTextView.text = "Your Score: $score"

        submitOrUpdateLeaderboardEntry(score)


        val restartButton = findViewById<Button>(R.id.restartGame)
        restartButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // Close GameOverActivity so the game restarts cleanly
        }

        val mainMenuButton = findViewById<Button>(R.id.mainMenuButton)
        mainMenuButton.setOnClickListener {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
            finish()  // Close GameOverActivity
        }
    }

    // This function retrieves the current user's name and profile image URL from SharedPreferences,
    // then submits a leaderboard entry that includes the username, score, profile image URL, and a timestamp.
    private fun submitOrUpdateLeaderboardEntry(score: Int) {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)
        if (username == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // First, fetch the user's profile image URL from the "users" collection.
        firestore.collection("users").document(username)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val profileImageUrl = userDoc.getString("profileImagePath")
                    // Now, check if a leaderboard entry exists for this user.
                    firestore.collection("leaderboard")
                        .whereEqualTo("name", username)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.documents.isEmpty()) {
                                // No entry exists, so create one.
                                val entry = hashMapOf(
                                    "name" to username,
                                    "score" to score,
                                    "profileImageUrl" to profileImageUrl,
                                    "timestamp" to System.currentTimeMillis()
                                )
                                firestore.collection("leaderboard")
                                    .add(entry)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "New personal best!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                // An entry exists; check if the new score is higher.
                                val document = querySnapshot.documents[0]
                                val currentScore = document.getLong("score")?.toInt() ?: 0
                                if (score > currentScore) {
                                    // Update the document with the new score, timestamp, and profileImageUrl.
                                    document.reference.update(
                                        "score", score,
                                        "timestamp", System.currentTimeMillis(),
                                        "profileImageUrl", profileImageUrl
                                    ).addOnSuccessListener {
                                        Toast.makeText(this, "New personal best!", Toast.LENGTH_SHORT).show()
                                    }.addOnFailureListener { e ->
                                        Toast.makeText(this, "Failed to update score: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Score did not beat your personal best.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error checking leaderboard: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
