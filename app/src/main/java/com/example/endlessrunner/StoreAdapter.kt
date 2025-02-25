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
/**
 * RecyclerView Adapter for displaying available skins in the store.
 *
 * @property skins A list of available [Skin] objects.
 * @property userCoins The current number of coins the user has.
 * @property unlockedSkins A list of skin IDs that the user has unlocked.
 * @property equippedSkin The skin ID of the currently equipped skin.
 * @property onBuySkin Callback invoked when a skin is purchased.
 * @property onEquipSkin Callback invoked when a skin is equipped.
 */
class StoreAdapter(
    private val skins: List<Skin>,
    private var userCoins: Int,
    private var unlockedSkins: List<String>,
    private var equippedSkin: String?,           // Currently equipped skin ID
    private val onBuySkin: (Skin) -> Unit,
    private val onEquipSkin: (Skin) -> Unit
) : RecyclerView.Adapter<StoreAdapter.SkinViewHolder>() {
    /**
     * Called when RecyclerView needs a new [SkinViewHolder].
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new view.
     * @return A new instance of [SkinViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skin, parent, false)
        return SkinViewHolder(view)
    }
    /**
     * Binds the skin data to the [SkinViewHolder].
     *
     * @param holder The holder to bind data to.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
        val skin = skins[position]
        holder.bind(skin, userCoins, unlockedSkins, equippedSkin, onBuySkin, onEquipSkin)
    }
    /**
     * Returns the total number of skins in the list.
     *
     * @return The size of the skins list.
     */
    override fun getItemCount(): Int = skins.size
    /**
     * Updates the adapter's data and refreshes the list.
     *
     * @param newUserCoins The updated number of user coins.
     * @param newUnlockedSkins The updated list of unlocked skin IDs.
     * @param newEquippedSkin The updated equipped skin ID.
     */
    // Call this method to update the adapter’s internal data and refresh the list.
    fun updateData(newUserCoins: Int, newUnlockedSkins: List<String>, newEquippedSkin: String?) {
        userCoins = newUserCoins
        unlockedSkins = newUnlockedSkins
        equippedSkin = newEquippedSkin
        notifyDataSetChanged()
    }
    /**
     * Helper function to create a solid colored square bitmap.
     *
     * @param color The color of the square.
     * @param size The size of the square in pixels.
     * @return A [Bitmap] representing the colored square.
     */
    // Helper function: Create a colored square bitmap of a given size.
    private fun createColorSquare(color: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { this.color = color }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return bmp
    }
    /**
     * ViewHolder for displaying a skin item.
     *
     * @property itemView The view representing a single skin item.
     */
    class SkinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val skinImageView: ImageView = itemView.findViewById(R.id.skinImageView)
        private val skinNameTextView: TextView = itemView.findViewById(R.id.skinNameTextView)
        private val skinPriceTextView: TextView = itemView.findViewById(R.id.skinPriceTextView)
        private val skinActionButton: Button = itemView.findViewById(R.id.skinActionButton)
        /**
         * Binds the skin data to the views and sets up the action button.
         *
         * @param skin The [Skin] object to display.
         * @param userCoins The current number of coins the user has.
         * @param unlockedSkins A list of unlocked skin IDs.
         * @param equippedSkin The currently equipped skin ID.
         * @param onBuySkin Callback for buying a skin.
         * @param onEquipSkin Callback for equipping a skin.
         */
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
                    "default" -> Color.BLACK
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
