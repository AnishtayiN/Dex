@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.dexboost.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.dexboost.ui.components.AppRow
import com.dexboost.viewmodel.AppsSort
import com.dexboost.viewmodel.AppsViewModel
import com.dexboost.viewmodel.OptimizationViewModel

@Composable
fun AppsScreen(
    appsViewModel: AppsViewModel,
    optimizationViewModel: OptimizationViewModel,
    onAppClick: (String) -> Unit,
    onQueueNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by appsViewModel.uiState.collectAsState()
    val requireConfirmation by optimizationViewModel.requireConfirmation.collectAsState()
    val defaultMode by optimizationViewModel.defaultMode.collectAsState()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.apps_confirm_batch_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.apps_confirm_batch_message,
                        state.selectedPackages.size,
                        stringResource(modeNameRes(defaultMode))
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    optimizationViewModel.startBatch(state.selectedPackages.toList(), defaultMode)
                    appsViewModel.clearSelection()
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.apps_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { appsViewModel.search(it) },
                label = { Text(stringResource(R.string.apps_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.apps_games_only))
                Switch(
                    checked = state.gamesOnly,
                    onCheckedChange = { appsViewModel.setGamesOnly(it) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.apps_include_system))
                Switch(
                    checked = state.includeSystem,
                    onCheckedChange = { appsViewModel.setIncludeSystem(it) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.apps_sort_label))
                OutlinedButton(onClick = { sortMenuExpanded = true }) {
                    Text(
                        stringResource(
                            when (state.sort) {
                                AppsSort.NAME_ASC -> R.string.apps_sort_name_asc
                                AppsSort.NAME_DESC -> R.string.apps_sort_name_desc
                                AppsSort.INSTALL_NEWEST -> R.string.apps_sort_newest
                                AppsSort.INSTALL_OLDEST -> R.string.apps_sort_oldest
                            }
                        )
                    )
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.apps_sort_name_asc)) },
                        onClick = { appsViewModel.setSort(AppsSort.NAME_ASC); sortMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.apps_sort_name_desc)) },
                        onClick = { appsViewModel.setSort(AppsSort.NAME_DESC); sortMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.apps_sort_newest)) },
                        onClick = { appsViewModel.setSort(AppsSort.INSTALL_NEWEST); sortMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.apps_sort_oldest)) },
                        onClick = { appsViewModel.setSort(AppsSort.INSTALL_OLDEST); sortMenuExpanded = false }
                    )
                }
            }
            if (state.selectedPackages.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.apps_selected_count, state.selectedPackages.size),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (requireConfirmation) {
                                showConfirm = true
                            } else {
                                optimizationViewModel.startBatch(
                                    state.selectedPackages.toList(), defaultMode
                                )
                                appsViewModel.clearSelection()
                                onQueueNavigate()
                            }
                        }
                    ) { Text(stringResource(R.string.apps_optimize_selected)) }
                    OutlinedButton(onClick = { appsViewModel.clearSelection() }) {
                        Text(stringResource(R.string.apps_clear_selection))
                    }
                }
            } else {
                OutlinedButton(onClick = { appsViewModel.selectAll() }) {
                    Text(stringResource(R.string.apps_select_all))
                }
            }
            state.error?.let {
                Text(text = stringResource(R.string.error_loading_apps), color = MaterialTheme.colorScheme.error)
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            if (state.isLoading) {
                Text(text = stringResource(R.string.apps_loading))
            } else if (state.apps.isEmpty()) {
                Text(text = stringResource(R.string.apps_empty))
                Text(text = stringResource(R.string.apps_empty_hint))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.apps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            selected = app.packageName in state.selectedPackages,
                            showCheckbox = true,
                            onCheckedChange = { appsViewModel.toggleSelection(app.packageName) },
                            onClick = { onAppClick(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}
