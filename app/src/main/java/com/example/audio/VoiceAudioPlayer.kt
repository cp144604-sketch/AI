package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "VoiceAudioPlayer"

class VoiceAudioPlayer(private val context: Context) {

  private var playbackJob: Job? = null
  private var activeAudioTrack: AudioTrack? = null
  private var activeMediaPlayer: MediaPlayer? = null
  private var tempAudioFile: File? = null

  private val _playingMessageId = MutableStateFlow<String?>(null)
  val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

  private val _amplitude = MutableStateFlow(0f)
  val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

  fun play(
    scope: CoroutineScope,
    messageId: String,
    audioBytes: ByteArray,
    mimeType: String?,
    onFinished: () -> Unit = {},
    onError: (String) -> Unit = {},
  ) {
    stop()
    _playingMessageId.value = messageId

    playbackJob = scope.launch {
      try {
        val parsedWav = parseWavIfApplicable(audioBytes)
        if (parsedWav != null) {
          playPcmViaAudioTrack(
            pcmData = parsedWav.pcmData,
            sampleRate = parsedWav.sampleRate,
            channels = parsedWav.channels,
          )
        } else if (isRawPcmMimeType(mimeType)) {
          val rate = extractSampleRateFromMime(mimeType) ?: 24000
          playPcmViaAudioTrack(
            pcmData = audioBytes,
            sampleRate = rate,
            channels = 1,
          )
        } else {
          playEncodedAudioViaMediaPlayer(audioBytes, mimeType)
        }
        onFinished()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Audio playback failed", e)
        onError(e.localizedMessage ?: "Audio playback failed.")
      } finally {
        cleanupMediaResources()
        if (_playingMessageId.value == messageId) {
          _playingMessageId.value = null
          _amplitude.value = 0f
        }
      }
    }
  }

  fun stop() {
    playbackJob?.cancel()
    playbackJob = null
    cleanupMediaResources()
    _playingMessageId.value = null
    _amplitude.value = 0f
  }

  private suspend fun playPcmViaAudioTrack(
    pcmData: ByteArray,
    sampleRate: Int,
    channels: Int,
  ) = withContext(Dispatchers.IO) {
    val channelMask = if (channels >= 2) {
      AudioFormat.CHANNEL_OUT_STEREO
    } else {
      AudioFormat.CHANNEL_OUT_MONO
    }
    val minBuf = AudioTrack.getMinBufferSize(
      sampleRate,
      channelMask,
      AudioFormat.ENCODING_PCM_16BIT,
    )
    val bufferSize = maxOf(minBuf * 2, 8192)

    val track = AudioTrack.Builder()
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()
      )
      .setAudioFormat(
        AudioFormat.Builder()
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setSampleRate(sampleRate)
          .setChannelMask(channelMask)
          .build()
      )
      .setBufferSizeInBytes(bufferSize)
      .setTransferMode(AudioTrack.MODE_STREAM)
      .build()

    activeAudioTrack = track
    track.play()

    val chunkSize = 2048
    var offset = 0
    while (isActive && offset < pcmData.size) {
      val toWrite = minOf(chunkSize, pcmData.size - offset)
      _amplitude.value = computeNormalizedRms(pcmData, offset, toWrite)
      val written = track.write(pcmData, offset, toWrite)
      if (written < 0) {
        throw IllegalStateException("AudioTrack write failed with code $written")
      }
      offset += written
    }

