package com.example.voctrainer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_LESSON_IDS = stringSetPreferencesKey("selected_lesson_ids")
    }

    val selectedLessonIds: Flow<Set<Long>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_LESSON_IDS]?.map { it.toLong() }?.toSet() ?: emptySet()
        }

    suspend fun setSelectedLessonIds(lessonIds: Set<Long>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LESSON_IDS] = lessonIds.map { it.toString() }.toSet()
        }
    }
}
