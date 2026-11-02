#!/usr/bin/env python3
"""Public chat extras: voice notes and owner/CEO badges."""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
BACKEND = APP / "java/net/kdt/pojavlaunch/aerix/social/SocialBackend.kt"
BADGE = APP / "java/net/kdt/pojavlaunch/aerix/social/ApexBadge.kt"
SCREEN = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/ChatsScreen.kt"
MANIFEST = APP / "AndroidManifest.xml"

backend = BACKEND.read_text()
# Voice notes upload to the media bucket with an audio MIME and post as a chat message.
assert "suspend fun uploadVoice(" in backend
assert '"audio/mp4".toMediaType()' in backend
assert "suspend fun sendPublicChatVoice(" in backend
assert 'VOICE_MESSAGE_LABEL' in backend
assert "attachmentUri = Uri.parse(url)" in backend

badge = BADGE.read_text()
assert "enum class ApexBadge" in badge
assert 'CEO("CEO"' in badge and 'OWNER("OWNER"' in badge and 'DEVELOPER(' in badge
assert '"yadavboy" to ApexBadge.CEO' in badge
assert '"cruz" to ApexBadge.OWNER' in badge
assert "fun forName(" in badge

screen = SCREEN.read_text()
# Recording: real MediaRecorder, real permission, uploaded clip.
assert "MediaRecorder" in screen and "AudioSource.MIC" in screen
assert "sendPublicChatVoice(bytes)" in screen
# Playback of the clip.
assert "MediaPlayer" in screen and "prepareAsync()" in screen
# Badges next to names.
assert "ApexBadges.forName(" in screen
# Tailed bubbles: the message shape is a custom outline with a corner tail.
assert "TailBubbleShape" in screen and "quadraticBezierTo" in screen
assert "onRight = mine" in screen

assert "android.permission.RECORD_AUDIO" in MANIFEST.read_text()

print("Voice notes and badges passed")
