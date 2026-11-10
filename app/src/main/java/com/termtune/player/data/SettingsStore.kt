package com.termtune.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.termtune.player.ui.theme.TerminalTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.termTuneDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "termtune_settings")

/** Everything TermTune remembers between launches. */
data class AppPreferences(
    val theme: TerminalTheme = TerminalTheme.GREEN,
    val history: List<String> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
)

/** Persistent settings backed by Preferences DataStore. */
class SettingsStore(context: Context) {

    private val dataStore = context.termTuneDataStore

    val preferences: Flow<AppPreferences> = dataStore.data.map { stored ->
        AppPreferences(
            theme = TerminalTheme.fromKey(stored[KEY_THEME]) ?: TerminalTheme.GREEN,
            history = decodeHistory(stored[KEY_HISTORY]),
            shuffleEnabled = stored[KEY_SHUFFLE] ?: false,
            repeatEnabled = stored[KEY_REPEAT] ?: false,
        )
    }

    suspend fun setTheme(theme: TerminalTheme) {
        dataStore.edit { it[KEY_THEME] = theme.key }
    }

    suspend fun setShuffle(enabled: Boolean) {
        dataStore.edit { it[KEY_SHUFFLE] = enabled }
    }

    suspend fun setRepeat(enabled: Boolean) {
        dataStore.edit { it[KEY_REPEAT] = enabled }
    }

    /** Appends a command, collapsing immediate repeats and capping the list. */
    suspend fun pushHistory(command: String) {
        val entry = command.trim()
        if (entry.isEmpty()) return
        dataStore.edit { stored ->
            val current = decodeHistory(stored[KEY_HISTORY])
            if (current.lastOrNull() == entry) return@edit
            val updated = (current + entry).takeLast(HISTORY_LIMIT)
            stored[KEY_HISTORY] = updated.joinToString(SEPARATOR)
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { it[KEY_HISTORY] = "" }
    }

    private fun decodeHistory(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    private companion object {
        const val HISTORY_LIMIT = 40
        const val SEPARATOR = "\n"

        val KEY_THEME = stringPreferencesKey("terminal_theme")
        val KEY_HISTORY = stringPreferencesKey("command_history")
        val KEY_SHUFFLE = booleanPreferencesKey("shuffle_enabled")
        val KEY_REPEAT = booleanPreferencesKey("repeat_enabled")
    }
}
