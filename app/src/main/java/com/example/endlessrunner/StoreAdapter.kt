package com.example.endlessrunner

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StoreAdapter(
    private val skins: List<Skin>,
    private var userCoins: Int,
    private var unlockedSkins: List<String>,
    private var equippedSkin: String?,           // Currently equipped skin ID
    private val onBuySkin: (Skin) -> Unit,
    private val onEquipSkin: (Skin) -> Unit
) : RecyclerView.Adapter<StoreAdapter.SkinViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skin, parent, false)
        return SkinViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
        val skin = skins[position]
        holder.bind(skin, userCoins, unlockedSkins, equippedSkin, onBuySkin, onEquipSkin)
    }

    override fun getItemCount(): Int = skins.size

    // Call this method to update the adapter’s internal data and refresh the list.
    fun updateData(newUserCoins: Int, newUnlockedSkins: List<String>, newEquippedSkin: String?) {
        userCoins = newUserCoins
        unlockedSkins = newUnlockedSkins
        equippedSkin = newEquippedSkin
        notifyDataSetChanged()
    }

    // Helper function: Create a colored square bitmap of a given size.
    private fun createColorSquare(color: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { this.color = color }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return bmp
    }

    class SkinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val skinImageView: ImageView = itemView.findViewById(R.id.skinImageView)
        private val skinNameTextView: TextView = itemView.findViewById(R.id.skinNameTextView)
        private val skinPriceTextView: TextView = itemView.findViewById(R.id.skinPriceTextView)
        private val skinActionButton: Button = itemView.findViewById(R.id.skinActionButton)

        fun bind(
            skin: Skin,
            userCoins: Int,
            unlockedSkins: List<String>,
            equippedSkin: String?,
            onBuySkin: (Skin) -> Unit,
            onEquipSkin: (Skin) -> Unit
        ) {
            skinNameTextView.text = skin.name

            // If the skin has no imageUrl, create a colored square.
            if (skin.imageUrl.isEmpty()) {
                val color = when (skin.id) {
                    "default" -> Color.BLUE
                    "red" -> Color.RED
                    "green" -> Color.GREEN
                    "profile" -> Color.MAGENTA
                    else -> Color.GRAY
                }
                // Create a 100x100 colored square.
                val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                val paint = Paint().apply { this.color = color }
                canvas.drawRect(0f, 0f, 100f, 100f, paint)
                skinImageView.setImageBitmap(bmp)
            } else {
                Glide.with(itemView.context)
                    .load(skin.imageUrl)
                    .into(skinImageView)
            }

            // Set the button text based on whether the skin is unlocked/equipped.
            if (unlockedSkins.contains(skin.id)) {
                if (skin.id == equippedSkin) {
                    skinActionButton.text = "Equipped"
                    skinActionButton.isEnabled = false
                    skinPriceTextView.text = ""
                } else {
                    skinActionButton.text = "Equip"
                    skinActionButton.isEnabled = true
                    skinPriceTextView.text = ""
                }
            } else {
                skinActionButton.text = "Buy"
                skinActionButton.isEnabled = true
                skinPriceTextView.text = "Price: ${skin.price}"
            }

            skinActionButton.setOnClickListener {
                if (unlockedSkins.contains(skin.id)) {
                    if (skin.id != equippedSkin) {
                        onEquipSkin(skin)
                    }
                } else {
                    onBuySkin(skin)
                }
            }
        }
    }
}
