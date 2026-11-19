package com.vortex.music.ui

import android.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.music.data.MusicRepository

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var audioFocus by remember {
        mutableStateOf(SettingsStore.audioFocusEnabled(context))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(VortexColors.Background)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", color = VortexColors.TextPrimary,
                fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("✕", color = VortexColors.TextSecondary, fontSize = 20.sp,
                modifier = Modifier.clickable(onClick = onClose).padding(6.dp))
        }
        Spacer(Modifier.height(18.dp))

        SettingRow(
            title = "Audio focus",
            subtitle = "Pause when a call/other app needs sound"
        ) {
            Switch(
                checked = audioFocus,
                onCheckedChange = {
                    audioFocus = it
                    SettingsStore.setAudioFocus(context, it)
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = VortexColors.Accent,
                    checkedThumbColor = Color.White
                )
            )
        }

        SettingRow(
            title = "Clear recent searches",
            subtitle = "Removes your local search history"
        ) {
            Text("Clear", color = VortexColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { MusicRepository.clearSearches(context) }
                    .padding(8.dp))
        }

        SettingRow(
            title = "Playback preferences",
            subtitle = "Shuffle and repeat state reset on restart"
        ) {
            Text("Info", color = VortexColors.TextSecondary, fontSize = 12.sp)
        }

        SettingRow(
            title = "Privacy",
            subtitle = "Vortex Music is fully offline: songs, playlists and history never leave your device. No accounts, no ads, no streaming."
        ) {}

        SettingRow(
            title = "About",
            subtitle = "Vortex Music v1.0.0 · local playback engine · MP3/WAV/M4A/FLAC/OGG"
        ) {}

        Spacer(Modifier.height(14.dp))
        Text(
            "Made with Vortex ❤",
            color = VortexColors.TextSecondary, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = VortexColors.TextPrimary, fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = VortexColors.TextSecondary, fontSize = 11.5.sp, lineHeight = 16.sp)
        }
        trailing()
    }
}
