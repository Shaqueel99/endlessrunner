package com.example.endlessrunner

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction

data class Skin(
    val id: String,
    val name: String,
    val price: Int,
    var imageUrl: String // mutable if you want to update from user data
)

class StoreActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var coinTextView: TextView
    private lateinit var skinsRecyclerView: RecyclerView
    private lateinit var storeAdapter: StoreAdapter

    private var userCoins: Int = 0
    private var unlockedSkins = mutableListOf<String>()
    private var equippedSkin: String? = null

    // Example set of skins:
    private val availableSkins = listOf(
        Skin("default", "Default (Blue)", 0, ""),
        Skin("red", "Red Skin", 10, ""),
        Skin("green", "Green Skin", 10, ""),
        Skin("profile", "Profile Skin", 50, "") // will fill in with user's profile image if available
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        firestore = FirebaseFirestore.getInstance()

        coinTextView = findViewById(R.id.coinTextView)
        skinsRecyclerView = findViewById(R.id.skinsRecyclerView)
        skinsRecyclerView.layoutManager = LinearLayoutManager(this)
        val backButton: Button = findViewById(R.id.back)
        backButton.setOnClickListener { finish() }

        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)
        if (username == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            loadUserData(username)
        }
    }

    private fun loadUserData(username: String) {
        firestore.collection("users").whereEqualTo("username", username)
            .get()
            .addOnSuccessListener {documents ->
                val document = documents.firstOrNull()  // Get the first matching document
                if (document != null)  {
                    userCoins = document.getLong("coinsCollected")?.toInt() ?: 0
                    val skins = document.get("unlockedSkins") as? List<String>
                    unlockedSkins = skins?.toMutableList() ?: mutableListOf("default")
                    if (!unlockedSkins.contains("default")) {
                        unlockedSkins.add("default")
                    }
                    // If equippedSkin is null, set it to "default" and optionally update Firestore.
                    equippedSkin = document.getString("equippedSkin")
                    if (equippedSkin.isNullOrEmpty()) {
                        equippedSkin = "default"
                        firestore.collection("users").document(username)
                            .update("equippedSkin", "default")
                    }

                    // If user has a profile image, apply it to the "profile" skin.
                    val profileImageUrl = document.getString("profileImagePath") ?: ""
                    if (profileImageUrl.isNotEmpty()) {
                        val profileSkinIndex = availableSkins.indexOfFirst { it.id == "profile" }
                        if (profileSkinIndex != -1) {
                            availableSkins[profileSkinIndex].imageUrl = profileImageUrl

                        }
                    }

                    updateUI()
                    setupRecyclerView(username)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        coinTextView.text = "Coins: $userCoins"
    }

    private fun setupRecyclerView(username: String) {
        // Create the adapter, providing user data and callback lambdas for buy/equip.
        storeAdapter = StoreAdapter(
            skins = availableSkins,
            userCoins = userCoins,
            unlockedSkins = unlockedSkins,
            equippedSkin = equippedSkin,
            onBuySkin = { skin -> purchaseSkin(username, skin) },
            onEquipSkin = { skin -> equipSkin(username, skin) }
        )
        skinsRecyclerView.adapter = storeAdapter
    }
    
    private fun purchaseSkin(username: String, skin: Skin) {
        if (userCoins < skin.price) {
            Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show()
            return
        }
        val userRef = firestore.collection("users").document(username)
        firestore.runTransaction { transaction: Transaction ->
            val snapshot = transaction.get(userRef)
            val currentCoins = snapshot.getLong("coinsCollected")?.toInt() ?: 0
            if (currentCoins < skin.price) {
                throw Exception("Not enough coins")
            }
            transaction.update(userRef, "coinsCollected", currentCoins - skin.price)
            transaction.update(userRef, "unlockedSkins", FieldValue.arrayUnion(skin.id))
        }.addOnSuccessListener {
            userCoins -= skin.price
            unlockedSkins = unlockedSkins.toMutableList().apply { add(skin.id) }
            updateUI()
            storeAdapter.updateData(userCoins, unlockedSkins, equippedSkin)
            Toast.makeText(this, "Purchased ${skin.name}!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Purchase failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun equipSkin(username: String, skin: Skin) {
        firestore.collection("users")
            .whereEqualTo("username", username) // Find the correct document
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    firestore.collection("users").document(document.id)
                        .update("equippedSkin", skin.id)
                        .addOnSuccessListener {
                            equippedSkin = skin.id
                            storeAdapter.updateData(userCoins, unlockedSkins, equippedSkin)
                            Toast.makeText(this, "${skin.name} equipped!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to equip skin: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
