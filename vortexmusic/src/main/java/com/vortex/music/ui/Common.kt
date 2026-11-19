package com.vortex.music.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.music.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Vortex Music palette: black / charcoal with the neon red accent. */
object VortexColors {
    val Background = Color(0xFF0A0A0C)
    val Surface = Color(0xFF141417)
    val SurfaceHigh = Color(0xFF1D1D22)
    val Accent = Color(0xFFFF2020)
    val AccentDim = Color(0x66FF2020)
    val TextPrimary = Color(0xFFF2F2F4)
    val TextSecondary = Color(0xFF9A9AA3)
}

/** Tiny persisted settings (audio focus). */
object SettingsStore {
    private const val PREFS = "vortex_music_settings"

    fun audioFocusEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("audio_focus", true)

    fun setAudioFocus(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("audio_focus", enabled).apply()
    }
}

fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Good Morning"
        h < 17 -> "Good Afternoon"
        h < 21 -> "Good Evening"
        else -> "Good Night"
    }
}

/** Loads embedded album art for a song; null falls back to the gradient tile. */
@Composable
fun rememberArtwork(song: Song?): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bmp by remember(song?.id) { mutableStateOf<ImageBitmap?>(null) }
    val uri = song?.artworkUri ?: ""
    LaunchedEffect(uri) {
        if (uri.isEmpty()) { bmp = null; return@LaunchedEffect }
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bmp
}

@Composable
fun Artwork(song: Song?, size: Int, modifier: Modifier = Modifier) {
    val bmp = rememberArtwork(song)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2A0E12), VortexColors.SurfaceHigh)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = song?.title,
                modifier = Modifier.size(size.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = (song?.title ?: "?").take(1).uppercase(),
                color = VortexColors.Accent,
                fontSize = (size / 2.4f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = VortexColors.TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SongRow(song: Song, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Artwork(song = song, size = 46)
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = VortexColors.TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${song.artist} · ${song.album}",
                color = VortexColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = VortexColors.TextSecondary, fontSize = 13.sp)
    }
}
