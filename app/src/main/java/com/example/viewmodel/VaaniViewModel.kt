package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.VaaniAiRepository
import com.example.audio.DeviceTtsSpeaker
import com.example.audio.VoiceAudioPlayer
import com.example.audio.VoiceRecorder
import com.example.data.CompanionSettings
import com.example.data.UserPreferencesRepository
import com.example.model.ChatMessage
import com.example.model.CompanionMood
import com.example.model.CompanionStatus
import com.example.model.FemaleVoicePersona
import com.example.model.LiveCallState
import com.example.model.PreferredLanguage
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "VaaniViewModel"

@OptIn(PublicPreviewAPI::class)
class VaaniViewModel(application: Application) : AndroidViewModel(application) {

  private val aiRepository = VaaniAiRepository()
  private val preferencesRepository = UserPreferencesRepository(application)
  private val voiceRecorder = VoiceRecorder()
  private val voiceAudioPlayer = VoiceAudioPlayer(application)
  private val deviceTtsSpeaker = DeviceTtsSpeaker(application)

  val settings: StateFlow<CompanionSettings> = preferencesRepository.settingsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = CompanionSettings(),
    )

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _isThinking = MutableStateFlow(false)
  private val _isSynthesizingVoice = MutableStateFlow(false)

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  val isRecording: StateFlow<Boolean> = voiceRecorder.isRecording
  val recordingAmplitude: StateFlow<Float> = voiceRecorder.amplitude
  val recordingSeconds: StateFlow<Int> = voiceRecorder.elapsedSeconds

  val playingMessageId: StateFlow<String?> = combine(
    voiceAudioPlayer.playingMessageId,
    deviceTtsSpeaker.speakingMessageId,
  ) { cloudId, deviceId ->
    cloudId ?: deviceId
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = null,
  )

  val playbackAmplitude: StateFlow<Float> = combine(
    voiceAudioPlayer.amplitude,
    deviceTtsSpeaker.speakingMessageId,
  ) { cloudAmp, deviceId ->
    if (deviceId != null) 0.55f else cloudAmp
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = 0f,
  )

  val status: StateFlow<CompanionStatus> = combine(
    voiceRecorder.isRecording,
    _isThinking,
    _isSynthesizingVoice,
    playingMessageId,
  ) { recording, thinking, synthesizing, playingId ->
    when {
      recording -> CompanionStatus.RECORDING
      thinking -> CompanionStatus.THINKING
      synthesizing -> CompanionStatus.GENERATING_VOICE
      playingId != null -> CompanionStatus.SPEAKING
      else -> CompanionStatus.IDLE
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = CompanionStatus.IDLE,
  )

  private val _liveCallState = MutableStateFlow<LiveCallState>(LiveCallState.Inactive)
  val liveCallState: StateFlow<LiveCallState> = _liveCallState.asStateFlow()

  private var activeLiveSession: LiveSession? = null
  private var liveTimerJob: Job? = null
  private var chatGenerationJob: Job? = null

  fun sendTextMessage(rawText: String) {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty() || _isThinking.value) return

    stopAllPlayback()
    _errorMessage.value = null

    val userMsg = ChatMessage(
      id = UUID.randomUUID().toString(),
      isFromUser = true,
      text = trimmed,
      isVoiceInput = false,
    )
    _messages.update { it + userMsg }

    streamAssistantReply {
      val currentSettings = settings.value
      aiRepository.sendTextMessageStream(
        userText = trimmed,
        persona = currentSettings.voicePersona,
        mood = currentSettings.mood,
        language = currentSettings.language,
      )
    }
  }

  fun startRecording() {
    if (_isThinking.value) return
    stopAllPlayback()
    _errorMessage.value = null
    voiceRecorder.start(viewModelScope) { err ->
      _errorMessage.value = err
    }
  }

  fun stopRecordingAndSend() {
    viewModelScope.launch {
      val wavBytes = voiceRecorder.stopAndGetWav()
      if (wavBytes == null) {
        _errorMessage.value = "અવાજ બહુ ટૂંકો હતો, ફરીથી થોડું વધુ બોલીને પ્રયાસ કરો."
        return@launch
      }

      _errorMessage.value = null
      val userMsg = ChatMessage(
        id = UUID.randomUUID().toString(),
        isFromUser = true,
        text = "વૉઇસ મેસેજ મોકલ્યો",
        isVoiceInput = true,
      )
      _messages.update { it + userMsg }

      streamAssistantReply {
        val currentSettings = settings.value
        aiRepository.sendVoiceMessageStream(
          wavBytes = wavBytes,
          persona = currentSettings.voicePersona,
          mood = currentSettings.mood,
          language = currentSettings.language,
        )
      }
    }
  }

  fun cancelRecording() {
    voiceRecorder.cancel()
  }

  private fun streamAssistantReply(
    streamProvider: () -> kotlinx.coroutines.flow.Flow<String>,
  ) {
    chatGenerationJob?.cancel()
    chatGenerationJob = viewModelScope.launch {
      val assistantMsgId = UUID.randomUUID().toString()
      val initialAssistantMsg = ChatMessage(
        id = assistantMsgId,
        isFromUser = false,
        text = "",
        isStreaming = true,
      )
      _messages.update { it + initialAssistantMsg }
      _isThinking.value = true

      val accumulated = StringBuilder()
      try {
        withTimeout(60_000L) {
          streamProvider().collect { chunk ->
            if (chunk.isNotEmpty()) {
              accumulated.append(chunk)
              val latest = accumulated.toString()
              _messages.update { list ->
                list.map { msg ->
                  if (msg.id == assistantMsgId) msg.copy(text = latest) else msg
                }
              }
            }
          }
        }

        val finalReply = accumulated.toString().trim()
        if (finalReply.isEmpty()) {
          _messages.update { list -> list.filterNot { it.id == assistantMsgId } }
          _errorMessage.value = "Model returned an empty response. Please try again."
          return@launch
        }

        _messages.update { list ->
          list.map { msg ->
            if (msg.id == assistantMsgId) {
              msg.copy(text = finalReply, isStreaming = false)
            } else {
              msg
            }
          }
        }
        _isThinking.value = false

        if (settings.value.autoSpeakReplies) {
          synthesizeAndPlayMessage(assistantMsgId, finalReply)
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Chat generation timed out", e)
        _messages.update { list -> list.filterNot { it.id == assistantMsgId && it.text.isBlank() } }
        _errorMessage.value = "જવાબ આવવામાં સમય લાગ્યો (60s timeout). ફરી પ્રયાસ કરો."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Chat generation failed", e)
        _messages.update { list ->
          list.mapNotNull { msg ->
            if (msg.id == assistantMsgId && msg.text.isBlank()) null
            else if (msg.id == assistantMsgId) msg.copy(isStreaming = false)
            else msg
          }
        }
        _errorMessage.value = e.localizedMessage ?: "AI સાથે જોડાણમાં મુશ્કેલી આવી."
      } finally {
        _isThinking.value = false
      }
    }
  }

  fun toggleMessagePlayback(messageId: String) {
    if (playingMessageId.value == messageId) {
      stopAllPlayback()
      return
    }

    val message = _messages.value.firstOrNull { it.id == messageId } ?: return
    val cachedAudio = message.audioBytes
    if (cachedAudio != null) {
      deviceTtsSpeaker.stop()
      voiceAudioPlayer.play(
        scope = viewModelScope,
        messageId = messageId,
        audioBytes = cachedAudio,
        mimeType = message.audioMimeType,
        onError = { err ->
          updateMessageSynthesisState(messageId, isSynthesizing = false, voiceError = err)
        },
      )
    } else {
      viewModelScope.launch {
        synthesizeAndPlayMessage(messageId, message.text)
      }
    }
  }

  fun speakViaDeviceVoice(messageId: String) {
    if (playingMessageId.value == messageId) {
      stopAllPlayback()
      return
    }
    val message = _messages.value.firstOrNull { it.id == messageId } ?: return
    voiceAudioPlayer.stop()
    deviceTtsSpeaker.speak(
      messageId = messageId,
      text = message.text,
      language = settings.value.language,
    )
  }

  private suspend fun synthesizeAndPlayMessage(messageId: String, text: String) {
    _isSynthesizingVoice.value = true
    updateMessageSynthesisState(messageId, isSynthesizing = true, voiceError = null)
    try {
      val persona = settings.value.voicePersona
      val audio = aiRepository.synthesizeFemaleVoice(text = text, persona = persona)

      _messages.update { list ->
        list.map { msg ->
          if (msg.id == messageId) {
            msg.copy(
              audioBytes = audio.bytes,
              audioMimeType = audio.mimeType,
              isSynthesizingVoice = false,
              voiceError = null,
            )
          } else {
            msg
          }
        }
      }
      _isSynthesizingVoice.value = false

      deviceTtsSpeaker.stop()
      voiceAudioPlayer.play(
        scope = viewModelScope,
        messageId = messageId,
        audioBytes = audio.bytes,
        mimeType = audio.mimeType,
        onError = { err ->
          updateMessageSynthesisState(messageId, isSynthesizing = false, voiceError = err)
        },
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "TTS timed out", e)
      updateMessageSynthesisState(
        messageId,
        isSynthesizing = false,
        voiceError = "Cloud voice timed out (60s).",
      )
    } catch (e: CancellationException) {
      updateMessageSynthesisState(messageId, isSynthesizing = false, voiceError = null)
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "TTS generation failed", e)
      val msg = e.localizedMessage ?: "Cloud voice synthesis failed."
      updateMessageSynthesisState(messageId, isSynthesizing = false, voiceError = msg)
    } finally {
      _isSynthesizingVoice.value = false
    }
  }

  private fun updateMessageSynthesisState(
    messageId: String,
    isSynthesizing: Boolean,
    voiceError: String?,
  ) {
    _messages.update { list ->
      list.map { msg ->
        if (msg.id == messageId) {
          msg.copy(isSynthesizingVoice = isSynthesizing, voiceError = voiceError)
        } else {
          msg
        }
      }
    }
  }

  fun startLiveCall() {
    if (_liveCallState.value is LiveCallState.Connecting || _liveCallState.value is LiveCallState.Active) {
      return
    }
    stopAllPlayback()
    voiceRecorder.cancel()
    _errorMessage.value = null
    _liveCallState.value = LiveCallState.Connecting

    viewModelScope.launch {
      try {
        val currentSettings = settings.value
        val session = aiRepository.connectLiveVoiceSession(
          persona = currentSettings.voicePersona,
          mood = currentSettings.mood,
          language = currentSettings.language,
        )
        activeLiveSession = session
        _liveCallState.value = LiveCallState.Active()
        startLiveTimer()

        session.startAudioConversation(
          functionCallHandler = null,
          transcriptHandler = { userTranscript, assistantTranscript ->
            _liveCallState.update { state ->
              if (state is LiveCallState.Active) {
                state.copy(
                  userTranscript = userTranscript?.text?.takeIf { it.isNotBlank() } ?: state.userTranscript,
                  assistantTranscript = assistantTranscript?.text?.takeIf { it.isNotBlank() }
                    ?: state.assistantTranscript,
                )
              } else {
                state
              }
            }
          },
          enableInterruptions = true,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Live voice call failed", e)
        closeLiveSessionResources()
        _liveCallState.value = LiveCallState.Error(
          e.localizedMessage ?: "લાઇવ વૉઇસ કૉલ શરૂ થઈ શક્યો નહીં."
        )
      }
    }
  }

  fun toggleLiveMute() {
    val current = _liveCallState.value
    val session = activeLiveSession ?: return
    if (current !is LiveCallState.Active) return

    viewModelScope.launch {
      try {
        if (current.isMuted) {
          _liveCallState.value = current.copy(isMuted = false)
          session.startAudioConversation(
            functionCallHandler = null,
            transcriptHandler = { userTranscript, assistantTranscript ->
              _liveCallState.update { state ->
                if (state is LiveCallState.Active) {
                  state.copy(
                    userTranscript = userTranscript?.text?.takeIf { it.isNotBlank() } ?: state.userTranscript,
                    assistantTranscript = assistantTranscript?.text?.takeIf { it.isNotBlank() }
                      ?: state.assistantTranscript,
                  )
                } else {
                  state
                }
              }
            },
            enableInterruptions = true,
          )
        } else {
          session.stopAudioConversation()
          _liveCallState.value = current.copy(isMuted = true)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Toggle mute failed", e)
        _liveCallState.value = LiveCallState.Error(
          e.localizedMessage ?: "માઇક્રોફોન બદલવામાં ભૂલ આવી."
        )
      }
    }
  }

  fun endLiveCall() {
    viewModelScope.launch {
      closeLiveSessionResources()
      _liveCallState.value = LiveCallState.Inactive
    }
  }

  private fun startLiveTimer() {
    liveTimerJob?.cancel()
    liveTimerJob = viewModelScope.launch {
      while (isActive && _liveCallState.value is LiveCallState.Active) {
        delay(1_000L)
        _liveCallState.update { state ->
          if (state is LiveCallState.Active) {
            state.copy(elapsedSeconds = state.elapsedSeconds + 1)
          } else {
            state
          }
        }
      }
    }
  }

  private suspend fun closeLiveSessionResources() {
    liveTimerJob?.cancel()
    liveTimerJob = null
    val session = activeLiveSession
    activeLiveSession = null
    if (session != null) {
      try {
        session.stopAudioConversation()
      } catch (_: Exception) {
      }
      try {
        session.close()
      } catch (_: Exception) {
      }
    }
  }

  fun updateVoicePersona(persona: FemaleVoicePersona) {
    viewModelScope.launch {
      preferencesRepository.setVoicePersona(persona)
    }
  }

  fun updateMood(mood: CompanionMood) {
    viewModelScope.launch {
      preferencesRepository.setMood(mood)
    }
  }

  fun updateLanguage(language: PreferredLanguage) {
    viewModelScope.launch {
      preferencesRepository.setLanguage(language)
    }
  }

  fun updateAutoSpeak(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setAutoSpeakReplies(enabled)
      if (!enabled) {
        stopAllPlayback()
      }
    }
  }

  fun clearConversation() {
    chatGenerationJob?.cancel()
    stopAllPlayback()
    voiceRecorder.cancel()
    aiRepository.resetConversation()
    _messages.value = emptyList()
    _isThinking.value = false
    _isSynthesizingVoice.value = false
    _errorMessage.value = null
  }

  fun dismissError() {
    _errorMessage.value = null
  }

  private fun stopAllPlayback() {
    voiceAudioPlayer.stop()
    deviceTtsSpeaker.stop()
  }

  override fun onCleared() {
    super.onCleared()
    voiceAudioPlayer.stop()
    deviceTtsSpeaker.release()
    voiceRecorder.cancel()
    liveTimerJob?.cancel()
    activeLiveSession?.let { session ->
      try {
        session.stopAudioConversation()
      } catch (_: Exception) {
      }
    }
  }
}
