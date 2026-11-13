package com.kuzmix.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Voice-input lifecycle states surfaced to the composer UI.
 */
sealed interface SpeechInputState {
    data object Idle : SpeechInputState
    data object Listening : SpeechInputState
    data object Processing : SpeechInputState
    data class Unavailable(val message: String) : SpeechInputState
    data class PermissionDenied(val permanently: Boolean) : SpeechInputState
    data class Error(val message: String) : SpeechInputState
}

/**
 * Offline-first voice dictation for the chat composer.
 *
 * Tier 1 uses Android's fully on-device SpeechRecognizer
 * ([SpeechRecognizer.createOnDeviceSpeechRecognizer], API 31+) — guaranteed
 * offline. If the device does not expose an on-device service (pre-Android 12,
 * OEM/AOSP images), tier 2 falls back to the system default recognizer with
 * [RecognizerIntent.EXTRA_PREFER_OFFLINE] set, so the system service is asked
 * to process audio on-device (offline packs). The app itself never creates
 * any network connection; [SpeechInputState.Unavailable] with
 * [OFFLINE_UNAVAILABLE_MESSAGE] appears only when the device has no
 * recognition service at all.
 *
 * Recognized text is delivered through [onTextRecognized]; the controller
 * never sends anything — populating and sending stay with the UI.
 * Kept fully isolated from the AI/knowledge engine; language selection
 * can be added later via [RecognizerIntent.EXTRA_LANGUAGE] (device
 * default locale is used today).
 */
