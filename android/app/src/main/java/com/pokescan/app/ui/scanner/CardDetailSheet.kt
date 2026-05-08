package com.pokescan.app.ui.scanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokescan.app.domain.model.Card
import com.pokescan.app.domain.model.CardLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailSheet(
    card: Card,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${card.setCode.uppercase()} · ${card.setNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = card.marketPrice?.let { "$${"%.2f".format(it)}" } ?: "—",
                style = MaterialTheme.typography.displaySmall,
            )
            card.priceSource?.let { source ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = source.raw.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (card.language == CardLanguage.JAPANESE) {
                Spacer(modifier = Modifier.height(12.dp))
                SuggestionChip(
                    onClick = {},
                    label = { Text("Japanese") },
                )
            }
        }
    }
}
