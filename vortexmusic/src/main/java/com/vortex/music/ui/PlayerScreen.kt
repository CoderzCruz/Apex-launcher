package com.vortex.music.ui

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.music.data.MusicRepository
import com.vortex.music.data.RepeatMode
import com.vortex.music.playback.PlayerState
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val queue by PlayerState.queue.collectAsState()
    val index by PlayerState.index.collectAsState()
    val playing by PlayerState.playing.collectAsState()
    val position by PlayerState.positionMs.collectAsState()
    val duration by PlayerState.durationMs.collectAsState()
    val shuffle by PlayerState.shuffle.collectAsState()
    val repeat by PlayerState.repeat.collectAsState()
    val liked by MusicRepository.liked.collectAsState()

    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var volume by remember {
        mutableFloatStateOf(
            run {
                val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
                am?.let {
                    it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                        it.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                } ?: 0.5f
            }
        )
    }

    // Keep the position label fresh even while the service is the source of truth.
    LaunchedEffect(playing) {
        while (playing) { delay(500) }
    }

    val song = queue.getOrNull(index)
    if (song == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nothing playing", color = VortexColors.TextPrimary,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Pick a song from Home, Search or Library.",
                    color = VortexColors.TextSecondary, fontSize = 12.5.sp)
            }
        }
        return
    }

    val isLiked = liked.contains(song.id)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⌄", color = VortexColors.TextPrimary, fontSize = 22.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(6.dp))
            Text("NOW PLAYING", color = VortexColors.TextSecondary, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("⚙", color = VortexColors.TextSecondary, fontSize = 18.sp,
                modifier = Modifier.clickable(onClick = onOpenSettings).padding(6.dp))
        }

        Spacer(Modifier.height(12.dp))
        Artwork(song = song, size = 280, modifier = Modifier.size(280.dp))
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(song.title, color = VortexColors.TextPrimary, fontSize = 20.sp,
                    fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = VortexColors.TextSecondary, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = if (isLiked) "♥" else "♡",
                fontSize = 26.sp,
                color = if (isLiked) VortexColors.Accent else VortexColors.TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { MusicRepository.toggleLike(context, song.id) }
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        Slider(
            value = if (seeking) seekValue else if (duration > 0) position.toFloat() / duration else 0f,
            onValueChange = {
                seeking = true
                seekValue = it
            },
            onValueChangeFinished = {
                PlayerState.seekTo((seekValue * duration).toLong())
                seeking = false
            },
            colors = SliderDefaults.colors(
                thumbColor = VortexColors.Accent,
                activeTrackColor = VortexColors.Accent,
                inactiveTrackColor = VortexColors.SurfaceHigh
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(if (seeking) seekValue * duration else position),
                color = VortexColors.TextSecondary, fontSize = 11.5.sp)
            Text(formatTime(duration), color = VortexColors.TextSecondary, fontSize = 11.5.sp)
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (shuffle) "⇄" else "⇄",
                fontSize = 20.sp,
                color = if (shuffle) VortexColors.Accent else VortexColors.TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { PlayerState.toggleShuffle() }
                    .padding(10.dp)
            )
            Text("⏮", fontSize = 26.sp, color = VortexColors.TextPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { PlayerState.prev() }
                    .padding(12.dp))
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(VortexColors.Accent)
                    .clickable { PlayerState.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (playing) "⏸" else "▶", fontSize = 26.sp, color = Color.White)
            }
            Text("⏭", fontSize = 26.sp, color = VortexColors.TextPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { PlayerState.next() }
                    .padding(12.dp))
            Text(
                text = when (repeat) {
                    RepeatMode.OFF -> "🔁"
                    RepeatMode.ALL -> "🔁"
                    RepeatMode.ONE -> "🔂"
                },
                fontSize = 20.sp,
                color = if (repeat != RepeatMode.OFF) VortexColors.Accent else VortexColors.TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { PlayerState.cycleRepeat() }
                    .padding(10.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🔈", fontSize = 14.sp, color = VortexColors.TextSecondary)
            Slider(
                value = volume,
                onValueChange = { v ->
                    volume = v
                    val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
                    am?.let {
                        val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        it.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).toInt(), 0)
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = VortexColors.TextPrimary,
                    activeTrackColor = VortexColors.TextPrimary,
                    inactiveTrackColor = VortexColors.SurfaceHigh
                ),
                modifier = Modifier.weight(1f)
            )
            Text("🔊", fontSize = 14.sp, color = VortexColors.TextSecondary)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Queue · ${queue.size} songs · tap ⏭ for next",
            color = VortexColors.TextSecondary, fontSize = 11.5.sp
        )
    }
}
