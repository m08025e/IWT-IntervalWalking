package com.example.iwt.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val fastSpm: Int = 120,
    val slowSpm: Int = 80,
    val isCalibrated: Boolean = false
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(CalibrationViewModel.PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPaces()
    }

    fun loadPaces() {
        val baseSpm = prefs.getInt(CalibrationViewModel.KEY_BASE_SPM, 0)
        val fastSpm = prefs.getInt(KEY_FAST_SPM, if (baseSpm > 0) baseSpm + 20 else 120)
        val slowSpm = prefs.getInt(KEY_SLOW_SPM, if (baseSpm > 0) baseSpm - 20 else 80)
        _uiState.value = HomeUiState(
            fastSpm = fastSpm,
            slowSpm = slowSpm,
            isCalibrated = baseSpm > 0
        )
    }

    fun saveFastSpm(spm: Int) {
        _uiState.value = _uiState.value.copy(fastSpm = spm)
        with(prefs.edit()) {
            putInt(KEY_FAST_SPM, spm)
            apply()
        }
    }

    fun saveSlowSpm(spm: Int) {
        _uiState.value = _uiState.value.copy(slowSpm = spm)
        with(prefs.edit()) {
            putInt(KEY_SLOW_SPM, spm)
            apply()
        }
    }

    companion object {
        const val KEY_FAST_SPM = "fast_spm"
        const val KEY_SLOW_SPM = "slow_spm"
    }
}
