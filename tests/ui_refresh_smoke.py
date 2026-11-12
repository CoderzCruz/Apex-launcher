#!/usr/bin/env python3
"""Smoke checks for option-row icons: every main Settings row carries an accent chip.

The chips ride the shared row components (icon defaults to null, so screens that were never
wired keep rendering text-only). This pass restores icons ONLY — the section headers, tile
chevron and gradient PLAY button of the earlier refresh stay removed, so the shell keeps
the released v2.0.0 look.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
COMPONENTS = APP / "java/net/kdt/pojavlaunch/aerix/components/MiraiSettingsComponents.kt"
AERIXCOMP = APP / "java/net/kdt/pojavlaunch/aerix/components/AerixComponents.kt"
SETTINGS = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt"

comp = COMPONENTS.read_text()
assert "fun OptionIconChip(icon: Int" in comp
assert comp.count("icon: Int? = null") >= 3, "switch/list/slider carry the icon param"
assert "icon = icon" in comp, "cards pass the icon through"

settings = SETTINGS.read_text()
assert "import net.kdt.pojavlaunch.aerix.components.OptionIconChip" in settings
assert settings.count("icon = R.drawable.aerix_ic_") >= 18, "all main rows wired"
assert settings.count("icon: Int? = null") >= 4, "private switch/slider/action/text cards"

# Released look guards: none of the rejected refresh pieces may come back.
assert "SettingsGroupHeader" not in settings, "no section headers"
assert "fun SettingsGroupHeader" not in settings
tiles = AERIXCOMP.read_text()
assert "aerix_ic_chevron_right" not in tiles, "no tile chevron"
assert "Brush.verticalGradient(listOf(accent" not in tiles, "no tile gradient chip"
home = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/HomeScreen.kt").read_text()
assert "accentGlow(accent, active = true" not in home, "no gradient PLAY"

print("Option icons: chips on all rows, released look guards passed")
