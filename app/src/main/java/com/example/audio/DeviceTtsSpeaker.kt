package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.PreferredLanguage
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "DeviceTtsSpeaker"

class DeviceTtsSpeaker(context: Context) {
  private var tts: TextToSpeech? = null
  private var isReady = false
  private var pendingText: String? = null
  private var pendingMessageId: String? = null
  private var pendingLanguage: PreferredLanguage = PreferredLanguage.AUTO

  private val _speakingMessageId = MutableStateFlow<String?>(null)
  val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

  init {
    tts = TextToSpeech(context.applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        isReady = true
        configureFemaleVoice(pendingLanguage, pendingText.orEmpty())
        val text = pendingText
        val msgId = pendingMessageId
        if (!text.isNullOrBlank() && msgId != null) {
          pendingText = null
          pendingMessageId = null
          speakInternal(msgId, text, pendingLanguage)
        }
      } else {
        Log.e(TAG, "Android TextToSpeech initialization failed with status=$status")
      }
    }.apply {
      setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          _speakingMessageId.value = utteranceId
        }

        override fun onDone(utteranceId: String?) {
          if (_speakingMessageId.value == utteranceId) {
            _speakingMessageId.value = null
          }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          if (_speakingMessageId.value == utteranceId) {
            _speakingMessageId.value = null
          }
        }
      })
    }
  }

  fun speak(messageId: String, text: String, language: PreferredLanguage) {
    if (!isReady) {
      pendingMessageId = messageId
      pendingText = text
      pendingLanguage = language
      return
    }
    speakInternal(messageId, text, language)
  }

  fun stop() {
    pendingText = null
    pendingMessageId = null
    try {
      tts?.stop()
    } catch (_: Exception) {
    }
    _speakingMessageId.value = null
  }

  fun release() {
    stop()
    try {
      tts?.shutdown()
    } catch (_: Exception) {
    }
    tts = null
  }

  private fun speakInternal(messageId: String, text: String, language: PreferredLanguage) {
    configureFemaleVoice(language, text)
    _speakingMessageId.value = messageId
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId)
  }

  private fun configureFemaleVoice(language: PreferredLanguage, text: String) {
    val engine = tts ?: return
    val locale = when (language) {
      PreferredLanguage.GUJARATI -> Locale("gu", "IN")
      PreferredLanguage.HINDI -> Locale("hi", "IN")
      PreferredLanguage.ENGLISH -> Locale("en", "IN")
      PreferredLanguage.AUTO -> detectLocaleFromText(text)
    }
    engine.language = locale
    engine.setPitch(1.12f)
    engine.setSpeechRate(0.96f)

    try {
      val femaleVoice = engine.voices?.firstOrNull { voice ->
        voice.locale.language == locale.language &&
          (voice.name.contains("female", ignoreCase = true) ||
            voice.name.contains("-f-", ignoreCase = true) ||
            voice.name.contains("in-x-", ignoreCase = true))
      }
      if (femaleVoice != null) {
        engine.voice = femaleVoice
      }
    } catch (_: Exception) {
    }
  }

  private fun detectLocaleFromText(text: String): Locale {
    val hasGujarati = text.any { it in '\u0A80'..'\u0AFF' }
    if (hasGujarati) return Locale("gu", "IN")
    val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
    if (hasDevanagari) return Locale("hi", "IN")
    return Locale("en", "IN")
  }
}
