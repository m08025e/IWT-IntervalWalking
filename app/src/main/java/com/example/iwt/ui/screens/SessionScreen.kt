package com.example.iwt.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iwt.viewmodel.SessionViewModel
import com.example.iwt.viewmodel.UiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SessionScreen(
    fastSpm: Int,
    slowSpm: Int,
    onFinish: (String) -> Unit,
    onCancel: () -> Unit,
    vm: SessionViewModel = viewModel()
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
    } else {
        emptyList()
    }

    val permissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        permissions = permissions
    )

    if (permissionsState.allPermissionsGranted) {
        SessionContent(fastSpm, slowSpm, onFinish, onCancel, vm)
    } else {
        PermissionRationale(
            onRequestPermission = { permissionsState.launchMultiplePermissionRequest() },
            onCancel = onCancel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionContent(
    fastSpm: Int,
    slowSpm: Int,
    onFinish: (String) -> Unit,
    onCancel: () -> Unit,
    vm: SessionViewModel
) {
    val ui by vm.uiState.collectAsState()
    LaunchedEffect(Unit) { vm.start(fastSpm, slowSpm) }

    if (ui.finished) {
        val statsJson = "{\"avgSpm\":${ui.avgSpm},\"sets\":${ui.set}}"
        onFinish(statsJson)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val phaseText = if (ui.fastPhase) "速歩" else "スロー"
                    Text("$phaseText - ${ui.set + 1} / 5 セット目")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            TimerDisplay(ui)
            SpmGauge(ui)
            SessionControls(ui, vm, onCancel)
        }
    }
}

@Composable
fun PermissionRationale(onRequestPermission: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "歩数計測の権限が必要です",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "このアプリは、歩数を正確に計測するために「身体活動データ」へのアクセス許可を必要とします。この権限がないと、SPM（毎分歩数）を計算できません。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRequestPermission) {
                Text("権限を許可する")
            }
            OutlinedButton(onClick = onCancel) {
                Text("キャンセル")
            }
        }
    }
}

@Composable
private fun TimerDisplay(ui: UiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${ui.remainingMinutes}:${ui.remainingSecondsPart}",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("残り時間", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SessionControls(ui: UiState, vm: SessionViewModel, onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { vm.togglePause() },
            modifier = Modifier.size(width = 180.dp, height = 56.dp)
        ) {
            val icon = if (ui.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause
            val text = if (ui.paused) "再開" else "一時停止"
            Icon(icon, contentDescription = text, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = { vm.cancel(); onCancel() },
            modifier = Modifier.size(width = 140.dp, height = 56.dp)
        ) {
            Icon(Icons.Filled.Cancel, contentDescription = "中止", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("中止")
        }
    }
}

@Composable
fun SpmGauge(ui: UiState) {
    val gaugeColor by animateColorAsState(
        targetValue = when {
            ui.cadenceSpm in ui.targetSpmRange -> MaterialTheme.colorScheme.primary
            ui.cadenceSpm in (ui.targetSpmRange.first - 10)..(ui.targetSpmRange.last + 10) -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        },
        animationSpec = tween(500), label = "gaugeColor"
    )

    val progress = ((ui.cadenceSpm.toFloat() - (ui.targetSpm - 50)).coerceIn(0f, 100f) / 100f).coerceIn(0f, 1f)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("SPM (歩/分)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${ui.cadenceSpm}",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "目標: ${ui.targetSpm}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(24.dp),
            color = gaugeColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}