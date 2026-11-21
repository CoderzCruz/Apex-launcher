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
# Home is the floating layout now; the compact dashboard remains in the file for reference.
assert "FloatingHome(selected = selected" in home_all, "home renders the floating layout"

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
assert "fun AerixMusicScreen" in music and "PlaybackService" in music, "music screen ships (vortex music host)"
engine = (ROOT / "vortexmusic/src/main/java/com/vortex/music/playback/PlaybackService.kt").read_text()
assert "MediaPlayer" in engine and "prepareAsync" in engine, "real playback, not a stub"
assert '"Music"' in rail and "aerix_ic_music_note" in rail, "rail music entry"
assert "LauncherSection.MUSIC -> AerixMusicScreen()" in launcher, "section routed"
assert (ROOT / "app_pojavlauncher/src/main/res/drawable/aerix_ic_music_note.xml").exists(), "music icon asset"
assert "MusicRepository" in music and "PlayerState" in music, "vortex music engine host"
assert "MiniPlayer" in music and "HomeScreen(" in music, "music tabs + mini player"
discover = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/DiscoverScreen.kt").read_text()
assert "Brush.horizontalGradient" in discover and "borderHighlight" in discover, "premium mod cards"
# The whole Vortex Client feature was removed (owner request): no client page, no cape shop,
# no Fast Client page and no client-only rail entries.
for screen in ("AerixClientScreen.kt", "AerixCapeShopScreen.kt", "FastClientScreen.kt"):
    assert not (APP / ("java/net/kdt/pojavlaunch/aerix/ui/screens/" + screen)).exists(), screen + " removed"
for section in ("LauncherSection.CLIENT", "LauncherSection.CAPE_SHOP", "LauncherSection.FAST_CLIENT"):
    assert section not in rail and section not in launcher, section + " unregistered"
for label in ('"Vortex Client"', '"Cape Shop"', '"Fast Client"'):
    assert label not in rail, label + " gone from the rail"
assert not (APP / "java/net/kdt/pojavlaunch/aerix/client").exists(), "client package removed"
other = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/OtherReleasesScreen.kt").read_text()
assert "FastClientReleaseCard" not in other, "fast client card removed"
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

cache = (APP / "java/net/kdt/pojavlaunch/game/renderer/RendererCache.java").read_text()
assert "COSMIC_RENDERX_RENDERER" in cache, "cosmic renderer offered"
game_renderer = (APP / "java/net/kdt/pojavlaunch/game/renderer/GameRenderer.java").read_text()
assert "CosmicRenderSpec" in game_renderer, "cosmic renderer wired"
settings_src = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt").read_text()
assert "Cosmic RenderX" in settings_src and "hasCosmic" in settings_src, "cosmic settings tile"
cosmic_spec = (APP / "java/net/kdt/pojavlaunch/game/renderer/impl/CosmicRenderSpec.java").read_text()
assert "ZinkRenderSpec" in cosmic_spec and "MESA_VK_WSI_PRESENT_MODE" not in cosmic_spec, "cosmic spec = pure zink"

panel = (APP / "assets/owner_panel.html").read_text()
assert "UPDATE_ROLLOUT" in panel and "debug-latest" in panel and "vortex_admin_pat" in panel, "owner panel page"
owner = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/OwnerPanelScreen.kt").read_text()
assert "owner_panel.html" in owner and "WebView" in owner, "owner panel screen"
assert "LauncherSection.OWNER_PANEL" in launcher, "owner panel hosted"
settings_panel = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt").read_text()
assert "Owner Panel" in settings_panel and "LauncherSection.OWNER_PANEL" in settings_panel, "owner panel entry"

# The owner chat banner lived inside the removed client HUD overlay / client mod.
assert not (APP / "java/net/kdt/pojavlaunch/aerix/client").exists(), "no client overlay host"
assert not (ROOT / "aerix_client_mod").exists(), "no client mod to read the owner broadcast"
assert "cancelRun" in panel, "panel cancel run"
assert "sendBroadcast" in panel and "Broadcast" in panel, "panel broadcast"

prefs_src = (APP / "java/net/kdt/pojavlaunch/prefs/LauncherPreferences.java").read_text()
assert "one-time move off the old GL defaults" not in prefs_src, "no forced cosmic migration"
assert "MOBILEGLUES_RENDERER" in prefs_src, "original GL default restored"
app_src = (APP / "java/net/kdt/pojavlaunch/PojavApplication.java").read_text()
assert "Auto-reset renderer: Cosmic RenderX -> GL4ES" in app_src, "crash auto-heal"
prefs_full = (APP / "java/net/kdt/pojavlaunch/prefs/LauncherPreferences.java").read_text()
assert "cosmic_confirmed" in prefs_full, "unconfirmed cosmic reset guard"
settings_full = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt").read_text()
assert settings_full.count("cosmic_confirmed") >= 3, "choice flags on every write path"
cosmic_src = (APP / "java/net/kdt/pojavlaunch/game/renderer/impl/CosmicRenderSpec.java").read_text()
assert "MESA_VK_WSI_PRESENT_MODE" not in cosmic_src, "mailbox removed (pure zink)"
instance_src = (APP / "java/net/kdt/pojavlaunch/instances/Instance.java").read_text()
assert "cosmic_confirmed" in instance_src and "COSMIC_RENDERX_RENDERER.equals(renderer)" in instance_src, "instance-level cosmic guard"
mirai_src = (APP / "java/net/kdt/pojavlaunch/aerix/components/MiraiSettingsComponents.kt").read_text()
assert "Vortex\u2019s Vulkan renderer" in mirai_src or "Vortex's Vulkan renderer" in mirai_src, "cosmic dropdown summary"

settings_check = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt").read_text()
assert "RendererStackOption(\"EMESIS\"" in settings_check, "emesis tile restored"
assert "Download Emesis" not in settings_check, "plugin download stays removed"
assert "Emesis and Cosmic RenderX are built in" in settings_check, "empty-state copy"
mirai_check = (APP / "java/net/kdt/pojavlaunch/aerix/components/MiraiSettingsComponents.kt").read_text()
assert "Renderers.EMESIS_RENDERER ->" in mirai_check, "emesis summary restored"

repo_src = (ROOT / "vortexmusic/src/main/java/com/vortex/music/data/MusicRepository.kt").read_text()
assert "addImported" in repo_src and "takePersistableUriPermission" in repo_src, "SAF import"
lib_src = (ROOT / "vortexmusic/src/main/java/com/vortex/music/ui/LibraryScreen.kt").read_text()
assert "OpenMultipleDocuments" in lib_src and "Aurora Funk" in lib_src, "import button + aurora playlist"
host_src = (ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/ui/screens/AerixMusicScreen.kt").read_text()
assert "musicPlaying" in host_src, "presence wired from music host"
pres_src = (ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/discord/AerixDiscordPresence.kt").read_text()
assert "Listening:" in pres_src and "Vortex Music" in pres_src, "music presence"
common_src = (ROOT / "vortexmusic/src/main/java/com/vortex/music/ui/Common.kt").read_text()
assert "embeddedPicture" in common_src, "embedded artwork fallback"

# .mrpack install/export still works through the normal launcher path (no client module needed).
mr_src = (ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/modloaders/modpacks/api/ModrinthApi.java").read_text()
assert "installMrpack" in mr_src and "modrinth.index.json" in mr_src, "mrpack installer intact"
