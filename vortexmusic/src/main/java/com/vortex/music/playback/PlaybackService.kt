package com.vortex.music.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.vortex.music.R
import com.vortex.music.data.MusicRepository
import com.vortex.music.data.RepeatMode
import com.vortex.music.data.Song
import com.vortex.music.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real playback engine: one MediaPlayer in a foreground media service with a
 * MediaSession (headset/media-button controls), audio focus handling and a
 * persistent notification with play/next/prev actions. Queue, shuffle and
 * repeat logic live here; UI reads [PlayerState].
 */
class PlaybackService : android.app.Service() {

    private var player: MediaPlayer? = null
    private var session: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var positionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var currentShuffleOrder: List<Int> = emptyList()
    private var shuffleCursor = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        PlayerState.service = this
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        createChannel()
        session = MediaSessionCompat(this, "VortexMusic").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = togglePlayPause()
                override fun onPause() = togglePlayPause()
                override fun onSkipToNext() = next(auto = false)
                override fun onSkipToPrevious() = prev()
                override fun onStop() {
                    togglePlayPause()
                }
            })
            isActive = true
        }
        rebuildShuffleOrder()
        startPositionTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(session, intent)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        PlayerState.service = null
        positionJob?.cancel()
        runCatching { player?.release() }
        player = null
        runCatching { session?.release() }
        session = null
        abandonFocus()
        super.onDestroy()
    }

    // ── Playback controls ──────────────────────────────────────────────

    fun playCurrent(auto: Boolean) {
        val song = PlayerState.current ?: return
        if (auto && PlayerState.repeat.value == RepeatMode.ONE) {
            seekTo(0); playPrepared(); return
        }
        runCatching { player?.release() }
        player = null
        val mp = MediaPlayer()
        player = mp
        mp.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        mp.setOnPreparedListener {
            PlayerState.setDuration(it.duration.toLong())
            if (!requestFocus()) {
                // Focus denied but user pressed play explicitly: still play (their choice).
            }
            it.start()
            PlayerState.setPlaying(true)
            pushNotification(song, true)
            MusicRepository.pushRecent(applicationContext, song.id)
        }
        mp.setOnCompletionListener { handleCompletion() }
        mp.setOnErrorListener { _, what, extra ->
            PlayerState.setPlaying(false)
            PlayerState.setError("Cannot play \"${song.title}\" (error $what/$extra)")
            pushNotification(song, false)
            true
        }
        try {
            mp.setDataSource(applicationContext, android.net.Uri.parse(song.uri))
            mp.prepareAsync()
            PlayerState.setPlaying(false)
            pushNotification(song, false)
        } catch (e: Exception) {
            PlayerState.setError("Cannot open \"${song.title}\": ${e.message}")
            PlayerState.setPlaying(false)
        }
    }

    private fun playPrepared() {
        val mp = player ?: return
        runCatching {
            if (mp.isPlaying) return
            mp.start()
            PlayerState.setPlaying(true)
            PlayerState.current?.let { pushNotification(it, true) }
        }
    }

    fun togglePlayPause() {
        val mp = player ?: run {
            if (PlayerState.current != null) playCurrent(auto = false)
            return
        }
        runCatching {
            if (mp.isPlaying) {
                mp.pause()
                PlayerState.setPlaying(false)
            } else {
                if (!requestFocus()) { /* explicit user action still plays */ }
                mp.start()
                PlayerState.setPlaying(true)
            }
            PlayerState.current?.let { pushNotification(it, PlayerState.playing.value) }
        }
    }

    fun seekTo(ms: Long) {
        runCatching { player?.seekTo(ms.toInt()) }
        PlayerState.setPosition(ms)
    }

    fun next(auto: Boolean) {
        val queue = PlayerState.queue.value
        if (queue.isEmpty()) return
        val nextIndex = pickNext(auto) ?: run {
            // End of queue (no repeat): stop playback but keep the last song loaded.
            runCatching { player?.pause() }
            PlayerState.setPlaying(false)
            PlayerState.current?.let { pushNotification(it, false) }
            return
        }
        PlayerState.setIndex(nextIndex)
        playCurrent(auto = false)
    }

    fun prev() {
        val mp = player
        if (mp != null && mp.currentPosition > 3000) {
            seekTo(0); return
        }
        val queue = PlayerState.queue.value
        if (queue.isEmpty()) return
        val idx = (PlayerState.index.value - 1).coerceAtLeast(0)
        PlayerState.setIndex(idx)
        playCurrent(auto = false)
    }

    fun onShuffleChanged() {
        rebuildShuffleOrder()
    }

    private fun rebuildShuffleOrder() {
        val n = PlayerState.queue.value.size
        currentShuffleOrder = (0 until n).shuffled()
        val cur = PlayerState.index.value
        shuffleCursor = currentShuffleOrder.indexOf(cur).takeIf { it >= 0 } ?: 0
    }

    private fun pickNext(auto: Boolean): Int? {
        val queue = PlayerState.queue.value
        if (queue.isEmpty()) return null
        val repeat = PlayerState.repeat.value
        if (PlayerState.shuffle.value) {
            if (currentShuffleOrder.size != queue.size) rebuildShuffleOrder()
            shuffleCursor++
            if (shuffleCursor >= currentShuffleOrder.size) {
                if (repeat == RepeatMode.OFF) return null
                rebuildShuffleOrder()
                shuffleCursor = 0
            }
            return currentShuffleOrder[shuffleCursor]
        }
        val idx = PlayerState.index.value + 1
        return when {
            idx < queue.size -> idx
            repeat == RepeatMode.ALL || !auto -> if (repeat == RepeatMode.OFF && auto) null else 0
            else -> null
        }
    }

    private fun handleCompletion() {
        when (PlayerState.repeat.value) {
            RepeatMode.ONE -> { seekTo(0); playPrepared() }
            else -> next(auto = true)
        }
    }

    // ── Audio focus ────────────────────────────────────────────────────

    private fun requestFocus(): Boolean {
        val am = audioManager ?: return true
        if (!MusicRepository.let { com.vortex.music.ui.SettingsStore.audioFocusEnabled(appContext()) }) return true
        return if (Build.VERSION.SDK_INT >= 26) {
            val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { change ->
                    when (change) {
                        AudioManager.AUDIOFOCUS_LOSS -> runCatching {
                            player?.pause(); PlayerState.setPlaying(false)
                            PlayerState.current?.let { pushNotification(it, false) }
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> runCatching {
                            player?.pause(); PlayerState.setPlaying(false)
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> runCatching {
                            player?.setVolume(0.3f, 0.3f)
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> runCatching {
                            player?.setVolume(1f, 1f)
                        }
                    }
                }
                .build()
            focusRequest = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                { change ->
                    if (change == AudioManager.AUDIOFOCUS_LOSS) runCatching {
                        player?.pause(); PlayerState.setPlaying(false)
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest?.let { runCatching { am.abandonAudioFocusRequest(it) } }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                am.abandonAudioFocus(AudioManager.OnAudioFocusChangeListener { })
            }
        }
    }

    private fun appContext(): android.content.Context = applicationContext

    // ── Position ticker ────────────────────────────────────────────────

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                delay(500)
                val mp = player ?: continue
                runCatching {
                    if (mp.isPlaying) PlayerState.setPosition(mp.currentPosition.toLong())
                }
            }
        }
    }

    // ── Notification + channel ─────────────────────────────────────────

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Vortex Music playback" }
            )
        }
    }

    private fun pushNotification(song: Song, playing: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPause = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this,
            if (playing) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        )
        val next = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
        val prev = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vortex_music)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(openApp)
            .setOngoing(playing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_vortex_music, "Previous", prev)
            .addAction(
                if (playing) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (playing) "Pause" else "Play", playPause
            )
            .addAction(R.drawable.ic_vortex_music, "Next", next)
        val n: Notification = builder.build()
        startForeground(NOTIFICATION_ID, n)
        if (!playing) {
            // Keep the service alive briefly for the paused state, then let the
            // system reclaim it if nothing restarts playback.
            scope.launch {
                delay(10_000)
                if (PlayerState.playing.value) return@launch
                runCatching { stopForeground(STOP_FOREGROUND_DETACH) }
                stopSelf()
            }
        } else {
            nm.notify(NOTIFICATION_ID, n)
        }
    }

    companion object {
        private const val CHANNEL_ID = "vortex_music_playback"
        private const val NOTIFICATION_ID = 42001
    }
}
