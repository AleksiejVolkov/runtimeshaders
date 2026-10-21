package com.offmind.runtimeshaders.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.offmind.runtimeshaders.navigation.predictive.PredictiveBackEffect
import com.offmind.runtimeshaders.ui.theme.AppAppearance
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `existing back effect preference is retained when appearance is introduced`() = runBlocking {
        val file = File(folder.root, "existing.preferences_pb")
        withStore(file) { store ->
            store.edit { it[stringPreferencesKey("selected_back_effect")] = "smoke_dissolve" }
            val repository = AppSettingsRepository(store)
            assertEquals(AppAppearance.Classic, repository.settings.first().appearance)
            assertEquals(PredictiveBackEffect.SmokeDissolve, repository.settings.first().backEffect)
            repository.selectAppearance(AppAppearance.Neumorphism)
        }
        withStore(file) { store ->
            assertEquals(
                AppSettings(PredictiveBackEffect.SmokeDissolve, AppAppearance.Neumorphism),
                AppSettingsRepository(store).settings.first(),
            )
        }
    }

    @Test
    fun `every appearance survives reopening the settings file`() = runBlocking {
        val file = File(folder.root, "appearances.preferences_pb")
        for (appearance in AppAppearance.entries) {
            withStore(file) { AppSettingsRepository(it).selectAppearance(appearance) }
            withStore(file) {
                assertEquals(appearance, AppSettingsRepository(it).settings.first().appearance)
            }
        }
    }

    @Test
    fun `changing either setting leaves the other selection intact`() = runBlocking {
        withStore(File(folder.root, "independent.preferences_pb")) { store ->
            val repository = AppSettingsRepository(store)
            repository.selectAppearance(AppAppearance.Glass)
            repository.selectBackEffect(PredictiveBackEffect.LiquidDrain)
            assertEquals(AppSettings(PredictiveBackEffect.LiquidDrain, AppAppearance.Glass), repository.settings.first())
            repository.selectAppearance(AppAppearance.Classic)
            assertEquals(AppSettings(PredictiveBackEffect.LiquidDrain, AppAppearance.Classic), repository.settings.first())
        }
    }

    @Test
    fun `missing or unknown saved IDs use defaults`() = runBlocking {
        withStore(File(folder.root, "unknown.preferences_pb")) { store ->
            val repository = AppSettingsRepository(store)
            assertEquals(AppSettings(), repository.settings.first())
            store.edit {
                it[stringPreferencesKey("app_appearance")] = "removed-style"
                it[stringPreferencesKey("selected_back_effect")] = "removed-effect"
            }
            assertEquals(AppSettings(), repository.settings.first())
        }
    }

    private suspend fun withStore(
        file: File,
        block: suspend (DataStore<Preferences>) -> Unit,
    ) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            block(store)
        } finally {
            scope.cancel()
            scope.coroutineContext[Job]!!.join()
        }
    }
}
