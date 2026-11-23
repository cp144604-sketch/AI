package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CompanionStatus
import com.example.model.FemaleVoicePersona
import com.example.model.LiveCallState
import com.example.ui.components.VaaniAvatarOrb
import com.example.ui.components.VoiceWaveBars

@Composable
fun LiveCallScreen(
  persona: FemaleVoicePersona,
  liveCallState: LiveCallState,
  onToggleMute: () -> Unit,
  onRetry: () -> Unit,
  onEndCall: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onEndCall)

  val backgroundColor = MaterialTheme.colorScheme.background
  val statusForOrb = when (liveCallState) {
    is LiveCallState.Connecting -> CompanionStatus.THINKING
    is LiveCallState.Active -> if (liveCallState.isMuted) CompanionStatus.IDLE else CompanionStatus.SPEAKING
    else -> CompanionStatus.IDLE
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(backgroundColor),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_ambient_backdrop),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .alpha(0.38f),
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              backgroundColor.copy(alpha = 0.8f),
              backgroundColor.copy(alpha = 0.6f),
              backgroundColor.copy(alpha = 0.94f),
            )
          )
        ),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 20.dp)
        .widthIn(max = 560.dp)
        .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Call Header
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        ) {
          Text(
            text = when (liveCallState) {
              is LiveCallState.Connecting -> stringResource(R.string.status_connecting)
              is LiveCallState.Active -> {
                val mins = liveCallState.elapsedSeconds / 60
                val secs = liveCallState.elapsedSeconds % 60
                "${stringResource(R.string.status_live_active)} • %02d:%02d".format(mins, secs)
              }
              is LiveCallState.Error -> "કૉલ કનેક્ટ થઈ શક્યો નહીં"
              LiveCallState.Inactive -> ""
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }

        Text(
          text = persona.displayNameGu,
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = persona.vibeTaglineGu,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }

      // Center Avatar Orb & Live Transcripts
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        VaaniAvatarOrb(
          status = statusForOrb,
          amplitude = if (liveCallState is LiveCallState.Active && !liveCallState.isMuted) 0.72f else 0.1f,
          orbSize = 180.dp,
        )

        when (liveCallState) {
          is LiveCallState.Connecting -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.secondary,
              )
              Text(
                text = "વાણી સાથે લાઇવ અવાજ જોડાઈ રહ્યો છે...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          }

          is LiveCallState.Active -> {
            VoiceWaveBars(
              amplitude = if (liveCallState.isMuted) 0.12f else 0.78f,
              isActive = !liveCallState.isMuted,
              barCount = 11,
              maxHeight = 32.dp,
              barColor = MaterialTheme.colorScheme.secondary,
            )

            Surface(
              shape = RoundedCornerShape(22.dp),
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                if (liveCallState.userTranscript.isNotBlank()) {
                  Text(
                    text = "તમે: ${liveCallState.userTranscript}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                  )
                }
                Text(
                  text = liveCallState.assistantTranscript.ifBlank {
                    if (liveCallState.isMuted) {
                      "માઇક્રોફોન મ્યૂટ છે. ફરી વાત કરવા અનમ્યૂટ કરો."
                    } else {
                      "હવે તમે સીધી વાત કરી શકો છો — વાણી તમને સાંભળી રહી છે..."
                    }
                  },
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          is LiveCallState.Error -> {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = liveCallState.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  textAlign = TextAlign.Center,
                )
                Button(
                  onClick = onRetry,
                  modifier = Modifier.testTag("live_call_retry_button"),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                ) {
                  Text("ફરી પ્રયાસ કરો")
                }
              }
            }
          }

          LiveCallState.Inactive -> Unit
        }
      }

      // Bottom Call Controls
      Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp),
      ) {
        if (liveCallState is LiveCallState.Active) {
          FilledIconButton(
            onClick = onToggleMute,
            modifier = Modifier
              .size(64.dp)
              .testTag("live_call_mute_button"),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = if (liveCallState.isMuted) {
                MaterialTheme.colorScheme.secondaryContainer
              } else {
                MaterialTheme.colorScheme.surfaceVariant
              },
              contentColor = if (liveCallState.isMuted) {
                MaterialTheme.colorScheme.secondary
              } else {
                MaterialTheme.colorScheme.onSurface
              },
            ),
          ) {
            Icon(
              imageVector = if (liveCallState.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
              contentDescription = stringResource(R.string.cd_mute_call),
              modifier = Modifier.size(28.dp),
            )
          }
        }

        FilledIconButton(
          onClick = onEndCall,
          modifier = Modifier
            .size(72.dp)
            .testTag("live_call_end_button"),
          shape = CircleShape,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color(0xFFE53955),
            contentColor = Color.White,
          ),
        ) {
          Icon(
            imageVector = Icons.Rounded.CallEnd,
            contentDescription = stringResource(R.string.cd_end_call),
            modifier = Modifier.size(32.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
    }
  }
}
