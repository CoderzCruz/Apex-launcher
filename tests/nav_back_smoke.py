#!/usr/bin/env python3
"""Smoke checks: a fresh launch opens Home, and Back never leaves the app from a tab."""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
NAV = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/ui/AerixNav.kt"
SCREEN = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/ui/AerixLauncherScreen.kt"

nav = NAV.read_text()
screen = SCREEN.read_text()

# The shell must not reopen the tab it was closed on.
init = nav[nav.index("fun init()"):nav.index("fun isPage(")]
assert "section = LauncherSection.HOME" in init, "init() must start on Home"
assert "AerixPreferences.lastSection" not in init, "init() must not restore the last section"

# Back: page -> tab -> Home -> leave the app.
assert "BackHandler(enabled = true)" in screen, "back must always be handled"
assert "AerixNav.canGoBack" in screen and "AerixNav.back()" in screen
assert "AerixNav.section != LauncherSection.HOME" in screen, "back must return Home from a tab"
assert "activity.finish()" in screen, "back on Home must leave the launcher"

print("Launcher back/home behaviour passed")
