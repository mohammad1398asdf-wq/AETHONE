package com.aethone.client.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.aethone.client.R
import com.aethone.client.data.ThemeMode
import com.aethone.client.ui.AppViewModel

/** Appearance: theme + language. */
@Composable
fun AppearanceScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    var showTheme by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }

    val themeLabels = listOf(
        stringResource(R.string.appearance_theme_system),
        stringResource(R.string.appearance_theme_light),
        stringResource(R.string.appearance_theme_dark),
    )
    val langLabels = listOf(
        stringResource(R.string.appearance_language_system),
        "فارسی",
        "English",
    )
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    SettingsScaffold(stringResource(R.string.appearance_title), onBack) {
        SettingsSection(stringResource(R.string.appearance_theme)) {
            SettingRow(
                title = themeLabels[ThemeMode.entries.indexOf(settings.themeMode)],
                onClick = { showTheme = true },
                trailing = { ValueText("›") }
            )
        }
        SettingsSection(stringResource(R.string.appearance_language)) {
            SettingRow(
                title = when {
                    currentLocale.startsWith("fa") -> "فارسی"
                    currentLocale.startsWith("en") -> "English"
                    else -> stringResource(R.string.appearance_language_system)
                },
                onClick = { showLang = true },
                trailing = { ValueText("›") }
            )
        }
    }

    if (showTheme) {
        ChoiceDialog(
            title = stringResource(R.string.appearance_theme),
            options = themeLabels.map { ChoiceOption(it) },
            selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
            onDismiss = { showTheme = false },
            onPick = { i ->
                vm.update { it.copy(themeMode = ThemeMode.entries[i]) }
                showTheme = false
            },
        )
    }
    if (showLang) {
        ChoiceDialog(
            title = stringResource(R.string.appearance_language),
            options = langLabels.map { ChoiceOption(it) },
            selectedIndex = when {
                currentLocale.startsWith("fa") -> 1
                currentLocale.startsWith("en") -> 2
                else -> 0
            },
            onDismiss = { showLang = false },
            onPick = { i ->
                when (i) {
                    0 -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    1 -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fa"))
                    2 -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                }
                showLang = false
            },
        )
    }
}

/** Features: kill switch, auto-connect, notification stats. */
@Composable
fun FeaturesScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current

    SettingsScaffold(stringResource(R.string.features_title), onBack) {
        SettingsSection(stringResource(R.string.features_kill_switch)) {
            SwitchRow(
                title = stringResource(R.string.features_kill_switch),
                desc = stringResource(R.string.features_kill_switch_desc),
                checked = settings.killSwitch,
                onChange = { v -> vm.update { it.copy(killSwitch = v) } },
            )
            SettingRow(
                title = stringResource(R.string.features_kill_switch_open_system),
                desc = stringResource(R.string.features_kill_switch_hint),
                onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
                },
                trailing = { ValueText("›") }
            )
        }
        SettingsSection {
            SwitchRow(
                title = stringResource(R.string.features_autostart),
                desc = stringResource(R.string.features_autostart_desc),
                checked = settings.autoConnectOnBoot,
                onChange = { v -> vm.update { it.copy(autoConnectOnBoot = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.features_notification_stats),
                desc = stringResource(R.string.features_notification_stats_desc),
                checked = settings.notificationStats,
                onChange = { v -> vm.update { it.copy(notificationStats = v) } },
            )
        }
    }
}
