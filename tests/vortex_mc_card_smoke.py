#!/usr/bin/env python3
"""Smoke checks for the Vortex MC quick-join section — the rail entry below Rules.

The feature lives on its own rail page now: the owner's server art with "Sooth Hosting",
the saved address shown with an edit option (pencil dialog persisted in AerixPreferences),
and the animated "Click to Play" button. A tap arms GameRunner's pending server address and
launches the selected instance; GameRunner turns that into --quickPlayMultiplayer on 1.20+
(or --server/--port on older versions) and clears it after the first launch.
"""
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
SCREEN = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/VortexMcScreen.kt"
HOME = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/HomeScreen.kt"
PREFS = APP / "java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt"
RAIL = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixNavigationRail.kt"
LAUNCHER = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixLauncherScreen.kt"
RUNNER = APP / "java/net/kdt/pojavlaunch/utils/jre/GameRunner.java"

prefs = PREFS.read_text()
assert 'KEY_VORTEX_MC_SERVER = "vortexMcServerAddress"' in prefs
assert 'DEFAULT_VORTEX_MC_SERVER = "in1.madflex.shop:19135"' in prefs
assert "fun saveVortexMcServer(value: String)" in prefs
assert "p.getString(KEY_VORTEX_MC_SERVER, DEFAULT_VORTEX_MC_SERVER)" in prefs

# ── The rail entry sits directly below Rules ──────────────────────────────
rail = RAIL.read_text()
assert "VORTEX_MC," in rail
rules_at = rail.index('RailIconItem(R.drawable.aerix_ic_description_outlined, "Rules"')
mc_at = rail.index('RailIconItem(R.drawable.aerix_ic_sports_esports_outlined, "Vortex MC"')
assert rules_at < mc_at, "Vortex MC entry must follow Rules"
launcher = LAUNCHER.read_text()
assert "LauncherSection.VORTEX_MC -> VortexMcScreen(" in launcher

# ── The page: art, hosting line, editable IP, animated play ───────────────
screen = SCREEN.read_text()
assert '"Vortex Mc ip"' in screen
assert '"Sooth Hosting"' in screen
assert "R.drawable.vortex_mc_card" in screen
assert 'contentDescription = "Edit server IP"' in screen
assert "aerix_ic_edit_outlined" in screen
assert "AerixPreferences.saveVortexMcServer(draft)" in screen, "pencil dialog persists the IP"
assert "AerixPreferences.vortexMcServer" in screen
assert '"Click to Play"' in screen
assert "GameRunner.setPendingServerAddress(serverAddress)" in screen
assert "PojavLaunchBridge.play(activity)" in screen
assert "onNeedInstance()" in screen
assert ".apexPress(onClick = {" in screen
assert ".accentGlow(accent, active = true" in screen

# ── Home no longer carries the card — it moved to the rail section ────────
home = HOME.read_text()
assert "VortexMcServerCard" not in home
assert "vortex_mc_card" not in home

# ── The art ships ─────────────────────────────────────────────────────────
data = (APP / "res/drawable-nodpi/vortex_mc_card.png").read_bytes()
assert data[:8] == b"\x89PNG\r\n\x1a\n"
assert struct.unpack(">II", data[16:24])[0] == 1280

# ── GameRunner keeps the auto-connect machinery ───────────────────────────
runner = RUNNER.read_text()
assert "setPendingServerAddress(String serverAddress)" in runner
assert 'launchArgs.add("--quickPlayMultiplayer")' in runner
assert 'launchArgs.add("--server")' in runner, "old-version fallback stays"
assert "appendServerAutoConnectArgs(launchArgs, versionInfo, versionId);" in runner

print("Vortex MC section: rail entry below Rules, editable IP and auto-join wiring passed")
