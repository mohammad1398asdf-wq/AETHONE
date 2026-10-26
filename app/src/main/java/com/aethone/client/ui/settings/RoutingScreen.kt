package com.aethone.client.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethone.client.R
import com.aethone.client.data.RoutingMode
import com.aethone.client.data.RouteRule
import com.aethone.client.ui.AppViewModel
import com.aethone.client.ui.theme.Aethone

/**
 * Pro routing editor: mode presets plus the full rule lists (direct / block)
 * with every rule type the Aether core understands.
 */
@Composable
fun RoutingScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val extras = Aethone.extras

    var showModePicker by remember { mutableStateOf(false) }
    var addDirect by remember { mutableStateOf(false) }
    var addBlock by remember { mutableStateOf(false) }

    val modeLabels = listOf(
        stringResource(R.string.routing_mode_global) to stringResource(R.string.routing_mode_global_desc),
        stringResource(R.string.routing_mode_bypass_iran) to stringResource(R.string.routing_mode_bypass_iran_desc),
        stringResource(R.string.routing_mode_bypass_iran_ads) to stringResource(R.string.routing_mode_bypass_iran_ads_desc),
        stringResource(R.string.routing_mode_custom) to stringResource(R.string.routing_mode_custom_desc),
    )

    SettingsScaffold(stringResource(R.string.routing_title), onBack) {
        SettingsSection(stringResource(R.string.routing_mode)) {
            SettingRow(
                title = modeLabels[RoutingMode.entries.indexOf(settings.routingMode)].first,
                desc = modeLabels[RoutingMode.entries.indexOf(settings.routingMode)].second,
                onClick = { showModePicker = true },
                trailing = { ValueText("›") }
            )
            NoteText(stringResource(R.string.routing_iran_list_note, "1955"))
        }

        RuleListSection(
            title = stringResource(R.string.routing_direct_rules),
            emptyHint = stringResource(R.string.routing_direct_rules_desc),
            rules = settings.directRules,
            onAdd = { addDirect = true },
            onDelete = { rule ->
                vm.update { it.copy(directRules = it.directRules - rule) }
            },
        )
        NoteText(stringResource(R.string.routing_private_note))

        RuleListSection(
            title = stringResource(R.string.routing_block_rules),
            emptyHint = stringResource(R.string.routing_block_rules_desc),
            rules = settings.blockRules,
            onAdd = { addBlock = true },
            onDelete = { rule ->
                vm.update { it.copy(blockRules = it.blockRules - rule) }
            },
        )
    }

    if (showModePicker) {
        ChoiceDialog(
            title = stringResource(R.string.routing_mode),
            options = modeLabels.map { ChoiceOption(it.first, it.second) },
            selectedIndex = RoutingMode.entries.indexOf(settings.routingMode),
            onDismiss = { showModePicker = false },
            onPick = { i ->
                vm.update { it.copy(routingMode = RoutingMode.entries[i]) }
                showModePicker = false
            },
        )
    }
    if (addDirect) {
        AddRuleDialog { rule ->
            if (rule != null) vm.update { it.copy(directRules = it.directRules + rule) }
            addDirect = false
        }
    }
    if (addBlock) {
        AddRuleDialog { rule ->
            if (rule != null) vm.update { it.copy(blockRules = it.blockRules + rule) }
            addBlock = false
        }
    }
}

@Composable
private fun RuleListSection(
    title: String,
    emptyHint: String,
    rules: List<RouteRule>,
    onAdd: () -> Unit,
    onDelete: (RouteRule) -> Unit,
) {
    SettingsSection(title) {
        rules.forEach { rule ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RuleChip(rule.kind)
                Spacer(Modifier.padding(start = 10.dp))
                Text(
                    rule.value.ifEmpty { rule.kind },
                    fontSize = 13.sp,
                    color = Aethone.extras.text,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDelete(rule) }, modifier = Modifier.padding(0.dp)) {
                    Icon(
                        Icons.Outlined.Delete, contentDescription = "delete",
                        tint = Aethone.extras.textDim,
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }
        if (rules.isEmpty()) {
            Text(
                emptyHint,
                fontSize = 11.sp,
                color = Aethone.extras.textDim,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onAdd() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Add, null, tint = Aethone.extras.connected)
            Text(
                stringResource(R.string.routing_add_rule),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Aethone.extras.connected,
            )
        }
    }
}

@Composable
private fun RuleChip(kind: String) {
    val extras = Aethone.extras
    val label = when (kind) {
        "domain" -> "DOMAIN"
        "full" -> "FULL"
        "keyword" -> "KEY"
        "regexp" -> "REGEX"
        "cidr" -> "IP"
        "port" -> "PORT"
        else -> "MISC"
    }
    Text(
        label,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = extras.textDim,
        modifier = Modifier
            .background(extras.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
private fun AddRuleDialog(onResult: (RouteRule?) -> Unit) {
    var kind by remember { mutableStateOf("domain") }
    var value by remember { mutableStateOf("") }
    val kinds = listOf(
        "domain" to stringResource(R.string.routing_type_domain),
        "full" to stringResource(R.string.routing_type_full),
        "keyword" to stringResource(R.string.routing_type_keyword),
        "regexp" to stringResource(R.string.routing_type_regexp),
        "cidr" to stringResource(R.string.routing_type_cidr),
        "port" to stringResource(R.string.routing_type_port),
        "private" to stringResource(R.string.routing_type_private),
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onResult(null) },
        containerColor = Aethone.extras.surface,
        title = {
            Text(stringResource(R.string.routing_add_rule), fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                // kind selector
                Column {
                    kinds.forEach { (k, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { kind = k }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = kind == k,
                                onClick = { kind = k }
                            )
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (kind != "private") {
                    androidx.compose.material3.OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        singleLine = true,
                        label = {
                            Text(
                                stringResource(R.string.routing_rule_value)
                            )
                        },
                        placeholder = {
                            Text(
                                when (kind) {
                                    "cidr" -> stringResource(R.string.routing_rule_hint_cidr)
                                    "port" -> stringResource(R.string.routing_rule_hint_port)
                                    "keyword" -> stringResource(R.string.routing_rule_hint_keyword)
                                    "regexp" -> stringResource(R.string.routing_rule_hint_regexp)
                                    else -> stringResource(R.string.routing_rule_hint_domain)
                                },
                                fontSize = 12.sp
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val v = value.trim()
                val valid = when (kind) {
                    "private" -> true
                    "cidr" -> v.contains('/') || v.count { it == '.' } == 3
                    "port" -> v.isNotEmpty() && v.all { it.isDigit() || it == '-' }
                    else -> v.isNotEmpty()
                }
                if (valid) {
                    onResult(RouteRule(kind, if (kind == "private") "" else v))
                } else {
                    onResult(null)
                }
            }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { onResult(null) }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
