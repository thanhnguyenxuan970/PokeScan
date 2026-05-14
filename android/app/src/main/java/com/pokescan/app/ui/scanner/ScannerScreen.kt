package com.pokescan.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment as UiAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokescan.app.BuildConfig

@Composable
fun ScannerScreen(
    onShowPaywall: () -> Unit,
    onViewCollection: () -> Unit = {},
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermanentDenial by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasCameraPermission = true
            showPermanentDenial = false
        } else {
            val activity = context as? android.app.Activity
            val canShowRationale = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            showPermanentDenial = !canShowRationale
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ScanEvent.ShowPaywall -> onShowPaywall()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onSurfaceProviderReady = { surfaceProvider ->
                    viewModel.startCamera(lifecycleOwner, surfaceProvider)
                },
            )
            ReticleOverlay(state = state, modifier = Modifier.fillMaxSize())
            ScanButton(
                state = state,
                modifier = Modifier
                    .align(UiAlignment.BottomCenter)
                    .padding(bottom = 64.dp),
                onScan = { viewModel.startScan() },
                onReset = { viewModel.resetScan() },
            )
            if (BuildConfig.DEBUG) {
                IconButton(
                    onClick = { viewModel.triggerMockScan() },
                    modifier = Modifier
                        .align(UiAlignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Mock scan",
                        tint = Color.Yellow,
                    )
                }
            }
        } else {
            PermissionRationale(
                isPermanentlyDenied = showPermanentDenial,
                onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                },
                modifier = Modifier
                    .align(UiAlignment.Center)
                    .padding(32.dp),
            )
        }

        if (state is ScanState.Result) {
            CardDetailSheet(
                card = (state as ScanState.Result).card,
                isPro = isPro,
                onDismiss = { viewModel.resetScan() },
                onReset = { viewModel.resetScan() },
                onViewCollection = onViewCollection,
            )
        }
    }
}

@Composable
private fun ReticleOverlay(state: ScanState, modifier: Modifier = Modifier) {
    val borderColor = when (state) {
        is ScanState.Idle -> Color.White
        is ScanState.Scanning -> Color.Yellow
        is ScanState.Detected -> Color.Green
        is ScanState.Loading -> Color(0xFF2196F3)
        is ScanState.Result -> Color.Green
    }
    val dimColor = Color(0x73000000)
    val reticleWidthFraction = 0.85f
    val cardAspect = 2.5f / 3.5f

    Box(modifier = modifier) {
        // Draw 4 dim rectangles around the reticle — no blendMode tricks needed
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
        // Reticle border overlay
        Box(
            modifier = Modifier
                .fillMaxWidth(reticleWidthFraction)
                .aspectRatio(cardAspect)
                .align(UiAlignment.Center)
                .border(
                    width = 2.dp,
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
        is ScanState.Idle -> "Tap to Scan"
        is ScanState.Scanning -> "Scanning…"
        is ScanState.Detected -> "Card Detected"
        is ScanState.Loading -> "Fetching Price…"
        is ScanState.Result -> "Scan Another"
    }
    val enabled = state is ScanState.Idle || state is ScanState.Result
    val onClick = if (state is ScanState.Result) onReset else onScan

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(label)
    }
}

@Composable
private fun PermissionRationale(
    isPermanentlyDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = UiAlignment.CenterHorizontally,
    ) {
        Text(
            text = "Camera access required",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isPermanentlyDenied)
                "Camera permission was denied. Enable it in Settings to scan cards."
            else
                "PokeScan needs camera access to scan Pokémon cards.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (isPermanentlyDenied) {
            OutlinedButton(onClick = onOpenSettings) { Text("Open Settings") }
        } else {
            Button(onClick = onGrant) { Text("Grant Camera Access") }
        }
    }
}
