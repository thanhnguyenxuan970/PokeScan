package com.pokescan.app.ui.navigation

import android.content.SharedPreferences
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pokescan.app.data.local.SecureStorage
import com.pokescan.app.data.remote.AuthEventBus
import com.pokescan.app.data.repository.AuthRepository
import com.pokescan.app.ui.auth.SignInScreen
import com.pokescan.app.ui.collection.CollectionScreen
import com.pokescan.app.ui.onboarding.OnboardingScreen
import com.pokescan.app.ui.paywall.PaywallScreen
import com.pokescan.app.ui.scanner.ScannerScreen
import kotlinx.coroutines.launch

object Routes {
    const val ONBOARDING  = "onboarding"
    const val SIGN_IN     = "sign_in"
    const val MAIN        = "main"
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
    authEventBus: AuthEventBus,
    authRepository: AuthRepository,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = remember {
        when {
            secureStorage.getToken() != null -> Routes.MAIN
            prefs.getBoolean("isGuest", false) -> Routes.MAIN
            !prefs.getBoolean("hasSeenOnboarding", false) -> Routes.ONBOARDING
            else -> Routes.SIGN_IN
        }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authEventBus.unauthorizedEvents.collect {
            val current = navController.currentDestination?.route
            if (current != Routes.SIGN_IN && current != Routes.ONBOARDING) {
                navController.navigate(Routes.SIGN_IN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val handleSignOut: () -> Unit = {
        scope.launch {
            authRepository.signOut()
            prefs.edit().putBoolean("isGuest", false).apply()
            navController.navigate(Routes.SIGN_IN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
            SignInScreen(
                onAuthSuccess = {
                    prefs.edit().putBoolean("isGuest", false).apply()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onGuestMode = {
                    prefs.edit().putBoolean("isGuest", true).apply()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onPaywall = { navController.navigate(Routes.PAYWALL) },
                onSignOut = handleSignOut,
            )
        }

        composable(Routes.PAYWALL) {
            PaywallScreen(onDismiss = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainScreen(onPaywall: () -> Unit, onSignOut: () -> Unit) {
    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.SCANNER,
                    onClick = {
                        innerNav.navigate(Routes.SCANNER) {
                            popUpTo(Routes.SCANNER) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scanner") },
                    label = { Text("Scanner") },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.COLLECTION,
                    onClick = {
                        innerNav.navigate(Routes.COLLECTION) {
                            popUpTo(Routes.SCANNER) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Style, contentDescription = "Collection") },
                    label = { Text("Collection") },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNav,
            startDestination = Routes.SCANNER,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.SCANNER) {
                ScannerScreen(
                    onShowPaywall = onPaywall,
                    onViewCollection = { innerNav.navigate(Routes.COLLECTION) },
                )
            }
            composable(Routes.COLLECTION) {
                CollectionScreen(onSignOut = onSignOut)
            }
        }
    }
}
