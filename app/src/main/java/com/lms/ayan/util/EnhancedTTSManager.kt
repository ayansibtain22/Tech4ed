package com.lms.ayan.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID
import kotlin.collections.asSequence

class EnhancedTTSManager(
    context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val speechRate: Float = 1.0f,
    private val pitch: Float = 1.0f,
    private val preferredEngine: String? = "com.google.android.tts" // prefer Google if present
) : TextToSpeech.OnInitListener {

    interface Listener {
        fun onReadyTTS() {}
        fun onStartTTS() {}
        fun onDoneTTS() {}
        fun onErrorTTS(message: String) {}
    }

    private val appContext = context.applicationContext
    private var listener: Listener? = null
    private var tts: TextToSpeech? = null

    @Volatile
    var isReady: Boolean = false
        private set

    private var pendingText: String? = null

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    init {
        tts = try {
            if (preferredEngine.isNullOrBlank()) {
                TextToSpeech(appContext, this)
            } else {
                TextToSpeech(appContext, this, preferredEngine)
            }
        } catch (_: Throwable) {
            TextToSpeech(appContext, this)
        }
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            listener?.onErrorTTS("TextToSpeech init failed (status=$status)")
            return
        }

        engine.setSpeechRate(speechRate)
        engine.setPitch(pitch)

        // Locale selection: ur-PK → ur → EN
        val chosenLocale = when (engine.isLanguageAvailable(locale)) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> locale
            TextToSpeech.LANG_MISSING_DATA -> {
                listener?.onErrorTTS("Language data missing for ${locale.toLanguageTag()}")
                val ur = Locale("ur")
                if (engine.isLanguageAvailable(ur) >= TextToSpeech.LANG_AVAILABLE) ur else Locale.ENGLISH
            }
            else -> {
                val ur = Locale("ur")
                if (engine.isLanguageAvailable(ur) >= TextToSpeech.LANG_AVAILABLE) ur else Locale.ENGLISH
            }
        }
        engine.language = chosenLocale

        // Voice selection: prefer high quality, low latency, offline; fall back gracefully
        try {
            val voices: Set<Voice> = engine.voices.orEmpty()
            val target = voices
                .asSequence()
                .filter { v ->
                    v.locale.language.equals("ur", ignoreCase = true) &&
                            (chosenLocale.country.isBlank() ||
                                    v.locale.country.equals(chosenLocale.country, ignoreCase = true))
                }
                .sortedWith(
                    compareByDescending<Voice> { it.quality }   // better quality first
                        .thenBy { it.latency }                  // lower latency first
                        .thenBy { it.isNetworkConnectionRequired } // prefer offline voices
                        .thenBy { it.name }
                )
                .firstOrNull()

            if (target != null) engine.voice = target
        } catch (_: Throwable) {
            // Ignore voice selection errors
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { listener?.onStartTTS() }
            override fun onDone(utteranceId: String?) { listener?.onDoneTTS() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { listener?.onErrorTTS("TTS engine error") }
            override fun onError(utteranceId: String?, errorCode: Int) {
                listener?.onErrorTTS("TTS engine error (code=$errorCode)")
            }
        })

        isReady = true
        listener?.onReadyTTS()

        pendingText?.let {
            pendingText = null
            speak(it)
        }
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun speak(text: String) {
        val engine = tts ?: return
        val clean = text.trim()
        if (clean.isEmpty()) {
            listener?.onErrorTTS("Nothing to read")
            return
        }
        if (!isReady) {
            // Queue until onInit completes (fixes "not immediately read aloud")
            pendingText = clean
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val result = engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) listener?.onErrorTTS("Speak failed")
        } else {
            @Suppress("DEPRECATION")
            engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun stop() {
        tts?.stop()
        pendingText = null
    }

    fun shutdown() {
        try { tts?.stop() } catch (_: Throwable) {}
        try { tts?.shutdown() } catch (_: Throwable) {}
        tts = null
        isReady = false
        pendingText = null
        listener = null
    }

    fun promptInstallVoiceData(activity: Activity) {
        val enginePkg = tts?.defaultEngine ?: return
        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply { setPackage(enginePkg) }
        activity.startActivity(intent)
    }
}