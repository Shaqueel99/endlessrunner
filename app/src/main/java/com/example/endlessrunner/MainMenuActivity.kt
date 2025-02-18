package com.example.endlessrunner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainMenuActivity : AppCompatActivity(), LoginRegisterDialog.LoginRegisterListener {
    private lateinit var db: LeaderboardDatabase  // Database instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        db = (application as EndlessRunnerApp).leaderboardDatabase

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
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser(username)

            if (user != null && user.hashedPassword == password) {
                runOnUiThread {
                    Toast.makeText(this@MainMenuActivity, "Logged in as $username", Toast.LENGTH_SHORT).show()
                    saveUser(username)
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainMenuActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    showLoginDialog()
                }
            }
        }
    }

    override fun onRegister(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingUser = db.userDao().getUser(username)

            if (existingUser == null) {
                val newUser = UserProfile(username, password, null, 0)
                db.userDao().insertUser(newUser)

                runOnUiThread {
                    Toast.makeText(this@MainMenuActivity, "Registered as $username", Toast.LENGTH_SHORT).show()
                    saveUser(username)
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainMenuActivity, "Username already exists!", Toast.LENGTH_SHORT).show()
                    showLoginDialog()
                }
            }
        }
    }

    private fun saveUser(username: String) {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("username", username)
            apply()
        }
    }


}
