#!/usr/bin/env python3
"""Smoke checks for the Pastel Light theme (variant 09).

The theme is a real light mode, not just an accent: selecting PASTEL flips
AerixSurface.lightMode, which switches every core token (canvas, panels, borders, text) to
its light value, and the Material scheme comes from a lightColorScheme base. White text on
token panels was swept to AerixSurface.textPrimary so it inverts with the theme; screens
with their own dark cards (chat, accounts, changelog) keep their whites on purpose.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
PREFS = APP / "java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt"
SURFACE = APP / "java/net/kdt/pojavlaunch/aerix/theme/AerixSurface.kt"
THEME = APP / "java/net/kdt/pojavlaunch/aerix/theme/AerixTheme.kt"
SETTINGS = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt"
STRINGS = APP / "res/values/aerix_strings.xml"

prefs = PREFS.read_text()
assert "enum class AerixColorTheme { AERIX, CUSTOM, OCEAN, SUNSET, PASTEL }" in prefs

surface = SURFACE.read_text()
assert "var lightMode by mutableStateOf(false)" in surface
assert "if (lightMode) Color(0xFFF3F4F8) else Color(0xFF08090B)" in surface, "canvas flips"
assert "if (lightMode) Color(0xFF14212B) else Color(0xFFF4F5F7)" in surface, "textPrimary flips"
assert "if (lightMode) Color(0xFFE7E9F0) else Color(0xFF2A2C31)" in surface, "glass tint flips"

theme = THEME.read_text()
assert "private val pastelAccent = Color(0xFF7C8CF8)" in theme
assert "private fun pastelScheme(): ColorScheme = lightColorScheme(" in theme
assert "theme == AerixColorTheme.PASTEL -> pastelScheme()" in theme
assert "AerixSurface.lightMode = theme == AerixColorTheme.PASTEL && !diwali" in theme

settings = SETTINGS.read_text()
assert "AerixColorTheme.PASTEL.name to pastelLabel" in settings
strings = STRINGS.read_text()
assert '<string name="aerix_settings_theme_pastel">Pastel Light</string>' in strings

# The white-text sweep: token-based screens no longer carry raw white text.
for rel, minimum in (
    ("ui/screens/SettingsScreen.kt", 10),
    ("ui/screens/LibraryScreen.kt", 3),
    ("components/MiraiSettingsComponents.kt", 10),
):
    src = (APP / "java/net/kdt/pojavlaunch/aerix" / rel).read_text()
    assert src.count("= AerixSurface.textPrimary") >= minimum, rel
    assert "= Color.White" not in src, f"raw white text left in {rel}"
# The launch banner keeps its white subtitle (dark gradient card in every mode).
home = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/HomeScreen.kt").read_text()
assert "color = Color.White.copy(alpha = 0.72f)" in home

print("Pastel Light: enum, light tokens, scheme, entry and text sweep passed")
