package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CompanionSettings
import com.example.model.ChatMessage
import com.example.model.CompanionStatus
import com.example.model.CompanionTopics
import com.example.model.LiveCallState
import com.example.model.TopicStarter
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.VaaniAvatarOrb
import com.example.ui.components.VoiceWaveBars
import com.example.viewmodel.VaaniViewModel

private enum class PendingAudioAction {
  RECORD_MESSAGE,
  START_LIVE_CALL,
}

@Composable
fun VaaniCompanionApp(
  viewModel: VaaniViewModel,
  modifier: Modifier = Modifier,
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val messages by viewModel.messages.collectAsStateWithLifecycle()
  val status by viewModel.status.collectAsStateWithLifecycle()
  val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
  val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
  val recordingAmplitude by viewModel.recordingAmplitude.collectAsStateWithLifecycle()
  val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()
  val playingMessageId by viewModel.playingMessageId.collectAsStateWithLifecycle()
  val playbackAmplitude by viewModel.playbackAmplitude.collectAsStateWithLifecycle()
  val liveCallState by viewModel.liveCallState.collectAsStateWithLifecycle()

  val context = LocalContext.current
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showMicRationale by remember { mutableStateOf(false) }
  var pendingAudioAction by remember { mutableStateOf<PendingAudioAction?>(null) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      when (pendingAudioAction) {
        PendingAudioAction.RECORD_MESSAGE -> viewModel.startRecording()
        PendingAudioAction.START_LIVE_CALL -> viewModel.startLiveCall()
        null -> Unit
      }
    } else {
      showMicRationale = true
    }
    pendingAudioAction = null
  }

  fun requestMicAndRun(action: PendingAudioAction) {
    val hasMic = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    if (hasMic) {
      when (action) {
        PendingAudioAction.RECORD_MESSAGE -> viewModel.startRecording()
        PendingAudioAction.START_LIVE_CALL -> viewModel.startLiveCall()
      }
    } else {
      pendingAudioAction = action
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  if (liveCallState !is LiveCallState.Inactive) {
    LiveCallScreen(
      persona = settings.voicePersona,
      liveCallState = liveCallState,
      onToggleMute = viewModel::toggleLiveMute,
      onRetry = viewModel::startLiveCall,
      onEndCall = viewModel::endLiveCall,
      modifier = modifier,
    )
    return
  }

  CompanionHomeScreen(
    settings = settings,
    messages = messages,
    status = status,
    errorMessage = errorMessage,
    isRecording = isRecording,
    recordingAmplitude = recordingAmplitude,
    recordingSeconds = recordingSeconds,
    playingMessageId = playingMessageId,
    playbackAmplitude = playbackAmplitude,
    onSendText = viewModel::sendTextMessage,
    onStartRecording = { requestMicAndRun(PendingAudioAction.RECORD_MESSAGE) },
    onStopRecordingAndSend = viewModel::stopRecordingAndSend,
    onCancelRecording = viewModel::cancelRecording,
    onToggleMessagePlayback = viewModel::toggleMessagePlayback,
    onSpeakViaDeviceVoice = viewModel::speakViaDeviceVoice,
    onStartLiveCall = { requestMicAndRun(PendingAudioAction.START_LIVE_CALL) },
    onOpenSettings = { showSettingsSheet = true },
    onClearChat = viewModel::clearConversation,
    onDismissError = viewModel::dismissError,
    modifier = modifier,
  )

  if (showSettingsSheet) {
    SettingsBottomSheet(
      settings = settings,
      onSelectVoicePersona = viewModel::updateVoicePersona,
      onSelectMood = viewModel::updateMood,
      onSelectLanguage = viewModel::updateLanguage,
      onToggleAutoSpeak = viewModel::updateAutoSpeak,
      onDismiss = { showSettingsSheet = false },
    )
  }

  if (showMicRationale) {
    AlertDialog(
      onDismissRequest = { showMicRationale = false },
      title = { Text(stringResource(R.string.permission_mic_title)) },
      text = { Text(stringResource(R.string.permission_mic_body)) },
      confirmButton = {
        Button(
          onClick = {
            showMicRationale = false
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          }
        ) {
          Text(stringResource(R.string.permission_mic_grant))
        }
      },
      dismissButton = {
        TextButton(onClick = { showMicRationale = false }) {
          Text("પછીથી")
        }
      },
    )
  }
}

@Composable
private fun CompanionHomeScreen(
  settings: CompanionSettings,
  messages: List<ChatMessage>,
  status: CompanionStatus,
  errorMessage: String?,
  isRecording: Boolean,
  recordingAmplitude: Float,
  recordingSeconds: Int,
  playingMessageId: String?,
  playbackAmplitude: Float,
  onSendText: (String) -> Unit,
  onStartRecording: () -> Unit,
  onStopRecordingAndSend: () -> Unit,
  onCancelRecording: () -> Unit,
  onToggleMessagePlayback: (String) -> Unit,
  onSpeakViaDeviceVoice: (String) -> Unit,
  onStartLiveCall: () -> Unit,
  onOpenSettings: () -> Unit,
  onClearChat: () -> Unit,
  onDismissError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var composerText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val bg = MaterialTheme.colorScheme.background
  val activeAmplitude = when (status) {
    CompanionStatus.RECORDING -> recordingAmplitude
    CompanionStatus.SPEAKING -> playbackAmplitude
    else -> 0.1f
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(bg),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_ambient_backdrop),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .alpha(0.28f),
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              bg.copy(alpha = 0.82f),
              bg.copy(alpha = 0.56f),
              bg.copy(alpha = 0.94f),
            )
          )
        ),
    )

    BoxWithConstraints(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.TopCenter,
    ) {
      val isWide = maxWidth >= 600.dp

      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .statusBarsPadding(),
      ) {
        // Top Companion Header Bar
        CompanionTopBar(
          settings = settings,
          status = status,
          hasMessages = messages.isNotEmpty(),
          onOpenSettings = onOpenSettings,
          onStartLiveCall = onStartLiveCall,
          onClearChat = onClearChat,
        )

        // Error Banner
        AnimatedVisibility(
          visible = errorMessage != null,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          errorMessage?.let { err ->
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = err,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.weight(1f),
                )
                IconButton(
                  onClick = onDismissError,
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
              }
            }
          }
        }

        // Conversation or Welcome Stage
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        ) {
          if (messages.isEmpty()) {
            WelcomeStage(
              settings = settings,
              status = status,
              amplitude = activeAmplitude,
              isWide = isWide,
              onSelectTopic = { starter -> onSendText(starter.prompt) },
              onOpenSettings = onOpenSettings,
            )
          } else {
            LazyColumn(
              state = listState,
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier.fillMaxSize(),
            ) {
              items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                  message = msg,
                  personaNameGu = settings.voicePersona.displayNameGu,
                  isPlaying = playingMessageId == msg.id,
                  playbackAmplitude = playbackAmplitude,
                  onToggleVoicePlayback = { onToggleMessagePlayback(msg.id) },
                  onSpeakViaDeviceVoice = { onSpeakViaDeviceVoice(msg.id) },
                )
              }
            }
          }
        }

        // Bottom Voice + Text Composer
        BottomComposerBar(
          composerText = composerText,
          onTextChange = { composerText = it },
          isRecording = isRecording,
          recordingAmplitude = recordingAmplitude,
          recordingSeconds = recordingSeconds,
          isBusy = status == CompanionStatus.THINKING,
          onSendText = {
            val toSend = composerText.trim()
            if (toSend.isNotEmpty()) {
              composerText = ""
              onSendText(toSend)
            }
          },
          onStartRecording = onStartRecording,
          onStopRecordingAndSend = onStopRecordingAndSend,
          onCancelRecording = onCancelRecording,
        )
      }
    }
  }
}

