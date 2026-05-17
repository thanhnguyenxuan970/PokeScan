package com.snapdex.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.snapdex.app.data.local.SecureStorage
import com.snapdex.app.data.remote.AuthEventBus
import com.snapdex.app.data.repository.AuthRepository
import com.snapdex.app.ui.navigation.NavGraph
import com.snapdex.app.ui.theme.SnapDexTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var secureStorage: SecureStorage
    @Inject lateinit var authEventBus: AuthEventBus
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        setContent {
            SnapDexTheme {
                NavGraph(
                    secureStorage = secureStorage,
                    prefs = prefs,
                    authEventBus = authEventBus,
                    authRepository = authRepository,
                )
            }
        }
    }
}
