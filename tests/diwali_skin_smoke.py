#!/usr/bin/env python3
"""Smoke checks for the temporary Diwali skin (4-8 November 2026).

The skin is three real pieces: two launcher-icon aliases that DiwaliSkin flips straight
from the calendar, a gold scheme that takes over the shell while the window is open, and a
window that closes itself on 9 November with no update. Nothing here may persist a toggle.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
MANIFEST = APP / "AndroidManifest.xml"
SKIN = APP / "java/net/kdt/pojavlaunch/aerix/festivals/DiwaliSkin.kt"
THEME = APP / "java/net/kdt/pojavlaunch/aerix/theme/AerixTheme.kt"
MANAGER = APP / "java/net/kdt/pojavlaunch/aerix/theme/AerixThemeManager.kt"
APPJAVA = APP / "java/net/kdt/pojavlaunch/PojavApplication.java"
FESTIVAL = APP / "java/net/kdt/pojavlaunch/aerix/festivals/Festival.kt"

manifest = MANIFEST.read_text()

# ── The real activity keeps no launcher filter; the two aliases carry it ──
act_start = manifest.index('net.kdt.pojavlaunch.TestStorageActivity')
segment = manifest[act_start:manifest.index("ApexLauncherIcon", act_start)]
assert "<intent-filter" not in segment, "TestStorageActivity must not keep a launcher filter"
assert 'android:exported="true"' in segment

def alias_block(name):
    start = manifest.index(f'android:name="{name}"')
    return manifest[start:manifest.index("</activity-alias>", start)]

normal = alias_block("net.kdt.pojavlaunch.ApexLauncherIcon")
assert 'android:targetActivity="net.kdt.pojavlaunch.TestStorageActivity"' in normal
assert 'android:enabled="true"' in normal
assert 'android:icon="@mipmap/ic_launcher"' in normal
assert "android.intent.action.MAIN" in normal
assert "android.intent.category.LAUNCHER" in normal

diwali = alias_block("net.kdt.pojavlaunch.ApexLauncherIconDiwali")
assert 'android:targetActivity="net.kdt.pojavlaunch.TestStorageActivity"' in diwali
assert 'android:enabled="false"' in diwali
assert 'android:icon="@mipmap/ic_launcher_diwali"' in diwali
assert "android.intent.action.MAIN" in diwali
assert "android.intent.category.LAUNCHER" in diwali
# A roundIcon would double the icon assets for a shape the launcher masks anyway.
assert "roundIcon" not in normal and "roundIcon" not in diwali

# ── The receiver that re-checks the window after boot or an update ───────
recv_start = manifest.index("net.kdt.pojavlaunch.aerix.festivals.DiwaliIconReceiver")
recv = manifest[recv_start:manifest.index("</receiver>", recv_start)]
assert "<receiver" in manifest[recv_start - 200:recv_start]
assert 'android:exported="false"' in recv
assert "android.intent.action.BOOT_COMPLETED" in recv
assert "android.intent.action.MY_PACKAGE_REPLACED" in recv

# ── The icon assets exist at every density the legacy path can pick ──────
for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
    png = APP / f"res/mipmap-{density}/ic_launcher_diwali.png"
    assert png.exists() and png.stat().st_size > 1024, png
art = APP / "res/mipmap-xxxhdpi/ic_launcher_diwali_art.png"
assert art.exists() and art.stat().st_size > 1024, art
adaptive = (APP / "res/mipmap-anydpi-v26/ic_launcher_diwali.xml").read_text()
assert "@color/ic_launcher_diwali_background" in adaptive
assert "@mipmap/ic_launcher_diwali_art" in adaptive
assert "monochrome" in adaptive
color = (APP / "res/values/ic_launcher_background.xml").read_text()
assert "ic_launcher_diwali_background" in color

# ── DiwaliSkin: date-windowed, never persisted, never zero icons ─────────
skin = SKIN.read_text()
assert "val GOLD = Color(0xFFFFC24B)" in skin
assert "FIRST_DAY = 4" in skin and "DAY_AFTER_LAST = 9" in skin and "YEAR = 2026" in skin
assert "fun active(now: Long" in skin
assert "millisToNextBoundary" in skin
assert "@JvmStatic" in skin and "fun syncIcon(context: Context)" in skin
assert "setComponentEnabledSetting" in skin
assert "PackageManager.DONT_KILL_APP" in skin
assert "COMPONENT_ENABLED_STATE_ENABLED" in skin
assert "class DiwaliIconReceiver : BroadcastReceiver()" in skin
# No preference writes: the window is read from the clock only.
assert "AerixPreferences" not in skin and "SharedPreferences" not in skin
sync_body = skin[skin.index("fun syncIcon"):skin.index("private fun setAliasEnabled")]
assert sync_body.index("ICON_DIWALI, true") < sync_body.index("ICON_NORMAL, false")
assert sync_body.index("ICON_NORMAL, true") < sync_body.index("ICON_DIWALI, false")

# ── The app start and the boot/update receiver both run the flip ─────────
appjava = APPJAVA.read_text()
assert "DiwaliSkin.syncIcon(this)" in appjava

# ── The theme follows the same window and gives the accent back afterwards
theme = THEME.read_text()
assert "remember(theme, custom, diwali)" in theme
assert "diwaliScheme()" in theme
assert "DiwaliSkin.active" in theme
assert "millisToNextBoundary" in theme and "delay(" in theme
assert "val gold = DiwaliSkin.GOLD" in theme
manager = MANAGER.read_text()
assert "DiwaliSkin.active()" in manager
assert "DiwaliSkin.GOLD" in manager

# ── Fireworks stay on through Diwali day, then the festival ends too ─────
festival = FESTIVAL.read_text()
assert "checkDateRange(11, 4..8)" in festival

print("Diwali skin: aliases, icon assets, date window and gold theme passed")
