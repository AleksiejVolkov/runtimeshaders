package com.offmind.runtimeshaders.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackEffect
import com.offmind.runtimeshaders.ui.theme.AppAppearance
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Keep the original file and back-effect key so existing selections survive the settings expansion.
private val Context.appSettingsDataStore by preferencesDataStore(name = "back_effect_settings")

internal data class AppSettings(
    val backEffect: PredictiveBackEffect = PredictiveBackEffect.default,
    val appearance: AppAppearance = AppAppearance.default,
)

internal class AppSettingsRepository(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.appSettingsDataStore)

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppSettings(
                backEffect = PredictiveBackEffect.fromId(preferences[SelectedBackEffectKey]),
                appearance = AppAppearance.fromId(preferences[AppearanceKey]),
            )
        }

    suspend fun selectBackEffect(effect: PredictiveBackEffect) {
        dataStore.edit { it[SelectedBackEffectKey] = effect.id }
    }

    suspend fun selectAppearance(appearance: AppAppearance) {
        dataStore.edit { it[AppearanceKey] = appearance.id }
    }

    private companion object {
        val SelectedBackEffectKey = stringPreferencesKey("selected_back_effect")
        val AppearanceKey = stringPreferencesKey("app_appearance")
    }
}
