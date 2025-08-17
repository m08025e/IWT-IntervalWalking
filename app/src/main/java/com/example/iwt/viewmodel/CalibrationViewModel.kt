package com.example.iwt.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class CalibrationUiState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val remainingSeconds: Int = 300, // 5 minutes
    val resultSpm: Int = 0
) {
    val remainingMinutes: Int get() = remainingSeconds / 60
    val remainingSecondsPart: String get() = (remainingSeconds % 60).toString().padStart(2, '0')
}

class CalibrationViewModel(app: Application) : AndroidViewModel(app), SensorEventListener {
    companion object {
        const val PREFS_NAME = "iwt_prefs"
        const val KEY_BASE_SPM = "base_spm"
    }

    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val sensorManager = app.getSystemService(SensorManager::class.java)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    // TODO: Add fallback to accelerometer

    private val _ui = MutableStateFlow(CalibrationUiState())
    val uiState = _ui.asStateFlow()

    private var totalSteps = 0

    fun start() {
        _ui.value = _ui.value.copy(running = true)
        totalSteps = 0
        sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_UI)
        startTimer()
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (_ui.value.remainingSeconds > 0) {
                delay(1000)
                _ui.value = _ui.value.copy(remainingSeconds = _ui.value.remainingSeconds - 1)
            }
            finishCalibration()
        }
    }

    private fun finishCalibration() {
        sensorManager.unregisterListener(this)
        val durationMinutes = 5
        val resultSpm = totalSteps / durationMinutes

        with(prefs.edit()) {
            putInt(KEY_BASE_SPM, resultSpm)
            apply()
        }

        _ui.value = _ui.value.copy(running = false, finished = true, resultSpm = resultSpm)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            totalSteps++
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}
