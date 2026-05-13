package com.pokescan.app.ui.paywall

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pokescan.app.config.AppConfig

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val isPro by viewModel.isPro.collectAsState()
    val products by viewModel.products.collectAsState()
    val activity = LocalContext.current as? ComponentActivity ?: return

    LaunchedEffect(isPro) {
        if (isPro) onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Go Pro",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Unlimited scans, all markets, Grade ROI",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        val monthly = products.firstOrNull { it.productId == "com.pokescan.app.pro.monthly" }
        val annual = products.firstOrNull { it.productId == "com.pokescan.app.pro.annual" }

        if (monthly != null) {
            val offer = monthly.subscriptionOfferDetails?.firstOrNull()
            val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
            Button(
                onClick = {
                    offer?.offerToken?.let { token ->
                        viewModel.purchase(activity, monthly, token)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Monthly $price")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (annual != null) {
            val offer = annual.subscriptionOfferDetails?.firstOrNull()
            val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
            OutlinedButton(
                onClick = {
                    offer?.offerToken?.let { token ->
                        viewModel.purchase(activity, annual, token)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Annual $price")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRIVACY_POLICY_URL)))
        }) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(onClick = { viewModel.restorePurchases() }) {
            Text("Restore Purchases")
        }

        TextButton(onClick = onDismiss) {
            Text("Not now")
        }
    }
}
