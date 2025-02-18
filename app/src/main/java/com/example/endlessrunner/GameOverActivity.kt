package com.example.endlessrunner

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class GameOverActivity : AppCompatActivity() {

    private lateinit var leaderboardRepository: LeaderboardRepository
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        // Get the score passed via Intent.
        val score = intent.getIntExtra("score", 0)

        val scoreTextView = findViewById<TextView>(R.id.scoreTextView)
        scoreTextView.text = "Your Score: $score"

        // Obtain repository from application.
        //leaderboardRepository = (application as EndlessRunnerApp).leaderboardRepository

        val submitButton = findViewById<Button>(R.id.submitScoreButton)
        submitButton.setOnClickListener {
            showNameInputDialog(score)
        }

        val mainMenuButton = findViewById<Button>(R.id.mainMenuButton)
        mainMenuButton.setOnClickListener {
            // Navigate back to Main Menu.
            finish()
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
                scope.launch {
                    leaderboardRepository.insert(LeaderboardEntry(name = name, score = score))
                    Toast.makeText(this@GameOverActivity, "Score submitted!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
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
