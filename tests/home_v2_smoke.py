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
for section in ("HomeHeaderRow(actions)", "PhoneSkinStage(accent, actions, selected, previewVisible",
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
assert '"LAUNCH"' not in play, "LAUNCH moved into the skin stage"
assert '"LAUNCH"' in home
assert '"START YOUR ADVENTURE"' in home, "sub-line moved with the button"
assert "Brush.verticalGradient(listOf(Color(0xFFB92736)" in home, "dark-red LAUNCH face"
assert "Brush.verticalGradient(listOf(accent" not in home, "old accent LAUNCH gradient gone"
assert "guideAnchor(AerixGuideState.PLAY)" in home, "tour anchor followed the button"
assert '"QUICK ACTIONS"' in home, "label moved with the cluster"
assert '"YOUR VERSION"' in play, "version label stays in the play block"
assert "accountTypeLabel(account.authType)" in play, "profile strip"
assert '"Offline Account"' in home and '"Ely.by Account"' in home
assert "showInstanceMenu" in play

# ── The five options exactly as asked ─────────────────────────────────────
# (WideHomeDashboard sits earlier in the file; the grid ends where QuickTiles begins.)
grid = home[home.index("private fun OptionsGrid"):home.index("private fun QuickTiles")]
for label in ("Modpacks", "Mods", "Settings", "Add Version"):
    assert f'"{label}"' in grid, label
assert "actions.mods" in grid and "AerixDownloadRequest.open(AssetType.MOD" in home
assert "actions.create" in grid and "actions.contribution" not in grid, "contribution removed"

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

# ── Menu card removed: the rail carries Execute / Controls / Downloads instead ──
assert "ReferenceMenuCard" not in home, "dashboard menu card is gone"
assert "item { ReferenceMenuCard" not in compact, "no menu item leads the dashboard"
rail = (APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixNavigationRail.kt").read_text()
for label in ('"Execute"', '"Control Layouts"', '"Downloads"'):
    assert label in rail, label
assert "onClick = onRunJar" in rail and "onClick = onControls" in rail
launcher = (APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixLauncherScreen.kt").read_text()
assert "onRunJar = {" in launcher and "onControls = {" in launcher, "rail actions wired"
assert "item { PhoneSkinStage(accent, actions, selected, previewVisible" in compact, "stage now leads the dashboard"
assert '"Customize"' in home and '"your skin"' in home and '"PREVIEW"' in home, "stage header"
assert '"Change Skin"' in home and "actions.accounts()" in home, "change-skin button"
assert '"QUICK ACTIONS"' in home, "reference section label"


# ── Music player, smooth swap, red-open icon, deeper transparency, bigger stage ──
music = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/AerixMusicScreen.kt").read_text()
assert "object AerixMusicPlayer" in music and "fun AerixMusicScreen" in music, "music screen ships"
assert "MediaPlayer" in music and "playAt" in music, "real playback, not a stub"
assert '"Music"' in rail and "aerix_ic_music_note" in rail, "rail music entry"
assert "LauncherSection.MUSIC -> AerixMusicScreen()" in launcher, "section routed"
assert (ROOT / "app_pojavlauncher/src/main/res/drawable/aerix_ic_music_note.xml").exists(), "music icon asset"
assert "Brush.horizontalGradient" in music, "music screen premium gradients"
assert "MusicTransportButton" in music, "circular transport controls"
assert "MusicEqBar" in music, "live equalizer bars"
discover = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/DiscoverScreen.kt").read_text()
assert "Brush.horizontalGradient" in discover and "borderHighlight" in discover, "premium mod cards"
client = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/AerixClientScreen.kt").read_text()
assert "VORTEX CLIENT" in client and "FPS Boost" in client and "ClientFeatureTile" in client, "client hero + features"
assert "ClientModuleSection" in client and "PvP preset applied" in client, "modules manager + presets"
assert "SUPPORTED" in client and "horizontalScroll" in client and "DOWNLOADS" in client, "supported-versions strip"
assert "ClientVersionTile" in client and "ClientActionDock" in client, "custom client UI"
assert "VORTEX MODS" in client and "ClientPackCard" in client and "Vortex Modpack" in client and "vulkanmod" in client, "vortex mods packs"
assert "CONFIG PROFILES" in client and "ClientConfigProfiles" in client and "applyConfigProfile" in client, "config profiles"
cape_shop = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/AerixCapeShopScreen.kt").read_text()
assert "CAPE SHOP" in cape_shop and "ClientCapeTile" in cape_shop and "equippedCapeId" in cape_shop, "cape shop screen"
assert '"Cape Shop"' in rail and "aerix_ic_style_outlined" in rail, "rail cape shop entry"
assert '"Vortex Client"' not in rail, "client icon removed from rail"
assert "AerixCapeShopScreen" in launcher, "cape shop hosted"
assert "selectedRed = Color(0xFFDC2626)" in rail, "opened section icon turns red"
assert ".height(300.dp)" in home, "bigger skin stage"
assert '"Welcome to Vortex Launcher"' in home, "welcome bubble"
assert "welcomeAlpha" in home and "welcomeBob" in home, "welcome bubble animates"
assert "cameraDistance = 52" in home, "closer camera, bigger skin"
assert "aerix_nav_contribution" not in rail, "contribution gone from rail"
prefs2 = (APP / "java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt").read_text()
assert "AerixTransitionType.SLICE_IN.name" in prefs2, "smooth default section swap"
surface2 = (APP / "java/net/kdt/pojavlaunch/aerix/theme/AerixSurface.kt").read_text()
assert "val panel = Color(0x2D152536)" in surface2, "refreshed transparent panels"
assert "val border = Color(0xB3DC2626)" in surface2, "light red outlines"
assert "val borderSoft = Color(0x73DC2626)" in surface2, "soft outlines match"
assert "val borderHighlight = Color(0xE0DC2626)" in surface2, "highlight outlines match"
assert "val accentDefault = Color(0xFFDC2626)" in surface2, "accent red like the screenshots"
assert "Color.White.copy(alpha = 0.30f)" not in home, "LAUNCH outline switched to red"

print("Home v2: stage, login row, middle PLAY, five options, header and press effects passed")
