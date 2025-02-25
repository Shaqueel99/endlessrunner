package com.example.endlessrunner

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
/**
 * Data class representing an entry on the leaderboard.
 *
 * @property name The player's name.
 * @property score The player's score.
 * @property profileImageUrl The URL for the player's profile image (optional).
 * @property timestamp The timestamp when the score was recorded.
 */
// Data class representing a leaderboard entry.
data class LeaderboardEntry(
    val name: String = "",
    val score: Int = 0,
    val profileImageUrl: String? = null,
    val timestamp: Long = 0L
)
/**
 * Activity for displaying the leaderboard.
 * Retrieves leaderboard data from Firestore and displays it in a RecyclerView.
 */
class LeaderboardActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter
    private val entries = mutableListOf<LeaderboardEntry>()
    /**
     * Called when the activity is created.
     * Sets up the RecyclerView, back button, and initiates the leaderboard load.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        firestore = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.leaderboardRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LeaderboardAdapter(entries)
        recyclerView.adapter = adapter
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()  // Returns to the previous screen
        }
        loadLeaderboard()
    }
    /**
     * Loads leaderboard data from Firestore.
     * Orders the entries by score in descending order and updates the RecyclerView.
     */
    private fun loadLeaderboard() {
        firestore.collection("leaderboard")
            .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                entries.clear()
                for (document in querySnapshot.documents) {
                    val entry = document.toObject(LeaderboardEntry::class.java)
                    if (entry != null) {
                        entries.add(entry)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load leaderboard: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
