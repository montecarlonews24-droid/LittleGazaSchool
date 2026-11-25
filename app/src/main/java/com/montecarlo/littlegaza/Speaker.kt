package com.montecarlo.littlegaza

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import java.security.MessageDigest
import java.util.Locale

/**
 * Speaks Arabic for the children.
 * 1. If a recorded clip exists for the exact sentence (assets/voice/<sha1>.ogg), it plays that — correct
 *    pronunciation on every phone, fully offline.
 * 2. Otherwise it falls back to the phone's text-to-speech (Google engine preferred, Arabic voice only).
 */
class Speaker(ctx: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    var arabicAvailable = false
        private set
    private var pending: String? = null
    private val app = ctx.applicationContext
    val googleInstalled: Boolean = isInstalled(app, GOOGLE_TTS)
    var phrases: Map<String, String> = emptyMap()

    private val clips: Set<String> = try {
        app.assets.list("voice")?.toSet() ?: emptySet()
    } catch (e: Exception) {
        emptySet()
    }
    val clipCount: Int get() = clips.size
    private var player: MediaPlayer? = null

    // Declared last so the fields above are initialised before onInit can run.
    private val tts: TextToSpeech =
        if (googleInstalled) TextToSpeech(app, this, GOOGLE_TTS) else TextToSpeech(app, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
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
        pending?.let { ttsSpeak(it) }
        pending = null
    }

    /** Speaks one of the fixed phrases from content.json ("praise", "retry", …). */
    fun say(key: String) {
        phrases[key]?.let { speak(it) }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        stop()
        val clip = clipName(text)
        if (clip in clips) playClip(clip)
        // No phone-voice fallback: it mispronounces Arabic. Sentences without a recorded clip stay silent.
    }

    private fun playClip(name: String): Boolean = try {
        val afd = app.assets.openFd("voice/$name")
        val mp = MediaPlayer()
        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
        mp.setOnCompletionListener {
            it.release()
            if (player === it) player = null
        }
        mp.prepare()
        mp.start()
        player = mp
        true
    } catch (e: Exception) {
        false
    }

    private fun ttsSpeak(text: String) {
        val c = clean(text)
        if (c.isBlank()) return
        if (!ready) {
            pending = text
            return
        }
        if (arabicAvailable) tts.speak(c, TextToSpeech.QUEUE_FLUSH, null, "lg")
    }

    fun stop() {
        pending = null
        player?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        player = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    companion object {
        const val GOOGLE_TTS = "com.google.android.tts"

        /** Must match tools/gen_audio.py: first 16 hex chars of SHA-1 of the exact UTF-8 text. */
        fun clipName(text: String): String {
            val d = MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
            val sb = StringBuilder()
            for (b in d) sb.append(String.format("%02x", b.toInt() and 0xff))
            return sb.substring(0, 16) + ".ogg"
        }

        fun isInstalled(ctx: Context, pkg: String): Boolean = try {
            ctx.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }

        /** For the phone voice: no emoji/symbols, and no tashkeel (except on single letters like بَ). */
        fun clean(text: String): String {
            val sb = StringBuilder(text.length)
            for (c in text) {
                if (Character.isSurrogate(c)) continue
                if (c == '️' || c == '‍' || c == '⁦' || c == '⁩') continue
                if (Character.getType(c) == Character.OTHER_SYMBOL.toInt()) continue
                sb.append(c)
            }
            return sb.toString().replace("…", "، ").split(" ").joinToString(" ") { w ->
                val base = w.count { it in 'ء'..'ي' }
                if (base <= 1) w else w.filterNot { it in 'ً'..'ْ' || it == 'ٰ' }
            }.trim()
        }

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
