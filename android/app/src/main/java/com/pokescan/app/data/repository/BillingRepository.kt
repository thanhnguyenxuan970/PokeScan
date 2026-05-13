package com.pokescan.app.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.pokescan.app.data.remote.ApiService
import com.pokescan.app.data.remote.dto.AndroidVerifyReceiptRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(::onPurchasesUpdated)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryAndVerifyEntitlements()
                    queryProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected — reconnecting")
                billingClient.startConnection(this)
            }
        })
    }

    private fun queryAndVerifyEntitlements() {
        scope.launch {
            val purchases = queryPurchasesAsync().filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            for (purchase in purchases) {
                val productId = purchase.products.firstOrNull() ?: continue
                try {
                    val response = apiService.verifyAndroidReceipt(
                        AndroidVerifyReceiptRequest(productId, purchase.purchaseToken)
                    )
                    if (response.active) {
                        _isPro.value = true
                        acknowledgePurchaseIfNeeded(purchase)
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "queryAndVerifyEntitlements verify failed: ${e.message}")
                }
            }
        }
    }

    fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            _products.value = productDetailsList
        }
    }

    fun purchase(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun restorePurchases() {
        scope.launch {
            val purchased = queryPurchasesAsync().filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            for (purchase in purchased) {
                val productId = purchase.products.firstOrNull() ?: continue
                try {
                    val response = apiService.verifyAndroidReceipt(
                        AndroidVerifyReceiptRequest(productId, purchase.purchaseToken)
                    )
                    if (response.active) {
                        _isPro.value = true
                        acknowledgePurchaseIfNeeded(purchase)
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "restorePurchases verify failed: ${e.message}")
                }
            }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        scope.launch {
            val productId = purchase.products.firstOrNull() ?: return@launch
            try {
                val response = apiService.verifyAndroidReceipt(
                    AndroidVerifyReceiptRequest(productId, purchase.purchaseToken)
                )
                if (response.active) {
                    _isPro.value = true
                    acknowledgePurchaseIfNeeded(purchase)
                }
            } catch (e: Exception) {
                Log.w(TAG, "handlePurchase verify failed: ${e.message}")
            }
        }
    }

    private suspend fun queryPurchasesAsync(): List<Purchase> = suspendCancellableCoroutine { cont ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS).build()
        billingClient.queryPurchasesAsync(params) { _, purchases ->
            cont.resume(purchases)
        }
    }

    private suspend fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            suspendCancellableCoroutine { cont ->
                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG, "acknowledgePurchase failed: ${result.responseCode} ${result.debugMessage}")
                    }
                    cont.resume(Unit)
                }
            }
        }
    }

    private companion object {
        const val TAG = "BillingRepository"
        const val PRODUCT_MONTHLY = "com.pokescan.app.pro.monthly"
        const val PRODUCT_ANNUAL = "com.pokescan.app.pro.annual"
    }
}
