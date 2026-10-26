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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dexboost.R
import com.dexboost.optimization.ArtCompilationManager
import com.dexboost.ui.components.SectionCard
import com.dexboost.ui.components.StatusChip
import com.dexboost.viewmodel.OptimizationViewModel

@Composable
fun QueueScreen(
    optimizationViewModel: OptimizationViewModel,
    modifier: Modifier = Modifier
) {
    val queue by optimizationViewModel.queueState.collectAsState()
    val isFinished = queue.items.isNotEmpty() &&
        queue.remainingCount == 0 && queue.currentPackage == null

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.queue_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (queue.items.isEmpty()) {
                Text(text = stringResource(R.string.queue_empty))
            } else {
                Text(
                    text = stringResource(
                        R.string.queue_counts,
                        queue.completedCount,
                        queue.failedCount,
                        queue.remainingCount
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                queue.currentPackage?.let {
                    Text(text = stringResource(R.string.queue_current, it))
                }
                if (isFinished) {
                    // The title must reflect the outcome, not just that
                    // the batch ended. A batch with 0 completed and
                    // 1 failed is a failure, not a completion.
                    val finishedTitleRes = when {
                        queue.failedCount > 0 && queue.completedCount == 0 ->
                            R.string.queue_art_failed
                        queue.failedCount > 0 ->
                            R.string.queue_art_finished_mixed
                        else -> R.string.queue_art_completed
                    }
                    SectionCard(titleRes = finishedTitleRes) {
                        Text(
                            text = stringResource(
                                R.string.queue_summary_text,
                                queue.completedCount,
                                queue.failedCount,
                                queue.items.size
                            )
                        )
                        Text(text = stringResource(R.string.queue_summary_note))
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(queue.items, key = { it.packageName + it.mode.name }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.packageName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = stringResource(modeNameRes(item.mode)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                item.result?.exitCode?.let { exitCode ->
                                    Text(
                                        text = stringResource(R.string.history_exit_code, exitCode),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                // Show the error text when there is one, so a
                                // failed item is not left unexplained. The
                                // full output stays in History.
                                item.result?.stderr?.takeIf { it.isNotBlank() }?.let { stderr ->
                                    Text(
                                        text = stderr.take(200),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            // A finished item's chip must show its actual
                            // outcome (Failed, Permission denied, Unknown,
                            // ...), not a generic green "Done" that hides
                            // a failure.
                            val chipLabelRes = if (
                                item.status == ArtCompilationManager.QueueStatus.DONE &&
                                item.result != null
                            ) {
                                outcomeLabelRes(item.result.interpret())
                            } else {
                                queueStatusLabelRes(item.status)
                            }
                            val chipTone = if (
                                item.status == ArtCompilationManager.QueueStatus.DONE &&
                                item.result != null
                            ) {
                                outcomeTone(item.result.interpret())
                            } else {
                                queueStatusTone(item.status)
                            }
                            StatusChip(
                                label = stringResource(chipLabelRes),
                                tone = chipTone
                            )
                        }
                    }
                }
                if (!isFinished) {
                    OutlinedButton(
                        onClick = { optimizationViewModel.cancelBatch() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.queue_cancel)) }
                }
            }
        }
    }
}
