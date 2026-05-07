package com.pokescan.app.ui.navigation

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pokescan.app.data.local.SecureStorage
import com.pokescan.app.ui.auth.SignInScreen
import com.pokescan.app.ui.onboarding.OnboardingScreen

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
fun NavGraph(
    secureStorage: SecureStorage,
    prefs: SharedPreferences,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = remember {
        when {
            secureStorage.getToken() != null -> Routes.SCANNER
            !prefs.getBoolean("hasSeenOnboarding", false) -> Routes.ONBOARDING
            else -> Routes.SIGN_IN
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onGetStarted = {
                prefs.edit().putBoolean("hasSeenOnboarding", true).apply()
                navController.navigate(Routes.SIGN_IN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Routes.SIGN_IN) {
            SignInScreen(onAuthSuccess = {
                navController.navigate(Routes.SCANNER) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }

        // Placeholder — Phase 3 replaces with ScannerScreen.
        composable(Routes.SCANNER) { Box(content = {}) }

        // Phase 4: remaining composable() calls added here as screens are implemented.
    }
}
