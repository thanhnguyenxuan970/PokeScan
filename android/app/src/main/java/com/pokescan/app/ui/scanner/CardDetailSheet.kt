package com.snapdex.app.ui.scanner

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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
import com.snapdex.app.domain.model.Card
import com.snapdex.app.domain.model.CardLanguage
import com.snapdex.app.domain.model.PriceSource
import com.snapdex.app.ui.theme.SnapDexGold
import com.snapdex.app.ui.theme.SnapDexRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailSheet(
    card: Card,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSaveToCollection: (() -> Unit)? = null,
    allCards: List<Card>? = null,
    initialIndex: Int = 0,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 16.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            if (allCards != null) {
                val pagerState = rememberPagerState(
                    initialPage = initialIndex,
                    pageCount = { allCards.size },
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    CardDetailContent(
                        card = allCards[page],
                        isPro = isPro,
                        onDismiss = onDismiss,
                        onReset = onReset,
                        onSaveToCollection = onSaveToCollection,
                    )
                }
            } else {
                CardDetailContent(
                    card = card,
                    isPro = isPro,
                    onDismiss = onDismiss,
                    onReset = onReset,
                    onSaveToCollection = onSaveToCollection,
                )
            }
        }
    }
}

@Composable
private fun CardDetailContent(
    card: Card,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSaveToCollection: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                            colors = listOf(SnapDexRed, Color(0xFFAA0000))
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
                val setDisplay = card.setName ?: card.setCode.uppercase()
                val yearSuffix = card.setYear?.let { " · $it" } ?: ""
                Text(
                    text = "$setDisplay · #${card.setNumber}$yearSuffix",
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
                    if (card.variant == "Holo") {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Holo", style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Near Mint", style = MaterialTheme.typography.labelSmall) },
                    )
                    if (card.isAuthentic == true) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "✓ Authentic",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF16A34A),
                                )
                            },
                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                    text = "MARKET PRICE · 30-DAY",
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
                val sourceLabel = when (card.priceSource) {
                    PriceSource.AGGREGATED -> "TCGPlayer + eBay weighted"
                    PriceSource.TCGPLAYER -> "TCGPlayer"
                    PriceSource.EBAY -> "eBay"
                    else -> null
                }
                val updatedSuffix = card.priceUpdatedAt?.let {
                    val hoursAgo = ((System.currentTimeMillis() - it) / 3_600_000).coerceAtLeast(0).toInt()
                    if (hoursAgo == 0) "just now" else "${hoursAgo}h ago"
                }
                val priceSubtitle = listOfNotNull(
                    sourceLabel,
                    updatedSuffix?.let { "updated $it" },
                ).joinToString(" · ")
                if (priceSubtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = priceSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tcgPrice = card.tcgPlayerPrice?.let { "$${"%.2f".format(it)}" } ?: "—"
            val ebayPrice = card.ebayPrice?.let { "$${"%.2f".format(it)}" } ?: "—"

            PriceGridCell(label = "TCGPLAYER", value = tcgPrice, modifier = Modifier.weight(1f))
            PriceGridCell(label = "EBAY SOLD", value = ebayPrice, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                        color = SnapDexGold.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "PRO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = SnapDexGold,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (isPro && card.gradeRoiPsaGrade != null) {
                        RoiStatCell(
                            label = "Predicted",
                            value = "PSA ${card.gradeRoiPsaGrade}",
                            green = true,
                        )
                        RoiStatCell(
                            label = "Sell value",
                            value = card.gradeRoiSellValue?.let { "$${"%.0f".format(it)}" } ?: "—",
                            green = true,
                        )
                        RoiStatCell(
                            label = "Net profit",
                            value = card.gradeRoiNetProfit?.let {
                                val sign = if (it >= 0) "+" else "-"
                                sign + "$" + "%.0f".format(kotlin.math.abs(it))
                            } ?: "—",
                            green = true,
                        )
                    } else {
                        RoiStatCell(label = "Predicted", value = "—")
                        RoiStatCell(label = "Sell value", value = "—")
                        RoiStatCell(label = "Net profit", value = "—")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (onSaveToCollection != null) {
            Button(
                onClick = onSaveToCollection,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save to Collection")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan another")
            }
        } else {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Close")
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
private fun RoiStatCell(label: String, value: String, green: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (green) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
