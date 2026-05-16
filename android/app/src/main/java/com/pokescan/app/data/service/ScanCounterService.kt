package com.pokescan.app.data.service

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private val Context.scanDataStore by preferencesDataStore(name = "scan_counter_prefs")
private val SCAN_COUNT_KEY = intPreferencesKey("pokescan.scanCount")
private val SCAN_RESET_DATE_KEY = longPreferencesKey("pokescan.scanCountResetDate")

@Singleton
class ScanCounterService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val FREE_MONTHLY_LIMIT = 10
    }

    private val mutex = Mutex()

    val scansThisMonth: Flow<Int> = context.scanDataStore.data.map { it[SCAN_COUNT_KEY] ?: 0 }

    suspend fun canScan(isPro: Boolean): Boolean {
        if (isPro) return true
        return mutex.withLock {
            resetIfNewMonth()
            (context.scanDataStore.data.first()[SCAN_COUNT_KEY] ?: 0) < FREE_MONTHLY_LIMIT
        }
    }

    suspend fun recordScan(isPro: Boolean) {
        if (isPro) return
        mutex.withLock {
            resetIfNewMonth()
            context.scanDataStore.edit { prefs ->
                prefs[SCAN_COUNT_KEY] = (prefs[SCAN_COUNT_KEY] ?: 0) + 1
            }
        }
    }

    suspend fun resetCount() {
        context.scanDataStore.edit { prefs ->
            prefs[SCAN_COUNT_KEY] = 0
            prefs[SCAN_RESET_DATE_KEY] = System.currentTimeMillis()
        }
    }

    private suspend fun resetIfNewMonth() {
        val storedMs = context.scanDataStore.data.first()[SCAN_RESET_DATE_KEY] ?: 0L
        val stored = Calendar.getInstance().apply { timeInMillis = storedMs }
        val now = Calendar.getInstance()
        val sameMonth = stored.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            stored.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        if (!sameMonth) {
            context.scanDataStore.edit { prefs ->
                prefs[SCAN_COUNT_KEY] = 0
                prefs[SCAN_RESET_DATE_KEY] = now.timeInMillis
            }
        }
    }
}
