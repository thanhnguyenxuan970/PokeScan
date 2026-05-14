package com.pokescan.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokescan.app.domain.model.Card
import com.pokescan.app.domain.model.CardLanguage
import com.pokescan.app.domain.model.PriceSource
import com.pokescan.app.ui.theme.PokeScanGold
import com.pokescan.app.ui.theme.PokeScanRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailSheet(
    card: Card,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onViewCollection: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Sheet handle
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 16.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Card thumbnail + meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 90.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(6.dp),
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(PokeScanRed, Color(0xFFAA0000))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                ) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${card.setCode.uppercase()} · ${card.setNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (card.language == CardLanguage.JAPANESE) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("JP", style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Near Mint", style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Featured price block
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "MARKET PRICE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.marketPrice?.let { "$${"%.2f".format(it)}" } ?: "—",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    card.priceSource?.let { source ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = source.raw.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price grid — TCGPlayer | eBay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tcgPrice = if (card.priceSource == PriceSource.TCGPLAYER ||
                    card.priceSource == PriceSource.AGGREGATED) {
                    card.marketPrice?.let { "$${"%.2f".format(it)}" } ?: "—"
                } else "—"
                val ebayPrice = if (card.priceSource == PriceSource.EBAY ||
                    card.priceSource == PriceSource.AGGREGATED) {
                    card.marketPrice?.let { "$${"%.2f".format(it)}" } ?: "—"
                } else "—"

                PriceGridCell(label = "TCGPlayer", value = tcgPrice, modifier = Modifier.weight(1f))
                PriceGridCell(label = "eBay", value = ebayPrice, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROI block
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Grade ROI",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Surface(
                            color = PokeScanGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = PokeScanGold,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        val roiValue = if (isPro) "—" else "—"
                        RoiStatCell(label = "PSA 10 Est.", value = roiValue)
                        RoiStatCell(label = "Net ROI", value = roiValue)
                        RoiStatCell(label = "Break-even", value = roiValue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan Another")
            }

            TextButton(
                onClick = onViewCollection,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("View Collection")
            }
        }
    }
}

@Composable
private fun PriceGridCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun RoiStatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
