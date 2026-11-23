package com.example.model

import androidx.compose.runtime.Immutable

enum class FemaleVoicePersona(
  val voiceId: String,
  val displayNameGu: String,
  val displayNameEn: String,
  val vibeTaglineGu: String,
  val bcp47Hint: String,
) {
  AOEDE(
    voiceId = "Aoede",
    displayNameGu = "વાણી",
    displayNameEn = "Vaani",
    vibeTaglineGu = "મધુર, સહજ અને દિલથી વાત કરનાર અવાજ",
    bcp47Hint = "gu-IN",
  ),
  ZEPHYR(
    voiceId = "Zephyr",
    displayNameGu = "આરાધ્યા",
    displayNameEn = "Aaradhya",
    vibeTaglineGu = "ઉત્સાહી, તેજસ્વી અને ખુશમિજાજ અવાજ",
    bcp47Hint = "gu-IN",
  ),
  LEDA(
    voiceId = "Leda",
    displayNameGu = "મીરા",
    displayNameEn = "Meera",
    vibeTaglineGu = "યુવાન, મીઠો અને જીવંત સહેલી જેવો અવાજ",
    bcp47Hint = "gu-IN",
  ),
  KORE(
    voiceId = "Kore",
    displayNameGu = "કાવ્યા",
    displayNameEn = "Kavya",
    vibeTaglineGu = "શાંત, સમજદાર અને ઊંડાણપૂર્વક વાત કરનાર અવાજ",
    bcp47Hint = "gu-IN",
  ),
}

enum class CompanionMood(
  val labelGu: String,
  val labelEn: String,
  val promptBehavior: String,
) {
  FRIENDLY_BESTIE(
    labelGu = "મિત્ર જેવી",
    labelEn = "Warm Bestie",
    promptBehavior = "Talk like a warm, caring, real female best friend — natural, empathetic, expressive, using casual human warmth rather than formal robotic language.",
  ),
  SMART_DEEP(
    labelGu = "બુદ્ધિશાળી",
    labelEn = "Deep Thinker",
    promptBehavior = "Talk like an insightful, articulate, curious young woman who loves deep discussions on science, philosophy, psychology, technology, and life while staying warm and conversational.",
  ),
  PLAYFUL_WITTY(
    labelGu = "મજાકિયા",
    labelEn = "Playful & Witty",
    promptBehavior = "Talk with lighthearted humor, gentle teasing, sparkle, and lively curiosity like a cheerful young woman chatting over coffee.",
  ),
  CALM_CARING(
    labelGu = "શાંત અને માયાળુ",
    labelEn = "Calm & Gentle",
    promptBehavior = "Talk softly, thoughtfully, and reassuringly like a calm, attentive companion who listens deeply and brings peace to the conversation.",
  ),
}

enum class PreferredLanguage(
  val labelGu: String,
  val labelEn: String,
  val instruction: String,
) {
  AUTO(
    labelGu = "ઑટો (ગુજરાતી / હિન્દી / Eng)",
    labelEn = "Auto Match",
    instruction = "Match the language the user speaks or writes in naturally (Gujarati by default if starting fresh, or Hindi/English/Hinglish if the user uses those).",
  ),
  GUJARATI(
    labelGu = "ગુજરાતી",
    labelEn = "Gujarati",
    instruction = "Always reply in natural, everyday spoken Gujarati script (સરળ અને મીઠી બોલચાલની ગુજરાતી) unless the user specifically asks for another language.",
  ),
  HINDI(
    labelGu = "હિન્દી",
    labelEn = "Hindi",
    instruction = "Always reply in natural, everyday spoken Hindi (सरल और अपनापन भरी हिंदी).",
  ),
  ENGLISH(
    labelGu = "English",
    labelEn = "English",
    instruction = "Always reply in natural, warm conversational English.",
  ),
}

enum class CompanionStatus {
  IDLE,
  RECORDING,
  THINKING,
  GENERATING_VOICE,
  SPEAKING,
}

