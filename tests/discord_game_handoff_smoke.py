#!/usr/bin/env python3
"""The card must follow the game even when the launcher process is reclaimed."""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
PRESENCE = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/discord/AerixDiscordPresence.kt"
GAME = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/game/GameActivity.java"

presence = PRESENCE.read_text()
game = GAME.read_text()

# The game publishes its own card instead of only hoping for a handoff.
assert "AerixDiscordPresence.attach(this)" in game, "game must bind Discord itself"
assert "AerixDiscordPresence.playing(" in game, "game must publish the in-game card"
# Java can only reach these through static accessors.
for member in ("fun attach(", "fun playing("):
    idx = presence.index(member)
    assert "@JvmStatic" in presence[max(0, idx - 60):idx], f"{member} needs @JvmStatic"

# A watchdog reconciles the shared state file with what is on the card.
assert "fun syncFromDisk(" in presence
assert "WATCHDOG_INTERVAL_MS" in presence and "stopWatchdog()" in presence
assert '"game" -> {' in presence

# Leaving the game keeps the card briefly, exactly like leaving the launcher.
stopped = presence.split("fun onGameStopped(", 1)[1].split("fun onProcessExiting", 1)[0]
assert "scheduleExitClear(app)" in stopped, "game exit must use the grace window"
assert "ACTION_DISCORD_SCHEDULE_CLEAR" not in stopped, "game exit must not clear instantly"

# Returning to the game republishes, and the elapsed timer is preserved.
resumed = presence.split("fun onGameResumed(", 1)[1].split("fun onGameStopped", 1)[0]
assert "playing(" in resumed
playing = presence.split("fun playing(", 1)[1].split("fun clear(", 1)[0]
assert 'readDiskState(it)' in playing and "CancelClear" not in playing  # sanity on shape
assert "cancelClearAlarm(app)" in playing and "cancelPendingExitClear()" in playing

print("Discord game handoff wiring passed")
