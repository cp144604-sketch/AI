package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val VaaniDarkColorScheme = darkColorScheme(
  primary = CoralRose,
  onPrimary = VelvetPlumDark,
  primaryContainer = CoralRoseContainer,
  onPrimaryContainer = PearlWhite,
  secondary = ChampagnePeach,
  onSecondary = VelvetPlumDark,
  secondaryContainer = ChampagnePeachDeep,
  onSecondaryContainer = ChampagnePeach,
  tertiary = LavenderMist,
  onTertiary = VelvetPlumDark,
  tertiaryContainer = LavenderMistDeep,
  onTertiaryContainer = PearlWhite,
  background = VelvetPlumDark,
  onBackground = PearlWhite,
  surface = VelvetPlumSurface,
  onSurface = PearlWhite,
  surfaceVariant = VelvetPlumElevated,
  onSurfaceVariant = SoftPlumGray,
  surfaceContainerHigh = VelvetPlumHighest,
  outline = PlumOutline,
  outlineVariant = Color(0xFF332245),
  error = ErrorRose,
  onError = VelvetPlumDark,
  errorContainer = ErrorRoseContainer,
  onErrorContainer = PearlWhite,
)

private val VaaniLightColorScheme = lightColorScheme(
  primary = BlossomCoral,
  onPrimary = Color.White,
  primaryContainer = BlossomCoralContainer,
  onPrimaryContainer = BlossomPlumInk,
  secondary = BlossomPeach,
  onSecondary = Color.White,
  secondaryContainer = BlossomPeachContainer,
  onSecondaryContainer = BlossomPlumInk,
  tertiary = BlossomLavender,
  onTertiary = Color.White,
  tertiaryContainer = BlossomLavenderContainer,
  onTertiaryContainer = BlossomPlumInk,
  background = BlossomIvory,
  onBackground = BlossomPlumInk,
  surface = BlossomSurface,
  onSurface = BlossomPlumInk,
  surfaceVariant = BlossomCard,
  onSurfaceVariant = BlossomMutedInk,
  surfaceContainerHigh = BlossomSurface,
  outline = BlossomOutline,
  outlineVariant = Color(0xFFF1E2EA),
)

private val VaaniShapes = Shapes(
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(28.dp),
  extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun VaaniTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> VaaniDarkColorScheme
    else -> VaaniLightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = VaaniTypography,
    shapes = VaaniShapes,
    content = content,
  )
}
