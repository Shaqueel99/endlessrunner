package com.example.endlessrunner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
/**
 * Main menu activity for the game.
 * Handles user login/registration, displays user info, and navigates to other parts of the app.
 */
class MainMenuActivity : AppCompatActivity(), LoginDialog.LoginListener, RegisterDialog.RegisterListener {

    var profileImageUrl: String? = null

    // Firebase Firestore and Storage instances
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI elements for header display (ensure these IDs are defined in your layout)
    private lateinit var welcomeTextView: TextView
    private lateinit var profileImageViewMain: ImageView
    private lateinit var coinsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Initialize Firestore & Storage
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Find header UI elements from layout
        welcomeTextView = findViewById(R.id.welcomeTextView)
        profileImageViewMain = findViewById(R.id.profileImageViewMain)
        coinsTextView = findViewById(R.id.coinsTextView)

        // Check if the user is logged in (using SharedPreferences)
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)
        if (username == null) {
            showLoginDialog()
        }
        else
        {
            Toast.makeText(this, "Welcome back, $username!", Toast.LENGTH_SHORT).show()
            loadUserData(username)  // Fetch user data after a successful login or registration.
            saveUser(username,profileImageUrl)
        }

        val playButton = findViewById<Button>(R.id.playButton)
        val leaderboardButton = findViewById<Button>(R.id.leaderboardButton)
        val quitButton = findViewById<Button>(R.id.quitButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        val storeButton = findViewById<Button>(R.id.storeButton)

        profileImageViewMain.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        playButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        leaderboardButton.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }

        storeButton.setOnClickListener{
            val intent = Intent(this, StoreActivity::class.java)
            startActivity(intent)
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
    /**
     * Callback to switch from login to registration.
     */
    override fun onSwitchToRegister() {
        showRegisterDialog()
    }
    /**
     * Displays the login dialog.
     */
    private fun showLoginDialog() {
        val loginDialog = LoginDialog(this)
        loginDialog.isCancelable = false
        loginDialog.show(supportFragmentManager, "LoginDialog")
    }
    /**
     * Displays the registration dialog.
     */
    private fun showRegisterDialog() {
        val registerDialog = RegisterDialog(this)
        registerDialog.isCancelable = false
        registerDialog.show(supportFragmentManager, "RegisterDialog")
    }
    /**
     * Called when the user logs in from the login dialog.
     * Authenticates the user using Firestore and updates the UI on success.
     *
     * @param username The entered username.
     * @param password The entered password.
     */
    override fun onLogin(username: String, password: String) {
        //firestore.collection("users").document(username)
        firestore.collection("users").whereEqualTo("username", username)
            .get()
            .addOnSuccessListener {
                    documents ->
                val document = documents.firstOrNull()  // Get the first matching document
                if (document != null) {
                    val storedHash = document.getString("hashedPassword") ?: ""
                    if (storedHash == HashUtil.hashPassword(password)) {
                        Toast.makeText(this, "Logged in as $username", Toast.LENGTH_SHORT).show()
                        saveUser(username,profileImageUrl)
                        loadUserData(username)  // Update header with user data.
                    }
                    else {
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
    /**
     * Called when the user registers from the registration dialog.
     * Creates a new user document in Firestore if the username is not already taken.
     *
     * @param username The chosen username.
     * @param password The chosen password.
     * @param imagePath The optional profile image URL.
     */
    override fun onRegister(username: String, password: String, imagePath: String?) {
        firestore.collection("users").document(username)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Toast.makeText(this, "Username already exists!", Toast.LENGTH_SHORT).show()
                    showRegisterDialog()
                }
                else {
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
                            saveUser(username,imagePath)
                            loadUserData(username)  // Fetch data to update header.
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
    /**
     * Saves the current user's details in SharedPreferences.
     *
     * @param username The username to save.
     * @param profileImageUrl The profile image URL to save (if available).
     */
    private fun saveUser(username: String, profileImageUrl: String?) {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("username", username)
            if (profileImageUrl != null) {
                putString("profileImageUrl", profileImageUrl)
            }
            apply()
        }
    }


    /**
     * Loads the user data from Firestore and updates the header UI elements.
     *
     * @param username The username whose data should be loaded.
     */
    private fun loadUserData(username: String) {
        //firestore.collection("users").document(username)
        firestore.collection("users").whereEqualTo("username", username)
            .get()
            .addOnSuccessListener {
                    documents ->
                val document = documents.firstOrNull()  // Get the first matching document
                if (document != null) {
                    profileImageUrl = document.getString("profileImagePath")
                    val coins = document.getLong("coinsCollected") ?: 0L
                    welcomeTextView.text = "Welcome $username"
                    coinsTextView.text = "Coins: $coins"
                    if (!profileImageUrl.isNullOrEmpty()) {
                        // Use Glide or any image loader to load the image.
                        Glide.with(this)
                            .load(profileImageUrl)
                            .into(profileImageViewMain)
                    }
                    else   {profileImageViewMain.setImageResource(R.drawable.ic_launcher_background)}
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
