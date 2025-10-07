package com.lms.ayan.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class TTSManager(
    context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val speechRate: Float = 1.0f,
    private val pitch: Float = 1.0f
) : TextToSpeech.OnInitListener {

    interface Listener {
        fun onReady()
        fun onStart()
        fun onDone()
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private var listener: Listener? = null
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    @Volatile
    var isReady: Boolean = false
        private set

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            listener?.onError("TextToSpeech init failed (status=$status).")
            return
        }

        val langResult = engine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            listener?.onError("Language ${locale.displayName} not supported on this device.")
            return
        }

        engine.setSpeechRate(speechRate)
        engine.setPitch(pitch)

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                listener?.onStart()
            }

            override fun onDone(utteranceId: String?) {
                listener?.onDone()
            }

            override fun onError(utteranceId: String?) {
                listener?.onError("TTS engine error.")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                listener?.onError("TTS engine error (code=$errorCode).")
            }
        })

        isReady = true
        listener?.onReady()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    fun speak(text: String) {
        val engine = tts ?: return
        if (!isReady) {
            listener?.onError("TTS not ready yet.")
            return
        }
        if (text.isBlank()) {
            listener?.onError("Nothing to read.")
            return
        }
        val params = Bundle().apply {
            // Why: Ensure ENGINE knows we want default stream; apps can redirect if needed.
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val utteranceId = UUID.randomUUID().toString()
        engine.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        // Why: Release engine threads; prevents leaks after Activity is destroyed.
        tts?.shutdown()
        tts = null
        isReady = false
        listener = null
    }
}