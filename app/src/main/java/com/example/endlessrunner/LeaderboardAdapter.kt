package com.example.endlessrunner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
/**
 * RecyclerView Adapter for displaying leaderboard entries.
 *
 * @property entries A list of [LeaderboardEntry] objects.
 */
class LeaderboardAdapter(private val entries: List<LeaderboardEntry>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {
    /**
     * ViewHolder that holds references to the views for each leaderboard item.
     *
     * @param itemView The view representing one leaderboard item.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankingText: TextView = itemView.findViewById(R.id.rankingText)
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val nameText: TextView = itemView.findViewById(R.id.nameText)
        val scoreText: TextView = itemView.findViewById(R.id.scoreText)
    }
    /**
     * Called when the RecyclerView needs a new ViewHolder.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new view.
     * @return A new [ViewHolder] instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_item, parent, false)
        return ViewHolder(view)
    }
    /**
     * Binds the leaderboard entry data to the ViewHolder's views.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        // Display rank as position+1
        holder.rankingText.text = "${position + 1}."
        holder.nameText.text = entry.name
        holder.scoreText.text = "Score: ${entry.score}"
        if (!entry.profileImageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(entry.profileImageUrl)
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }
    /**
     * Returns the total number of leaderboard entries.
     *
     * @return The size of the entries list.
     */
    override fun getItemCount(): Int = entries.size
}
