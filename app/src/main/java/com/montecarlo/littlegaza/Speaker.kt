package com.montecarlo.littlegaza

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Arabic voice using the phone's text-to-speech.
 * - Prefers Google's engine (much better Arabic than some built-in engines).
 * - Picks an installed, offline Arabic voice explicitly, so it never reads Arabic with a foreign voice.
 * - Strips emoji before speaking, so the engine never reads emoji names aloud.
 */
class Speaker(ctx: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    var arabicAvailable = false
        private set
    var usingGoogle = false
        private set
    private var pending: String? = null
    private val app = ctx.applicationContext
    val googleInstalled: Boolean = isInstalled(app, GOOGLE_TTS)

    // Declared last so the fields above are initialised before onInit can run.
    private val tts: TextToSpeech =
        if (googleInstalled) TextToSpeech(app, this, GOOGLE_TTS) else TextToSpeech(app, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        usingGoogle = try { tts.defaultEngine == GOOGLE_TTS || googleInstalled } catch (e: Exception) { false }
        val r = tts.setLanguage(Locale.forLanguageTag("ar"))
        var ok = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        val best = try {
            tts.voices
                ?.filter {
                    it.locale.language == "ar" &&
                        !it.isNetworkConnectionRequired &&
                        !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                }
                ?.maxByOrNull { it.quality }
        } catch (e: Exception) {
            null
        }
        if (best != null) {
            tts.voice = best
            ok = true
        }
        val current = try { tts.voice } catch (e: Exception) { null }
        arabicAvailable = ok && (current == null || current.locale.language == "ar")
        tts.setSpeechRate(0.85f)
        ready = true
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        val clean = clean(text)
        if (clean.isBlank()) return
        if (!ready) {
            pending = clean
            return
        }
        if (arabicAvailable) tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "lg")
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }

    companion object {
        const val GOOGLE_TTS = "com.google.android.tts"

        fun isInstalled(ctx: Context, pkg: String): Boolean = try {
            ctx.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }

        /** Removes emoji, symbols and direction marks so only words are spoken. */
        fun clean(text: String): String {
            val sb = StringBuilder(text.length)
            for (c in text) {
                if (Character.isSurrogate(c)) continue
                if (c == '️' || c == '‍' || c == '⁦' || c == '⁩') continue
                if (Character.getType(c) == Character.OTHER_SYMBOL.toInt()) continue
                sb.append(c)
            }
            // No tashkeel: Google's Arabic voice reads plain text more naturally
            // (with tashkeel it said "العربيتي" / "أحسنتو"). Single letters keep their
            // harakah so the harakat lessons still say "بَ / بُ / بِ".
            return sb.toString().replace("…", "، ").split(" ").joinToString(" ") { w ->
                val base = w.count { it in '\u0621'..'\u064A' }
                if (base <= 1) w else w.filterNot { it in '\u064B'..'\u0652' || it == '\u0670' }
            }.trim()
        }

        /** Opens the phone's text-to-speech settings. */
        fun openTtsSettings(ctx: Context) {
            try {
                ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
            } catch (e: Exception) {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {
                }
            }
        }

        /** Opens Google's screen for downloading the Arabic voice (or its Play Store page if missing). */
        fun installArabicVoice(ctx: Context) {
            if (isInstalled(ctx, GOOGLE_TTS)) {
                try {
                    ctx.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(GOOGLE_TTS))
                    return
                } catch (_: Exception) {
                }
            }
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_TTS")))
            } catch (e: Exception) {
                openLink(ctx, "https://play.google.com/store/apps/details?id=$GOOGLE_TTS")
            }
        }
    }
}
