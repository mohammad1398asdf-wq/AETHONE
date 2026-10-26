package com.aethone.client.ui.settings

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethone.client.R
import com.aethone.client.data.AppsMode
import com.aethone.client.ui.AppViewModel
import com.aethone.client.ui.theme.Aethone
import android.graphics.drawable.BitmapDrawable

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: android.graphics.Bitmap?,
)

/** Split tunneling: which apps use the tunnel. */
@Composable
fun AppsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val extras = Aethone.extras
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        apps = pm.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || it.enabled }
            .map {
                AppEntry(
                    packageName = it.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = (it.loadIcon(pm) as? BitmapDrawable)?.bitmap,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
        loading = false
    }

    val modeLabels = listOf(
        stringResource(R.string.apps_mode_all) to stringResource(R.string.apps_mode_all_desc),
        stringResource(R.string.apps_mode_exclude) to stringResource(R.string.apps_mode_exclude_desc),
        stringResource(R.string.apps_mode_include) to stringResource(R.string.apps_mode_include_desc),
    )
    var showModePicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = extras.text)
            }
            Column {
                Text(
                    stringResource(R.string.apps_title),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = extras.text
                )
            }
        }

        SettingsSection {
            SettingRow(
                title = stringResource(R.string.apps_mode),
                desc = modeLabels[AppsMode.entries.indexOf(settings.appsMode)].second,
                onClick = { showModePicker = true },
                trailing = { ValueText("›") }
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(stringResource(R.string.apps_search_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Text(
            stringResource(R.string.apps_selected, settings.selectedApps.size),
            fontSize = 11.sp,
            color = extras.textDim,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        val visible = apps.filter {
            query.isBlank() || it.label.contains(query, true) ||
                it.packageName.contains(query, true)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(visible, key = { it.packageName }) { app ->
                val checked = app.packageName in settings.selectedApps
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.update {
                                it.copy(
                                    selectedApps = if (checked)
                                        it.selectedApps - app.packageName
                                    else it.selectedApps + app.packageName
                                )
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (app.icon != null) {
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Android,
                            null,
                            tint = extras.textDim,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, fontSize = 13.sp, color = extras.text, maxLines = 1)
                        Text(
                            app.packageName,
                            fontSize = 10.sp, color = extras.textDim, maxLines = 1
                        )
                    }
                    Checkbox(checked = checked, onCheckedChange = null)
                }
            }
        }
    }

    if (showModePicker) {
        ChoiceDialog(
            title = stringResource(R.string.apps_mode),
            options = modeLabels.map { ChoiceOption(it.first, it.second) },
            selectedIndex = AppsMode.entries.indexOf(settings.appsMode),
            onDismiss = { showModePicker = false },
            onPick = { i ->
                vm.update { it.copy(appsMode = AppsMode.entries[i]) }
                showModePicker = false
            },
        )
    }
}

