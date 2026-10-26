package com.aethone.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethone.client.core.ConnectionBus
import com.aethone.client.core.ExitInfoProbe
import com.aethone.client.core.VpnStatus
import com.aethone.client.data.AppSettings
import com.aethone.client.data.SettingsRepository
import com.aethone.client.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Shared view model: settings in, settings out, plus the one-off direct
 * location probe that fills the home card before the first connect.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun toggleTheme(systemDark: Boolean) {
        update {
            val isDark = when (it.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            it.copy(themeMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK)
        }
    }

    /** One-shot direct lookup of the user's real location (no tunnel). */
    fun probeRealLocation() {
        viewModelScope.launch {
            val lang = if (Locale.getDefault().language == "fa") "fa" else "en"
            val info = ExitInfoProbe(null).lookup(lang)
            if (ConnectionBus.state.value.status == VpnStatus.DISCONNECTED &&
                info.country.isNotBlank()
            ) {
                ConnectionBus.update { it.copy(exitInfo = info, exitIsRealLocation = true) }
            }
        }
    }
}