class SpeechInputController(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit
) {

    private val _state = MutableStateFlow<SpeechInputState>(SpeechInputState.Idle)
    val state: StateFlow<SpeechInputState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var released = false

    /** True when this device can recognize speech fully on-device. */
    val isOnDeviceAvailable: Boolean by lazy {
        Build.VERSION.SDK_INT >= 31 && try {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } catch (_: Throwable) {
            false
        }
    }

    /** Which recognizer kind the current [recognizer] instance is (for tier reuse). */
    private var recognizerIsOnDevice = false

    /** Toggle used by the mic button: tap to start, tap again to stop. */
    fun toggle() {
        when (_state.value) {
            SpeechInputState.Listening, SpeechInputState.Processing -> stop()
            else -> start()
        }
    }

    /**
     * Two-tier start:
     *  1. Fully on-device recognizer (API 31+) — guaranteed offline.
     *  2. System default recognizer with EXTRA_PREFER_OFFLINE — for devices
     *     that do not expose an on-device recognition service (very common:
     *     pre-Android 12 devices, OEM services, AOSP emulator images). The
     *     service is asked to stay offline; the app itself never touches
     *     the network. Only when the device has NO recognition service at
     *     all do we surface OFFLINE_UNAVAILABLE_MESSAGE.
     */
    fun start() {
        if (released) return
        if (_state.value == SpeechInputState.Listening || _state.value == SpeechInputState.Processing) return

        if (Build.VERSION.SDK_INT >= 31) {
            try {
                if (recognizer == null || !recognizerIsOnDevice) {
                    destroyRecognizer()
                    recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    recognizerIsOnDevice = true
                }
                recognizer!!.setRecognitionListener(listener)
                recognizer!!.startListening(recognitionIntent())
                _state.value = SpeechInputState.Listening
                armTimeout()
                Log.i(TAG, "listening via ON-DEVICE recognizer (sdk=${Build.VERSION.SDK_INT})")
                return
            } catch (t: Throwable) {
                Log.w(
                    TAG,
                    "on-device recognizer unavailable (sdk=${Build.VERSION.SDK_INT}, " +
                        "isOnDeviceAvailable=$isOnDeviceAvailable, " +
                        "isRecognitionAvailable=${isRecognitionAvailableSafe()})",
                    t
                )
                destroyRecognizer()
            }
        }

        try {
            if (!isRecognitionAvailableSafe()) {
                Log.w(TAG, "no speech recognition service on device (sdk=${Build.VERSION.SDK_INT})")
                _state.value = SpeechInputState.Unavailable(OFFLINE_UNAVAILABLE_MESSAGE)
                return
            }
            if (recognizer == null || recognizerIsOnDevice) {
                destroyRecognizer()
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizerIsOnDevice = false
            }
            recognizer!!.setRecognitionListener(listener)
            recognizer!!.startListening(recognitionIntent())
            _state.value = SpeechInputState.Listening
            armTimeout()
            Log.i(TAG, "listening via system recognizer, offline preferred (sdk=${Build.VERSION.SDK_INT})")
        } catch (t: Throwable) {
            Log.w(TAG, "failed to start speech recognition", t)
            destroyRecognizer()
            _state.value = SpeechInputState.Error("Couldn't start the microphone. Please try again.")
        }
    }

    private fun isRecognitionAvailableSafe(): Boolean = try {
        SpeechRecognizer.isRecognitionAvailable(context)
    } catch (_: Throwable) {
        false
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.destroy()
        } catch (_: Throwable) {
        }
        recognizer = null
    }

    fun stop() {
        cancelTimeout()
        try {
            recognizer?.cancel()
        } catch (_: Throwable) {
        }
        if (_state.value == SpeechInputState.Listening || _state.value == SpeechInputState.Processing) {
            _state.value = SpeechInputState.Idle
        }
    }

    /** Called by the UI after a RECORD_AUDIO permission request resolves. */
    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        if (released) return
        if (granted) start() else _state.value = SpeechInputState.PermissionDenied(permanentlyDenied)
    }

    /** Resets transient states (Error / Unavailable / PermissionDenied) after they were surfaced. */
    fun clearTransient() {
        when (_state.value) {
            is SpeechInputState.Error,
            is SpeechInputState.Unavailable,
            is SpeechInputState.PermissionDenied -> _state.value = SpeechInputState.Idle
            else -> Unit
        }
    }

    /** Full teardown: cancel recognition, destroy the recognizer, drop callbacks. */
    fun release() {
        released = true
        cancelTimeout()
        try {
            recognizer?.cancel()
        } catch (_: Throwable) {
        }
        destroyRecognizer()
        _state.value = SpeechInputState.Idle
    }

    private fun recognitionIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Device/default locale today; single hook for future language selection.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Ask the recognition service to process audio on-device (hint honoured
            // by Google/OEM offline packs; ignored harmlessly by on-device tier).
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

    private fun armTimeout() {
        cancelTimeout()
        timeoutJob = scope.launch {
            delay(RECOGNITION_TIMEOUT_MS)
            if (_state.value == SpeechInputState.Listening || _state.value == SpeechInputState.Processing) {
                try {
                    recognizer?.cancel()
                } catch (_: Throwable) {
                }
                _state.value = SpeechInputState.Error("Voice recognition timed out.")
            }
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            if (_state.value == SpeechInputState.Listening) {
                _state.value = SpeechInputState.Processing
            }
        }

        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onResults(results: Bundle?) {
            cancelTimeout()
            if (released) return
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isEmpty()) {
                _state.value = SpeechInputState.Error("No speech detected.")
                return
            }
            onTextRecognized(text)
            _state.value = SpeechInputState.Idle
        }

        override fun onError(error: Int) {
            cancelTimeout()
            // cancel()/release() can still deliver a late error — ignore it.
            if (released || _state.value == SpeechInputState.Idle) return
            _state.value = stateForErrorCode(error)
        }
    }

    private fun stateForErrorCode(code: Int): SpeechInputState = when (code) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            SpeechInputState.PermissionDenied(permanently = false)

        SpeechRecognizer.ERROR_AUDIO ->
            SpeechInputState.Error("Microphone is unavailable or busy.")

        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            SpeechInputState.Error("No speech detected.")

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            SpeechInputState.Error("Speech recognition is busy. Try again in a moment.")

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER ->
            // Offline pack/service couldn't process audio; surface it as an
            // offline failure instead of retrying or touching the network.
            SpeechInputState.Error("Offline speech recognition failed. Please try again.")

        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            SpeechInputState.Error("Offline speech for this language isn't installed. Download the offline voice pack in Settings.")

        else ->
            SpeechInputState.Error("Speech recognition failed. Please try again.")
    }

    companion object {
        private const val TAG = "KuzmixVoice"
        private const val RECOGNITION_TIMEOUT_MS = 20_000L

        /** Exact user-facing message required when on-device recognition is missing. */
        const val OFFLINE_UNAVAILABLE_MESSAGE = "Offline voice input isn't available on this device."

        /**
         * Appends recognized text to existing composer text without
         * destroying what the user already typed.
         */
        fun mergeDictation(existing: String, dictation: String): String {
            val recognized = dictation.trim()
            if (recognized.isEmpty()) return existing
            val current = existing.trimEnd()
            return if (current.isEmpty()) recognized else "$current $recognized"
        }
    }
}
