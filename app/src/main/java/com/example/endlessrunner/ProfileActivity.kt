    package com.example.endlessrunner

    import android.content.Intent
    import android.content.pm.PackageManager
    import android.net.Uri
    import android.os.Build
    import android.os.Bundle
    import android.os.Environment
    import android.provider.MediaStore
    import android.text.InputType
    import android.util.Log
    import android.widget.Button
    import android.widget.EditText
    import android.widget.ImageView
    import android.widget.LinearLayout
    import android.widget.TextView
    import android.widget.Toast
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AlertDialog
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.core.content.FileProvider
    import com.bumptech.glide.Glide
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.storage.FirebaseStorage
    import java.io.File
    import java.io.IOException
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    /**
     * Activity for displaying and editing the user profile.
     * Allows the user to change their name, password, and profile picture.
     */
    class ProfileActivity : AppCompatActivity() {

        // Firebase Firestore and Storage instances
        private lateinit var firestore: FirebaseFirestore
        private lateinit var storage: FirebaseStorage
        private lateinit var profileImageView: ImageView
        private lateinit var nameTextView: TextView
        private lateinit var coinsTextView: TextView
        var profileImageUrl: String? = null
        /**
         * Called when the activity is created.
         * Initializes Firebase, loads user data, and sets up UI event listeners.
         *
         * @param savedInstanceState The saved instance state bundle.
         */
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
                firestore.collection("users").whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener {
                            documents ->
                        val document = documents.firstOrNull()  // Get the first matching document
                        if (document != null) {
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
                showChangeNameDialog()
            }

            // Change Password
            findViewById<Button>(R.id.changePasswordProfileButton).setOnClickListener {
                showChangePasswordDialog()
            }

            // Back Button
            findViewById<Button>(R.id.backProfileButton).setOnClickListener {
                val intent = Intent(this, MainMenuActivity::class.java)
                startActivity(intent)
                finish()  // Returns to the previous screen
            }
        }

        // ======================= NAME ==================================
        /**
         * Displays a dialog allowing the user to change their display name.
         */
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
                    Toast.makeText(this, "Invalid username! Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Validate the new name
                if (!isValidUsername(newName)) {
                    Toast.makeText(this, "Invalid username! Use 3-15 letters/numbers.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check if the name exists in the database
                checkIfNameExists(newName) { exists ->
                    if (exists) {
                        Toast.makeText(this, "Invalid username! Name already exists", Toast.LENGTH_SHORT).show()
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

        /**
         * Checks if the new username already exists in the database.
         *
         * @param newName The new username to check.
         * @param callback Callback returning true if the name exists.
         */
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

        /**
         * Updates the user's name in both the users collection and leaderboard.
         *
         * @param newName The new username.
         */
        private fun updateNameInDatabase(newName: String) {
            // Get Username
            val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val username = sharedPrefs.getString("username", null)

            if(username!= null)
            {
                    // Change name
                    firestore.collection("users").whereEqualTo("username", username)
                        .get()
                        .addOnSuccessListener {
                                documents ->
                                    val document = documents.firstOrNull()  // Get the first matching document
                                    if (document != null) {
                                        firestore.collection("users").document(document.id)
                                            .update("username", newName)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    this,
                                                    "username updated to $newName",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .addOnFailureListener { exception ->
                                                Toast.makeText(
                                                    this,
                                                    "Failed to update name in users: ${exception.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "No document found for username $username",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                    // Set leaderboard
                    firestore.collection("leaderboard")
                        .whereEqualTo("name", username) // Find the correct document
                        .get()
                        .addOnSuccessListener { documents ->
                            val document = documents.firstOrNull()  // Get the first matching document
                            if (document != null) {
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
                            } else {
                                Toast.makeText(
                                    this,
                                    "No document found for username $username",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                    Toast.makeText(this, "Name updated successfully to $newName", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating name: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            // Update shared preference

            val editor = sharedPrefs.edit()
            editor.putString("username", newName)  // Update username with newName
            editor.apply()
            val test = sharedPrefs.getString("username",null)
            if(test != null)
            {
                Log.d("share pref name: ",test)
            }
            Log.d("New name: ",newName)
            nameTextView.text = newName
        }

        // ======================= PASSWORD =============================
        /**
         * Displays a dialog for changing the user's password.
         * Validates the old password and ensures the new password meets requirements.
         */
        private fun showChangePasswordDialog() {
            // Create a LinearLayout to hold the EditText fields
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL

            // Create the EditText fields
            val oldPasswordEditText = EditText(this)
            oldPasswordEditText.hint = "Enter old password"
            oldPasswordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

            val newPasswordEditText = EditText(this)
            newPasswordEditText.hint = "Enter new password"
            newPasswordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

            val confirmPasswordEditText = EditText(this)
            confirmPasswordEditText.hint = "Confirm new password"
            confirmPasswordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

            // Add EditText fields to the LinearLayout
            layout.addView(oldPasswordEditText)
            layout.addView(newPasswordEditText)
            layout.addView(confirmPasswordEditText)

            // Set up an alert dialog
            val dialog = AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(layout) // Set the LinearLayout with the EditText fields
                .setPositiveButton("Change") {
                                             dialogInterface, i ->
                    val oldPassword = oldPasswordEditText.text.toString()
                    val newPassword = newPasswordEditText.text.toString()
                    val newPassword2 = confirmPasswordEditText.text.toString()

                    // Check if password from database is correct
                    // Get Current user's data from database
                    val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val username = sharedPrefs.getString("username", null)
                    firestore.collection("users").whereEqualTo("username", username)
                        .get()
                        .addOnSuccessListener {
                                documents ->
                            val document = documents.firstOrNull()  // Get the first matching document
                            if (document != null) {
                                // get the whole entry
                                firestore.collection("users").document(document.id)
                                    .get()
                                    .addOnSuccessListener { userDocument ->
                                        // get password from current entry
                                        val password = userDocument.getString("hashedPassword")
                                        if (password != null)
                                        {
                                            // check if password is correct and new password valid
                                            if(password == HashUtil.hashPassword(oldPassword) &&
                                                isValidPassword(newPassword) &&
                                                newPassword2 == newPassword)
                                            {
                                                // update database
                                                firestore.collection("users").document(document.id)
                                                    // Change password
                                                    .update("hashedPassword", HashUtil.hashPassword(newPassword))
                                                    .addOnSuccessListener {
                                                        Toast.makeText(this, "Password successfully changed", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            else
                                            {
                                                // issue correct error message
                                                if(password != HashUtil.hashPassword(oldPassword))
                                                    Toast.makeText(this, "Incorrect old password", Toast.LENGTH_SHORT).show()
                                                if(!isValidPassword(newPassword))
                                                    Toast.makeText(this, "Invalid new password! Use 6-20 characters, 1 digit, 1 letter.", Toast.LENGTH_SHORT).show()
                                                if(newPassword2 != newPassword)
                                                    Toast.makeText(this, "Invalid new password! Both are different.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        else
                                        {
                                            // Password doesn't exist or is null
                                            println("Password not found.")
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            this,
                                            "Failed to obtained entry: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }

                // ======================= CAMERA ==================================
                private fun showImageSelectionDialog() {
                    val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
                    AlertDialog.Builder(this)
                        .setTitle("Change Profile Picture")
                        .setItems(options) { dialog, which ->
                            when (which) {
                                0 -> checkCameraPermission()
                                1 -> checkStoragePermission()
                                2 -> dialog.dismiss()
                            }
                        }
                        .show()
                }
                private val CAMERA_PERMISSION_CODE = 101
                private val STORAGE_PERMISSION_CODE = 102
                private val REQUEST_IMAGE_CAPTURE = 1
                private val REQUEST_IMAGE_PICK = 2
                private var imageUri: Uri? = null

                private fun checkCameraPermission() {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(android.Manifest.permission.CAMERA),
                            CAMERA_PERMISSION_CODE
                        )
                    } else {
                        openCamera()
                    }
                }

                private fun checkStoragePermission() {
                    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    if (ContextCompat.checkSelfPermission(this, storagePermission)
                        != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(storagePermission),
                            STORAGE_PERMISSION_CODE
                        )
                    } else {
                        openGallery()
                    }
                }


                private fun openCamera() {
                    val photoFile: File? = createImageFile()
                    if (photoFile != null) {
                        imageUri = FileProvider.getUriForFile(
                            this,
                            "${applicationContext.packageName}.provider",
                            photoFile
                        )

                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                        }
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
                    } else {
                        Toast.makeText(this, "Unable to create image file", Toast.LENGTH_SHORT).show()
                    }
                }

                // ======================= GALLERY ==================================
                private fun openGallery() {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    intent.type = "image/*"
                    startActivityForResult(intent, REQUEST_IMAGE_PICK)
                }

        /**
         * Handles the result from image capture or gallery pick.
         */
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
        /**
         * Handles permission request results for camera and storage.
         */
        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            when (requestCode) {
                CAMERA_PERMISSION_CODE -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        openCamera()
                    } else {
                        Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
                    }
                }
                STORAGE_PERMISSION_CODE -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        openGallery()
                    } else {
                        Toast.makeText(this, "Storage permission is required to pick an image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        /**
         * Uploads the selected image to Firebase Storage.
         *
         * @param imageUri The URI of the image to upload.
         */
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
        /**
         * Saves the downloaded image URL to Firestore.
         *
         * @param imageUrl The URL of the uploaded image.
         */
        private fun saveImageUrlToFirestore(imageUrl: String) {
            val username =
                getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null)
            if (username != null)
            {
                firestore.collection("users")
                    .whereEqualTo("username", username) // Find the correct document
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            firestore.collection("users").document(document.id)
                                .update("profileImagePath", imageUrl)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Profile image in user updated",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "in user, Failed to update profile image: ${exception.message}",
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
        /**
         * Creates a temporary image file for capturing a photo.
         *
         * @return The created image [File] or null if creation fails.
         */
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


        // ========================= Validation =========================
        /**
         * Validates the username format.
         *
         * @param username The username to validate.
         * @return True if valid, false otherwise.
         */
        private fun isValidUsername(username: String): Boolean {
            return username.matches(Regex("^[a-zA-Z0-9]{3,15}$"))
        }
        /**
         * Validates the password format.
         *
         * @param password The password to validate.
         * @return True if valid, false otherwise.
         */
        private fun isValidPassword(password: String): Boolean {
            return password.matches(Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$"))
        }
    }