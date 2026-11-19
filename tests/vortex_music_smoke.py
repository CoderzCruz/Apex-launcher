"""Vortex Music module: real playback pieces must exist, no mockups."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MOD = ROOT / "vortexmusic"

# Module skeleton
assert (MOD / "build.gradle").exists(), "module build.gradle"
manifest = (MOD / "src/main/AndroidManifest.xml").read_text()
assert ".ui.MainActivity" in manifest and "PlaybackService" in manifest
assert "FOREGROUND_SERVICE_MEDIA_PLAYBACK" in manifest, "background playback permission"
assert "MediaButtonReceiver" in manifest, "headset/media button support"

# Real engine, not a mockup
service = (MOD / "src/main/java/com/vortex/music/playback/PlaybackService.kt").read_text()
for token in ["MediaPlayer", "MediaSessionCompat", "AudioFocusRequest", "prepareAsync", "seekTo"]:
    assert token in service, f"service missing {token}"
state = (MOD / "src/main/java/com/vortex/music/playback/PlayerState.kt").read_text()
assert "playQueue" in state and "toggleShuffle" in state

# Local library only — no network/streaming URLs in the engine
repo = (MOD / "src/main/java/com/vortex/music/data/MusicRepository.kt").read_text()
assert "MediaStore" in repo, "real local scan"
assert "playlists.json" in repo or "vortex_music_library.json" in repo, "persisted library"
for banned in ["http://", "https://"]:
    assert banned not in repo, "library must stay local-only"

# All four tabs exist and are wired
main = (MOD / "src/main/java/com/vortex/music/ui/MainActivity.kt").read_text()
for tab in ["HOME", "SEARCH", "LIBRARY", "PLAYER"]:
    assert tab in main, f"tab {tab}"
assert "SettingsScreen" in main, "settings wired"

# Screens exist with real controls
player = (MOD / "src/main/java/com/vortex/music/ui/PlayerScreen.kt").read_text()
for token in ["Slider", "PlayerState.toggle", "PlayerState.next", "PlayerState.prev", "toggleLike", "cycleRepeat"]:
    assert token in player, f"player missing {token}"
for screen in ["HomeScreen.kt", "SearchScreen.kt", "LibraryScreen.kt", "SettingsScreen.kt"]:
    assert (MOD / "src/main/java/com/vortex/music/ui" / screen).exists(), screen

# Workflow publishes the APK
wf = (ROOT / ".github/workflows/vortex-music.yml").read_text()
assert "vortexmusic:assembleDebug" in wf and "music-latest" in wf

print("Vortex Music smoke test passed")
