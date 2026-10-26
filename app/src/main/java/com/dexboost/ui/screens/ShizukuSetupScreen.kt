@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.dexboost.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.dexboost.viewmodel.ShizukuViewModel

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Setup steps and troubleshooting are taken from the string resources, which
 * mirror ShizukuSetupGuide.steps() and troubleshooting() in order and content:
 * install the official app, enable Developer options / debugging, start and
 * pair Shizuku, grant DexBoost permission, restart Shizuku after a reboot;
 * troubleshooting covers not running, permission denied, binder died and
 * app not found. No guide text is hardcoded in Kotlin.
 */
@Composable
fun ShizukuSetupScreen(
    shizukuViewModel: ShizukuViewModel,
    modifier: Modifier = Modifier
) {
    val rawState by shizukuViewModel.state.collectAsState()
    val context = LocalContext.current
    // The manager never produces NotInstalled itself; combine the binder
    // state with the installed-app check, as its documentation prescribes.
    val state = remember(rawState, context.packageManager) {
        if (shizukuViewModel.isShizukuAppPresent(context.packageManager)) rawState
        else ShizukuState.NotInstalled
    }
    val ready = state == ShizukuState.Ready

    val steps = listOf(
        Pair(R.string.setup_step_1_title, R.string.setup_step_1_desc),
        Pair(R.string.setup_step_2_title, R.string.setup_step_2_desc),
        Pair(R.string.setup_step_3_title, R.string.setup_step_3_desc),
        Pair(R.string.setup_step_4_title, R.string.setup_step_4_desc),
        Pair(R.string.setup_step_5_title, R.string.setup_step_5_desc)
    )
    val troubles = listOf(
        Pair(R.string.setup_trouble_1_title, R.string.setup_trouble_1_desc),
        Pair(R.string.setup_trouble_2_title, R.string.setup_trouble_2_desc),
        Pair(R.string.setup_trouble_3_title, R.string.setup_trouble_3_desc),
        Pair(R.string.setup_trouble_4_title, R.string.setup_trouble_4_desc)
    )

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.setup_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(titleRes = R.string.setup_status_title) {
                StatusChip(
                    label = stringResource(shizukuLabelRes(state)),
                    tone = shizukuTone(state)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (ready) R.string.setup_ready_message
                        else R.string.setup_not_ready_message
                    )
                )
            }
            SectionCard(titleRes = R.string.setup_steps_title) {
                steps.forEachIndexed { index, step ->
                    Text(text = stringResource(R.string.setup_step_number, index + 1))
                    Text(text = stringResource(step.first))
                    Text(text = stringResource(step.second))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            SectionCard(titleRes = R.string.setup_troubleshooting_title) {
                troubles.forEach { trouble ->
                    Text(text = stringResource(trouble.first))
                    Text(text = stringResource(trouble.second))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Button(
                onClick = {
                    context.findActivity()?.let { shizukuViewModel.requestPermission(it) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.setup_button_request_permission)) }
            OutlinedButton(
                onClick = { shizukuViewModel.refresh() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.setup_button_retry)) }
            Text(text = stringResource(R.string.setup_reboot_note))
        }
    }
}
