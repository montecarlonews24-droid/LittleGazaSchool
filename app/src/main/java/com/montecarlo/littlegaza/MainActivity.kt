package com.montecarlo.littlegaza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var speaker: Speaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val content = Content.load(this)
        val store = Store(this)
        speaker = Speaker(this)
        speaker.phrases = content.phrases
        setContent { LittleGazaApp(content, store, speaker) }
    }

    override fun onPause() {
        speaker.stop()
        super.onPause()
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }
}
