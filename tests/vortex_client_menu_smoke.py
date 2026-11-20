#!/usr/bin/env python3
"""Vortex Client lives INSIDE the launcher.

The client page is no longer a grid of decorative showcase tiles: it is a working CLIENT MENU
(every button has a real destination), plus a MULTIPLAYER server list that pings real servers
and joins them through the launcher's normal pending-server launch path.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app_pojavlauncher/src/main"
SCREEN = SRC / "java/net/kdt/pojavlaunch/aerix/ui/screens/AerixClientScreen.kt"
SERVERS = SRC / "java/net/kdt/pojavlaunch/aerix/bridge/AerixClientServers.kt"
PREFS = SRC / "java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt"

screen = SCREEN.read_text()
servers = SERVERS.read_text()
prefs = PREFS.read_text()

# ── The menu replaced the decorative tiles ────────────────────────────────
assert "private fun ClientMenuButton(" in screen, "red client menu button"
assert "private fun ClientServersSection(" in screen, "multiplayer section"
assert "ClientFeatureTile" not in screen, "showcase tiles are gone"

menu = screen[screen.index("// ── CLIENT MENU"):screen.index("// ── MULTIPLAYER")]
for label in ("SINGLEPLAYER", "MULTIPLAYER", "MODS", "COSMETICS",
              "PERFORMANCE", "MUSIC", "ACCOUNTS", "SETTINGS"):
    assert f'label = "{label}"' in menu, label
# No dead buttons: each one opens a real section or scrolls to a real block.
for target in ("LauncherSection.LIBRARY", "LauncherSection.CAPE_SHOP",
               "LauncherSection.MUSIC", "LauncherSection.ACCOUNTS",
               "LauncherSection.SETTINGS"):
    assert target in menu, target
for anchor in ("scrollToAnchor(serversTop)", "scrollToAnchor(modsTop)",
               "scrollToAnchor(profilesTop)"):
    assert anchor in menu, anchor
assert "PojavAccountBridge.current?.username" in menu, "welcome line uses the real account"
assert "Welcome back, " in menu

# ── The anchors are measured from the live layout ────────────────────────
assert screen.count("positionInWindow()") >= 4, "viewport + section anchors measured"
assert "animateScrollTo(target)" in screen

# ── Multiplayer: real status ping, saved list, one-tap join ──────────────
assert "data class Server(" in servers and "data class Ping(" in servers
assert "suspend fun ping(" in servers and "writeVarInt" in servers and "readVarInt" in servers
assert "InetSocketAddress(host, port)" in servers and "soTimeout = TIMEOUT_MS" in servers
assert "fun normalizeAddress(" in servers and "fun add(" in servers and "fun remove(" in servers
assert "AerixPreferences.saveClientServersJson" in servers
assert "AerixPreferences.vortexMcServer" in servers, "fresh install seeds Vortex MC"

assert 'const val KEY_CLIENT_SERVERS = "vortexClientServers"' in prefs
assert "var clientServersJson" in prefs
assert "fun saveClientServersJson(" in prefs

# Join is the launcher's real launch path, never a fake \"connected\" state.
assert "GameRunner.setPendingServerAddress(address)" in screen
assert "PojavLaunchBridge.play(activity)" in screen
assert "AerixClientServers.add(" in screen and "AerixClientServers.remove(" in screen
assert "checking…" in screen and "offline / not answering" in servers

# ── The separate products stay separate ──────────────────────────────────
assert "vortexclientapp" not in screen and "com.vortex.clientapp" not in screen
print("vortex client menu smoke: OK")
