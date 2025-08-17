package com.example.iwt.sensor

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionManager(
    private val context: Context,
    private val onActivityChanged: (isWalking: Boolean) -> Unit
) {
    private val client = ActivityRecognition.getClient(context)
    private var pendingIntent: PendingIntent? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                if (result != null) {
                    handleTransitionResult(result)
                }
            }
        }
    }

    private fun handleTransitionResult(result: ActivityTransitionResult) {
        val isWalkingOrRunning = result.transitionEvents.any { event ->
            (event.activityType == DetectedActivity.WALKING || event.activityType == DetectedActivity.RUNNING) &&
                    event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        }
        val isStill = result.transitionEvents.any {
            it.activityType == DetectedActivity.STILL && it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        }

        if (isWalkingOrRunning) {
            onActivityChanged(true)
        } else if (isStill) {
            onActivityChanged(false)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = com.google.android.gms.location.ActivityTransitionRequest(transitions)

        ContextCompat.registerReceiver(
            context, 
            receiver, 
            IntentFilter(ActivityRecognitionReceiver.INTENT_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        client.requestActivityTransitionUpdates(request, pendingIntent!!)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        pendingIntent?.let { client.removeActivityTransitionUpdates(it) }
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered, ignore
        }
    }
}

// This receiver is needed to be declared in the manifest
class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Repackage and send to our internal receiver to handle logic
        val newIntent = Intent(INTENT_ACTION)
        newIntent.setPackage(context.packageName)
        newIntent.putExtras(intent.extras!!)
        context.sendBroadcast(newIntent)
    }

    companion object {
        const val INTENT_ACTION = "com.example.iwt.ACTIVITY_RECOGNITION"
    }
}
