"""The raven mark from the owner's artwork is the launcher's logo.

Pins the cut-out assets, the adaptive-icon wiring and the places that show the mark, so a
future edit cannot quietly bring back the cyan background or a lettering "V+" placeholder.
"""
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app_pojavlauncher/src/main/res"


def png_size(path: Path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"{path} is not a PNG"
    return struct.unpack(">II", data[16:24])


# ── The mark: transparent cut-out, square, big enough for every slot ─────────────────
mark = RES / "drawable-nodpi/ic_vortex_raven.png"
assert mark.exists(), "raven mark missing"
assert png_size(mark) == (512, 512), f"unexpected mark size {png_size(mark)}"
assert (RES / "drawable-nodpi/ic_vortex_raven_large.png").exists(), "large artwork missing"
assert (RES / "drawable-nodpi/ic_vortex_icon_512.png").exists(), "512 store icon missing"
# A cut-out must actually be transparent somewhere, or the cyan tile came back.
raw = mark.read_bytes()
assert b"tRNS" in raw or raw[25] == 6, "mark must carry an alpha channel"

# ── Legacy + adaptive icons for every density ────────────────────────────────────────
expected = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for density, px in expected.items():
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        icon = RES / f"mipmap-{density}/{name}"
        assert icon.exists(), f"{icon} missing"
        assert png_size(icon) == (px, px), f"{icon} is {png_size(icon)}, want {px}"
    fg = RES / f"mipmap-{density}/ic_launcher_foreground.png"
    assert png_size(fg) == (px * 108 // 48, px * 108 // 48), f"{fg} has the wrong adaptive size"

# ── Wiring: rail/About/etc. use the replaced mark; Home shows the raven and "PLUS" ───
screens = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix"
assert (RES / "drawable/ic_aerix_mark.png").exists(), "the in-app mark must be the raven"
assert png_size(RES / "drawable/ic_aerix_mark.png")[0] >= 192, "in-app mark is too small"

floating = (screens / "ui/screens/FloatingHome.kt").read_text()
assert "R.drawable.ic_vortex_raven" in floating, "Home does not show the raven"
assert '"PLUS"' in floating, "the Home card does not say PLUS"
assert 'Text("V+"' not in floating, "the old V+ lettering is still in Home"

# Tint must be unspecified wherever the mark is drawn, or the raven turns into a flat glyph.
rail = (screens / "ui/AerixNavigationRail.kt").read_text()
assert "R.drawable.ic_aerix_mark" in rail and "Color.Unspecified" in rail

print("Raven logo: cut-out, icon set, adaptive foreground, Home and rail passed")
