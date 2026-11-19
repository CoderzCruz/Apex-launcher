package com.vortex.music.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for playback state, shared between [PlaybackService]
 * (writer) and every screen (readers). All controls here are thin forwards
 * onto the running service; when no service exists the calls are no-ops.
 */
object PlayerState {

    private val _queue = MutableStateFlow<List<com.vortex.music.data.Song>>(emptyList())
    val queue: StateFlow<List<com.vortex.music.data.Song>> = _queue

    private val _index = MutableStateFlow(-1)
    val index: StateFlow<Int> = _index

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle

    private val _repeat = MutableStateFlow(com.vortex.music.data.RepeatMode.OFF)
    val repeat: StateFlow<com.vortex.music.data.RepeatMode> = _repeat

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val current: com.vortex.music.data.Song?
        get() = _index.value.takeIf { it in _queue.value.indices }?.let { _queue.value[it] }

    @Volatile var service: PlaybackService? = null

    fun playQueue(songs: List<com.vortex.music.data.Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        _queue.value = songs
        _index.value = startIndex.coerceIn(0, songs.size - 1)
        service?.playCurrent(auto = false)
    }

    fun toggle() = service?.togglePlayPause()
    fun next() = service?.next(auto = false)
    fun prev() = service?.prev()
    fun seekTo(ms: Long) = service?.seekTo(ms)
    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        service?.onShuffleChanged()
    }
    fun cycleRepeat() {
        _repeat.value = when (_repeat.value) {
            com.vortex.music.data.RepeatMode.OFF -> com.vortex.music.data.RepeatMode.ALL
            com.vortex.music.data.RepeatMode.ALL -> com.vortex.music.data.RepeatMode.ONE
            com.vortex.music.data.RepeatMode.ONE -> com.vortex.music.data.RepeatMode.OFF
        }
    }
    fun clearError() { _error.value = null }

    // Internal writers used by the service.
    internal fun setPlaying(v: Boolean) { _playing.value = v }
    internal fun setPosition(ms: Long) { _positionMs.value = ms }
    internal fun setDuration(ms: Long) { _durationMs.value = ms }
    internal fun setIndex(i: Int) { _index.value = i }
    internal fun setError(msg: String?) { _error.value = msg }
}
