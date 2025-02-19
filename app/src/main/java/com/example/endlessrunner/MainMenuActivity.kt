package com.example.endlessrunner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainMenuActivity : AppCompatActivity(), LoginDialog.LoginListener, RegisterDialog.RegisterListener {

    // Firebase Firestore instance
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Check if the user is logged in (using SharedPreferences)
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)
        if (username == null) {
            showLoginDialog()
        } else {
            Toast.makeText(this, "Welcome back, $username!", Toast.LENGTH_SHORT).show()
        }

        val playButton = findViewById<Button>(R.id.playButton)
        val leaderboardButton = findViewById<Button>(R.id.leaderboardButton)
        val quitButton = findViewById<Button>(R.id.quitButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        playButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        leaderboardButton.setOnClickListener {
            // Open leaderboard (implement as needed)
        }

        quitButton.setOnClickListener {
            finishAffinity()
        }

        signOutButton.setOnClickListener {
            with(sharedPrefs.edit()) {
                remove("username")
                apply()
            }
            Toast.makeText(this, "Signed out successfully!", Toast.LENGTH_SHORT).show()
            showLoginDialog()
        }
    }
    override fun onSwitchToRegister() {
        // Called from LoginDialog when the user chooses to register
        showRegisterDialog()
    }
    private fun showLoginDialog() {
        val loginDialog = LoginDialog(this)
        loginDialog.isCancelable = false
        loginDialog.show(supportFragmentManager, "LoginDialog")
    }

    private fun showRegisterDialog() {
        val registerDialog = RegisterDialog(this)
        registerDialog.isCancelable = false
        registerDialog.show(supportFragmentManager, "RegisterDialog")
    }

    // Called when the user logs in from the login dialog.
    override fun onLogin(username: String, password: String) {
        firestore.collection("users").document(username)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedHash = document.getString("hashedPassword") ?: ""
                    if (storedHash == HashUtil.hashPassword(password)) {
                        Toast.makeText(this, "Logged in as $username", Toast.LENGTH_SHORT).show()
                        saveUser(username)
                    } else {
                        Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                        showLoginDialog()
                    }
                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    showLoginDialog()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoginDialog()
            }
    }



    // Called when the user registers from the registration dialog.
    override fun onRegister(username: String, password: String, imagePath: String?) {
        firestore.collection("users").document(username)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Toast.makeText(this, "Username already exists!", Toast.LENGTH_SHORT).show()
                    showRegisterDialog()
                } else {
                    val hashedPassword = HashUtil.hashPassword(password)
                    val userData = hashMapOf(
                        "username" to username,
                        "hashedPassword" to hashedPassword,
                        "profileImagePath" to imagePath,
                        "coinsCollected" to 0
                    )
                    firestore.collection("users").document(username)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registered as $username", Toast.LENGTH_SHORT).show()
                            saveUser(username)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            showRegisterDialog()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                showRegisterDialog()
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
