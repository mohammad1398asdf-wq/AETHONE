package com.aethone.client.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethone.client.ui.theme.Aethone

/** Screen scaffold with a back arrow and the app's scrollable body. */
@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val extras = Aethone.extras
    Column(
        Modifier
            .fillMaxSize()
            .background(extras.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = extras.text
                )
            }
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = extras.text,
            )
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column { content() }
        }
    }
}

/** Rounded card grouping related settings. */
@Composable
fun SettingsSection(
    title: String? = null,
    content: @Composable () -> Unit,
) {
    val extras = Aethone.extras
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        if (title != null) {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = extras.textDim,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(extras.surface, RoundedCornerShape(20.dp))
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    desc: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val extras = Aethone.extras
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = extras.text)
            if (desc != null) {
                Text(desc, fontSize = 11.sp, color = extras.textDim)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SwitchRow(
    title: String,
    desc: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val extras = Aethone.extras
    SettingRow(
        title = title,
        desc = desc,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = extras.connected,
                    checkedThumbColor = extras.surface,
                )
            )
        }
    )
}

@Composable
fun NavRow(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
) {
    val extras = Aethone.extras
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = extras.connected, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = extras.text)
            Text(desc, fontSize = 11.sp, color = extras.textDim)
        }
        Text("›", fontSize = 20.sp, color = extras.textDim)
    }
}

data class ChoiceOption(val label: String, val desc: String? = null, val value: Int = 0)

/** Radio dialog used by every choice-type setting. */
@Composable
fun ChoiceDialog(
    title: String,
    options: List<ChoiceOption>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val extras = Aethone.extras
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extras.surface,
        titleContentColor = extras.text,
        textContentColor = extras.text,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEachIndexed { i, o ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(i) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = i == selectedIndex, onClick = { onPick(i) })
                        Column {
                            Text(o.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (o.desc != null) {
                                Text(o.desc, fontSize = 11.sp, color = extras.textDim)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("✕") }
        },
    )
}

/** Text/number input dialog with optional validation. */
@Composable
fun InputDialog(
    title: String,
    initial: String,
    hint: String = "",
    numeric: Boolean = false,
    multiline: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val extras = Aethone.extras
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extras.surface,
        titleContentColor = extras.text,
        textContentColor = extras.text,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = if (numeric) it.filter { c -> c.isDigit() } else it },
                    singleLine = !multiline,
                    minLines = if (multiline) 4 else 1,
                    label = { Text(hint) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) {
                Text(androidx.compose.ui.res.stringResource(com.aethone.client.R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.aethone.client.R.string.cancel))
            }
        },
    )
}

/** Small label used above inline lists. */
@Composable
fun NoteText(text: String) {
    val extras = Aethone.extras
    Text(
        text,
        fontSize = 11.sp,
        color = extras.textDim,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}
