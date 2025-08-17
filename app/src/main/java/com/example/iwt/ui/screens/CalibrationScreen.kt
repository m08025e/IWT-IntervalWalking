package com.example.iwt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iwt.viewmodel.CalibrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    onComplete: (Int) -> Unit,
    onCancel: () -> Unit,
    vm: CalibrationViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()

    if (ui.finished) {
        onComplete(ui.resultSpm)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("基準ペース測定") },
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
            if (ui.running) {
                // UI during calibration
                Text("測定中", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${ui.remainingMinutes}:${ui.remainingSecondsPart}",
                    style = MaterialTheme.typography.displayLarge
                )
                Text("快適なペースで歩き続けてください", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(32.dp))
                OutlinedButton(onClick = onCancel) {
                    Icon(Icons.Filled.Cancel, contentDescription = "キャンセル")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("測定を中止")
                }
            } else {
                // UI before starting calibration
                Text("基準ペースを測定します", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Text(
                    "5分間、あなたが「快適だ」と感じる自然なペースで歩いてください。\nあなたの最適なトレーニング強度を計算するために使用します。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { vm.start() },
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "測定開始")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("測定を開始する", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}