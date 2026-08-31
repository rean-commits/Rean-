package com.rehan.voicecontrol

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object VoiceSpeaker {

    private var tts: TextToSpeech? = null
    private var isReady = false

    fun init(context: Context) {
        if (tts != null) return

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val urdu = tts?.setLanguage(Locale("ur", "PK"))
                if (urdu == TextToSpeech.LANG_MISSING_DATA || urdu == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val hindi = tts?.setLanguage(Locale("hi", "IN"))
                    if (hindi == TextToSpeech.LANG_MISSING_DATA || hindi == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                }
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
