package com.example.iwt

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.iwt.ui.IwtApp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val activityRecognitionPermissionState =
                rememberPermissionState(Manifest.permission.ACTIVITY_RECOGNITION)

            if (activityRecognitionPermissionState.status.isGranted) {
                IwtApp()
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val textToShow = if (activityRecognitionPermissionState.status.shouldShowRationale) {
                        "Activity Recognition permission is required for the app to function correctly."
                    } else {
                        "Activity Recognition permission is required for this app. Please grant the permission."
                    }
                    Text(textToShow)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        if (activityRecognitionPermissionState.status.shouldShowRationale) {
                            activityRecognitionPermissionState.launchPermissionRequest()
                        } else {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                    }) {
                        Text("Request Permission")
                    }
                }
            }
        }
    }
}