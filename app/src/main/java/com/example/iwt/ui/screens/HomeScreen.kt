package com.example.iwt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "インターバル・ウォーキング")
            Spacer(Modifier.height(12.dp))
            Text("速歩3分 + スロー3分 × 5 セット")
            Spacer(Modifier.height(24.dp))
            Button(onClick = onStart) { Text("開始する") }
        }
    }
}