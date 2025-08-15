package com.example.iwt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SummaryScreen(onDone: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("おつかれさま！")
            Spacer(Modifier.height(12.dp))
            Text("次回のヒント：みぞおちを軽く上へ")
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone) { Text("ホームに戻る") }
        }
    }
}