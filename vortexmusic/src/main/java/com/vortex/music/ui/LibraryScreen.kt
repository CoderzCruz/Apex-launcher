package com.vortex.music.ui

import android.app.AlertDialog
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.music.data.MusicRepository
import com.vortex.music.data.Song
import kotlinx.coroutines.launch

private enum class LibraryTab { PLAYLISTS, LIKED, RECENT, LOCAL }

@Composable
fun LibraryScreen(
    songs: List<Song>,
    onPlay: (List<Song>, Int) -> Unit,
    openSettings: () -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    val playlists by MusicRepository.playlists.collectAsState()
    val liked by MusicRepository.liked.collectAsState()
    val recentIds by MusicRepository.recentIds.collectAsState()
    var openPlaylist by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<String?>(null) }

    // Create / rename dialogs
    if (showCreate || renameTarget != null) {
        var name by remember { mutableStateOf(renameTarget ?: "") }
        AlertDialog(
            onDismissRequest = { showCreate = false; renameTarget = null },
            title = { Text(if (renameTarget != null) "Rename playlist" else "New playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Playlist name") }
                )
            },
            confirmButton = {
                Text("Save", color = VortexColors.Accent, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            if (renameTarget != null) {
                                MusicRepository.renamePlaylist(context, renameTarget!!, name)
                            } else {
                                MusicRepository.createPlaylist(context, name)
                            }
                            showCreate = false
                            renameTarget = null
                        }
                        .padding(8.dp))
            },
            dismissButton = {
                Text("Cancel", color = VortexColors.TextSecondary,
                    modifier = Modifier
                        .clickable { showCreate = false; renameTarget = null }
                        .padding(8.dp))
            }
        )
    }

    openPlaylist?.let { plName ->
        PlaylistDetail(
            name = plName,
            songs = songs,
            onPlay = onPlay,
            onBack = { openPlaylist = null },
            onRename = { renameTarget = plName },
            onDelete = {
                MusicRepository.deletePlaylist(context, plName)
                openPlaylist = null
            },
            onRemoveSong = { id -> MusicRepository.toggleSongInPlaylist(context, plName, id) }
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LibraryTab.entries.forEach { t ->
                Text(
                    text = when (t) {
                        LibraryTab.PLAYLISTS -> "Playlists"
                        LibraryTab.LIKED -> "Liked"
                        LibraryTab.RECENT -> "Recent"
                        LibraryTab.LOCAL -> "Local"
                    },
                    color = if (tab == t) Color.White else VortexColors.TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (tab == t) VortexColors.Accent else VortexColors.Surface)
                        .clickable { tab = t }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text("⚙", fontSize = 17.sp, color = VortexColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = openSettings)
                    .padding(6.dp))
        }

        when (tab) {
            LibraryTab.PLAYLISTS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${playlists.size} playlists",
                        color = VortexColors.TextSecondary, fontSize = 12.sp
                    )
                    Text(
                        "+ New playlist",
                        color = VortexColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showCreate = true }
                            .padding(8.dp)
                    )
                }
                if (playlists.isEmpty()) {
                    EmptyHint("Create your first playlist with '+ New playlist'.")
                } else {
                    LazyColumn {
                        items(playlists.keys.sorted(), key = { it }) { name ->
                            val ids = playlists[name] ?: emptyList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openPlaylist = name }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF3A1216), VortexColors.SurfaceHigh)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) { Text("♪", color = VortexColors.Accent, fontSize = 20.sp) }
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = VortexColors.TextPrimary,
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${ids.size} songs", color = VortexColors.TextSecondary, fontSize = 11.5.sp)
                                }
                                Text("›", color = VortexColors.TextSecondary, fontSize = 18.sp)
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
            LibraryTab.LIKED -> {
                val likedSongs = MusicRepository.likedSongs()
                if (likedSongs.isEmpty()) {
                    EmptyHint("Songs you like appear here — tap ♡ on the player.")
                } else {
                    LazyColumn {
                        items(likedSongs, key = { "l${it.id}" }) { song ->
                            SongRow(song) {
                                onPlay(likedSongs, likedSongs.indexOf(song))
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
            LibraryTab.RECENT -> {
                val recent = recentIds.mapNotNull { id -> songs.find { it.id == id } }
                if (recent.isEmpty()) {
                    EmptyHint("Songs you play show up here.")
                } else {
                    LazyColumn {
                        items(recent, key = { "rc${it.id}" }) { song ->
                            SongRow(song) { onPlay(recent, recent.indexOf(song)) }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
            LibraryTab.LOCAL -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${songs.size} songs on device", color = VortexColors.TextSecondary, fontSize = 12.sp)
                    Text("Rescan", color = VortexColors.Accent, fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                kotlinx.coroutines.MainScope().launch { MusicRepository.scan(context) }
                            }
                            .padding(4.dp))
                }
                if (songs.isEmpty()) {
                    EmptyHint("No local audio found. Add music files and tap Rescan.")
                } else {
                    LazyColumn {
                        items(songs, key = { "loc${it.id}" }) { song ->
                            SongRow(song) { onPlay(songs, songs.indexOf(song)) }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    name: String,
    songs: List<Song>,
    onPlay: (List<Song>, Int) -> Unit,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRemoveSong: (Long) -> Unit
) {
    val context = LocalContext.current
    val ids by MusicRepository.playlists.collectAsState()
    val playlistSongs = (ids[name] ?: emptyList()).mapNotNull { id -> songs.find { it.id == id } }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("‹", color = VortexColors.TextPrimary, fontSize = 24.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(4.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = VortexColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("${playlistSongs.size} songs", color = VortexColors.TextSecondary, fontSize = 12.sp)
            }
            Text("Rename", color = VortexColors.Accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRename).padding(6.dp))
            Text("Delete", color = VortexColors.Accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    AlertDialog.Builder(context)
                        .setTitle("Delete \"$name\"?")
                        .setMessage("The playlist is removed. Songs stay on your device.")
                        .setPositiveButton("Delete") { _, _ -> onDelete() }
                        .setNegativeButton("Cancel", null)
                        .show()
                }.padding(6.dp))
        }

        if (playlistSongs.isEmpty()) {
            EmptyHint("Open Local / Liked and use 'Add to playlist' from a song.")
        } else {
            LazyColumn {
                items(playlistSongs, key = { "pl${it.id}" }) { song ->
                    SongRow(
                        song = song,
                        onClick = { onPlay(playlistSongs, playlistSongs.indexOf(song)) },
                        trailing = {
                            Text("✕", color = VortexColors.TextSecondary, fontSize = 15.sp,
                                modifier = Modifier
                                    .clickable { onRemoveSong(song.id) }
                                    .padding(8.dp))
                        }
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val candidates = songs.filter { s -> playlistSongs.none { it.id == s.id } }
                                if (candidates.isEmpty()) {
                                    android.widget.Toast
                                        .makeText(context, "Every local song is already here",
                                            android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    // Add the first candidate as a quick-add; full picker = Local tab.
                                    MusicRepository.toggleSongInPlaylist(context, name, candidates.first().id)
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("+ Add a song", color = VortexColors.Accent,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
