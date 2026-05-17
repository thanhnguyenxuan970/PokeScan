package com.pokescan.app.ui.scanner

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokescan.app.data.service.ScanCounterService

@Composable
fun ScannerScreen(
    onShowPaywall: () -> Unit,
    onSaveToCollection: () -> Unit = {},
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val scansThisMonth by viewModel.scansThisMonth.collectAsStateWithLifecycle(initialValue = 0)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ScanEvent.ShowPaywall -> onShowPaywall()
                ScanEvent.NoCardDetected -> snackbarHostState.showSnackbar(
                    message = "No card detected. Try better lighting or hold card steady.",
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
    ) {
        ReticleOverlay(state = state, modifier = Modifier.fillMaxSize().padding(bottom = 96.dp))
        ScanCounterPill(
            scansUsed = scansThisMonth,
            limit = ScanCounterService.FREE_MONTHLY_LIMIT,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 60.dp, start = 16.dp),
        )
        ScanButton(
            state = state,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            onScan = { viewModel.startScan() },
            onReset = { viewModel.resetScan() },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
        )
        if (state is ScanState.Result) {
            CardDetailSheet(
                card = (state as ScanState.Result).card,
                isPro = isPro,
                onDismiss = { viewModel.resetScan() },
                onReset = { viewModel.resetScan() },
                onSaveToCollection = onSaveToCollection,
            )
        }
    }
}

@Composable
private fun ScanCounterPill(
    scansUsed: Int,
    limit: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0x99000000),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = "$scansUsed/$limit scans",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun ReticleOverlay(state: ScanState, modifier: Modifier = Modifier) {
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            is ScanState.Idle -> Color.White
            is ScanState.Scanning -> MaterialTheme.colorScheme.primary
            is ScanState.Result -> Color(0xFF22C55E)
        },
        label = "reticle-border",
    )
    val dimColor = Color(0x73000000)
    val reticleWidthFraction = 0.85f
    val cardAspect = 2.5f / 3.5f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rw = size.width * reticleWidthFraction
            val rh = rw / cardAspect
            val left = (size.width - rw) / 2f
            val top = (size.height - rh) / 2f
            val right = left + rw
            val bottom = top + rh
            drawRect(dimColor, topLeft = Offset.Zero, size = Size(size.width, top))
            drawRect(dimColor, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
            drawRect(dimColor, topLeft = Offset(0f, top), size = Size(left, rh))
            drawRect(dimColor, topLeft = Offset(right, top), size = Size(size.width - right, rh))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(reticleWidthFraction)
                .aspectRatio(cardAspect)
                .align(Alignment.Center)
                .border(
                    width = 3.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp),
                )
        )
    }
}

@Composable
private fun ScanButton(
    state: ScanState,
    modifier: Modifier = Modifier,
    onScan: () -> Unit,
    onReset: () -> Unit,
) {
    val label = when (state) {
        is ScanState.Idle -> "Scan"
        is ScanState.Scanning -> "Scanning…"
        is ScanState.Result -> "Scan Another"
    }
    val enabled = state is ScanState.Idle || state is ScanState.Result
    val onClick = if (state is ScanState.Result) onReset else onScan

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = Color.White.copy(alpha = 0.15f),
            disabledContentColor = Color.White.copy(alpha = 0.75f),
        ),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}
