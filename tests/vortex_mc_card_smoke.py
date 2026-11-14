#!/usr/bin/env python3
"""Smoke checks for the "Vortex Mc ip" quick-join card on Home.

The card shows the owner's server art with "Sooth Hosting" beneath it and an animated
"Click to Play" button. A tap arms GameRunner's pending-server address and launches the
selected instance; GameRunner turns that into --quickPlayMultiplayer on 1.20+ (or
--server/--port on older versions) and clears it after the first launch.
"""
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
HOME = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/HomeScreen.kt"
RUNNER = APP / "java/net/kdt/pojavlaunch/utils/jre/GameRunner.java"

home = HOME.read_text()

# ── The card exists on the phone dashboard with the owner's copy ──────────
assert "item { VortexMcServerCard(selected, accent, actions) }" in home
assert '"Vortex Mc ip"' in home
assert '"Sooth Hosting"' in home
assert "in1.madflex.shop:19135" in home
assert '"Click to Play"' in home
card = home[home.index("private fun VortexMcServerCard"):home.index("private fun QuickTiles")]
assert "R.drawable.vortex_mc_card" in card

# ── Tap: arm the pending server, then launch (or create when nothing is selected)
assert 'GameRunner.setPendingServerAddress("in1.madflex.shop:19135")' in card
assert "actions.play()" in card and "actions.create()" in card
# The button animates: press dip + breathing glow.
assert ".apexPress {" in card
assert ".accentGlow(accent, active = true, enabled = ThemeMotion.rememberEnabled())" in card

# ── The art ships ─────────────────────────────────────────────────────────
art = APP / "res/drawable-nodpi/vortex_mc_card.png"
data = art.read_bytes()
assert data[:8] == b"\x89PNG\r\n\x1a\n"
w, _h = struct.unpack(">II", data[16:24])
assert w == 1280

# ── GameRunner keeps the auto-connect machinery ───────────────────────────
runner = RUNNER.read_text()
assert "setPendingServerAddress(String serverAddress)" in runner
assert 'launchArgs.add("--quickPlayMultiplayer")' in runner
assert 'launchArgs.add("--server")' in runner, "old-version fallback stays"
assert "appendServerAutoConnectArgs(launchArgs, versionInfo, versionId);" in runner

print("Vortex Mc ip card: art, copy, animated button and auto-join wiring passed")
