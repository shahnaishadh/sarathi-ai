package com.pathhelper.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.navigation.GuidanceAction
import java.util.Locale
/**
* Coordinates Text To Speech Manager operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Text To Speech Manager.
* * Role within the Sarathi architecture: Part of the core module supporting the Sarathi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { t ->
                val result = t.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeechManager", "Language US is not supported or missing resources.")
                } else {
                    t.setSpeechRate(1.2f) // Optimized for assistive responsiveness
                    isInitialized = true
                }
            }
        } else {
            Log.e("TextToSpeechManager", "TextToSpeech engine initialization failed status: $status")
        }
    }

    /**
     * Speaks the given command.
     * 
     * LATENCY FIX:
     * We use QUEUE_FLUSH for any command with priority >= 60 (STOP, MOVE_LEFT, MOVE_RIGHT).
     * This ensures that safety-critical navigation instructions interrupt longer
     * informational descriptions immediately.
     */
    fun speak(command: SpeechCommand) {
        if (!isInitialized) return

        if (BuildConfig.DEBUG) {
            Log.d("TTS_QUEUE", "text=${command.text} action=${command.action} priority=${command.priority}")
        }

        val queueMode = if (command.priority >= 60 || command.action == GuidanceAction.STOP) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }

        tts?.speak(command.text, queueMode, null, command.timestamp.toString())
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
