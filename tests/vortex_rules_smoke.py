#!/usr/bin/env python3
"""Smoke checks for the Vortex Launcher rename, the removed Aerix Client page, and Rules.

The product is now Vortex Launcher everywhere a player can see it (labels, strings, visible
code text); packages, class names, updater URLs and network user-agents stay untouched on
purpose. The rail's old Aerix Client entry became the Rules page, which shows the owner's
banner art, the big heading and the Discord invite.
"""
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
RES = APP / "res"
RAIL = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixNavigationRail.kt"
LAUNCHER = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixLauncherScreen.kt"
RULES = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/RulesScreen.kt"
TOOLS = APP / "java/net/kdt/pojavlaunch/Tools.java"

# ── The name is Vortex wherever players see it ────────────────────────────
gradle = (ROOT / "app_pojavlauncher/build.gradle").read_text()
assert gradle.count('resValue "string", "app_name", "Vortex Launcher') == 3, "3 variants"
assert gradle.count('resValue "string", "aerix_app_label", "Vortex Launcher') == 3
assert 'Aerix Launcher (Debug)' not in gradle and '"Aerix Launcher"' not in gradle
assert 'APP_NAME = "Vortex Launcher v1"' in TOOLS.read_text()
strings = (RES / "values/strings.xml").read_text()
assert 'name="app_short_name">Vortex Launcher v1<' in strings
assert 'name="lazy_service_default_title">Vortex Foreground Service<' in strings
aerix_strings = (RES / "values/aerix_strings.xml").read_text()
assert 'name="aerix_brand" translatable="false">Vortex<' in aerix_strings

# ── Vortex Client sections were removed on request (old id stays gone too) ─
rail = RAIL.read_text()
assert "AERIX_CLIENT" not in rail, "legacy client id stays gone"
for gone in ('"Vortex Client"', '"Cape Shop"', '"Fast Client"', "LauncherSection.CLIENT", "LauncherSection.CAPE_SHOP"):
    assert gone not in rail, gone + " removed from the rail"
assert '"Rules"' in rail and "LauncherSection.RULES" in rail
launcher = LAUNCHER.read_text()
assert "AerixClientScreen" not in launcher, "client page unhosted"
assert "LauncherSection.RULES -> RulesScreen(" in launcher

# ── Rules page: banner art, big heading, Discord invite ───────────────────
rules = RULES.read_text()
assert '"Rules Of Vortex Launcher"' in rules
assert "https://discord.gg/wpjAVkf2V" in rules
assert "VortexRules" in rules
assert "R.drawable.rules_banner" in rules
banner = RES / "drawable-nodpi/rules_banner.png"
assert banner.exists(), "owner's rules banner art shipped"
data = banner.read_bytes()
assert data[:8] == b"\x89PNG\r\n\x1a\n"
assert struct.unpack(">II", data[16:24]) == (2170, 725)

# ── No "(Debug)" in the label, panels are transparent glass, mark == golden V ──
assert "Vortex Launcher v1 (Debug)" not in gradle, "debug label has no suffix"
surface = (APP / "java/net/kdt/pojavlaunch/aerix/theme/AerixSurface.kt").read_text()
assert "val panel = Color(0x2D152536)" in surface, "translucent panel (owner ordered transparent)"
# All-black shell: the default wallpaper is dropped once, chip still available.

assert "val panel = Color(0xFF000000)" not in surface, "black panel reverted"
# The mark is the owner's raven artwork (2026-11): cut-out, colours intact, never tinted.
mark_file = RES / "drawable-nodpi/ic_aerix_mark.png"
assert mark_file.exists(), "in-app mark missing"
assert mark_file.read_bytes()[:8] == b"\x89PNG\r\n\x1a\n"
raven = RES / "drawable-nodpi/ic_vortex_raven.png"
assert raven.exists(), "raven artwork missing"
assert struct.unpack(">II", raven.read_bytes()[16:24]) == (512, 512)

# ── The raven ships as the app icon at every density ──────────────────────
for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        data = (RES / ("mipmap-" + density) / name).read_bytes()
        assert data[:8] == b"\x89PNG\r\n\x1a\n", name
        assert struct.unpack(">II", data[16:24]) == (size, size), f"{density}/{name}"
adaptive = (RES / "mipmap-anydpi-v26/ic_launcher.xml").read_text()
assert '@drawable/ic_launcher_foreground' in adaptive, "adaptive foreground = new logo"
assert "monochrome" not in adaptive, "old aerix monochrome dropped"
fg = (RES / "drawable-nodpi/ic_launcher_foreground.png").read_bytes()
assert fg[:8] == b"\x89PNG\r\n\x1a\n"
assert struct.unpack(">II", fg[16:24]) == (432, 432), "108dp foreground at xxxhdpi density"

# ── Upstream contract untouched (standing rule) ───────────────────────────
policy = (APP / "java/net/kdt/pojavlaunch/aerix/bridge/LauncherUpdatePolicy.kt").read_text()
assert 'const val OWNER_REPO = "CoderzCruz/Apex-launcher"' in policy, "only our repo is trusted"
assert 'const val OWNER_RELEASE_TAG = "debug-latest"' in policy, "only debug-latest is trusted"
assert "entitybrian69-bit/Aerix-launcher/releases" not in policy, "upstream download path banned"
updater = (APP / "java/net/kdt/pojavlaunch/aerix/bridge/LauncherUpdater.kt").read_text()
assert 'userAgent = "AerixLauncher/${BuildConfig.VERSION_NAME}"' in updater

prefs_src = (APP / "java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt").read_text()
assert "KEY_VORTEX_BLACK_SHELL" not in prefs_src, "black-shell flag removed"
assert 'KEY_VORTEX_TRANSPARENT_DONE = "vortexTransparentDone"' in prefs_src, "one-time transparent restore flag"
assert "selectedWallpaper = DEFAULT_WALLPAPER" in prefs_src, "wallpaper comes back after the black era"

print("Vortex rename, client removal and Rules page passed")

# ── Settings About tab removed; user-facing copy fully rebranded ────────
settings = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt").read_text()
assert "SettingTabItem(AerixNav.TAB_ABOUT" not in settings, "Settings About tab removed"
for rel, banned in [
    ("java/net/kdt/pojavlaunch/recording/RecordingOutput.kt", "Movies/Aerix"),
    ("java/net/kdt/pojavlaunch/recording/RecorderController.kt", "Movies/Aerix"),
    ("java/net/kdt/pojavlaunch/aerix/ui/screens/FirstRunNotificationPopup.kt", "Aerix Launcher notifies"),
    ("java/net/kdt/pojavlaunch/aerix/ui/screens/ContributionScreen.kt", "Aerix Launcher"),
    ("java/net/kdt/pojavlaunch/aerix/bridge/PojavInstanceBridge.kt", "Exported by Aerix Launcher"),
    ("java/net/kdt/pojavlaunch/aerix/ui/screens/SocialProfileScreen.kt", "Aerix Client capes"),
    ("java/net/kdt/pojavlaunch/aerix/ui/screens/LauncherUpdateDialog.kt", "A new Aerix is ready"),
    ("java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt", "maintainer of Aerix Launcher"),
]:
    body = (APP / rel).read_text()
    assert banned not in body, f"{rel} still contains {banned!r}"
