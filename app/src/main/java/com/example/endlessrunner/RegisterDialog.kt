package com.example.endlessrunner

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class RegisterDialog(private val listener: RegisterListener) : DialogFragment() {

    interface RegisterListener {
        fun onRegister(username: String, password: String, imagePath: String?)
    }

    private var selectedImagePath: String? = null
    var profileImageView: ImageView? = null

    private val storage = FirebaseStorage.getInstance()
    private var currentPhotoFile: File? = null
    private var imageUri: Uri? = null

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
            openImageChooser()
        }

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Basic validations
            if (!isValidUsername(username)) {
                Toast.makeText(requireContext(), "Invalid username! Use 3-15 letters/numbers.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidPassword(password)) {
                Toast.makeText(requireContext(), "Invalid password! Use 6-20 characters, 1 digit, 1 letter.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Return data to activity
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
                    0 -> requestCameraPermission()
                    1 -> requestGalleryPermission()
                }
            }
            .show()
    }

    // ========================= Camera logic =========================
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            // Confirm file existence
            if (currentPhotoFile == null || !currentPhotoFile!!.exists()) {
                Toast.makeText(requireContext(), "Photo file does not exist", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            // Upload to Firebase
            uploadImageToFirebase(imageUri!!) { downloadUrl ->
                if (downloadUrl != null) {
                    selectedImagePath = downloadUrl
                    profileImageView?.setImageURI(imageUri)
                    Toast.makeText(requireContext(), "Photo captured and uploaded!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        currentPhotoFile = photoFile
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(imageUri)
    }

    private fun createImageFile(): File {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("profile_image", ".jpg", storageDir)
        println("Created image file at: ${file.absolutePath}")
        return file
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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

    // ========================= Gallery logic =========================
    private val galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Gallery permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestGalleryPermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            galleryPermissionLauncher.launch(permission)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            val galleryUri = data?.data
            if (galleryUri != null) {
                uploadImageToFirebase(galleryUri) { downloadUrl ->
                    if (downloadUrl != null) {
                        selectedImagePath = downloadUrl
                        profileImageView?.setImageURI(galleryUri)
                        Toast.makeText(requireContext(), "Image selected and uploaded!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun openGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhotoIntent, 100)
    }

    // ========================= Firebase Upload =========================
    private fun uploadImageToFirebase(uri: Uri, onResult: (String?) -> Unit) {
        val storageRef = storage.reference.child("profile_images/${System.currentTimeMillis()}.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onResult(downloadUri.toString())
                }.addOnFailureListener { exception ->
                    onResult(null)
                    println("Download URL error: ${exception.message}")
                }
            }
            .addOnFailureListener { exception ->
                onResult(null)
                println("Upload failed: ${exception.message}")
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
