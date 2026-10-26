@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.dexboost.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.dexboost.domain.model.CommandOutcome
import com.dexboost.ui.components.SectionCard
import com.dexboost.ui.components.StatusChip
import com.dexboost.viewmodel.OptimizationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    optimizationViewModel: OptimizationViewModel,
    modifier: Modifier = Modifier
) {
    val history by optimizationViewModel.history.collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf<CommandOutcome?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    // Any? so this does not assume whether the record id is Int or Long.
    var expandedId by remember { mutableStateOf<Any?>(null) }

    val activeFilter = filter
    val visible = if (activeFilter == null) history
        else history.filter { it.outcome == activeFilter.name }

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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter == null,
                        onClick = { filter = null },
                        label = { Text(stringResource(R.string.history_filter_all)) }
                    )
                }
                items(CommandOutcome.values().toList()) { outcome ->
                    FilterChip(
                        selected = filter == outcome,
                        onClick = { filter = outcome },
                        label = { Text(stringResource(outcomeLabelRes(outcome))) }
                    )
                }
            }
            OutlinedButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.history_clear)) }
            if (visible.isEmpty()) {
                Text(text = stringResource(R.string.history_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.id }) { record ->
                        val expanded = expandedId == record.id
                        SectionCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusChip(
                                    label = stringResource(outcomeLabelRes(outcomeFromName(record.outcome))),
                                    tone = outcomeTone(outcomeFromName(record.outcome))
                                )
                                Text(
                                    text = stringResource(modeNameRes(modeFromName(record.mode))),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(text = record.packageName, style = MaterialTheme.typography.bodyMedium)
                            record.exitCode?.let { exitCode ->
                                Text(
                                    text = stringResource(R.string.history_exit_code, exitCode),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = stringResource(R.string.history_duration_ms, record.durationMs),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(
                                    R.string.history_started,
                                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        .format(Date(record.startedAtMs))
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = {
                                expandedId = if (expanded) null else record.id
                            }) {
                                Text(
                                    stringResource(
                                        if (expanded) R.string.history_hide_output
                                        else R.string.history_show_output
                                    )
                                )
                            }
                            if (expanded) {
                                val stdout = record.stdout
                                val stderr = record.stderr
                                if (stdout.isBlank() && stderr.isBlank()) {
                                    Text(
                                        text = stringResource(R.string.history_no_output),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    if (stdout.isNotBlank()) {
                                        Text(stringResource(R.string.history_output_label))
                                        Text(text = stdout, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (stderr.isNotBlank()) {
                                        Text(stringResource(R.string.history_error_label))
                                        Text(text = stderr, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
