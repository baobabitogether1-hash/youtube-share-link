package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.HomeScreen
import com.example.ui.YoutubeShareViewModel
import com.example.ui.YoutubeShareViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: YoutubeShareViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the ViewModel using our custom Factory
        val factory = YoutubeShareViewModelFactory(applicationContext)
        viewModel = ViewModelProvider(this, factory)[YoutubeShareViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }

        // Handle intent if started from a SEND action (sharing a link)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            val url = extractUrlFromText(text)
            if (url != null) {
                viewModel.openCreateForm(url)
            }
        }
    }

    private fun extractUrlFromText(text: String): String? {
        val regex = "(https?://[\\w.\\-]+youtube\\.com/\\S+)|(https?://youtu\\.be/\\S+)".toRegex()
        return regex.find(text)?.value
    }
}
