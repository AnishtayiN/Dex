@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.dexboost.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dexboost.R
import com.dexboost.data.preferences.Settings
import com.dexboost.domain.model.CompilationMode
import com.dexboost.ui.components.SectionCard
import com.dexboost.viewmodel.OptimizationViewModel
import com.dexboost.viewmodel.SettingsViewModel

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label)
    }
}

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    optimizationViewModel: OptimizationViewModel,
    modifier: Modifier = Modifier
) {
    val settings by settingsViewModel.settings.collectAsState(initial = Settings())
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    optimizationViewModel.clearHistory()
                }) { Text(stringResource(R.string.button_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(titleRes = R.string.settings_language_title) {
                RadioRow(
                    label = stringResource(R.string.settings_language_en),
                    selected = settings.language == "en",
                    onClick = { settingsViewModel.setLanguage("en") }
                )
                RadioRow(
                    label = stringResource(R.string.settings_language_fa),
                    selected = settings.language == "fa",
                    onClick = { settingsViewModel.setLanguage("fa") }
                )
                RadioRow(
                    label = stringResource(R.string.settings_language_system),
                    selected = settings.language == "system",
                    onClick = { settingsViewModel.setLanguage("system") }
                )
                Text(
                    text = stringResource(R.string.settings_language_note),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            SectionCard(titleRes = R.string.settings_theme_title) {
                RadioRow(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = settings.theme == "dark",
                    onClick = { settingsViewModel.setTheme("dark") }
                )
                RadioRow(
                    label = stringResource(R.string.settings_theme_light),
                    selected = settings.theme == "light",
                    onClick = { settingsViewModel.setTheme("light") }
                )
                RadioRow(
                    label = stringResource(R.string.settings_theme_system),
                    selected = settings.theme == "system",
                    onClick = { settingsViewModel.setTheme("system") }
                )
            }
            SectionCard(titleRes = R.string.settings_default_mode_title) {
                CompilationMode.values().forEach { mode ->
                    RadioRow(
                        label = stringResource(modeNameRes(mode)),
                        selected = settings.defaultMode == mode.name,
                        onClick = { settingsViewModel.setDefaultMode(mode.name) }
                    )
                }
            }
            SectionCard(titleRes = R.string.settings_confirmation_title) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_confirmation_desc),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.requireConfirmation,
                        onCheckedChange = { settingsViewModel.setRequireConfirmation(it) }
                    )
                }
            }
            SectionCard(titleRes = R.string.settings_history_title) {
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_clear_history)) }
            }
            SectionCard(titleRes = R.string.settings_about_title) {
                Text(text = stringResource(R.string.settings_about_version, "1.0.0"))
            }
            SectionCard(titleRes = R.string.settings_privacy_title) {
                Text(text = stringResource(R.string.settings_privacy_text))
            }
            SectionCard(titleRes = R.string.settings_limitations_title) {
                Text(text = stringResource(R.string.settings_limitations_text))
            }
        }
    }
}
