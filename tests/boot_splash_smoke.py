#!/usr/bin/env python3
"""Smoke checks for the boot splash: real artwork, live progress, no baked-in 72%."""
import pathlib
import struct
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
VIEW = APP / "java/net/kdt/pojavlaunch/aerix/ui/ApexBootSplashView.kt"
LAYOUT = APP / "res/layout/activity_apex_boot.xml"
ACTIVITY = APP / "java/net/kdt/pojavlaunch/TestStorageActivity.java"
ASSETS = APP / "java/net/kdt/pojavlaunch/tasks/AsyncAssetManager.java"
ART = APP / "res/drawable-nodpi/apex_boot_splash.png"

# The artwork ships at full resolution, not stretched down.
data = ART.read_bytes()
assert data[:8] == b"\x89PNG\r\n\x1a\n", "splash art must be a PNG"
w, h = struct.unpack(">II", data[16:24])
assert w >= 1024 and h >= 700, f"splash art too small: {w}x{h}"
assert abs(w / h - 1.5) < 0.05, f"unexpected artwork aspect {w}x{h}"

view = VIEW.read_text()
# The bar drawn into the artwork is covered, and the real one is drawn on top.
assert "R.drawable.apex_boot_splash" in view
assert "drawnProgress" in view and "setProgress" in view
assert "BAR_LEFT = 0.250f" in view and "BAR_BOTTOM = 0.830f" in view, "track geometry must match the art"
# The percentage is computed, never a fixed number.
assert '${drawnProgress.roundToInt()}%' in view
assert 'drawText(' in view

layout = LAYOUT.read_text()
assert "net.kdt.pojavlaunch.aerix.ui.ApexBootSplashView" in layout

activity = ACTIVITY.read_text()
assert "setContentView(R.layout.activity_apex_boot)" in activity
assert "AsyncAssetManager.boot(" in activity
assert "mSplash.setProgress(percent)" in activity

assets = ASSETS.read_text()
assert "public interface BootCallback" in assets
assert "report(callback, 100)" in assets
assert "BOOT_TIMEOUT_MS" in assets, "splash must never trap the user"

print("Boot splash wiring passed")
