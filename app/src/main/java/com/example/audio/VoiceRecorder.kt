package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceRecorder {
  companion object {
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val CHANNELS = 1
    private const val BITS_PER_SAMPLE = 16
  }

  private var audioRecord: AudioRecord? = null
  private var recordJob: Job? = null
  private var timerJob: Job? = null
  private val pcmBuffer = ByteArrayOutputStream()

  private val _isRecording = MutableStateFlow(false)
  val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

  private val _amplitude = MutableStateFlow(0f)
  val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

  private val _elapsedSeconds = MutableStateFlow(0)
  val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

  @SuppressLint("MissingPermission")
  fun start(scope: CoroutineScope, onError: (String) -> Unit) {
    if (_isRecording.value) return

    val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    if (minBufferSize <= 0) {
      onError("Audio recording is not supported on this device.")
      return
    }

    val bufferSize = maxOf(minBufferSize * 2, 4096)
    val recorder = try {
      AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        bufferSize,
      )
    } catch (e: Exception) {
      onError(e.localizedMessage ?: "Unable to initialize microphone.")
      return
    }

    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
      recorder.release()
      onError("Microphone failed to initialize.")
      return
    }

    synchronized(pcmBuffer) {
      pcmBuffer.reset()
    }

    audioRecord = recorder
    _isRecording.value = true
    _elapsedSeconds.value = 0
    _amplitude.value = 0f

    try {
      recorder.startRecording()
    } catch (e: Exception) {
      recorder.release()
      audioRecord = null
      _isRecording.value = false
      onError(e.localizedMessage ?: "Unable to start recording.")
      return
    }

    timerJob = scope.launch(Dispatchers.Default) {
      while (isActive && _isRecording.value) {
        delay(1000L)
        _elapsedSeconds.value += 1
      }
    }

    recordJob = scope.launch(Dispatchers.IO) {
      val readBuffer = ByteArray(bufferSize / 2)
      while (isActive && _isRecording.value) {
        val bytesRead = recorder.read(readBuffer, 0, readBuffer.size)
        if (bytesRead > 0) {
          synchronized(pcmBuffer) {
            pcmBuffer.write(readBuffer, 0, bytesRead)
          }
          _amplitude.value = computeNormalizedRms(readBuffer, bytesRead)
        }
      }
    }
  }

  suspend fun stopAndGetWav(): ByteArray? {
    _isRecording.value = false
    _amplitude.value = 0f
    timerJob?.cancel()
    recordJob?.join()

    withContext(Dispatchers.IO) {
      try {
        audioRecord?.stop()
      } catch (_: Exception) {
      }
      try {
        audioRecord?.release()
      } catch (_: Exception) {
      }
      audioRecord = null
    }

    val rawPcm = synchronized(pcmBuffer) {
      val data = pcmBuffer.toByteArray()
      pcmBuffer.reset()
      data
    }

    // Require at least ~0.25s of audio
    if (rawPcm.size < SAMPLE_RATE / 2) {
      return null
    }

    return encodeWav(rawPcm)
  }

  fun cancel() {
    _isRecording.value = false
    _amplitude.value = 0f
    _elapsedSeconds.value = 0
    timerJob?.cancel()
    recordJob?.cancel()
    try {
      audioRecord?.stop()
    } catch (_: Exception) {
    }
    try {
      audioRecord?.release()
    } catch (_: Exception) {
    }
    audioRecord = null
    synchronized(pcmBuffer) {
      pcmBuffer.reset()
    }
  }

  private fun computeNormalizedRms(buffer: ByteArray, length: Int): Float {
    if (length < 2) return 0f
    var sumSquares = 0.0
    val samples = length / 2
    var i = 0
    while (i + 1 < length) {
      val low = buffer[i].toInt() and 0xFF
      val high = buffer[i + 1].toInt()
      val sample = ((high shl 8) or low).toShort().toDouble()
      sumSquares += sample * sample
      i += 2
    }
    val rms = sqrt(sumSquares / samples)
    return (rms / 12000.0).toFloat().coerceIn(0f, 1f)
  }

  private fun encodeWav(pcmData: ByteArray): ByteArray {
    val totalDataLen = pcmData.size + 36
    val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
    val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(totalDataLen)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16) // PCM chunk size
    header.putShort(1) // PCM format
    header.putShort(CHANNELS.toShort())
    header.putInt(SAMPLE_RATE)
    header.putInt(byteRate)
    header.putShort(blockAlign.toShort())
    header.putShort(BITS_PER_SAMPLE.toShort())
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcmData.size)

    return header.array() + pcmData
  }
}
