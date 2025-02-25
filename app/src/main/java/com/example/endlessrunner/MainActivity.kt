package com.example.endlessrunner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
/**
 * Called when the activity is created.
 * Initializes the activity by setting the content view to the game view.
 *
 * @param savedInstanceState The saved instance state bundle.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the GameView as the content view
        setContentView(GameView(this))
    }
}
