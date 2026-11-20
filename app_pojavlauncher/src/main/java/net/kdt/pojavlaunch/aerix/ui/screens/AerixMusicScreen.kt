package net.kdt.pojavlaunch.aerix.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.ContextCompat
import com.vortex.music.data.MusicRepository
import com.vortex.music.playback.PlaybackService
import com.vortex.music.playback.PlayerState
import com.vortex.music.ui.HomeScreen
import com.vortex.music.ui.LibraryScreen
import com.vortex.music.ui.MiniPlayer
import com.vortex.music.ui.PlayerScreen
import com.vortex.music.ui.SearchScreen
import com.vortex.music.ui.SettingsScreen
import com.vortex.music.ui.VortexColors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private val MUSIC_TABS = listOf("Home", "Search", "Library", "Player")

/**
 * Vortex Music inside the launcher: the rail's Music entry hosts the full
 * Vortex Music experience (library scan, tabs, mini player, full player and
 * settings) backed by the shared :vortexmusic engine (foreground playback
 * service + MediaSession). The old simple transport screen is replaced.
 */
@Composable
fun AerixMusicScreen() {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val songs by MusicRepository.songs.collectAsState()
    val playing by PlayerState.playing.collectAsState()
    val index by PlayerState.index.collectAsState()
    val queue by PlayerState.queue.collectAsState()

    val neededPermission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            MainScope().launch { MusicRepository.scan(context) }
        }
    }

    LaunchedEffect(Unit) {
        MusicRepository.ensureLoaded(context)
        val granted = ContextCompat.checkSelfPermission(
            context, neededPermission
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            MusicRepository.scan(context)
        } else {
            permissionLauncher.launch(neededPermission)
        }
        runCatching {
            context.startForegroundService(Intent(context, PlaybackService::class.java))
        }
    }

    val currentSong = queue.getOrNull(index)

    // Discord Rich Presence: show what is playing while it plays.
    LaunchedEffect(playing, currentSong) {
        if (playing && currentSong != null) {
            net.kdt.pojavlaunch.aerix.discord.AerixDiscordPresence.musicPlaying(
                currentSong.title, currentSong.artist
            )
        } else {
            net.kdt.pojavlaunch.aerix.discord.AerixDiscordPresence.inLauncher("MUSIC")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Vortex tab bar (Home / Search / Library / Player)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MUSIC_TABS.forEachIndexed { i, label ->
                    val selected = tab == i
                    Text(
                        text = label,
                        color = if (selected) Color.White else VortexColors.TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (selected) VortexColors.Accent
                                else VortexColors.Surface
                            )
                            .clickable { tab = i }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (tab) {
                    0 -> HomeScreen(
                        songs = songs,
                        onPlay = { list, i -> PlayerState.playQueue(list, i) },
                        openPlayer = { tab = 3 }
                    )
                    1 -> SearchScreen(
                        songs = songs,
                        onPlay = { list, i -> PlayerState.playQueue(list, i) }
                    )
                    2 -> LibraryScreen(
                        songs = songs,
                        onPlay = { list, i -> PlayerState.playQueue(list, i) },
                        openSettings = { showSettings = true }
                    )
                    else -> PlayerScreen(
                        onBack = { tab = 0 },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }

            if (currentSong != null) {
                MiniPlayer(
                    song = currentSong,
                    playing = playing,
                    onTap = { tab = 3 },
                    onToggle = { PlayerState.toggle() }
                )
            }
        }

        if (showSettings) {
            SettingsScreen(onClose = { showSettings = false })
        }
    }
}
