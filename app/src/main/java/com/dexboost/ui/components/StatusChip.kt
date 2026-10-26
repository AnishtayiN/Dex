package com.dexboost.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dexboost.R
import com.dexboost.ui.theme.CyanAccentDark
import com.dexboost.ui.theme.ElectricGreenDark
import com.dexboost.ui.theme.WarningAmber

enum class ChipTone {
    POSITIVE, NEGATIVE, NEUTRAL, WARNING, INFO
}

/** Small coloured label, used for Shizuku status, queue status and outcomes. */
@Composable
fun StatusChip(
    label: String,
    tone: ChipTone,
    modifier: Modifier = Modifier
) {
    val background: Color = when (tone) {
        ChipTone.POSITIVE -> ElectricGreenDark
        ChipTone.NEGATIVE -> MaterialTheme.colorScheme.error
        ChipTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        ChipTone.WARNING -> WarningAmber
        ChipTone.INFO -> CyanAccentDark
    }
    val foreground: Color = when (tone) {
        ChipTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        ChipTone.WARNING -> Color(0xFF451A03)
        else -> Color.White
    }
    val description = stringResource(R.string.status_dot_desc, label)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = foreground)
    }
}
