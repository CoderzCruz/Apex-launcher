"""Landscape overhaul: Home, Library and Discover share one mockup-driven layout.

Pins the pieces the owner approved in the generated mockups (design/ui-overhaul/):
a wide page header, three landscape columns on Home, the LAUNCH pill, the quick-action
grid, the two-column catalog grid in Library, and the roomier Discover grid.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCREENS = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/ui/screens"

shell = (SCREENS / "AerixWideShell.kt").read_text()
home = (SCREENS / "HomeScreen.kt").read_text()
library = (SCREENS / "LibraryScreen.kt").read_text()
discover = (SCREENS / "DiscoverScreen.kt").read_text()

# ── The shared shell exists and is the one place the mockup styling lives ──────────────
for piece in ("fun AerixWidePage(", "fun GlassCard(", "fun CrimsonPill(", "fun QuickActionCard(",
              "fun LandscapeColumns(", "fun isLandscapeLayout("):
    assert piece in shell, f"shell is missing {piece}"

# ── Home moved to the floating layout (see floating_home_smoke.py) ────────────────────
assert "FloatingHome(" in home, "Home renders the floating layout"
# The old dashboards stay in the file as the pieces they are made of, unused by Home.
for panel in ("TaskSummaryPanel(", "RecentWorldsPanel(", "SameVersionNudgeStrip("):
    assert panel in home, panel

# ── Library: landscape branch, catalog grid, detail pane ──────────────────────────────
assert "val wideLandscape = maxWidth > maxHeight && maxWidth >= 840.dp" in library
assert "if (wideLandscape)" in library, "landscape arrangement is chosen first"
assert "InstanceCatalogGrid(" in library, "catalog grid"
assert "LibraryWideFilterRow(" in library, "filter chips above the grid"
assert "columns = GridCells.Fixed(2)" in library, "two-column card grid like the mockup"
assert "gridItemsIndexed(" in library, "grid items use their own import alias"
# the old arrangements are still there for shorter screens
assert "if (sideBySide)" in library or "} else if (sideBySide)" in library, "short-landscape path kept"
assert "InstanceStrip(" in library and "InstanceListPane(" in library, "portrait paths kept"

# ── Discover: roomier search bar and a real 3-column grid in landscape ───────────────
assert "val wideBar = isLandscapeLayout()" in discover
assert "if (wideBar) 46.dp else 36.dp" in discover, "landscape search bar is taller"
assert "maxWidth > maxHeight && maxWidth >= 1180.dp -> 4" in discover
assert "maxWidth > maxHeight && maxWidth >= 760.dp -> 3" in discover

# ── The mockups themselves are kept next to the code they produced ───────────────────
design = ROOT / "design/ui-overhaul"
for name in ("landscape-home.png", "landscape-library.png", "landscape-discover.png"):
    asset = design / name
    assert asset.exists(), f"mockup {name} missing"
    assert asset.read_bytes()[:8] == b"\x89PNG\r\n\x1a\n", f"{name} is not a PNG"

print("Landscape overhaul: shell, Home, Library, Discover and mockups passed")