    // Allow remaining audio in buffer to drain before releasing
    if (isActive) {
      try {
        track.stop()
      } catch (_: Exception) {
      }
    }
  }

  private suspend fun playEncodedAudioViaMediaPlayer(
    audioBytes: ByteArray,
    mimeType: String?,
  ) = withContext(Dispatchers.IO) {
    val ext = when {
      mimeType?.contains("mp3", ignoreCase = true) == true ||
        mimeType?.contains("mpeg", ignoreCase = true) == true -> "mp3"
      mimeType?.contains("ogg", ignoreCase = true) == true -> "ogg"
      mimeType?.contains("aac", ignoreCase = true) == true ||
        mimeType?.contains("mp4", ignoreCase = true) == true -> "m4a"
      else -> "wav"
    }
    val file = File(context.cacheDir, "vaani_voice_${System.currentTimeMillis()}.$ext")
    file.writeBytes(audioBytes)
    tempAudioFile = file

    val completionLatch = kotlinx.coroutines.CompletableDeferred<Unit>()
    val player = MediaPlayer().apply {
      setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()
      )
      setDataSource(file.absolutePath)
      setOnCompletionListener {
        completionLatch.complete(Unit)
      }
      setOnErrorListener { _, what, extra ->
        completionLatch.completeExceptionally(
          IllegalStateException("MediaPlayer error what=$what extra=$extra")
        )
        true
      }
      prepare()
      start()
    }
    activeMediaPlayer = player

    // Pulse gentle synthetic amplitude while MediaPlayer plays
    var phase = 0.0
    while (isActive && !completionLatch.isCompleted) {
      phase += 0.45
      _amplitude.value = (0.35f + 0.45f * kotlin.math.abs(kotlin.math.sin(phase)).toFloat())
      kotlinx.coroutines.delay(60L)
    }
    completionLatch.await()
  }

  private fun cleanupMediaResources() {
    try {
      activeAudioTrack?.pause()
      activeAudioTrack?.flush()
      activeAudioTrack?.release()
    } catch (_: Exception) {
    }
    activeAudioTrack = null

    try {
      activeMediaPlayer?.stop()
      activeMediaPlayer?.release()
    } catch (_: Exception) {
    }
    activeMediaPlayer = null

    try {
      tempAudioFile?.delete()
    } catch (_: Exception) {
    }
    tempAudioFile = null
  }

  private data class ParsedWav(
    val sampleRate: Int,
    val channels: Int,
    val pcmData: ByteArray,
  )

  private fun parseWavIfApplicable(bytes: ByteArray): ParsedWav? {
    if (bytes.size < 44) return null
    val header = String(bytes, 0, 4, Charsets.US_ASCII)
    val wave = String(bytes, 8, 4, Charsets.US_ASCII)
    if (header != "RIFF" || wave != "WAVE") return null

    var pos = 12
    var sampleRate = 24000
    var channels = 1
    var dataOffset = -1
    var dataLength = 0

    while (pos + 8 <= bytes.size) {
      val chunkId = String(bytes, pos, 4, Charsets.US_ASCII)
      val chunkSize = ByteBuffer.wrap(bytes, pos + 4, 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .int
      val chunkStart = pos + 8
      if (chunkSize < 0 || chunkStart + chunkSize > bytes.size) {
        if (chunkId == "data" && chunkStart < bytes.size) {
          dataOffset = chunkStart
          dataLength = bytes.size - chunkStart
        }
        break
      }

      if (chunkId == "fmt " && chunkSize >= 16) {
        val fmtBuf = ByteBuffer.wrap(bytes, chunkStart, 16).order(ByteOrder.LITTLE_ENDIAN)
        fmtBuf.short // audioFormat
        channels = fmtBuf.short.toInt().coerceAtLeast(1)
        sampleRate = fmtBuf.int.coerceAtLeast(8000)
      } else if (chunkId == "data") {
        dataOffset = chunkStart
        dataLength = chunkSize
        break
      }

      pos = chunkStart + chunkSize + (chunkSize and 1)
    }

    if (dataOffset < 0 || dataLength <= 0) return null
    val pcm = bytes.copyOfRange(dataOffset, minOf(bytes.size, dataOffset + dataLength))
    return ParsedWav(sampleRate = sampleRate, channels = channels, pcmData = pcm)
  }

  private fun isRawPcmMimeType(mimeType: String?): Boolean {
    if (mimeType == null) return false
    val lower = mimeType.lowercase()
    return lower.contains("audio/l16") || lower.contains("audio/pcm")
  }

  private fun extractSampleRateFromMime(mimeType: String?): Int? {
    if (mimeType == null) return null
    val match = Regex("""rate=(\d+)""", RegexOption.IGNORE_CASE).find(mimeType)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull()
  }

  private fun computeNormalizedRms(buffer: ByteArray, offset: Int, length: Int): Float {
    if (length < 2) return 0f
    var sumSquares = 0.0
    val samples = length / 2
    var i = offset
    val end = offset + length
    while (i + 1 < end) {
      val low = buffer[i].toInt() and 0xFF
      val high = buffer[i + 1].toInt()
      val sample = ((high shl 8) or low).toShort().toDouble()
      sumSquares += sample * sample
      i += 2
    }
    val rms = sqrt(sumSquares / samples.coerceAtLeast(1))
    return (rms / 10000.0).toFloat().coerceIn(0.08f, 1f)
  }
}
