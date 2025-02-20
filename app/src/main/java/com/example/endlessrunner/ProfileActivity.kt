    package com.example.endlessrunner

    import android.content.Intent
    import android.content.pm.PackageManager
    import android.net.Uri
    import android.os.Bundle
    import android.os.Environment
    import android.provider.MediaStore
    import android.text.InputType
    import android.widget.Button
    import android.widget.EditText
    import android.widget.ImageView
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AlertDialog
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.content.FileProvider
    import com.bumptech.glide.Glide
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.storage.FirebaseStorage
    import java.io.File
    import java.io.IOException
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale

    class ProfileActivity : AppCompatActivity() {

        // Firebase Firestore and Storage instances
        private lateinit var firestore: FirebaseFirestore
        private lateinit var storage: FirebaseStorage
        private lateinit var profileImageView: ImageView
        private lateinit var nameTextView: TextView
        private lateinit var coinsTextView: TextView
        var profileImageUrl: String? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_profile)

            // Initialize Firestore & Storage
            firestore = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()

            // Get Username
            val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val username = sharedPrefs.getString("username", null)

            // Find Text UI elements from layout
            nameTextView = findViewById(R.id.nameProfileText)
            coinsTextView = findViewById(R.id.coinProfileText)

            // Find Profile Pic UI element from layout
            profileImageView = findViewById(R.id.imageProfileView)

            // Retrieve from Database
            if (username != null) {
                firestore.collection("users").document(username)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Name
                            nameTextView.text = "$username"

                            // Coins
                            val coins = document.getLong("coinsCollected") ?: 0L
                            coinsTextView.text = "Coins: $coins"

                            // Profile Pic
                            profileImageUrl = document.getString("profileImagePath")
                            if (!profileImageUrl.isNullOrEmpty()) {
                                // Use Glide or any image loader to load the image.
                                Glide.with(this)
                                    .load(profileImageUrl)
                                    .into(profileImageView)
                            } else {
                                profileImageView.setImageResource(R.drawable.ic_launcher_background)
                            }
                        }
                    }
            }

            // Change Profile pic
            findViewById<Button>(R.id.changePhotoProfileButton).setOnClickListener {
                showImageSelectionDialog()
            }

            // Change Name
            findViewById<Button>(R.id.changeNameProfileButton).setOnClickListener {
                //showChangeNameDialog()
            }

            // Change Password
            findViewById<Button>(R.id.changePasswordProfileButton).setOnClickListener {
                //showChangePasswordDialog()
            }

            // Back Button
            findViewById<Button>(R.id.backProfileButton).setOnClickListener {
                with(sharedPrefs.edit()) {
                    putString("username", username)
                    if (profileImageUrl != null) {
                        putString("profileImageUrl", profileImageUrl)
                    }
                    apply()
                }
                finish()  // Returns to the previous screen
            }
        }
        private fun showChangeNameDialog() {
            // Create an AlertDialog for input
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Change Name")

            // Set up the input field for the new name
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            // Set up "OK" button action
            builder.setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString().trim()

                // Validate the new name
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Validate the new name
                if (!isValidUsername(newName)) {
                    Toast.makeText(this, "Name is not valid", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check if the name exists in the database
                checkIfNameExists(newName) { exists ->
                    if (exists) {
                        Toast.makeText(this, "Name already exists", Toast.LENGTH_SHORT).show()
                    } else {
                        // Update the name in the database
                        updateNameInDatabase(newName)
                    }
                }
            }

            // Set up "Cancel" button action
            builder.setNegativeButton("Cancel", null)

            // Show the dialog
            builder.show()
        }

        // Function to check if the name exists in the database
        private fun checkIfNameExists(newName: String, callback: (Boolean) -> Unit) {
            val database = FirebaseFirestore.getInstance()
            val userRef = database.collection("users").whereEqualTo("username", newName)

            userRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documents = task.result
                    callback(documents?.isEmpty == false) // True if the name exists
                } else {
                    callback(false) // If error happens, assume the name doesn't exist
                }
            }
        }

        // Function to update the name in the database
        private fun updateNameInDatabase(newName: String) {
            // Get Username
            val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val username = sharedPrefs.getString("username", null)

            val database = FirebaseFirestore.getInstance()
            if(username!= null){
            val userRef = database.collection("users").document(username)
            userRef.update("username", newName)
                .addOnSuccessListener {
                    // Name

                    // Set leaderboard
                    firestore.collection("leaderboard")
                        .whereEqualTo("name", username) // Find the correct document
                        .get()
                        .addOnSuccessListener {
                                documents ->
                            for (document in documents) {
                                firestore.collection("leaderboard").document(document.id)
                                    .update("name", newName)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Name in leaderboard updated",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            this,
                                            "Failed to update name in leaderboard: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                                exception ->
                            Toast.makeText(
                                this,
                                "Failed to set leaderboard name: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show()
                    with(sharedPrefs.edit()) {
                        putString("username", newName)
                        apply()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating name in firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun showChangePasswordDialog() {
            TODO("Not yet implemented")
        }

        private fun showImageSelectionDialog() {
            val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
            AlertDialog.Builder(this)
                .setTitle("Change Profile Picture")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> openCamera()
                        1 -> openGallery()
                        2 -> dialog.dismiss()
                    }
                }
                .show()
        }
        // ======================= CAMERA ==================================
        private val REQUEST_IMAGE_CAPTURE = 1
        private val REQUEST_IMAGE_PICK = 2
        private var imageUri: Uri? = null

        private fun openCamera() {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                val photoFile: File? = createImageFile()
                if (photoFile != null) {
                    imageUri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.provider",
                        photoFile
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
                }
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        }

        // ======================= GALLERY ==================================
        private fun openGallery() {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == RESULT_OK) {
                when (requestCode) {
                    REQUEST_IMAGE_CAPTURE -> {
                        imageUri?.let { uri ->
                            Glide.with(this)
                                .load(uri)
                                .into(profileImageView)
                            uploadImageToFirebase(uri)
                        }
                    }
                    REQUEST_IMAGE_PICK -> {
                        data?.data?.let { uri ->
                            Glide.with(this)
                                .load(uri)
                                .into(profileImageView)
                            uploadImageToFirebase(uri)
                        }
                    }
                }
            }
        }

        private fun uploadImageToFirebase(imageUri: Uri) {
            val storageRef = storage.reference
            val username = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null)
            if (username != null) {
                val imageRef = storageRef.child("profile_images/$username.jpg")
                imageRef.putFile(imageUri)
                    .addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            saveImageUrlToFirestore(uri.toString())
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun saveImageUrlToFirestore(imageUrl: String) {
            val username =
                getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null)
            if (username != null) {
                firestore.collection("users").document(username)
                    .update("profileImagePath", imageUrl)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile image in user updated", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "in user,Failed to update profile image: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                firestore.collection("leaderboard")
                    .whereEqualTo("name", username) // Find the correct document
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            firestore.collection("leaderboard").document(document.id)
                                .update("profileImageUrl", imageUrl)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Profile image in leaderboard updated",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(
                                        this,
                                        "Failed to update profile image in leaderboard: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Failed to find leaderboard entry: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        private fun createImageFile(): File? {
            return try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        // ======================= NAME ==================================
        private fun updateUserName(newName: String) {
            val username = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null)

            if (username != null) {
                val userDocRef = firestore.collection("users").document(username)

                userDocRef.update("name", newName)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to update name: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // ========================= Validation =========================
        private fun isValidUsername(username: String): Boolean {
            return username.matches(Regex("^[a-zA-Z0-9]{3,15}$"))
        }

        private fun isValidPassword(password: String): Boolean {
            return password.matches(Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$"))
        }
    }