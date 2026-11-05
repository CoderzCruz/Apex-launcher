#!/usr/bin/env python3
"""Smoke checks for the UI refresh: option icon chips, section headers, tile affordances.

Every option row in the main Settings page can now carry a leading accent-tinted icon chip,
the groups are introduced by a visible header, and the home quick tiles show a tap chevron.
The shared row components keep their old signature shape (icon defaults to null), so screens
that were not wired yet still compile and render text-only rows.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
COMPONENTS = APP / "java/net/kdt/pojavlaunch/aerix/components/MiraiSettingsComponents.kt"
AERIXCOMP = APP / "java/net/kdt/pojavlaunch/aerix/components/AerixComponents.kt"
SETTINGS = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt"
STRINGS = APP / "res/values/aerix_strings.xml"

comp = COMPONENTS.read_text()

# ── The shared chip + icon threading in row components ───────────────────
assert "fun OptionIconChip(icon: Int" in comp
assert "accent.copy(alpha = 0.16f)" in comp
assert "icon: Int? = null" in comp
assert "icon = icon" in comp, "switch/list/slider must pass the icon through"
# Titles got the bigger style for the chip layout.
assert "style = MaterialTheme.typography.titleMedium" in comp

settings = SETTINGS.read_text()

# ── Section headers exist and are used by all five groups ────────────────
assert "fun SettingsGroupHeader(title: String)" in settings
for group in ("appearance", "background", "motion", "downloads", "aetherium"):
    assert f"SettingsGroupHeader(stringResource(R.string.aerix_settings_group_{group}))" in settings, group

# ── Icon wiring on the main Settings page (18 rows) ──────────────────────
wired = settings.count("icon = R.drawable.aerix_ic_")
assert wired >= 18, f"expected at least 18 wired rows, found {wired}"
for drawable in (
    "aerix_ic_style_outlined",       # colour theme
    "aerix_ic_styler_outlined",      # custom colour
    "aerix_ic_diwali",               # festival effects
    "aerix_ic_visibility_outlined",  # fullscreen
    "aerix_ic_wallpaper",            # background opacity
    "aerix_ic_touch_app_outlined",   # theme motion
    "aerix_ic_lightbulb",            # Apex Service key
    "aerix_ic_share_filled",         # share logs
    "aerix_ic_client_crystal",       # Aetherium
):
    assert f"icon = R.drawable.{drawable}" in settings, drawable

# ── The five header labels exist ─────────────────────────────────────────
strings = STRINGS.read_text()
for group in ("appearance", "background", "motion", "downloads", "aetherium"):
    assert f'name="aerix_settings_group_{group}"' in strings, group

# ── Home quick tiles: gradient chip + tap chevron ────────────────────────
tiles = AERIXCOMP.read_text()
assert "Brush.verticalGradient(listOf(accent.copy(alpha = 0.30f)" in tiles
assert "R.drawable.aerix_ic_chevron_right" in tiles
# The QuickTiles signature the Crimson Noir test locks must stay untouched.
home = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/HomeScreen.kt").read_text()
assert "private fun QuickTiles(accent: Color, actions: HomeActions" in home

print("UI refresh: option chips, group headers and tile affordances passed")
