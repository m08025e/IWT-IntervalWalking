package com.example.iwt.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.*

class CadenceEstimator(
    private val sensorManager: SensorManager,
    private val onCadenceChanged: (Int) -> Unit
) : SensorEventListener {

    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var lastStepCount: Int = 0
    private var isFirstReading: Boolean = true
    @Volatile
    private var isWalking: Boolean = false

    private val stepEvents = mutableListOf<Pair<Long, Int>>()
    private var smoothedSpm = 0.0
    private val emaAlpha = 0.3

    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private var stopDetectionJob: Job? = null

    fun start() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isFirstReading = true
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        stopDetectionJob?.cancel()
        coroutineScope.cancel()
    }

    fun setWalkingState(isWalking: Boolean) {
        this.isWalking = isWalking
        if (!isWalking) {
            smoothedSpm = 0.0
            onCadenceChanged(0)
            stepEvents.clear()
            stopDetectionJob?.cancel()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER || !isWalking) return

        val currentStepCount = event.values[0].toInt()
        val currentTime = System.currentTimeMillis()

        if (isFirstReading) {
            lastStepCount = currentStepCount
            isFirstReading = false
            return
        }

        val newSteps = currentStepCount - lastStepCount
        if (newSteps > 0) {
            lastStepCount = currentStepCount
            stepEvents.add(Pair(currentTime, newSteps))
            scheduleStopDetection()
        }

        val windowStartTime = currentTime - 15000
        stepEvents.removeAll { it.first < windowStartTime }

        val stepsInWindow = stepEvents.sumOf { it.second }
        val elapsedTimeInSeconds = if (stepEvents.isNotEmpty()) {
            (stepEvents.last().first - stepEvents.first().first) / 1000.0
        } else {
            0.0
        }

        val currentSpm = if (elapsedTimeInSeconds > 0) {
            (stepsInWindow / elapsedTimeInSeconds) * 60
        } else {
            0.0
        }

        smoothedSpm = (currentSpm * emaAlpha) + (smoothedSpm * (1 - emaAlpha))
        onCadenceChanged(smoothedSpm.toInt())
    }

    private fun scheduleStopDetection() {
        stopDetectionJob?.cancel()
        stopDetectionJob = coroutineScope.launch {
            delay(5000) // 5 seconds
            smoothedSpm = 0.0
            onCadenceChanged(0)
            stepEvents.clear()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
