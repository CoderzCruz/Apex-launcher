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
assert 'APP_NAME = "Vortex Launcher"' in TOOLS.read_text()
strings = (RES / "values/strings.xml").read_text()
assert 'name="app_short_name">Vortex Launcher<' in strings
assert 'name="lazy_service_default_title">Vortex Foreground Service<' in strings
aerix_strings = (RES / "values/aerix_strings.xml").read_text()
assert 'name="aerix_brand" translatable="false">Vortex<' in aerix_strings

# ── The Aerix Client page is gone from navigation ─────────────────────────
rail = RAIL.read_text()
assert "AERIX_CLIENT" not in rail, "no client entry in the rail"
assert '"Rules"' in rail and "LauncherSection.RULES" in rail
launcher = LAUNCHER.read_text()
assert "AerixClientScreen" not in launcher, "client page not hosted"
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

# ── Upstream contract untouched (standing rule) ───────────────────────────
policy = (APP / "java/net/kdt/pojavlaunch/aerix/bridge/LauncherUpdatePolicy.kt").read_text()
assert '"Aerix-Launcher-$plain"' in policy, "updater asset names stay upstream"
updater = (APP / "java/net/kdt/pojavlaunch/aerix/bridge/LauncherUpdater.kt").read_text()
assert 'userAgent = "AerixLauncher/${BuildConfig.VERSION_NAME}"' in updater

print("Vortex rename, client removal and Rules page passed")
