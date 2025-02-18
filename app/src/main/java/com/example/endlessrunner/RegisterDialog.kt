package com.example.endlessrunner

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import java.io.File
import java.io.FileOutputStream

class RegisterDialog(private val listener: RegisterListener) : DialogFragment() {

    interface RegisterListener {
        fun onRegister(username: String, password: String, imagePath: String?)
    }

    private var selectedImagePath: String? = null
    var profileImageView: ImageView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_register, null)
        profileImageView = view.findViewById(R.id.profileImageView)

        val usernameInput = view.findViewById<EditText>(R.id.usernameInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val imageUploadButton = view.findViewById<Button>(R.id.imageUploadButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        imageUploadButton.setOnClickListener {
            openImageChooser()  // Prompt user to pick Camera or Gallery
        }

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!isValidUsername(username)) {
                Toast.makeText(requireContext(), "Invalid username! Use 3-15 letters/numbers.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                Toast.makeText(requireContext(), "Invalid password! Use 6-20 characters, 1 digit, 1 letter.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            listener.onRegister(username, password, selectedImagePath)
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }

    private fun openImageChooser() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestCameraPermission() // "Take Photo"
                    1 -> openGallery()             // "Choose from Gallery"
                }
            }
            .show()
    }

    // Camera capture using Activity Result API.
    private var imageUri: Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            selectedImagePath = imageUri.toString()
            profileImageView?.setImageURI(imageUri)  // Update preview
            Toast.makeText(requireContext(), "Photo captured!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(imageUri)
    }

    private fun createImageFile(): File {
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile("profile_image", ".jpg", storageDir)
    }

    private fun openGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhotoIntent, 100)  // Use legacy onActivityResult for gallery
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Use onActivityResult for gallery selection only.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            val imageUri = data?.data
            selectedImagePath = imageUri?.toString()
            profileImageView?.setImageURI(imageUri)
            Toast.makeText(requireContext(), "Image selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidUsername(username: String): Boolean {
        return username.matches(Regex("^[a-zA-Z0-9]{3,15}$"))
    }

    private fun isValidPassword(password: String): Boolean {
        return password.matches(Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$"))
    }

    private fun saveImageToCache(bitmap: Bitmap): File {
        val file = File(requireContext().cacheDir, "profile_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }
}
