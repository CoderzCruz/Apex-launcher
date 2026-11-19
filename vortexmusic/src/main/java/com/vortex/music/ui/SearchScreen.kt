package com.vortex.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.music.data.MusicRepository
import com.vortex.music.data.Song

private val FILTERS = listOf("All", "Songs", "Artists", "Albums", "Playlists")

@Composable
fun SearchScreen(songs: List<Song>, onPlay: (List<Song>, Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val recents by MusicRepository.recentSearches.collectAsState()
    val playlists by MusicRepository.playlists.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Songs, artists, albums, playlists", color = VortexColors.TextSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VortexColors.Accent,
                unfocusedBorderColor = VortexColors.SurfaceHigh,
                cursorColor = VortexColors.Accent,
                focusedTextColor = VortexColors.TextPrimary,
                unfocusedTextColor = VortexColors.TextPrimary
            )
        )

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FILTERS, key = { it }) { f ->
                Text(
                    text = f,
                    color = if (filter == f) Color.White else VortexColors.TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (filter == f) VortexColors.Accent else VortexColors.Surface)
                        .clickable {
                            filter = f
                            if (query.isNotBlank()) MusicRepository.addSearch(context, query)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (query.isBlank()) {
            if (recents.isNotEmpty()) {
                Text(
                    "Recent searches",
                    color = VortexColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                LazyColumn {
                    items(recents, key = { it }) { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = q }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("⌕", color = VortexColors.TextSecondary, fontSize = 15.sp)
                            Text(q, color = VortexColors.TextPrimary, fontSize = 13.5.sp)
                        }
                    }
                    item {
                        Text(
                            "Clear history",
                            color = VortexColors.Accent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(16.dp)
                                .clickable { MusicRepository.clearSearches(context) }
                        )
                    }
                }
            } else {
                EmptyHint("Search your local library — songs, artists, albums and playlists.")
            }
        } else {
            val results = MusicRepository.search(query)
            val artists = if (filter == "All" || filter == "Artists")
                MusicRepository.artists().filter { it.contains(query, true) } else emptyList()
            val albums = if (filter == "All" || filter == "Albums")
                MusicRepository.albums().filter { it.contains(query, true) } else emptyList()
            val playlistNames = if (filter == "All" || filter == "Playlists")
                playlists.keys.filter { it.contains(query, true) } else emptyList()

            LazyColumn {
                if ((filter == "All" || filter == "Songs") && results.isNotEmpty()) {
                    item {
                        Text("Songs", color = VortexColors.TextPrimary, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 8.dp))
                    }
                    items(results, key = { "s${it.id}" }) { song ->
                        SongRow(song) {
                            MusicRepository.addSearch(context, query)
                            onPlay(results, results.indexOf(song))
                        }
                    }
                }
                if (artists.isNotEmpty()) {
                    item {
                        Text("Artists", color = VortexColors.TextPrimary, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 8.dp))
                    }
                    items(artists, key = { "ar$it" }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    MusicRepository.addSearch(context, query)
                                    val list = MusicRepository.byArtist(artist)
                                    if (list.isNotEmpty()) onPlay(list, 0)
                                }
                                .padding(16.dp, 8.dp)
                        ) {
                            Text(artist, color = VortexColors.TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
                if (albums.isNotEmpty()) {
                    item {
                        Text("Albums", color = VortexColors.TextPrimary, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 8.dp))
                    }
                    items(albums, key = { "al$it" }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    MusicRepository.addSearch(context, query)
                                    val list = MusicRepository.byAlbum(album)
                                    if (list.isNotEmpty()) onPlay(list, 0)
                                }
                                .padding(16.dp, 8.dp)
                        ) {
                            Text(album, color = VortexColors.TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
                if (playlistNames.isNotEmpty()) {
                    item {
                        Text("Playlists", color = VortexColors.TextPrimary, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 8.dp))
                    }
                    items(playlistNames, key = { "p$it" }) { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    MusicRepository.addSearch(context, query)
                                    val list = (playlists[name] ?: emptyList())
                                        .mapNotNull { id -> songs.find { it.id == id } }
                                    if (list.isNotEmpty()) onPlay(list, 0)
                                }
                                .padding(16.dp, 8.dp)
                        ) {
                            Text(name, color = VortexColors.TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
                if (results.isEmpty() && artists.isEmpty() && albums.isEmpty() && playlistNames.isEmpty()) {
                    item { EmptyHint("No matches for \"$query\"") }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
