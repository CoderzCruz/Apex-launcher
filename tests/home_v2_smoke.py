#!/usr/bin/env python3
"""Smoke checks for Home v2 on the phone: skin stage, login quick row, middle PLAY,
the five-option grid, header entries, and the shared press effect.

The v2 dashboard is the ONLY dashboard now: the old right-hand ActionMenu Steve card was
removed as the player's duplicate, so every width shows exactly one skin stage.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
HOME = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/HomeScreen.kt"
COMPONENTS = APP / "java/net/kdt/pojavlaunch/aerix/components/AerixComponents.kt"

home = HOME.read_text()

# ── The compact dashboard orders the v2 sections ──────────────────────────
compact = home[home.index("private fun CompactHomeDashboard"):home.index("private fun HomeHeaderRow")]
for section in ("HomeHeaderRow(actions)", "PhoneSkinStage(accent, actions)",
                "MiddlePlayBlock(selected, accent, actions)", "OptionsGrid(accent, actions)"):
    assert section in compact, section
# Replaced pieces are gone from the phone dashboard.
assert "QuickTiles(count" not in compact
assert "QuickToolsPanel" not in compact
# v2 is the only layout: no wide branch, no ActionMenu Steve card anywhere.
home_all = home
assert "WideHomeDashboard(selected" not in home_all, "wide branch removed"
assert "showActionMenu" not in home_all
assert home_all.count("CompactHomeDashboard(selected, recent") == 1

# ── Skin stage: drag + fling rotation, account pill, login quick row ──────
stage = home[home.index("private fun PhoneSkinStage"):home.index("private fun LoginPill")]
assert "detectHorizontalDragGestures" in stage, "Steve can be spun by drag"
assert "AerixSkinPreview3D(" in stage
assert '"+ Add Account"' in stage
for label, action in (("Microsoft", "actions.loginMicrosoft"),
                      ("Ely.by", "actions.loginEly"),
                      ("Offline", "actions.loginOffline")):
    assert f'LoginPill("{label}"' in stage and action in home, label

# ── Middle PLAY + selector, tour anchor intact ────────────────────────────
play = home[home.index("private fun MiddlePlayBlock"):home.index("private fun OptionsGrid")]
assert '"LAUNCH"' in play
assert '"START YOUR ADVENTURE"' in play, "reference sub-line merged in"
assert "Brush.verticalGradient(listOf(accent" in play, "gradient red face"
assert "guideAnchor(AerixGuideState.PLAY)" in play
assert '"YOUR VERSION"' in play, "reference version label"
assert "accountTypeLabel(account.authType)" in play, "profile strip"
assert '"Offline Account"' in home and '"Ely.by Account"' in home
assert "showInstanceMenu" in play

# ── The five options exactly as asked ─────────────────────────────────────
# (WideHomeDashboard sits earlier in the file; the grid ends where QuickTiles begins.)
grid = home[home.index("private fun OptionsGrid"):home.index("private fun QuickTiles")]
for label in ("Modpacks", "Mods", "Settings", "Contribution", "Add Version"):
    assert f'"{label}"' in grid, label
assert "actions.mods" in grid and "AerixDownloadRequest.open(AssetType.MOD" in home
assert "actions.contribution" in grid and "actions.create" in grid

# ── Header entries: AI, background, import ────────────────────────────────
header = home[home.index("private fun HomeHeaderRow"):home.index("private fun HeaderChip")]
assert '"AI"' in header and '"Background"' in header and '"Import skin"' in header
assert "actions.ai" in home and "navigate(LauncherSection.AI)" in home
assert "navigate(LauncherSection.WALLPAPERS)" in home

# ── Press effect exists and the shared tile uses it ───────────────────────
comp = COMPONENTS.read_text()
assert "fun Modifier.apexPress(" in comp
assert "Spring.DampingRatioMediumBouncy" in comp
assert ".apexPress(onClick)" in comp, "QuickActionTile presses with motion"
assert home.count(".apexPress(") >= 3, "home actions carry the press effect"

print("Home v2: stage, login row, middle PLAY, five options, header and press effects passed")
