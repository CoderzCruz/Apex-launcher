#!/usr/bin/env python3
"""Smoke checks for the 2.0 launch kit.

The kit is three real pieces: a What's New changelog destination, a countdown banner on the
home dashboard that flips LIVE on launch day, and fireworks through the festival system for
20-22 November. Nothing here may promise a feature that does not ship.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
RAIL = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixNavigationRail.kt"
SCREEN = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixLauncherScreen.kt"
DIWALI = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/DiwaliSpecialScreen.kt"
WHATS = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/WhatsNewScreen.kt"
HOME = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/HomeScreen.kt"
FESTIVAL = APP / "java/net/kdt/pojavlaunch/aerix/festivals/Festival.kt"
EFFECTS = APP / "java/net/kdt/pojavlaunch/aerix/festivals/FestivalEffectType.kt"
STRINGS = APP / "res/values/aerix_strings.xml"

# ── What's New: a real page with honest feature copy ───────────────────────
whats = WHATS.read_text()
assert "fun WhatsNewScreen(" in whats
for phrase in (
    "Apex Service",
    "brand-new look",
    "Socials that feel alive",
    "Diwali Special",
    "Honest by design",
    "Under the hood",
):
    assert phrase in whats, phrase
# The attribution boundary stays on the changelog too.
assert "GPL-3.0 fork of PojavLauncher" in whats and "built by entitybrian" in whats
assert "version 2.0.0" in whats

# ── it is a real destination: enum, route, and the button that opens it ────
rail = RAIL.read_text()
assert "WHATS_NEW" in rail
screen = SCREEN.read_text()
assert "LauncherSection.WHATS_NEW ->" in screen
assert "WhatsNewScreen(" in screen
assert "onOpenWhatsNew = { navigate(LauncherSection.WHATS_NEW) }" in screen
diwali = DIWALI.read_text()
assert "onOpenWhatsNew: () -> Unit" in diwali
assert "What's new in 2.0" in diwali

# ── the countdown lives on ApexDiwali so Home can share it ─────────────────
assert "fun releaseMoment(" in diwali
assert "fun millisUntilRelease(" in diwali
assert "ApexDiwali.millisUntilRelease()" in diwali

# ── Home: the countdown banner, on both dashboards, LIVE on the day ────────
home = HOME.read_text()
assert "private fun ApexLaunchBanner(" in home
assert home.count("ApexLaunchBanner()") >= 2, "both dashboards must show the banner"
assert "ApexDiwali.millisUntilRelease()" in home
assert "LauncherSection.DIWALI" in home, "the banner opens the announcement page"
assert "is LIVE" in home, "the banner flips on launch day"
assert "days to go" in home

# ── launch day: real fireworks through the festival system ─────────────────
festival = FESTIVAL.read_text()
assert "APEX_LAUNCH(" in festival
assert "checkDateRange(11, 20..22)" in festival, "launch day plus two"
assert "isInternational = true" in festival, "every locale celebrates"
effects = EFFECTS.read_text()
assert "Festival.APEX_LAUNCH to listOf(FIREWORKS)" in effects
strings = STRINGS.read_text()
assert "aerix_festivals_apex_launch" in strings

print("Launch kit: What's New, home countdown and launch fireworks passed")
