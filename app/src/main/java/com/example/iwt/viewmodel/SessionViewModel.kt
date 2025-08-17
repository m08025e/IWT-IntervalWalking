package com.example.iwt.viewmodel

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.iwt.sensor.ActivityRecognitionManager
import com.example.iwt.sensor.CadenceEstimator
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

data class UiState(
    // Session state
    val fastPhase: Boolean = true,
    val set: Int = 0,
    val formHint: String = "",
    val targetSpm: Int = 120,
    val targetSpmRange: IntRange = 115..125,

    // Common state
    val remainingSeconds: Int = 180,
    val cadenceSpm: Int = 0,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val avgSpm: Int = 0
) {
    val remainingMinutes: Int get() = remainingSeconds / 60
    val remainingSecondsPart: String get() = (remainingSeconds % 60).toString().padStart(2,'0')
}

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorManager = app.getSystemService(SensorManager::class.java)
    private lateinit var tts: TextToSpeech
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    private lateinit var cadenceEstimator: CadenceEstimator
    private lateinit var activityRecognitionManager: ActivityRecognitionManager


    private var metronomeJob: Job? = null

    init {
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.JAPANESE
            }
        }
        cadenceEstimator = CadenceEstimator(sensorManager) { spm ->
            _ui.value = _ui.value.copy(cadenceSpm = spm)
        }
        activityRecognitionManager = ActivityRecognitionManager(app) { isWalking ->
            cadenceEstimator.setWalkingState(isWalking)
        }
    }

    private val _ui = MutableStateFlow(UiState())
    val uiState = _ui.asStateFlow()

    private var running = false
    private var sumSpm = 0
    private var samples = 0

    private fun resetState() {
        _ui.value = UiState()
        sumSpm = 0
        samples = 0
        running = false
    }

    fun start(fastSpm: Int, slowSpm: Int) {
        if (running) return
        resetState()
        running = true
        cadenceEstimator.start()
        activityRecognitionManager.start()
        startTimer(fastSpm, slowSpm)
        startMetronome()
    }

    fun togglePause() { _ui.value = _ui.value.copy(paused = !_ui.value.paused) }

    fun cancel() {
        running = false
        metronomeJob?.cancel()
        cadenceEstimator.stop()
        activityRecognitionManager.stop()
    }

    private fun startMetronome() {
        metronomeJob?.cancel()
        metronomeJob = viewModelScope.launch(Dispatchers.Default) {
            while (running) {
                if (!uiState.value.paused && uiState.value.targetSpm > 0) {
                    val delayMs = 60000L / uiState.value.targetSpm
                    try {
                        tone.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                    } catch (_: Exception) {}
                    delay(delayMs)
                } else {
                    delay(100) // Check again in 100ms if paused
                }
            }
        }
    }

    private fun startTimer(fastSpm: Int, slowSpm: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            var fast = true
            var remain = 180
            var totalSets = 0

            // Initial phase setup
            setPhase(fast, fastSpm, slowSpm)

            while (running) {
                if (!_ui.value.paused) {
                    remain -= 1
                    if (remain <= 0) {
                        fast = !fast
                        remain = 180
                        if (fast) {
                            totalSets += 1
                        }
                        setPhase(fast, fastSpm, slowSpm, totalSets)
                    }

                    // SPM calculation is now handled by CadenceEstimator, just need to update avg
                    sumSpm += uiState.value.cadenceSpm
                    samples += 1

                    _ui.value = _ui.value.copy(
                        remainingSeconds = remain,
                        set = totalSets
                    )
                    if (remain == 10) {
                        val nextPhaseIsFast = !fast
                        if (nextPhaseIsFast) {
                            speak("まもなく速歩に切り替わります")
                        } else {
                            speak("まもなくスローに切り替わります")
                        }
                    }
                }
                delay(1000)
                if (totalSets >= 5) {
                    running = false
                    val avg = if (samples > 0) sumSpm / samples else 0
                    _ui.value = _ui.value.copy(finished = true, avgSpm = avg)
                    speak("お疲れ様でした。")
                    cancel()
                }
            }
        }
    }

    private fun setPhase(fast: Boolean, fastSpm: Int, slowSpm: Int, sets: Int = 0) {
        val target: Int
        val announcement: String

        if (fast) {
            target = fastSpm
            announcement = if (sets > 0) "次の速歩を始めます" else "速歩を始めます"
        } else {
            target = slowSpm
            announcement = "スローに切り替えます"
        }

        _ui.value = _ui.value.copy(
            fastPhase = fast,
            targetSpm = target,
            targetSpmRange = target-5..target+5,
            formHint = ""
        )
        speak(announcement)
    }

    private fun speak(text: String) { try { tts.speak(text, TextToSpeech.QUEUE_ADD, null, "iwt") } catch (_: Exception) {} }
    private fun beep() { try { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200) } catch (_: Exception) {} }

    override fun onCleared() {
        super.onCleared()
        metronomeJob?.cancel()
        cadenceEstimator.stop()
        activityRecognitionManager.stop()
        try {
            tts.shutdown()
        } catch (_: Exception) {
        }
        try {
            tone.release()
        } catch (_: Exception) {
        }
    }
}