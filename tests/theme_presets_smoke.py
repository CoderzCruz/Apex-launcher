#!/usr/bin/env python3
"""Smoke checks for the built-in theme presets: Ocean and Sunset.

The colour picker already covers any single accent; these are whole palettes (accent plus a
related secondary) offered as one-tap entries in the theme list next to Crimson Noir and
Custom. The preference stores the enum name, so appending values keeps old saves working.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
PREFS = APP / "java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt"
THEME = APP / "java/net/kdt/pojavlaunch/aerix/theme/AerixTheme.kt"
SETTINGS = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt"
STRINGS = APP / "res/values/aerix_strings.xml"

# ── The enum appends, so existing saved names still resolve ──────────────
prefs = PREFS.read_text()
assert "enum class AerixColorTheme { AERIX, CUSTOM, OCEAN, SUNSET }" in prefs

# ── The scheme picks each preset by name; Custom still falls to the picker
theme = THEME.read_text()
assert "theme == AerixColorTheme.OCEAN -> presetScheme(oceanAccent, oceanSecondary)" in theme
assert "theme == AerixColorTheme.SUNSET -> presetScheme(sunsetAccent, sunsetSecondary)" in theme
assert "val oceanAccent = Color(0xFF38BDF8)" in theme
assert "val sunsetAccent = Color(0xFFF59E0B)" in theme
# Presets ride the same accent-role helper the custom theme uses.
assert "customAccentScheme(accent).copy(" in theme

# ── The settings list offers both entries beside Crimson Noir and Custom ─
settings = SETTINGS.read_text()
assert "AerixColorTheme.OCEAN.name to oceanLabel" in settings
assert "AerixColorTheme.SUNSET.name to sunsetLabel" in settings
assert "AerixColorTheme.AERIX.name to aerixLabel" in settings
assert "AerixColorTheme.CUSTOM.name to customLabel" in settings

# ── Labels exist for the picker ──────────────────────────────────────────
strings = STRINGS.read_text()
assert '<string name="aerix_settings_theme_ocean">Ocean</string>' in strings
assert '<string name="aerix_settings_theme_sunset">Sunset</string>' in strings
assert '<string name="aerix_settings_theme_aerix">Crimson Noir</string>' in strings

print("Theme presets: Ocean and Sunset enum, scheme, picker entries and labels passed")
