package com.snapdex.app.data.service

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.snapdex.app.data.local.dao.SetEntryDao
import com.snapdex.app.data.local.entity.toDomain
import com.snapdex.app.data.local.entity.toEntity
import com.snapdex.app.data.remote.dto.PokemonTCGSetsResponse
import com.snapdex.app.data.remote.dto.SetEntryDto
import com.snapdex.app.di.PlainOkHttpClient
import com.snapdex.app.domain.model.SetEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

private val Context.setDbDataStore by preferencesDataStore(name = "set_db_prefs")
private val LAST_REFRESH_KEY = longPreferencesKey("set_db_last_refresh_ms")
private const val REFRESH_INTERVAL_MS = 86_400_000L
private const val API_URL = "https://api.pokemontcg.io/v2/sets"
private const val TAG = "SetDatabaseService"

@Singleton
class SetDatabaseService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val setEntryDao: SetEntryDao,
    @PlainOkHttpClient private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val sets: StateFlow<List<SetEntry>> = setEntryDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            if (setEntryDao.getAll().isEmpty()) seedFromBundle()
            refreshIfNeeded()
        }
    }

    private suspend fun seedFromBundle() {
        try {
            val json = context.assets.open("set_database.json").bufferedReader().use { it.readText() }
            val type = Types.newParameterizedType(List::class.java, SetEntryDto::class.java)
            val adapter = moshi.adapter<List<SetEntryDto>>(type)
            val entries = adapter.fromJson(json) ?: return
            setEntryDao.upsertAll(entries.map { it.toDomain().toEntity() })
        } catch (e: Exception) {
            Log.e(TAG, "Bundle seed failed — sets will be empty until API refresh", e)
        }
    }

    suspend fun refreshIfNeeded() {
        val lastRefresh = context.setDbDataStore.data.first()[LAST_REFRESH_KEY] ?: 0L
        if (System.currentTimeMillis() - lastRefresh < REFRESH_INTERVAL_MS) return
        fetchFromAPI()
    }

    private suspend fun fetchFromAPI() {
        try {
            val request = Request.Builder().url(API_URL).build()
            val body = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return
                response.body?.string() ?: return
            }
            val adapter = moshi.adapter(PokemonTCGSetsResponse::class.java)
            val response = adapter.fromJson(body) ?: return
            val entries = response.data.map { dto ->
                SetEntry(
                    setCode = dto.id,
                    name = dto.name,
                    total = dto.total,
                    printedTotal = dto.printedTotal,
                    releaseYear = dto.releaseDate.take(4).toIntOrNull() ?: 0,
                    series = dto.series,
                    language = if (dto.id.contains("-jp")) "japanese" else "english",
                )
            }
            setEntryDao.replaceAll(entries.map { it.toEntity() })
            context.setDbDataStore.edit { prefs ->
                prefs[LAST_REFRESH_KEY] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "API refresh failed — retaining existing data", e)
        }
    }
}
