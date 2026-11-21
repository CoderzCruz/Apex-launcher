"""Home is the floating layout from the owner's reference screenshot.

Wallpaper edge to edge, every control a glass pill floating over it, the player model in the
middle, and one action dock at the bottom with Play + the section shortcuts.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCREENS = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/ui/screens"

floating = (SCREENS / "FloatingHome.kt").read_text()
home = (SCREENS / "HomeScreen.kt").read_text()

# Home renders the floating layout, not a card stack.
assert "FloatingHome(selected = selected, accent = accent, actions = actions)" in home, \
    "Home uses the floating layout"
assert "WideMockupHomeDashboard(" not in home.split("private fun WideMockupHomeDashboard")[0], \
    "the card dashboard is no longer the entry point"

# The pieces the reference shows, all real controls.
for piece in ("fun FloatingHome(", "fun GlassPlate(", "fun FloatingIconButton(",
              "fun FloatingTextPill(", "fun DockEntry(", "fun InstancePickerPlate("):
    assert piece in floating, f"missing {piece}"
for label in ('"Play"', '"Mods"', '"Packs"', '"Shaders"', '"Installed"', '"Skins"', '"Settings"'):
    assert label in floating, f"dock entry {label} missing"
assert 'Download Minecraft ' in floating, "download pill"
assert "Renderer: " in floating, "renderer pill"
assert "Touch layout" in floating and "Profile 1" in floating, "quick pills"

# The model in the middle is the real 3D skin preview, drag-to-rotate, paused when hidden.
assert "AerixSkinPreview3D(" in floating
assert "azimuth = (azimuth + drag.x * 0.6f).mod(360f)" in floating, "drag to rotate"

# Glass chrome: translucent fills and a bright rim, never a solid black bar.
assert "Color(0x590A1520)" in floating and "Color(0x7307131D)" in floating, "translucent plates"
assert "Color.White.copy(alpha = 0.22f)" in floating, "bright rim"

# Real actions, nothing faked.
for action in ("actions.play", "actions.create", "actions.mods", "actions.explore",
               "actions.library", "actions.accounts", "actions.settings", "actions.controls"):
    assert action in floating, f"{action} not wired"

print("Floating Home: glass chrome, model stage, dock and real actions passed")
