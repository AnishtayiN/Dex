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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.dexboost.domain.model.CompilationMode
import com.dexboost.domain.model.InstalledApp
import com.dexboost.ui.components.AppRow
import com.dexboost.ui.components.SectionCard
import com.dexboost.ui.components.StatusChip
import com.dexboost.viewmodel.AppsViewModel
import com.dexboost.viewmodel.OptimizationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppDetailsScreen(
    packageName: String,
    appsViewModel: AppsViewModel,
    optimizationViewModel: OptimizationViewModel,
    onQueueNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var app by remember { mutableStateOf<InstalledApp?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedMode by remember { mutableStateOf<CompilationMode?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    val defaultMode by optimizationViewModel.defaultMode.collectAsState()
    val requireConfirmation by optimizationViewModel.requireConfirmation.collectAsState()
    val history by optimizationViewModel.history.collectAsState(initial = emptyList())
    val previous = history.filter { it.packageName == packageName }

    LaunchedEffect(packageName) {
        loading = true
        app = appsViewModel.getApp(packageName)
        loading = false
    }
    val mode = selectedMode ?: defaultMode
    LaunchedEffect(defaultMode) {
        if (selectedMode == null) selectedMode = defaultMode
    }

    if (showConfirm && app != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.details_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.details_confirm_message,
                        app!!.label,
                        stringResource(modeNameRes(mode))
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    optimizationViewModel.startBatch(listOf(packageName), mode)
                    onQueueNavigate()
                }) { Text(stringResource(R.string.button_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.details_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                loading -> Text(stringResource(R.string.details_loading))
                app == null -> Text(stringResource(R.string.details_not_found))
                else -> {
                    val current = app!!
                    AppRow(app = current)
                    SectionCard {
                        Text(stringResource(R.string.details_package))
                        Text(text = current.packageName, style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.details_version))
                        Text(
                            text = current.versionName ?: stringResource(R.string.details_unknown_date),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(stringResource(R.string.details_installed))
                        Text(
                            text = if (current.installedTimeMs > 0) {
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .format(Date(current.installedTimeMs))
                            } else {
                                stringResource(R.string.details_unknown_date)
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    SectionCard(titleRes = R.string.details_mode_title) {
                        Text(stringResource(R.string.details_honest_note))
                        CompilationMode.values().forEach { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMode = candidate }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = mode == candidate,
                                    onClick = { selectedMode = candidate }
                                )
                                Column {
                                    Text(
                                        text = stringResource(modeNameRes(candidate)),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = stringResource(modeDescRes(candidate)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    SectionCard(titleRes = R.string.details_previous_title) {
                        if (previous.isEmpty()) {
                            Text(stringResource(R.string.details_previous_none))
                        } else {
                            previous.forEach { record ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatusChip(
                                        label = stringResource(outcomeLabelRes(outcomeFromName(record.outcome))),
                                        tone = outcomeTone(outcomeFromName(record.outcome))
                                    )
                                    record.exitCode?.let { exitCode ->
                                        Text(
                                            text = stringResource(
                                                R.string.history_exit_code, exitCode
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = stringResource(
                                            R.string.history_duration_ms, record.durationMs
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (requireConfirmation) showConfirm = true
                            else {
                                optimizationViewModel.startBatch(listOf(packageName), mode)
                                onQueueNavigate()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.details_start_button)) }
                }
            }
        }
    }
}
