package com.example.iwt.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iwt.viewmodel.HomeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStart: (fastSpm: Int, slowSpm: Int) -> Unit,
    onStartCalibration: () -> Unit,
    homeVm: HomeViewModel = viewModel()
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION)
    } else {
        listOf(Manifest.permission.BODY_SENSORS)
    }
    val permissionsState = rememberMultiplePermissionsState(permissions)
    val ui by homeVm.uiState.collectAsState()

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
        homeVm.loadPaces()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IWT - インターバル歩行") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (permissionsState.allPermissionsGranted) {
                PaceSettingsCard(ui, homeVm, onStartCalibration)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onStart(ui.fastSpm, ui.slowSpm) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "セッション開始", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("このペースで開始", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text(
                    "このアプリを使用するには、身体活動とセンサーへのアクセス許可が必要です.\n許可されていない場合、機能は制限されます。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaceSettingsCard(ui: com.example.iwt.viewmodel.HomeUiState, homeVm: HomeViewModel, onStartCalibration: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaceEditor("スローペース (SPM)", ui.slowSpm, modifier = Modifier.weight(1f)) { homeVm.saveSlowSpm(it) }
                PaceEditor("速歩ペース (SPM)", ui.fastSpm, modifier = Modifier.weight(1f)) { homeVm.saveFastSpm(it) }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onStartCalibration,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Tune, contentDescription = "キャリブレーション", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(if (ui.isCalibrated) "再キャリブレーション" else "キャリブレーションを開始")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaceEditor(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { newValue -> newValue.toIntOrNull()?.let(onValueChange) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
        modifier = modifier,
        singleLine = true
    )
}
