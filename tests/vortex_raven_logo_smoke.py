"""The raven mark from the owner's artwork is the launcher's logo (classic V optional).

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


def alpha_coverage(path: Path) -> float:
    """Share of pixels that are (mostly) transparent — the old glyphs were flat RGB."""
    data = path.read_bytes()
    colour_type = data[25]
    if colour_type not in (4, 6):
        return 0.0
    # Decode just enough with zlib + unfiltering would be overkill; sample the IDAT
    # entropy instead: a real cut-out has long transparent runs, a flat image does not.
    import zlib
    pos, idat = 8, b""
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        if kind == b"IDAT":
            idat += data[pos + 8:pos + 8 + length]
        pos += 12 + length
    raw = zlib.decompress(idat)
    width, height = struct.unpack(">II", data[16:24])
    channels = 4 if colour_type == 6 else 2
    stride = width * channels
    prev = bytearray(stride)
    clear = total = 0
    i = 0
    for _ in range(height):
        filt = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - channels] if x >= channels else 0
            b = prev[x]
            c = prev[x - channels] if x >= channels else 0
            if filt == 1:
                line[x] = (line[x] + a) & 0xFF
            elif filt == 2:
                line[x] = (line[x] + b) & 0xFF
            elif filt == 3:
                line[x] = (line[x] + (a + b) // 2) & 0xFF
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 0xFF
        alphas = line[channels - 1::channels]
        clear += sum(1 for v in alphas if v < 24)
        total += len(alphas)
        prev = line
    return clear / total


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
# Every surface that draws the launcher mark must show the raven, not an old glyph.
for rel in ("drawable-nodpi/ic_aerix_mark.png", "drawable-nodpi/aerix_logo.png"):
    f = RES / rel
    assert f.exists(), f"missing {rel}"
    assert alpha_coverage(f) > 0.30, f"{rel} is not the cut-out raven (no transparency)"

floating = (screens / "ui/screens/FloatingHome.kt").read_text()
assert "AerixPreferences.logoSkin" in floating, "Home does not follow the logo setting"
assert '"PLUS"' in floating, "the Home card does not say PLUS"
assert 'Text("V+"' not in floating, "the old V+ lettering is still in Home"

# Tint must be unspecified wherever the mark is drawn, or the raven turns into a flat glyph.
rail = (screens / "ui/AerixNavigationRail.kt").read_text()
assert "AerixPreferences.markSkin" in rail and "Color.Unspecified" in rail

# ── Classic mode: the owner can switch back to the old dashboard and the old mark ─────────
prefs = (ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/bridge/"
         "AerixPreferences.kt").read_text()
assert 'const val KEY_CLASSIC_HOME = "aerixClassicHome"' in prefs
assert 'const val KEY_CLASSIC_LOGO = "aerixClassicLogo"' in prefs
assert "val logoSkin: Int" in prefs and "R.drawable.aerix_logo_classic" in prefs
assert "val markSkin: Int" in prefs and "R.drawable.ic_aerix_mark_classic" in prefs
home = (screens / "ui/screens/HomeScreen.kt").read_text()
assert "AerixPreferences.classicHome" in home, "Home does not check the classic layout switch"
assert "ClassicHome(" in home and "FloatingHome(" in home, "both layouts must stay reachable"
assert "CompactHomeDashboard(" in home and "WideMockupHomeDashboard(" in home
settings = (screens / "ui/screens/SettingsScreen.kt").read_text()
assert "aerix_settings_classic_home" in settings and "aerix_settings_classic_logo" in settings
for rel, size in (("drawable/ic_aerix_mark_classic.png", 192),
                  ("drawable-nodpi/aerix_logo_classic.png", 512)):
    assert png_size(RES / rel)[0] == size, f"{rel} classic asset changed size"
for density, px in expected.items():
    legacy = RES / f"mipmap-{density}/ic_launcher_classic.png"
    assert png_size(legacy) == (px, px), f"{legacy} missing/wrong size"
# The home-screen entry follows the setting through two manifest aliases, never by disabling
# the app itself (that would kill the running process on some vendors).
manifest = (ROOT / "app_pojavlauncher/src/main/AndroidManifest.xml").read_text()
assert manifest.count("android.intent.category.LAUNCHER") >= 2
raven_alias = manifest.split('net.kdt.pojavlaunch.RavenLauncher', 1)[1].split("</activity-alias>", 1)[0]
classic_alias = manifest.split('net.kdt.pojavlaunch.ClassicLauncher', 1)[1].split("</activity-alias>", 1)[0]
assert 'android:enabled="true"' in raven_alias and "@mipmap/ic_launcher" == raven_alias.split('android:icon="', 1)[1].split('"', 1)[0]
assert 'android:enabled="false"' in classic_alias
assert "@mipmap/ic_launcher_classic" in classic_alias
for alias in (raven_alias, classic_alias):
    assert 'android:targetActivity="net.kdt.pojavlaunch.TestStorageActivity"' in alias
assert "setComponentEnabledSetting" in prefs and "DONT_KILL_APP" in prefs

print("Raven logo: cut-out, icon set, adaptive foreground, Home and rail passed")
