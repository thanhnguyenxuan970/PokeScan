package com.pokescan.app.ui.paywall

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokescan.app.config.AppConfig

private val proFeatures = listOf(
    "Unlimited scans" to "every day, every card",
    "Grade ROI" to "should you grade it?",
    "Fake detection" to "risk score every scan",
    "Price alerts" to "track up to 50 cards",
    "Unlimited collection" to "free tier: 50 cards",
)

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? ComponentActivity ?: return

    LaunchedEffect(isPro) {
        if (isPro) onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Unlock Pro",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Unlimited scans, all markets, Grade ROI",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            proFeatures.forEach { (title, subtitle) ->
                ProFeatureRow(title = title, subtitle = subtitle)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            val monthly = products.firstOrNull { it.productId == "com.pokescan.app.pro.monthly" }
            val annual = products.firstOrNull { it.productId == "com.pokescan.app.pro.annual" }

            if (monthly != null) {
                val offer = monthly.subscriptionOfferDetails
                    ?.firstOrNull { it.offerTags.contains("base-plan") }
                    ?: monthly.subscriptionOfferDetails?.lastOrNull()
                val price = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice ?: ""
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
                val offer = annual.subscriptionOfferDetails
                    ?.firstOrNull { it.offerTags.contains("base-plan") }
                    ?: annual.subscriptionOfferDetails?.lastOrNull()
                val price = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice ?: ""
                OutlinedButton(
                    onClick = {
                        offer?.offerToken?.let { token ->
                            viewModel.purchase(activity, annual, token)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Annual $price · Save 35%")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { viewModel.restorePurchases() }) {
                Text("Restore Purchases")
            }

            TextButton(onClick = {
                if (AppConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRIVACY_POLICY_URL)))
                }
            }) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProFeatureRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
