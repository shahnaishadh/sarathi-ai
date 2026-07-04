package com.pathhelper.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.pathhelper.ai.BuildConfig
import com.pathhelper.ai.navigation.GuidanceAction
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
* Coordinates Text To Speech Manager operations and logic.
*
* Explain:
* * Purpose of the component: Manages state and calculations for Text To Speech Manager.
* * Role within the Sarthi architecture: Part of the core module supporting the Sarthi AI mobility platform.
* * Major inputs and outputs: Refer to member methods for input/output definitions.
*/
class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val activeCommands = ConcurrentHashMap<String, SpeechCommand>()

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

                    t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) {
                            val command = activeCommands[utteranceId]
                            Log.i("SARTHI_ANNOUNCEMENT", "time=${System.currentTimeMillis()} guidanceAction=${command?.action ?: "null"} announcementText=\"${command?.text ?: ""}\" speechStarted=true speechCompleted=false")
                        }

                        override fun onDone(utteranceId: String) {
                            val command = activeCommands.remove(utteranceId)
                            Log.i("SARTHI_ANNOUNCEMENT", "time=${System.currentTimeMillis()} guidanceAction=${command?.action ?: "null"} announcementText=\"${command?.text ?: ""}\" speechStarted=false speechCompleted=true")
                        }

                        override fun onError(utteranceId: String) {
                            val command = activeCommands.remove(utteranceId)
                            Log.i("SARTHI_ANNOUNCEMENT", "time=${System.currentTimeMillis()} guidanceAction=${command?.action ?: "null"} announcementText=\"${command?.text ?: ""}\" speechStarted=false speechCompleted=true")
                        }
                    })
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
    fun speak(command: SpeechCommand, frameId: Long = 0L) {
        if (!isInitialized) return

        val queueMode = if (command.priority >= 60 || command.action == GuidanceAction.STOP) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }

        Log.i("SARTHI_DEBUG", """
            [TTS_MANAGER]
            time=${System.currentTimeMillis()}
            frameId=$frameId
            action=${command.action}
            text="${command.text}"
            queueMode=${if (queueMode == TextToSpeech.QUEUE_FLUSH) "QUEUE_FLUSH" else "QUEUE_ADD"}
        """.trimIndent())

        val utteranceId = command.timestamp.toString()
        activeCommands[utteranceId] = command
        tts?.speak(command.text, queueMode, null, utteranceId)
    }

    fun isSpeaking(): Boolean {
        return isInitialized && tts?.isSpeaking == true
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
