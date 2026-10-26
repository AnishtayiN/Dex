@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.dexboost.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dexboost.R
import com.dexboost.shizuku.ShizukuState
import com.dexboost.ui.components.SectionCard
import com.dexboost.ui.components.StatusChip
import com.dexboost.ui.navigation.DexBoostRoutes
import com.dexboost.viewmodel.AppsViewModel
import com.dexboost.viewmodel.OptimizationViewModel
import com.dexboost.viewmodel.ShizukuViewModel

@Composable
fun HomeScreen(
    appsViewModel: AppsViewModel,
    shizukuViewModel: ShizukuViewModel,
    optimizationViewModel: OptimizationViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appsState by appsViewModel.uiState.collectAsState()
    val rawShizukuState by shizukuViewModel.state.collectAsState()
    // The manager never produces NotInstalled itself; combine the binder
    // state with the installed-app check, as its documentation prescribes.
    val packageManager = LocalContext.current.packageManager
    val shizukuState = remember(rawShizukuState, packageManager) {
        if (shizukuViewModel.isShizukuAppPresent(packageManager)) rawShizukuState
        else ShizukuState.NotInstalled
    }
    val history by optimizationViewModel.history.collectAsState(initial = emptyList())
    val last = history.firstOrNull()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(text = stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium)

            SectionCard(titleRes = R.string.home_status_title) {
                StatusChip(
                    label = stringResource(shizukuLabelRes(shizukuState)),
                    tone = shizukuTone(shizukuState)
                )
            }

            SectionCard(titleRes = R.string.home_apps_title) {
                if (appsState.isLoading) {
                    Text(text = stringResource(R.string.home_apps_loading))
                } else {
                    Text(text = stringResource(R.string.home_apps_count, appsState.allApps.size))
                }
                appsState.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }

            SectionCard(titleRes = R.string.home_last_operation_title) {
                if (last == null) {
                    Text(text = stringResource(R.string.home_last_operation_none))
                } else {
                    Text(
                        text = stringResource(
                            R.string.home_last_operation_text,
                            last.packageName,
                            stringResource(outcomeLabelRes(outcomeFromName(last.outcome)))
                        )
                    )
                }
            }

            SectionCard(titleRes = R.string.home_can_title) {
                Text(text = stringResource(R.string.home_can_1))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.home_can_2))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.home_can_3))
            }

            SectionCard(titleRes = R.string.home_cannot_title) {
                Text(text = stringResource(R.string.home_cannot_1))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.home_cannot_2))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.home_cannot_3))
            }

            Button(
                onClick = { onNavigate(DexBoostRoutes.APPS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.home_button_apps)) }
            OutlinedButton(
                onClick = { onNavigate(DexBoostRoutes.SETUP) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.home_button_setup)) }
            OutlinedButton(
                onClick = { onNavigate(DexBoostRoutes.HISTORY) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.home_button_history)) }
            OutlinedButton(
                onClick = { onNavigate(DexBoostRoutes.QUEUE) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.home_button_queue)) }
            OutlinedButton(
                onClick = { onNavigate(DexBoostRoutes.SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.home_button_settings)) }
        }
    }
}
