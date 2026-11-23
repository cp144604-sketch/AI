package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CompanionMood
import com.example.model.FemaleVoicePersona
import com.example.model.PreferredLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaaniDataStore: DataStore<Preferences> by preferencesDataStore(name = "vaani_preferences")

data class CompanionSettings(
  val voicePersona: FemaleVoicePersona = FemaleVoicePersona.AOEDE,
  val mood: CompanionMood = CompanionMood.FRIENDLY_BESTIE,
  val language: PreferredLanguage = PreferredLanguage.AUTO,
  val autoSpeakReplies: Boolean = true,
)

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val VOICE_PERSONA = stringPreferencesKey("voice_persona")
    val COMPANION_MOOD = stringPreferencesKey("companion_mood")
    val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
    val AUTO_SPEAK_REPLIES = booleanPreferencesKey("auto_speak_replies")
  }

  val settingsFlow: Flow<CompanionSettings> = context.vaaniDataStore.data.map { prefs ->
    val persona = prefs[Keys.VOICE_PERSONA]?.let { name ->
      FemaleVoicePersona.entries.firstOrNull { it.name == name }
    } ?: FemaleVoicePersona.AOEDE

    val mood = prefs[Keys.COMPANION_MOOD]?.let { name ->
      CompanionMood.entries.firstOrNull { it.name == name }
    } ?: CompanionMood.FRIENDLY_BESTIE

    val language = prefs[Keys.PREFERRED_LANGUAGE]?.let { name ->
      PreferredLanguage.entries.firstOrNull { it.name == name }
    } ?: PreferredLanguage.AUTO

    val autoSpeak = prefs[Keys.AUTO_SPEAK_REPLIES] ?: true

    CompanionSettings(
      voicePersona = persona,
      mood = mood,
      language = language,
      autoSpeakReplies = autoSpeak,
    )
  }

  suspend fun setVoicePersona(persona: FemaleVoicePersona) {
    context.vaaniDataStore.edit { it[Keys.VOICE_PERSONA] = persona.name }
  }

  suspend fun setMood(mood: CompanionMood) {
    context.vaaniDataStore.edit { it[Keys.COMPANION_MOOD] = mood.name }
  }

  suspend fun setLanguage(language: PreferredLanguage) {
    context.vaaniDataStore.edit { it[Keys.PREFERRED_LANGUAGE] = language.name }
  }

  suspend fun setAutoSpeakReplies(enabled: Boolean) {
    context.vaaniDataStore.edit { it[Keys.AUTO_SPEAK_REPLIES] = enabled }
  }
}
