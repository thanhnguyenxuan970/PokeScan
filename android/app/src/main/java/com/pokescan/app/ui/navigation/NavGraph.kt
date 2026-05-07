package com.pokescan.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val ONBOARDING  = "onboarding"
    const val SIGN_IN     = "sign_in"
    const val SCANNER     = "scanner"
    const val COLLECTION  = "collection"
    const val CARD_DETAIL = "card_detail"
    const val GRADE_ROI   = "grade_roi"
    const val PAYWALL     = "paywall"
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.SCANNER
    ) {
        // Placeholder — Phase 3 replaces with ScannerScreen.
        composable(Routes.SCANNER) { Box(content = {}) }

        // Phase 2–4: remaining composable() calls added here as screens are implemented.
    }
}
