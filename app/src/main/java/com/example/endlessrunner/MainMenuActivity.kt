package com.example.endlessrunner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast

class MainMenuActivity : AppCompatActivity(), LoginRegisterDialog.LoginRegisterListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Check if the user is logged in, if not, show the login dialog
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)

        if (username == null) {
            showLoginDialog()
        }

        val playButton = findViewById<Button>(R.id.playButton)
        val leaderboardButton = findViewById<Button>(R.id.leaderboardButton)
        val quitButton = findViewById<Button>(R.id.quitButton)

        playButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        leaderboardButton.setOnClickListener {
            // Open leaderboard
        }

        quitButton.setOnClickListener {
            finishAffinity() // Quit app
        }
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener {
            val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                remove("username")  // Clear stored username
                apply()
            }
            showLoginDialog()  // Show login dialog again
        }

    }

    private fun showLoginDialog() {
        val dialog = LoginRegisterDialog(this)
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, "LoginRegisterDialog")
    }

    override fun onLogin(username: String, password: String) {
        // TODO: Implement login logic (check database)
        Toast.makeText(this, "Logged in as $username", Toast.LENGTH_SHORT).show()
        saveUser(username)
    }

    override fun onRegister(username: String, password: String) {
        // TODO: Implement registration logic (save to database)
        Toast.makeText(this, "Registered as $username", Toast.LENGTH_SHORT).show()
        saveUser(username)
    }

    private fun saveUser(username: String) {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("username", username)
            apply()
        }
    }

}
