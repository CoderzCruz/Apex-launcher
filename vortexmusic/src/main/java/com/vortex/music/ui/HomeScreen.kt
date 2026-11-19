package com.vortex.music.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.music.data.MusicRepository
import com.vortex.music.data.Song

/**
 * Home: every section is derived from the device's real local library
 * (no fake or streamed content) — recents, play counts, date added and the
 * user's own playlists drive the shelves.
 */
@Composable
fun HomeScreen(songs: List<Song>, onPlay: (List<Song>, Int) -> Unit, openPlayer: () -> Unit) {
    val playlists by MusicRepository.playlists.collectAsState()
    val recentIds by MusicRepository.recentIds.collectAsState()
    rememberPlayCounts()

    if (songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No local music found", color = VortexColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Add mp3 / wav / m4a files to your device music folders, then pull to refresh.",
                    color = VortexColors.TextSecondary, fontSize = 12.5.sp
                )
            }
        }
        return
    }

    val recentlyPlayed = MusicRepository.recentlyPlayed(10)
    val madeForYou = songs.shuffled().take(10)
    val trending = MusicRepository.recentlyAdded(10)
    val popularArtists = MusicRepository.popularArtists(8)
    val featured = playlists.keys.take(8)
    val newMusic = MusicRepository.recentlyAdded(10)
    val recommendedAlbums = songs.groupBy { it.album }.values
        .sortedByDescending { it.size }.map { it.first() }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = greeting(),
                color = VortexColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            )
        }

        if (recentlyPlayed.isNotEmpty()) {
            item { SectionHeader("Recently Played") }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentlyPlayed, key = { "r${it.id}" }) { song ->
                        BigCard(song) { onPlay(recentlyPlayed, recentlyPlayed.indexOf(song)) }
                    }
                }
            }
        }

        item { SectionHeader("Made For You") }
        item {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(madeForYou, key = { "m${it.id}" }) { song ->
                    BigCard(song) { onPlay(madeForYou, madeForYou.indexOf(song)) }
                }
            }
        }

        item { SectionHeader("Trending Songs") }
        item {
            Column {
                trending.take(5).forEachIndexed { i, song ->
                    NumberedRow(i + 1, song) { onPlay(trending, i) }
                }
            }
        }

        if (popularArtists.isNotEmpty()) {
            item { SectionHeader("Popular Artists") }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(popularArtists, key = { it }) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(84.dp)
                                .clickable {
                                    val list = MusicRepository.byArtist(artist)
                                    if (list.isNotEmpty()) onPlay(list, 0)
                                }
                        ) {
                            Box(
                                Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF3A1216), VortexColors.SurfaceHigh)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    artist.take(1).uppercase(),
                                    color = VortexColors.Accent,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                artist, color = VortexColors.TextPrimary, fontSize = 11.5.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (featured.isNotEmpty()) {
            item { SectionHeader("Featured Playlists") }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(featured, key = { it }) { name ->
                        val ids = playlists[name] ?: emptyList()
                        val plSongs = ids.mapNotNull { id -> songs.find { it.id == id } }
                        PlaylistCard(name, plSongs.size) {
                            if (plSongs.isNotEmpty()) onPlay(plSongs, 0)
                        }
                    }
                }
            }
        }

        item { SectionHeader("New Music") }
        item {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(newMusic, key = { "n${it.id}" }) { song ->
                    BigCard(song) { onPlay(newMusic, newMusic.indexOf(song)) }
                }
            }
        }

        if (recommendedAlbums.isNotEmpty()) {
            item { SectionHeader("Recommended Albums") }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recommendedAlbums, key = { "a${it.id}" }) { song ->
                        BigCard(song, subtitle = song.album) {
                            val list = MusicRepository.byAlbum(song.album)
                            onPlay(list, list.indexOf(song).coerceAtLeast(0))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun BigCard(song: Song, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Artwork(song = song, size = 128, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle ?: song.title,
            color = VortexColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle?.let { song.title } ?: song.artist,
            color = VortexColors.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistCard(name: String, count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VortexColors.Surface)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF40141A), VortexColors.SurfaceHigh))),
            contentAlignment = Alignment.Center
        ) {
            Text("♪", color = VortexColors.Accent, fontSize = 34.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        Text(name, color = VortexColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count songs", color = VortexColors.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun NumberedRow(index: Int, song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "$index",
            color = VortexColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp)
        )
        Artwork(song = song, size = 44)
        Column(Modifier.weight(1f)) {
            Text(song.title, color = VortexColors.TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = VortexColors.TextSecondary, fontSize = 11.5.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val plays = playCountLabel(song.id)
        if (plays != null) {
            Text(plays, color = VortexColors.TextSecondary, fontSize = 11.sp)
        }
    }
}

private var playCountsCache: Map<Long, Int> = emptyMap()

private fun playCountLabel(id: Long): String? {
    val n = playCountsCache[id] ?: return null
    return if (n > 0) "$n plays" else null
}

/** Refresh the small play-count labels used by the trending rows. */
@Composable
private fun rememberPlayCounts(): Map<Long, Int> {
    val counts by MusicRepository.playCounts.collectAsState()
    playCountsCache = counts
    return counts
}
