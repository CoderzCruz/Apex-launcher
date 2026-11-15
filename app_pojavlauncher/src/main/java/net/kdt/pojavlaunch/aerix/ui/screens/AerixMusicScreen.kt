/*
 * Aerix Launcher — Vortex Launcher music player.
 * Songs are picked through the system document picker (persistable read grants) and playback
 * lives in [AerixMusicPlayer], a process-wide object: leaving this screen — or even entering a
 * Minecraft world in the :game process — does not stop the music while this process lives.
 */
package net.kdt.pojavlaunch.aerix.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.aerix.components.apexPress
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface

/** System picker for any audio file; results carry a persistable read grant. */
private class OpenAudioDocument : ActivityResultContract<Void?, Uri?>() {
    override fun createIntent(context: Context, input: Void?): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("audio/*")

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

/**
 * Process-wide playlist + playback. State is Compose-observable so the screen updates live;
 * the MediaPlayer keeps playing across launcher screens and while the game runs.
 */
object AerixMusicPlayer {
    data class Song(val title: String, val uri: Uri)

    val songs = mutableStateListOf<Song>()
    var currentIndex by mutableIntStateOf(-1)
    var isPlaying by mutableStateOf(false)
    private var player: MediaPlayer? = null
    private var loadedOnce = false

    private const val PREFS = "aerix_music_prefs"
    private const val KEY_SONGS = "songUris"

    private fun songTitle(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull() ?: uri.lastPathSegment ?: "Song"

    /** Restores the persisted list once per process; revoked URIs are dropped silently. */
    fun load(context: Context) {
        if (loadedOnce) return
        loadedOnce = true
        val app = context.applicationContext
        val stored = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SONGS, "") ?: ""
        stored.split("\n").filter { it.isNotBlank() }.forEach { raw ->
            runCatching { Uri.parse(raw) }.onSuccess { uri ->
                runCatching {
                    app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                songs.add(Song(songTitle(app, uri), uri))
            }
        }
        currentIndex = -1
    }

    private fun persist(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SONGS, songs.joinToString("\n") { it.uri.toString() })
            .apply()
    }

    fun addSong(context: Context, uri: Uri) {
        val app = context.applicationContext
        runCatching {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        songs.add(Song(songTitle(app, uri), uri))
        persist(app)
        if (songs.size == 1) playAt(app, 0)
    }

    fun playAt(context: Context, index: Int) {
        if (index !in songs.indices) return
        currentIndex = index
        val app = context.applicationContext
        val fresh = MediaPlayer()
        val prepared = runCatching {
            fresh.setDataSource(app, songs[index].uri)
            fresh.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            fresh.setOnPreparedListener { mp -> mp.start(); isPlaying = true }
            fresh.setOnCompletionListener { next(app) }
            fresh.prepareAsync()
        }.isSuccess
        if (!prepared) {
            runCatching { fresh.release() }
            isPlaying = false
            return
        }
        player?.let { old ->
            runCatching {
                old.setOnCompletionListener(null)
                old.release()
            }
        }
        player = fresh
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            isPlaying = false
        } else {
            p.start()
            isPlaying = true
        }
    }

    fun next(context: Context) {
        if (songs.isEmpty()) return
        playAt(context, (currentIndex + 1).coerceAtLeast(0) % songs.size)
    }

    fun prev(context: Context) {
        if (songs.isEmpty()) return
        playAt(context, if (currentIndex <= 0) songs.lastIndex else currentIndex - 1)
    }
}

/** Small pill control used by the transport row. */
@Composable
private fun MusicPillButton(label: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(AerixRadii.control))
            .apexPress(onClick = onClick),
        shape = RoundedCornerShape(AerixRadii.control),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(AerixSpacing.hairline, accent.copy(alpha = 0.7f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.xs)
        )
    }
}

@Composable
fun AerixMusicScreen() {
    val context = LocalContext.current
    val red = Color(0xFFDC2626)
    val addLauncher = rememberLauncherForActivityResult(OpenAudioDocument()) { uri ->
        if (uri != null) AerixMusicPlayer.addSong(context, uri)
    }
    LaunchedEffect(Unit) { AerixMusicPlayer.load(context) }
    val songs = AerixMusicPlayer.songs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AerixSpacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        Text(
            text = "Music",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AerixSurface.textPrimary
        )
        Text(
            text = "Pick songs and keep listening — playback continues while you play Minecraft.",
            style = MaterialTheme.typography.labelMedium,
            color = AerixSurface.textSecondary
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AerixRadii.panel))
                .apexPress(onClick = { addLauncher.launch(null) }),
            shape = RoundedCornerShape(AerixRadii.panel),
            color = red.copy(alpha = 0.16f),
            border = BorderStroke(AerixSpacing.hairline, red)
        ) {
            Box(
                modifier = Modifier.padding(vertical = AerixSpacing.sm, horizontal = AerixSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Add songs",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (songs.isEmpty()) {
            Text(
                text = "No songs yet — tap \u201c+ Add songs\u201d.",
                style = MaterialTheme.typography.bodyMedium,
                color = AerixSurface.textMuted
            )
        } else {
            val currentTitle = songs.getOrNull(AerixMusicPlayer.currentIndex)?.title
            Surface(
                shape = RoundedCornerShape(AerixRadii.panel),
                color = AerixSurface.panel,
                border = BorderStroke(AerixSpacing.hairline, red)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AerixSpacing.smPlus),
                    verticalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
                ) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold,
                        color = red
                    )
                    Text(
                        text = currentTitle ?: "Nothing yet",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AerixSurface.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicPillButton("Prev", red) { AerixMusicPlayer.prev(context) }
                        MusicPillButton(if (AerixMusicPlayer.isPlaying) "Pause" else "Play", red) {
                            AerixMusicPlayer.toggle()
                        }
                        MusicPillButton("Next", red) { AerixMusicPlayer.next(context) }
                    }
                }
            }

            songs.forEachIndexed { index, song ->
                val active = index == AerixMusicPlayer.currentIndex
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AerixRadii.control))
                        .apexPress(onClick = { AerixMusicPlayer.playAt(context, index) }),
                    shape = RoundedCornerShape(AerixRadii.control),
                    color = if (active) red.copy(alpha = 0.14f) else Color.Transparent,
                    border = BorderStroke(
                        AerixSpacing.hairline,
                        if (active) red else AerixSurface.borderSoft
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AerixSpacing.smPlus,
                            vertical = AerixSpacing.smTight
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) red else AerixSurface.textMuted
                        )
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = AerixSurface.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (active && AerixMusicPlayer.isPlaying) "playing" else "",
                            fontSize = 10.sp,
                            color = red
                        )
                    }
                }
            }
        }
    }
}
