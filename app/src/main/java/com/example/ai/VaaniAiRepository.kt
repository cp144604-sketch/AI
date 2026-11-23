package com.example.ai

import android.util.Log
import com.example.model.CompanionMood
import com.example.model.FemaleVoicePersona
import com.example.model.PreferredLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.LiveGenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

private const val TAG = "VaaniAiRepository"
private const val CHAT_MODEL_ID = "gemini-3.8-flash"
private val TTS_MODEL_CANDIDATES = listOf(
  "gemini-3.1-flash-tts-preview",
  "gemini-3.8-flash-tts",
  "gemini-3.8-flash-lite-tts",
)
private const val LIVE_MODEL_ID = "gemini-3.1-flash-live-preview"

data class SynthesizedAudio(
  val bytes: ByteArray,
  val mimeType: String,
)

@OptIn(PublicPreviewAPI::class)
class VaaniAiRepository {

  private var activeChat: Chat? = null
  private var activeConfigSignature: String? = null

  private fun buildSystemInstruction(
    persona: FemaleVoicePersona,
    mood: CompanionMood,
    language: PreferredLanguage,
  ): Content = content {
    text(
      """
      You are ${persona.displayNameEn} (${persona.displayNameGu}), a warm, natural, intelligent young woman AI voice companion.
      Personality style: ${mood.promptBehavior}
      Language preference: ${language.instruction}

      How you speak and converse:
      - Talk like a real girl chatting naturally with a close friend — never robotic, never overly formal, and never listicle-heavy unless the user explicitly asks for a list or steps.
      - Keep responses engaging, thoughtful, and comfortably spoken-length (usually 2 to 5 natural sentences so it feels great when spoken aloud, or slightly deeper when discussing complex topics like science, philosophy, technology, relationships, or life).
      - You can openly discuss ANY everyday, creative, emotional, scientific, cultural, tech, cricket, cinema, or life topic with genuine curiosity and warmth.
      - When speaking Gujarati, use natural, affectionate, everyday spoken Gujarati (not stiff textbook translation), and avoid awkward symbols or markdown asterisks (*, **, #) because your reply will be read aloud by a voice model.
      - Feel free to express gentle warmth, curiosity, and natural follow-up questions so the conversation flows like two real people talking.
      """.trimIndent()
    )
  }

  private fun getOrCreateChat(
    persona: FemaleVoicePersona,
    mood: CompanionMood,
    language: PreferredLanguage,
  ): Chat {
    val signature = "${persona.name}|${mood.name}|${language.name}"
    val existing = activeChat
    if (existing != null && activeConfigSignature == signature) {
      return existing
    }

    val history = existing?.history ?: emptyList()
    val model: GenerativeModel = Firebase.ai.generativeModel(
      modelName = CHAT_MODEL_ID,
      generationConfig = generationConfig {
        temperature = 0.85f
        topP = 0.95f
      },
      systemInstruction = buildSystemInstruction(persona, mood, language),
    )

    val newChat = model.startChat(history = history)
    activeChat = newChat
    activeConfigSignature = signature
    return newChat
  }

  fun resetConversation() {
    activeChat = null
    activeConfigSignature = null
  }

  fun sendTextMessageStream(
    userText: String,
    persona: FemaleVoicePersona,
    mood: CompanionMood,
    language: PreferredLanguage,
  ): Flow<String> {
    val chat = getOrCreateChat(persona, mood, language)
    return chat.sendMessageStream(userText).map { chunk -> chunk.text.orEmpty() }
  }

  fun sendVoiceMessageStream(
    wavBytes: ByteArray,
    persona: FemaleVoicePersona,
    mood: CompanionMood,
    language: PreferredLanguage,
  ): Flow<String> {
    val chat = getOrCreateChat(persona, mood, language)
    val voiceTurnContent = content("user") {
      inlineData(wavBytes, "audio/wav")
      text(
        "Listen carefully to my voice message and respond directly and naturally in character as ${persona.displayNameEn}. Do not transcribe verbatim unless helpful — just reply to what I said like a real conversation."
      )
    }
    return chat.sendMessageStream(voiceTurnContent).map { chunk -> chunk.text.orEmpty() }
  }

  suspend fun synthesizeFemaleVoice(
    text: String,
    persona: FemaleVoicePersona,
  ): SynthesizedAudio {
    val cleanSpeechText = sanitizeTextForSpeech(text)
    if (cleanSpeechText.isBlank()) {
      throw IllegalArgumentException("Cannot synthesize empty text.")
    }

    val ttsConfig = generationConfig {
      responseModalities = listOf(ResponseModality.AUDIO)
      speechConfig = SpeechConfig(voice = Voice(persona.voiceId))
    }

    var lastError: Exception? = null
    for (modelId in TTS_MODEL_CANDIDATES) {
      try {
        return requestSpeechAudio(modelId, ttsConfig, cleanSpeechText)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "TTS model $modelId failed: ${e.message}", e)
        lastError = e
      }
    }
    throw (lastError ?: IllegalStateException("Female voice synthesis failed."))
  }

  private suspend fun requestSpeechAudio(
    modelId: String,
    config: com.google.firebase.ai.type.GenerationConfig,
    speechText: String,
  ): SynthesizedAudio {
    val ttsModel by lazy {
      Firebase.ai.generativeModel(
        modelName = modelId,
        generationConfig = config,
      )
    }

    val response = withTimeout(60_000L) {
      ttsModel.generateContent(
        "Read the following reply aloud warmly, naturally, and expressively like a real young woman talking to a friend. Do not add extra words:\n\n$speechText"
      )
    }

    val audioPart = response.inlineDataParts.firstOrNull { part ->
      part.mimeType.startsWith("audio/", ignoreCase = true)
    } ?: response.inlineDataParts.firstOrNull()
      ?: throw IllegalStateException("Voice model returned no audio data.")

    return SynthesizedAudio(
      bytes = audioPart.inlineData,
      mimeType = audioPart.mimeType,
    )
  }

  suspend fun connectLiveVoiceSession(
    persona: FemaleVoicePersona,
    mood: CompanionMood,
    language: PreferredLanguage,
  ): LiveSession {
    val liveModel: LiveGenerativeModel by lazy {
      Firebase.ai.liveModel(
        modelName = LIVE_MODEL_ID,
        generationConfig = liveGenerationConfig {
          responseModality = ResponseModality.AUDIO
          speechConfig = SpeechConfig(voice = Voice(persona.voiceId))
          inputAudioTranscription = AudioTranscriptionConfig()
          outputAudioTranscription = AudioTranscriptionConfig()
        },
        systemInstruction = buildSystemInstruction(persona, mood, language),
      )
    }

    return withTimeout(30_000L) {
      liveModel.connect()
    }
  }

  private fun sanitizeTextForSpeech(raw: String): String {
    return raw
      .replace(Regex("""[*_`#>]"""), "")
      .replace(Regex("""\s+"""), " ")
      .trim()
  }
}
