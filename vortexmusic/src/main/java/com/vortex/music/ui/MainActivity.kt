package com.vortex.music.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vortex.music.data.MusicRepository
import com.vortex.music.data.Song
import com.vortex.music.playback.PlayerState
import com.vortex.music.playback.PlaybackService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private enum class Tab(val label: String) {
    HOME("Home"), SEARCH("Search"), LIBRARY("Library"), PLAYER("Player")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicRepository.ensureLoaded(this)
        setContent {
            MaterialTheme(
                colorScheme = androidx.compose.material3.darkColorScheme(
                    primary = VortexColors.Accent,
                    background = VortexColors.Background,
                    surface = VortexColors.Surface
                )
            ) {
                VortexMusicApp(activity = this)
            }
        }
    }
}

@Composable
private fun VortexMusicApp(activity: MainActivity) {
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val songs by MusicRepository.songs.collectAsState()
    val playing by PlayerState.playing.collectAsState()
    val current by PlayerState.index.collectAsState()
    val queue by PlayerState.queue.collectAsState()
    val error by PlayerState.error.collectAsState()

    val neededPermission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            MainScope().launch { MusicRepository.scan(activity) }
            if (Build.VERSION.SDK_INT >= 33) {
                // Ask for notification permission next so the media notification can show.
                notifLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    var notifLauncher by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not: playback works either way, notification just won't show */ }
    notifLauncher = { perm -> notificationsLauncher.launch(perm) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            activity, neededPermission
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            MusicRepository.scan(activity)
            if (Build.VERSION.SDK_INT >= 33) {
                notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            permissionLauncher.launch(neededPermission)
        }
    }

    // Start the playback service lazily so controls always have a target.
    LaunchedEffect(Unit) {
        runCatching {
            activity.startForegroundService(Intent(activity, PlaybackService::class.java))
        }
    }

    val currentSong = queue.getOrNull(current)

    Scaffold(
        containerColor = VortexColors.Background,
        bottomBar = {
            Column {
                error?.let { msg ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(VortexColors.Accent)
                            .clickable { PlayerState.clearError() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(msg, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (currentSong != null) {
                    MiniPlayer(
                        song = currentSong,
                        playing = playing,
                        onTap = { tab = Tab.PLAYER.ordinal },
                        onToggle = { PlayerState.toggle() }
                    )
                }
                NavigationBar(containerColor = VortexColors.Surface) {
                    Tab.entries.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = {
                                Text(
                                    text = when (t) {
                                        Tab.HOME -> "\u2302"
                                        Tab.SEARCH -> "\u2315"
                                        Tab.LIBRARY -> "\u25A4"
                                        Tab.PLAYER -> "\u25B6"
                                    },
                                    fontSize = 18.sp,
                                    color = if (tab == i) VortexColors.Accent else VortexColors.TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    t.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VortexColors.Accent,
                                selectedTextColor = VortexColors.Accent,
                                unselectedIconColor = VortexColors.TextSecondary,
                                unselectedTextColor = VortexColors.TextSecondary,
                                indicatorColor = VortexColors.AccentDim
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.HOME.ordinal -> HomeScreen(
                    songs = songs,
                    onPlay = { list, index -> PlayerState.playQueue(list, index) },
                    openPlayer = { tab = Tab.PLAYER.ordinal }
                )
                Tab.SEARCH.ordinal -> SearchScreen(
                    songs = songs,
                    onPlay = { list, index -> PlayerState.playQueue(list, index) }
                )
                Tab.LIBRARY.ordinal -> LibraryScreen(
                    songs = songs,
                    onPlay = { list, index -> PlayerState.playQueue(list, index) },
                    openSettings = { showSettings = true }
                )
                Tab.PLAYER.ordinal -> PlayerScreen(
                    onBack = { tab = Tab.HOME.ordinal },
                    onOpenSettings = { showSettings = true }
                )
            }
            if (showSettings) {
                SettingsScreen(onClose = { showSettings = false })
            }
        }
    }
}

@Composable
private fun MiniPlayer(song: Song, playing: Boolean, onTap: () -> Unit, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VortexColors.SurfaceHigh)
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        Artwork(song = song, size = 40)
        Column(Modifier.weight(1f)) {
            Text(
                song.title, color = VortexColors.TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist, color = VortexColors.TextSecondary, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (playing) "\u23F8" else "\u25B6",
            fontSize = 20.sp,
            color = VortexColors.Accent,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onToggle)
                .padding(8.dp)
        )
    }
}
