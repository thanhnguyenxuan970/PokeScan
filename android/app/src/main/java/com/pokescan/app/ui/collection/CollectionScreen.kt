package com.snapdex.app.ui.collection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snapdex.app.data.local.entity.CardRecordEntity
import com.snapdex.app.data.local.entity.toDomain
import com.snapdex.app.domain.model.Card
import com.snapdex.app.ui.scanner.CardDetailSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onSignOut: () -> Unit = {},
    isGuest: Boolean = false,
    onCreateAccount: () -> Unit = {},
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isSelectMode by viewModel.isSelectMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAuthSignOutDialog by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<CardRecordEntity?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var detailCard by remember { mutableStateOf<Card?>(null) }

    if (showAuthSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showAuthSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("Your collection is saved to your account and will be available when you sign back in.") },
            confirmButton = {
                TextButton(onClick = { showAuthSignOutDialog = false; onSignOut() }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthSignOutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You're browsing as a guest. Your scanned cards will be lost when you sign out.") },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOut() }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false; onCreateAccount() }) {
                    Text("Create Account")
                }
            },
        )
    }

    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("Delete card?") },
            text = { Text("Are you sure you want to delete ${card.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard(card)
                    cardToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Delete ${selectedIds.size} cards?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    showBatchDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    detailCard?.let { card ->
        CardDetailSheet(
            card = card,
            isPro = isPro,
            onDismiss = { detailCard = null },
            onReset = { detailCard = null },
            onSaveToCollection = null,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Collection") },
            actions = {
                IconButton(onClick = { if (isGuest) showSignOutDialog = true else showAuthSignOutDialog = true }) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign out")
                }
            },
        )

        StatRow(cards = cards)
        HorizontalDivider()

        if (isSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { viewModel.clearSelectMode() }) { Text("Cancel") }
                Text(
                    text = "${selectedIds.size} selected",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = { if (selectedIds.isNotEmpty()) showBatchDeleteDialog = true },
                    enabled = selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                    ),
                ) {
                    Text("Delete")
                }
            }
        }

        when {
            syncState is SyncState.Loading && cards.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            cards.isNotEmpty() -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (syncState is SyncState.Error) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Couldn't sync",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(cards, key = { it.id }) { card ->
                            CardRow(
                                card = card,
                                isSelectMode = isSelectMode,
                                isSelected = card.id in selectedIds,
                                onLongPress = {
                                    if (isSelectMode) viewModel.toggleSelection(card.id)
                                    else viewModel.enterSelectMode(card.id)
                                },
                                onClick = { detailCard = card.toDomain() },
                                onDeleteClick = { cardToDelete = card },
                            )
                        }
                    }
                }
            }
            syncState is SyncState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Couldn't load collection",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No cards yet. Scan one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(cards: List<CardRecordEntity>) {
    val totalValue = cards.sumOf { it.marketPrice ?: 0.0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(
            label = "TOTAL VALUE",
            value = "$${"%.2f".format(totalValue)}",
            featured = true,
            modifier = Modifier.weight(1.4f),
        )
        StatCard(
            label = "CARDS",
            value = "${cards.size}",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "30D CHANGE",
            value = "—",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(0.dp))
            Text(
                text = value,
                style = if (featured) {
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    MaterialTheme.typography.titleSmall.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                color = if (featured) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardRow(
    card: CardRecordEntity,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectMode) onLongPress() else onClick() },
                onLongClick = { if (!isSelectMode) onLongPress() },
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${card.setCode} · ${card.setNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (card.marketPrice != null) {
            Text(
                text = "$${String.format("%.2f", card.marketPrice)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (isSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onLongPress() },
            )
        } else {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
