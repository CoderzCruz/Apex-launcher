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
assert "detectDragGestures" in floating and "azimuth = (azimuth + dragAmount.x * 0.55f).mod(360f)" in floating, "drag to rotate"

# Glass chrome: translucent fills and a bright rim, never a solid black bar.
assert "Color(0x590A1520)" in floating and "Color(0x7307131D)" in floating, "translucent plates"
assert "Color.White.copy(alpha = 0.22f)" in floating, "bright rim"

# Real actions, nothing faked.
for action in ("actions.play", "actions.create", "actions.mods", "actions.explore",
               "actions.library", "actions.accounts", "actions.settings", "actions.controls"):
    assert action in floating, f"{action} not wired"

# ── No dangling symbols: every capitalised name in the CODE is imported or declared here ─
import re


def _code_only(text: str) -> str:
    """Drop comments and string literals so prose never looks like a symbol."""
    out, i, n = [], 0, len(text)
    while i < n:
        if text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
        elif text.startswith("/*", i):
            depth, i = 1, i + 2
            while i < n and depth:
                if text.startswith("/*", i):
                    depth, i = depth + 1, i + 2
                elif text.startswith("*/", i):
                    depth, i = depth - 1, i + 2
                else:
                    i += 1
        elif text[i] == '"':
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                elif text[i] == '"':
                    i += 1
                    break
                else:
                    i += 1
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


_code = _code_only(floating)
_imported = set(re.findall(r"^import .*\.(\w+)$", floating, re.M))
_declared = set(re.findall(r"^(?:private |internal )?(?:fun|class|data class|object|val) (\w+)", floating, re.M))
_imported |= _declared
_used = set(re.findall(r"(?<![.\w])([A-Z][A-Za-z0-9]+)\b", _code))
# Known-good names: types declared in other files of the app, stdlib, and Compose/Android types.
_known = {
    "AerixInstance", "AerixSkinPreview3D", "AerixImageWell", "AerixRadii", "AerixSpacing", "AerixSurface",
    "HomeActions", "R", "Modifier", "Dp", "BuildConfig", "LauncherPreferences", "Alignment", "Arrangement",
    "Box", "BoxWithConstraints", "Column", "Row", "Spacer", "Icon", "Text", "Color", "Brush", "RoundedCornerShape",
    "FontWeight", "Role", "Offset", "LocalConfiguration", "mutableFloatStateOf", "Composable",
    # Kotlin/stdlib builtins, never imported
    "Boolean", "Int", "String", "Unit", "Regex", "Float", "Double",
}
_dangling = {u for u in _used if u not in _imported and u not in _known}
assert not _dangling, f"symbols used but never imported: {sorted(_dangling)}"

print("Floating Home: glass chrome, model stage, dock and real actions passed")
