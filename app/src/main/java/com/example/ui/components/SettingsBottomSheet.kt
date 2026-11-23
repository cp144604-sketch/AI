package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CompanionSettings
import com.example.model.CompanionMood
import com.example.model.FemaleVoicePersona
import com.example.model.PreferredLanguage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
  settings: CompanionSettings,
  onSelectVoicePersona: (FemaleVoicePersona) -> Unit,
  onSelectMood: (CompanionMood) -> Unit,
  onSelectLanguage: (PreferredLanguage) -> Unit,
  onToggleAutoSpeak: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "અવાજ અને વ્યક્તિત્વ પસંદગી",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "તમને ગમતો છોકરી જેવો અવાજ, વાતચીતનો અંદાજ અને ભાષા પસંદ કરો.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Auto-Speak toggle card
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.VolumeUp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = "જવાબ આપમેળે અવાજમાં સંભળાવો",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "દરેક જવાબ રિયલ છોકરીના અવાજમાં પ્લે થશે",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = settings.autoSpeakReplies,
            onCheckedChange = onToggleAutoSpeak,
            modifier = Modifier.testTag("auto_speak_switch"),
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
              checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
          )
        }
      }

      // Female Voice Personas
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "છોકરીનો અવાજ (Voice Persona)",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.secondary,
        )
        FemaleVoicePersona.entries.forEach { persona ->
          val selected = settings.voicePersona == persona
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (selected) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(
              width = if (selected) 1.5.dp else 1.dp,
              color = if (selected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.outlineVariant
              },
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .clickable { onSelectVoicePersona(persona) }
              .testTag("voice_persona_${persona.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.GraphicEq,
                  contentDescription = null,
                  tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  modifier = Modifier.size(24.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "${persona.displayNameGu} (${persona.displayNameEn})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = persona.vibeTaglineGu,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              if (selected) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = "Selected voice",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }

      // Mood / Personality
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "વાતચીતનો અંદાજ (Personality Vibe)",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.secondary,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CompanionMood.entries.forEach { mood ->
            val selected = settings.mood == mood
            FilterChip(
              selected = selected,
              onClick = { onSelectMood(mood) },
              label = { Text("${mood.labelGu} • ${mood.labelEn}") },
              modifier = Modifier.testTag("mood_chip_${mood.name.lowercase()}"),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            )
          }
        }
      }

      // Language Selection
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "ભાષા પસંદગી (Language)",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.secondary,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PreferredLanguage.entries.forEach { lang ->
            val selected = settings.language == lang
            FilterChip(
              selected = selected,
              onClick = { onSelectLanguage(lang) },
              label = { Text(lang.labelGu) },
              modifier = Modifier.testTag("language_chip_${lang.name.lowercase()}"),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
              ),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
    }
  }
}
