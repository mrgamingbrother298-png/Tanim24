package com.example.nova.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.nova.model.AssistantState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onStateChanged: (AssistantState) -> Unit
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isSpeakingEnabled = MutableStateFlow(true)
    val isSpeakingEnabled: StateFlow<Boolean> = _isSpeakingEnabled.asStateFlow()

    init {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onStateChanged(AssistantState.SPEAKING)
                }

                override fun onDone(utteranceId: String?) {
                    onStateChanged(AssistantState.IDLE)
                }

                override fun onError(utteranceId: String?) {
                    onStateChanged(AssistantState.IDLE)
                }
            })
            isTtsReady = true
        }
    }

    fun toggleSpeaking(enabled: Boolean) {
        _isSpeakingEnabled.value = enabled
        if (!enabled) {
            stopSpeaking()
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStateChanged(AssistantState.ERROR)
            return
        }

        try {
            stopSpeaking()
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onStateChanged(AssistantState.LISTENING)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        onStateChanged(AssistantState.THINKING)
                    }

                    override fun onError(error: Int) {
                        onStateChanged(AssistantState.IDLE)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()
                        if (!spokenText.isNullOrBlank()) {
                            onSpeechRecognized(spokenText)
                        } else {
                            onStateChanged(AssistantState.IDLE)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening to NOVA command...")
            }
            speechRecognizer?.startListening(intent)
            onStateChanged(AssistantState.LISTENING)
        } catch (_: Exception) {
            onStateChanged(AssistantState.ERROR)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        onStateChanged(AssistantState.IDLE)
    }

    fun speak(text: String) {
        if (!_isSpeakingEnabled.value || !isTtsReady) return
        val cleanText = text.replace(Regex("[*#_`•]"), " ").trim()
        if (cleanText.isBlank()) return

        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "NOVA_UTTERANCE_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        onStateChanged(AssistantState.IDLE)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
