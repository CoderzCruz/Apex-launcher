#!/usr/bin/env python3
"""Smoke checks for the UI polish pass: one section-header language, hero CTA depth.

Section headers (home and settings) now share the accent-dot look, settings headers use the
spaced uppercase style with rhythm above their group, the home PLAY button gets a gradient
face plus the theme-motion glow, and summary/radio greys unify to the surface textSecondary.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
HOME = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/HomeScreen.kt"
SETTINGS = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt"
COMPONENTS = APP / "java/net/kdt/pojavlaunch/aerix/components/MiraiSettingsComponents.kt"

home = HOME.read_text()
settings = SETTINGS.read_text()
comp = COMPONENTS.read_text()

# ── Settings group header: uppercase, letter-spaced, rhythm above ────────
header = settings[settings.index("private fun SettingsGroupHeader"):settings.index("private fun MiraiSwitchSettingsCard")]
assert "title.uppercase()" in header
assert "letterSpacing = 1.sp" in header
assert "padding(top = AerixSpacing.md)" in header, "header keeps extra room above its group"
assert "size(7.dp)" in header and "currentAccent()" in header

# ── Home section label carries the same accent dot ───────────────────────
label = home[home.index("private fun HomeSectionLabel"):home.index("private fun ActiveWorldPanel")]
assert "size(7.dp)" in label and "currentAccent()" in label
assert "title.uppercase()" in label

# ── Hero PLAY button: gradient face + motion glow ────────────────────────
play = home[home.index("Vibrant Pill PLAY"):home.index("private fun WideHomeDashboard")]
assert "Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.84f)))" in play
assert "accentGlow(accent, active = true" in play
assert "ThemeMotion.rememberEnabled()" in play
assert "color = Color.Transparent" in play
assert "import net.kdt.pojavlaunch.aerix.theme.accentGlow" in home
assert "import net.kdt.pojavlaunch.aerix.theme.ThemeMotion" in home

# ── Shared greys now use the surface textSecondary ───────────────────────
texts = comp[comp.index("private fun TitleSummaryTexts"):comp.index("fun MiraiSwitchSettingsCard")]
assert "color = AerixSurface.textSecondary" in texts
assert "0.70f" not in texts, "summary grey unified to textSecondary"
assert "unselectedColor = AerixSurface.textSecondary" in comp

print("UI polish: section headers, PLAY gradient and unified greys passed")
