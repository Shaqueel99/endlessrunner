package com.example.endlessrunner

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

// Data class representing a leaderboard entry.
data class LeaderboardEntry(
    val name: String = "",
    val score: Int = 0,
    val profileImageUrl: String? = null,
    val timestamp: Long = 0L
)

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter
    private val entries = mutableListOf<LeaderboardEntry>()

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