@Composable
private fun CompanionTopBar(
  settings: CompanionSettings,
  status: CompanionStatus,
  hasMessages: Boolean,
  onOpenSettings: () -> Unit,
  onStartLiveCall: () -> Unit,
  onClearChat: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .clickable(onClick = onOpenSettings)
          .padding(end = 6.dp)
          .testTag("persona_header_button"),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_vaani_avatar),
          contentDescription = stringResource(R.string.cd_vaani_avatar),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = settings.voicePersona.displayNameGu,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = settings.mood.labelGu,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                maxLines = 1,
              )
            }
          }
          Text(
            text = when (status) {
              CompanionStatus.IDLE -> stringResource(R.string.status_ready)
              CompanionStatus.RECORDING -> stringResource(R.string.status_listening)
              CompanionStatus.THINKING -> stringResource(R.string.status_thinking)
              CompanionStatus.GENERATING_VOICE -> "અવાજ તૈયાર થાય છે..."
              CompanionStatus.SPEAKING -> stringResource(R.string.status_speaking)
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (status == CompanionStatus.IDLE) {
              MaterialTheme.colorScheme.onSurfaceVariant
            } else {
              MaterialTheme.colorScheme.secondary
            },
            maxLines = 1,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        FilledIconButton(
          onClick = onStartLiveCall,
          modifier = Modifier
            .size(48.dp)
            .testTag("live_call_button"),
          shape = CircleShape,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = stringResource(R.string.cd_live_call),
            modifier = Modifier.size(22.dp),
          )
        }

        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("voice_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = stringResource(R.string.cd_voice_settings),
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }

        if (hasMessages) {
          IconButton(
            onClick = onClearChat,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("clear_chat_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = stringResource(R.string.cd_clear_chat),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeStage(
  settings: CompanionSettings,
  status: CompanionStatus,
  amplitude: Float,
  isWide: Boolean,
  onSelectTopic: (TopicStarter) -> Unit,
  onOpenSettings: () -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 8.dp),
      ) {
        VaaniAvatarOrb(
          status = status,
          amplitude = amplitude,
          orbSize = if (isWide) 168.dp else 148.dp,
        )

        Text(
          text = "નમસ્તે! હું ${settings.voicePersona.displayNameGu} છું ✨",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )

        Text(
          text = stringResource(R.string.welcome_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.widthIn(max = 460.dp),
        )

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onOpenSettings)
            .testTag("welcome_voice_chip"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.GraphicEq,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "અવાજ: ${settings.voicePersona.displayNameGu} • ${settings.language.labelGu}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "આજે કયા વિષય પર વાત કરવી છે?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
          )
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          maxItemsInEachRow = if (isWide) 3 else 2,
          modifier = Modifier.fillMaxWidth(),
        ) {
          CompanionTopics.starters.forEachIndexed { idx, topic ->
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onSelectTopic(topic) }
                .testTag("topic_starter_$idx"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = topic.categoryTag,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = topic.titleGu,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChatBubbleItem(
  message: ChatMessage,
  personaNameGu: String,
  isPlaying: Boolean,
  playbackAmplitude: Float,
  onToggleVoicePlayback: () -> Unit,
  onSpeakViaDeviceVoice: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
    verticalAlignment = Alignment.Top,
  ) {
    if (!message.isFromUser) {
      Image(
        painter = painterResource(id = R.drawable.img_vaani_avatar),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .padding(top = 4.dp, end = 8.dp)
          .size(34.dp)
          .clip(CircleShape),
      )
    }

    Surface(
      shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (message.isFromUser) 20.dp else 6.dp,
        bottomEnd = if (message.isFromUser) 6.dp else 20.dp,
      ),
      color = if (message.isFromUser) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
      },
      border = BorderStroke(
        width = 1.dp,
        color = if (message.isFromUser) {
          MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        } else {
          MaterialTheme.colorScheme.outlineVariant
        },
      ),
      modifier = Modifier
        .widthIn(max = 500.dp)
        .testTag(if (message.isFromUser) "user_message_bubble" else "assistant_message_bubble"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (message.isFromUser && message.isVoiceInput) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Mic,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = message.text,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        } else {
          if (!message.isFromUser) {
            Text(
              text = personaNameGu,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
            )
          }

          if (message.text.isBlank() && message.isStreaming) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = stringResource(R.string.status_thinking),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            Text(
              text = message.text,
              style = MaterialTheme.typography.bodyLarge,
              color = if (message.isFromUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSurface
              },
            )
          }
        }

        // Female Voice Playback Controls on Vaani's completed replies
        if (!message.isFromUser && !message.isStreaming && message.text.isNotBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = if (isPlaying) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.surface
              },
              border = BorderStroke(
                width = 1.dp,
                color = if (isPlaying) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.outline
                },
              ),
              modifier = Modifier
                .clip(CircleShape)
                .clickable(enabled = !message.isSynthesizingVoice, onClick = onToggleVoicePlayback)
                .testTag("play_voice_button_${message.id}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                when {
                  message.isSynthesizingVoice -> {
                    CircularProgressIndicator(
                      modifier = Modifier.size(14.dp),
                      strokeWidth = 2.dp,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = "અવાજ બને છે...",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }

                  isPlaying -> {
                    Icon(
                      imageVector = Icons.Rounded.Stop,
                      contentDescription = stringResource(R.string.cd_stop_voice),
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(16.dp),
                    )
                    VoiceWaveBars(
                      amplitude = playbackAmplitude,
                      isActive = true,
                      barCount = 5,
                      maxHeight = 16.dp,
                      barColor = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                      text = "બંધ કરો",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onPrimary,
                    )
                  }

                  else -> {
                    Icon(
                      imageVector = Icons.Rounded.VolumeUp,
                      contentDescription = stringResource(R.string.cd_play_voice),
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp),
                    )
                    Text(
                      text = "સાંભળો",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }
            }

            if (message.voiceError != null) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                  .clip(CircleShape)
                  .clickable(onClick = onSpeakViaDeviceVoice)
                  .testTag("device_voice_button_${message.id}"),
              ) {
                Text(
                  text = if (isPlaying) "બંધ કરો" else "ફોન અવાજ",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
              }
            }
          }

          if (message.voiceError != null) {
            Text(
              text = "AI વૉઇસ એરર: ${message.voiceError}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BottomComposerBar(
  composerText: String,
  onTextChange: (String) -> Unit,
  isRecording: Boolean,
  recordingAmplitude: Float,
  recordingSeconds: Int,
  isBusy: Boolean,
  onSendText: () -> Unit,
  onStartRecording: () -> Unit,
  onStopRecordingAndSend: () -> Unit,
  onCancelRecording: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    modifier = Modifier
      .fillMaxWidth()
      .imePadding(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      if (isRecording) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          IconButton(
            onClick = onCancelRecording,
            modifier = Modifier
              .size(48.dp)
              .testTag("cancel_recording_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = stringResource(R.string.cd_cancel_recording),
              tint = MaterialTheme.colorScheme.error,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            VoiceWaveBars(
              amplitude = recordingAmplitude,
              isActive = true,
              barCount = 9,
              maxHeight = 28.dp,
              barColor = MaterialTheme.colorScheme.primary,
            )
            val mins = recordingSeconds / 60
            val secs = recordingSeconds % 60
            Text(
              text = "%02d:%02d".format(mins, secs),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Button(
            onClick = onStopRecordingAndSend,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
              .height(48.dp)
              .testTag("send_recording_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Send,
              contentDescription = stringResource(R.string.cd_stop_recording),
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("મોકલો")
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TextField(
            value = composerText,
            onValueChange = onTextChange,
            placeholder = {
              Text(
                text = stringResource(R.string.input_hint),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendText() }),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("message_input_field"),
          )

          if (composerText.isNotBlank()) {
            FilledIconButton(
              onClick = onSendText,
              enabled = !isBusy,
              shape = CircleShape,
              colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier
                .size(52.dp)
                .testTag("send_text_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(R.string.cd_send_message),
              )
            }
          }

          FilledIconButton(
            onClick = onStartRecording,
            enabled = !isBusy,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = if (composerText.isBlank()) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
              contentColor = if (composerText.isBlank()) {
                MaterialTheme.colorScheme.onPrimary
              } else {
                MaterialTheme.colorScheme.secondary
              },
            ),
            modifier = Modifier
              .size(52.dp)
              .testTag("mic_record_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Mic,
              contentDescription = stringResource(R.string.cd_mic_button),
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }
    }
  }
}
