package com.example.core.speech

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isReady = true
            }
        }
    }

    fun speak(text: String, speechRate: Float = 1.0f) {
        if (!isReady || text.isBlank()) return
        tts?.setSpeechRate(speechRate.coerceIn(0.6f, 1.8f))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "euee_tts_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun isSpeechRecognitionAvailable(context: Context): Boolean {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            return intent.resolveActivity(context.packageManager) != null
        }

        fun createVoiceInputIntent(prompt: String = "Speak your answer"): Intent {
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
        }
    }
}
