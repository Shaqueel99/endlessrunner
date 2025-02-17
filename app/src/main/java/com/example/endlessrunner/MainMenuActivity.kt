package com.example.endlessrunner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the main menu layout.
        setContentView(R.layout.activity_main_menu)

        val playButton = findViewById<Button>(R.id.playButton)
        val leaderboardButton = findViewById<Button>(R.id.leaderboardButton)
        val quitButton = findViewById<Button>(R.id.quitButton)

        playButton.setOnClickListener {
            // Launch the game. For instance, if you want to use your GameView in a GameActivity:
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        leaderboardButton.setOnClickListener {
            // Display a toast or launch your leaderboard activity.
            Toast.makeText(this, "Leaderboard not implemented yet", Toast.LENGTH_SHORT).show()
        }

        quitButton.setOnClickListener {
            // Quit the application.
            finishAffinity()
        }
    }
}
