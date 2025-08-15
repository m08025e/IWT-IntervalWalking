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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

data class UiState(
    val fastPhase: Boolean = true,
    val remainingSeconds: Int = 180,
    val cadenceSpm: Int = 0,
    val formHint: String = "胸を張り 目線は水平",
    val paused: Boolean = false,
    val finished: Boolean = false,
    val avgSpm: Int = 0,
    val set: Int = 0
) {
    val remainingMinutes: Int get() = remainingSeconds / 60
    val remainingSecondsPart: String get() = (remainingSeconds % 60).toString().padStart(2,'0')
}

class SessionViewModel(app: Application) : AndroidViewModel(app), SensorEventListener {
    private val sensorManager = app.getSystemService(SensorManager::class.java)
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val tts = TextToSpeech(app) { status -> if (status == TextToSpeech.SUCCESS) tts.language = Locale.JAPANESE }
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 60)

    private val _ui = MutableStateFlow(UiState())
    val uiState = _ui.asStateFlow()

    private var running = false
    private var stepsInWindow = 0
    private var lastPeakTime = 0L
    private var lastZ = 0f
    private var sumSpm = 0
    private var samples = 0

    fun start() {
        if (running) return
        running = true
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        speak("速歩を始めます")
        startTimer()
    }

    fun togglePause() { _ui.value = _ui.value.copy(paused = !_ui.value.paused) }

    fun cancel() {
        running = false
        sensorManager.unregisterListener(this)
        try { tts.shutdown() } catch (_: Exception) {}
        try { tone.release() } catch (_: Exception) {}
    }

    private fun startTimer() {
        viewModelScope.launch(Dispatchers.Default) {
            var fast = true
            var remain = 180
            var totalSets = 0
            while (running) {
                if (!_ui.value.paused) {
                    remain -= 1
                    if (remain <= 0) {
                        fast = !fast
                        remain = 180
                        if (fast) { totalSets += 1; speak("次の速歩を始めます") }
                        else { speak("スローに切り替えます") }
                    }
                    val spm = stepsInWindow * 2  // rough estimate; update every second
                    sumSpm += spm; samples += 1
                    _ui.value = _ui.value.copy(
                        fastPhase = fast,
                        remainingSeconds = remain,
                        cadenceSpm = spm,
                        formHint = if (fast) "胸を張り 肘を後ろへ引く" else "呼吸を整え 歩幅を小さく",
                        set = totalSets
                    )
                    if (remain == 10) beep()
                }
                stepsInWindow = 0
                delay(1000)
                if (totalSets >= 5 && fast && remain == 180) {
                    running = false
                    val avg = if (samples>0) sumSpm / samples else 0
                    _ui.value = _ui.value.copy(finished = true, avgSpm = avg)
                    cancel()
                }
            }
        }
    }

    private fun speak(text: String) { try { tts.speak(text, TextToSpeech.QUEUE_ADD, null, "iwt") } catch (_: Exception) {} }
    private fun beep() { try { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200) } catch (_: Exception) {} }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val z = event.values[2]
        val dz = z - lastZ
        val now = System.currentTimeMillis()
        if (kotlin.math.abs(dz) > 2.0 && now - lastPeakTime > 300) {
            stepsInWindow += 1
            lastPeakTime = now
        }
        lastZ = z
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}