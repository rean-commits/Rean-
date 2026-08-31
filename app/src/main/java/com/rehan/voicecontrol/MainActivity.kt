package com.rehan.voicecontrol

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var commandProcessor: CommandProcessor

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        commandProcessor = CommandProcessor(this)
        VoiceSpeaker.init(this)

        requestNeededPermissions()

        val micButton = findViewById<Button>(R.id.micButton)
        micButton.setOnClickListener { startListening() }

        val enableAccessibilityButton = findViewById<Button>(R.id.enableAccessibilityButton)
        enableAccessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        val enableNotificationButton = findViewById<Button>(R.id.enableNotificationButton)
        enableNotificationButton.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        setupSpeechRecognizer()
        registerTapResultReceiver()
    }

    private fun registerTapResultReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val target = intent?.getStringExtra("target") ?: return
                val success = intent.getBooleanExtra("success", false)
                val message = if (success) "\"$target\" ho gaya" else "\"$target\" nahi mila screen par"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                VoiceSpeaker.speak(message)
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(VoiceAccessibilityService.ACTION_TAP_RESULT),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun requestNeededPermissions() {
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                resultText.text = "Sunaa: $text"
                statusText.text = "Command process ho raha hai..."
                commandProcessor.process(text)
            }

            override fun onError(error: Int) {
                statusText.text = "Error aaya, dobara try karo"
                VoiceSpeaker.speak("Sunai nahi diya, dobara boliye")
            }

            override fun onReadyForSpeech(params: Bundle?) {
                statusText.text = "Bolo..."
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { statusText.text = "Process ho raha hai..." }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Mic permission chahiye", Toast.LENGTH_SHORT).show()
            requestNeededPermissions()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
        }
        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        VoiceSpeaker.shutdown()
    }
}
