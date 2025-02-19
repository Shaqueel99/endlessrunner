package com.example.endlessrunner

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class GameOverActivity : AppCompatActivity() {

    // Use Firebase Firestore instead of a Room repository.
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

        val submitButton = findViewById<Button>(R.id.submitScoreButton)
        submitButton.setOnClickListener {
            showNameInputDialog(score)
        }

        val restartButton = findViewById<Button>(R.id.restartGame)
        restartButton.setOnClickListener{

        }

        val mainMenuButton = findViewById<Button>(R.id.mainMenuButton)
        mainMenuButton.setOnClickListener {
            // Navigate back to Main Menu.

        }
    }

    private fun showNameInputDialog(score: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter your name")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                // Create a new leaderboard entry as a map.
                val entry = hashMapOf(
                    "name" to name,
                    "score" to score,
                    "timestamp" to System.currentTimeMillis()
                )
                // Add the entry to the "leaderboard" collection in Firestore.
                firestore.collection("leaderboard")
                    .add(entry)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Score submitted!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
