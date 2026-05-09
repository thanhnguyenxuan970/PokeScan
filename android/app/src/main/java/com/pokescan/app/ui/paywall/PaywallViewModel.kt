package com.pokescan.app.ui.paywall

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.pokescan.app.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ViewModel
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingRepository.isPro
    val products: StateFlow<List<ProductDetails>> = billingRepository.products

    init {
        billingRepository.queryProducts()
    }

    fun purchase(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        billingRepository.purchase(activity, productDetails, offerToken)
    }

    fun restorePurchases() {
        billingRepository.restorePurchases()
    }
}
