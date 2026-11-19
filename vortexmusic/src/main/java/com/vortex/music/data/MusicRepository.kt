package com.vortex.music.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One playable local track. ids are MediaStore ids (or file path ids for imports). */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val dateAddedSec: Long,
    val isImported: Boolean = false
) {
    val artworkUri: String
        get() = if (albumId > 0)
            ContentUris.withAppendedId(
                android.net.Uri.parse("content://media/external/audio/albumart"),
                albumId
            ).toString() else ""
}

enum class RepeatMode { OFF, ONE, ALL }

/**
 * Local-only music library: MediaStore scan + JSON persistence for playlists,
 * liked songs, recents and search history. Nothing ever leaves the device.
 */
object MusicRepository {

    private const val LIB_FILE = "vortex_music_library.json"

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _playlists = MutableStateFlow<Map<String, List<Long>>>(emptyMap())
    val playlists: StateFlow<Map<String, List<Long>>> = _playlists

    private val _liked = MutableStateFlow<Set<Long>>(emptySet())
    val liked: StateFlow<Set<Long>> = _liked

    private val _recentIds = MutableStateFlow<List<Long>>(emptyList())
    val recentIds: StateFlow<List<Long>> = _recentIds

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    private val _playCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val playCounts: StateFlow<Map<Long, Int>> = _playCounts

    @Volatile private var loaded = false

    private fun libFile(context: Context) = File(context.filesDir, LIB_FILE)

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            runCatching {
                val f = libFile(context)
                if (f.isFile) {
                    val json = JSONObject(f.readText())
                    _playlists.value = json.optJSONObject("playlists")?.let { obj ->
                        obj.keys().asSequence().associateWith { k ->
                            val arr = obj.getJSONArray(k)
                            (0 until arr.length()).map { arr.getLong(it) }
                        }
                    } ?: emptyMap()
                    _liked.value = json.optJSONArray("liked")?.let { arr ->
                        (0 until arr.length()).map { arr.getLong(it) }.toSet()
                    } ?: emptySet()
                    _recentIds.value = json.optJSONArray("recent")?.let { arr ->
                        (0 until arr.length()).map { arr.getLong(it) }
                    } ?: emptyList()
                    _recentSearches.value = json.optJSONArray("searches")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()
                    _playCounts.value = json.optJSONObject("counts")?.let { obj ->
                        obj.keys().asSequence().map { k -> k.toLong() to obj.getInt(k) }.toMap()
                    } ?: emptyMap()
                }
            }
            loaded = true
        }
    }

    private fun persist(context: Context) {
        runCatching {
            val json = JSONObject()
            val pl = JSONObject()
            _playlists.value.forEach { (name, ids) ->
                pl.put(name, JSONArray(ids))
            }
            json.put("playlists", pl)
            json.put("liked", JSONArray(_liked.value.toList()))
            json.put("recent", JSONArray(_recentIds.value))
            json.put("searches", JSONArray(_recentSearches.value))
            val counts = JSONObject()
            _playCounts.value.forEach { (id, n) -> counts.put(id.toString(), n) }
            json.put("counts", counts)
            libFile(context).writeText(json.toString())
        }
    }

    /** Full MediaStore scan (music only). Safe to call repeatedly to refresh. */
    suspend fun scan(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val out = ArrayList<Song>()
        runCatching {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.IS_MUSIC
            )
            val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, sort
            )?.use { c ->
                val idIx = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val tIx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val aIx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val alIx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val alIdIx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dIx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val daIx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(idIx)
                    val dur = c.getLong(dIx)
                    if (dur <= 0) continue
                    out.add(
                        Song(
                            id = id,
                            title = c.getString(tIx) ?: "Unknown",
                            artist = c.getString(aIx) ?: "Unknown artist",
                            album = c.getString(alIx) ?: "Unknown album",
                            albumId = c.getLong(alIdIx),
                            durationMs = dur,
                            uri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                            ).toString(),
                            dateAddedSec = c.getLong(daIx)
                        )
                    )
                }
            }
        }
        _songs.value = out
        out
    }

    fun songsById(): Map<Long, Song> = _songs.value.associateBy { it.id }

    // ── Library mutations (all persisted) ──────────────────────────────

    fun toggleLike(context: Context, id: Long) {
        val next = _liked.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _liked.value = next
        persist(context)
    }

    fun pushRecent(context: Context, id: Long) {
        val next = _recentIds.value.toMutableList()
        next.remove(id)
        next.add(0, id)
        _recentIds.value = next.take(100)
        val counts = _playCounts.value.toMutableMap()
        counts[id] = (counts[id] ?: 0) + 1
        _playCounts.value = counts
        persist(context)
    }

    fun addSearch(context: Context, query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val next = _recentSearches.value.toMutableList()
        next.remove(q)
        next.add(0, q)
        _recentSearches.value = next.take(20)
        persist(context)
    }

    fun clearSearches(context: Context) {
        _recentSearches.value = emptyList()
        persist(context)
    }

    fun createPlaylist(context: Context, name: String) {
        val n = name.trim()
        if (n.isEmpty() || _playlists.value.containsKey(n)) return
        _playlists.value = _playlists.value + (n to emptyList())
        persist(context)
    }

    fun renamePlaylist(context: Context, old: String, new: String) {
        val n = new.trim()
        if (n.isEmpty() || n == old) return
        val ids = _playlists.value[old] ?: return
        val map = _playlists.value.toMutableMap()
        map.remove(old)
        map[n] = ids
        _playlists.value = map
        persist(context)
    }

    fun deletePlaylist(context: Context, name: String) {
        _playlists.value = _playlists.value - name
        persist(context)
    }

    fun toggleSongInPlaylist(context: Context, name: String, songId: Long) {
        val ids = (_playlists.value[name] ?: emptyList()).toMutableList()
        if (!ids.remove(songId)) ids.add(songId)
        _playlists.value = _playlists.value + (name to ids)
        persist(context)
    }

    // ── Query helpers ──────────────────────────────────────────────────

    fun search(query: String): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return _songs.value.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }

    fun artists(): List<String> =
        _songs.value.map { it.artist }.distinct().sorted()

    fun albums(): List<String> =
        _songs.value.map { it.album }.distinct().sorted()

    fun byArtist(artist: String): List<Song> =
        _songs.value.filter { it.artist == artist }

    fun byAlbum(album: String): List<Song> =
        _songs.value.filter { it.album == album }

    fun recentlyAdded(limit: Int): List<Song> =
        _songs.value.sortedByDescending { it.dateAddedSec }.take(limit)

    fun recentlyPlayed(limit: Int): List<Song> =
        _recentIds.value.mapNotNull { id -> _songs.value.find { it.id == id } }.take(limit)

    fun mostPlayed(limit: Int): List<Song> =
        _songs.value.sortedByDescending { _playCounts.value[it.id] ?: 0 }
            .filter { (_playCounts.value[it.id] ?: 0) > 0 }.take(limit)

    fun popularArtists(limit: Int): List<String> =
        _songs.value.groupBy { it.artist }
            .map { (artist, list) -> artist to list.size }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)

    fun likedSongs(): List<Song> =
        _liked.value.mapNotNull { id -> _songs.value.find { it.id == id } }
}
