package com.pokescan.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pokescan.app.data.local.SecureStorage
import com.pokescan.app.data.remote.AuthEventBus
import com.pokescan.app.data.repository.AuthRepository
import com.pokescan.app.ui.navigation.NavGraph
import com.pokescan.app.ui.theme.PokeScanTheme
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
            PokeScanTheme {
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
