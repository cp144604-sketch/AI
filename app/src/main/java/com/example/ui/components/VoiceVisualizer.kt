package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CompanionStatus
import kotlin.math.sin

@Composable
fun VaaniAvatarOrb(
  status: CompanionStatus,
  amplitude: Float,
  modifier: Modifier = Modifier,
  orbSize: Dp = 148.dp,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
  val idleBreath by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "idle_breath",
  )

  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "orbit_angle",
  )

  val smoothedAmplitude by animateFloatAsState(
    targetValue = amplitude.coerceIn(0f, 1f),
    animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f),
    label = "smoothed_amp",
  )

  val activeBoost = when (status) {
    CompanionStatus.SPEAKING -> 0.08f + (smoothedAmplitude * 0.14f)
    CompanionStatus.RECORDING -> 0.06f + (smoothedAmplitude * 0.14f)
    CompanionStatus.THINKING, CompanionStatus.GENERATING_VOICE -> 0.05f + (idleBreath * 0.04f)
    CompanionStatus.IDLE -> idleBreath * 0.03f
  }

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary

  val outerGlowColor = when (status) {
    CompanionStatus.RECORDING -> tertiaryColor
    CompanionStatus.THINKING, CompanionStatus.GENERATING_VOICE -> secondaryColor
    else -> primaryColor
  }

  Box(
    modifier = modifier.size(orbSize + 56.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val centerRadius = (orbSize.toPx() / 2f)
      val outerRadius = centerRadius + 14.dp.toPx() + (activeBoost * 42.dp.toPx())
      val midRadius = centerRadius + 6.dp.toPx() + (activeBoost * 24.dp.toPx())

      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            outerGlowColor.copy(alpha = 0.34f + (smoothedAmplitude * 0.2f)),
            outerGlowColor.copy(alpha = 0.08f),
            Color.Transparent,
          ),
          center = center,
          radius = outerRadius + 18.dp.toPx(),
        ),
        radius = outerRadius + 18.dp.toPx(),
      )

      drawCircle(
        color = outerGlowColor.copy(alpha = 0.25f + (idleBreath * 0.15f)),
        radius = outerRadius,
        style = Stroke(width = 1.5.dp.toPx()),
      )

      drawCircle(
        brush = Brush.sweepGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.7f),
            secondaryColor.copy(alpha = 0.75f),
            tertiaryColor.copy(alpha = 0.65f),
            primaryColor.copy(alpha = 0.7f),
          ),
        ),
        radius = midRadius,
        style = Stroke(width = 2.5.dp.toPx()),
      )

      if (status == CompanionStatus.THINKING || status == CompanionStatus.GENERATING_VOICE) {
        val rad = Math.toRadians(rotationAngle.toDouble())
        val dotOffsetX = (midRadius * kotlin.math.cos(rad)).toFloat()
        val dotOffsetY = (midRadius * sin(rad)).toFloat()
        drawCircle(
          color = secondaryColor,
          radius = 4.5.dp.toPx(),
          center = center.copy(x = center.x + dotOffsetX, y = center.y + dotOffsetY),
        )
      }
    }

    Image(
      painter = painterResource(id = R.drawable.img_vaani_avatar),
      contentDescription = stringResource(R.string.cd_vaani_avatar),
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(orbSize)
        .scale(1f + (activeBoost * 0.22f))
        .clip(CircleShape)
        .border(
          width = 2.dp,
          brush = Brush.linearGradient(listOf(primaryColor, secondaryColor)),
          shape = CircleShape,
        ),
    )
  }
}

@Composable
fun VoiceWaveBars(
  amplitude: Float,
  isActive: Boolean,
  barCount: Int = 7,
  barColor: Color = MaterialTheme.colorScheme.primary,
  maxHeight: Dp = 26.dp,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "wave_bars")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (Math.PI * 2.0).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "wave_phase",
  )

  val smoothAmp by animateFloatAsState(
    targetValue = if (isActive) amplitude.coerceIn(0.18f, 1f) else 0.15f,
    animationSpec = tween(durationMillis = 90),
    label = "bar_amp",
  )

  Row(
    modifier = modifier.height(maxHeight),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    for (index in 0 until barCount) {
      val wave = if (isActive) {
        0.35f + 0.65f * kotlin.math.abs(sin(phase + index * 0.72f))
      } else {
        0.22f
      }
      val fraction = (wave * smoothAmp).coerceIn(0.18f, 1f)
      val barHeight = maxHeight * fraction
      Canvas(
        modifier = Modifier
          .width(3.5.dp)
          .height(barHeight),
      ) {
        drawRoundRect(
          color = barColor,
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
      }
    }
  }
}
