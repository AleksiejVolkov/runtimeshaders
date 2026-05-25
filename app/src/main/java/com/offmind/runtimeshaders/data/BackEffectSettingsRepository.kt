package com.offmind.runtimeshaders.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backEffectDataStore by preferencesDataStore(name = "back_effect_settings")

internal class BackEffectSettingsRepository(
    context: Context
) {
    private val dataStore = context.applicationContext.backEffectDataStore

    val selectedEffect: Flow<PredictiveBackEffect> = dataStore.data.map { preferences ->
        PredictiveBackEffect.fromId(preferences[SelectedBackEffectKey])
    }

    suspend fun selectEffect(effect: PredictiveBackEffect) {
        dataStore.edit { preferences ->
            preferences[SelectedBackEffectKey] = effect.id
        }
    }

    private companion object {
        val SelectedBackEffectKey = stringPreferencesKey("selected_back_effect")
    }
}