@Immutable
data class ChatMessage(
  val id: String,
  val isFromUser: Boolean,
  val text: String,
  val isVoiceInput: Boolean = false,
  val audioBytes: ByteArray? = null,
  val audioMimeType: String? = null,
  val isStreaming: Boolean = false,
  val isSynthesizingVoice: Boolean = false,
  val voiceError: String? = null,
  val timestampMillis: Long = System.currentTimeMillis(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChatMessage) return false
    return id == other.id &&
      isFromUser == other.isFromUser &&
      text == other.text &&
      isVoiceInput == other.isVoiceInput &&
      audioMimeType == other.audioMimeType &&
      isStreaming == other.isStreaming &&
      isSynthesizingVoice == other.isSynthesizingVoice &&
      voiceError == other.voiceError &&
      timestampMillis == other.timestampMillis &&
      audioBytes.contentEquals(other.audioBytes)
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + isFromUser.hashCode()
    result = 31 * result + text.hashCode()
    result = 31 * result + isVoiceInput.hashCode()
    result = 31 * result + (audioBytes?.contentHashCode() ?: 0)
    result = 31 * result + (audioMimeType?.hashCode() ?: 0)
    result = 31 * result + isStreaming.hashCode()
    result = 31 * result + isSynthesizingVoice.hashCode()
    result = 31 * result + (voiceError?.hashCode() ?: 0)
    result = 31 * result + timestampMillis.hashCode()
    return result
  }
}

@Immutable
data class TopicStarter(
  val titleGu: String,
  val prompt: String,
  val categoryTag: String,
)

sealed interface LiveCallState {
  data object Inactive : LiveCallState
  data object Connecting : LiveCallState
  data class Active(
    val isMuted: Boolean = false,
    val userTranscript: String = "",
    val assistantTranscript: String = "",
    val elapsedSeconds: Int = 0,
  ) : LiveCallState
  data class Error(val message: String) : LiveCallState
}

object CompanionTopics {
  val starters = listOf(
    TopicStarter(
      titleGu = "આજની મીઠી વાત",
      prompt = "હાય! મારો આજનો દિવસ થોડો વ્યસ્ત હતો, ચાલો થોડી હળવી અને મજાની વાતો કરીએ.",
      categoryTag = "Daily Chat",
    ),
    TopicStarter(
      titleGu = "AI અને ભવિષ્ય",
      prompt = "આર્ટિફિશિયલ ઈન્ટેલિજન્સ આવનારા ૧૦ વર્ષમાં આપણું જીવન કેવી રીતે બદલશે? ચાલો ચર્ચા કરીએ.",
      categoryTag = "Tech & AI",
    ),
    TopicStarter(
      titleGu = "ગુજરાતી શાયરી અને વાર્તા",
      prompt = "મને એક સરસ નાની પ્રેરણાદાયી વાર્તા અથવા હૃદયસ્પર્શી ગુજરાતી શાયરી સંભળાવો.",
      categoryTag = "Poetry",
    ),
    TopicStarter(
      titleGu = "અંગ્રેજી બોલવાની પ્રેક્ટિસ",
      prompt = "મારે English speaking improve કરવું છે. તું મારી સાથે રોજિંદા અંગ્રેજીમાં પ્રેક્ટિસ કરાવીશ?",
      categoryTag = "Practice",
    ),
    TopicStarter(
      titleGu = "બ્રહ્માંડ અને વિજ્ઞાન",
      prompt = "બ્લેક હોલ અને ટાઇમ ટ્રાવેલ વિશે સરળ ભાષામાં સમજાવ, જાણે આપણે બંને સાથે બેસીને ચર્ચા કરતા હોઈએ!",
      categoryTag = "Science",
    ),
    TopicStarter(
      titleGu = "જીવન અને સફળતા",
      prompt = "જ્યારે મન થાકી જાય કે મૂંઝવણ અનુભવાય ત્યારે ફરીથી ઉત્સાહ કેવી રીતે લાવવો?",
      categoryTag = "Life Advice",
    ),
  )
}
