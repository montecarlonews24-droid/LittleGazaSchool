package com.montecarlo.littlegaza

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Arabic voice using the phone's built-in text-to-speech (works offline once the Arabic voice is installed). */
class Speaker(ctx: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    var arabicAvailable = false
        private set
    private var pending: String? = null
    // Declared last so the fields above are initialised before onInit can run.
    private val tts = TextToSpeech(ctx.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val r = tts.setLanguage(Locale.forLanguageTag("ar"))
        arabicAvailable = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        tts.setSpeechRate(0.85f)
        ready = true
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pending = text
            return
        }
        if (arabicAvailable) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lg")
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }
}
