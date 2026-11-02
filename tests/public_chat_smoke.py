#!/usr/bin/env python3
"""Public Chat: one shared room, reachable from the rail, readable by every player."""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
BACKEND = APP / "java/net/kdt/pojavlaunch/aerix/social/SocialBackend.kt"
SCREEN = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/PublicChatScreen.kt"
RAIL = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixNavigationRail.kt"
HOST = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixLauncherScreen.kt"

backend = BACKEND.read_text()
assert 'const val PUBLIC_CHAT_TITLE = "Apex Public Chat"' in backend
assert "suspend fun openPublicChat(" in backend
assert "suspend fun loadPublicChatMessages(" in backend
assert "suspend fun sendPublicChatMessage(" in backend
# The room is joined, not just read: chat rows are per party_members membership.
join = backend.split("suspend fun openPublicChat(", 1)[1].split("suspend fun loadPublicChatMessages", 1)[0]
assert "party_members" in join and "on_conflict=party_id,member" in join
# Missing rooms are created once, by the first player online.
assert "createParty(" in join and "PUBLIC_CHAT_TITLE" in join
# Author names prefer the in-game name.
assert "mcUsername" in backend.split("fun publicChatAuthorName(", 1)[1][:400]

screen = SCREEN.read_text()
assert "fun PublicChatScreen(" in screen
assert "SocialBackend.openPublicChat()" in screen
assert "SocialBackend.sendPublicChatMessage(" in screen
assert "publicChatAuthorName(" in screen
assert "signedIn" in screen, "the screen must ask for a sign-in instead of failing"

rail = RAIL.read_text()
assert "PUBLIC_CHAT" in rail and "Public Chat" in rail
host = HOST.read_text()
assert "LauncherSection.PUBLIC_CHAT ->" in host

print("Public chat wiring passed")
