package com.example.iwt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iwt.viewmodel.SessionViewModel

@Composable
fun SessionScreen(
    onFinish: (String) -> Unit,
    onCancel: () -> Unit,
    vm: SessionViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()
    LaunchedEffect(Unit) { vm.start() }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (ui.fastPhase) "速歩" else "スロー")
            Spacer(Modifier.height(8.dp))
            Text("${ui.remainingMinutes}:${ui.remainingSecondsPart} 残り")
            Spacer(Modifier.height(16.dp))
            Text("${ui.cadenceSpm} SPM")
            Spacer(Modifier.height(8.dp))
            Text(ui.formHint)

            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = { vm.togglePause() }) {
                    Text(if (ui.paused) "再開" else "一時停止")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { vm.cancel(); onCancel() }) { Text("中止") }
            }
        }
    }

    if (ui.finished) {
        val statsJson = "{\"avgSpm\":${ui.avgSpm},\"sets\":${ui.set}}"
        onFinish(statsJson)
    }
}