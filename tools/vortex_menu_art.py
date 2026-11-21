#!/usr/bin/env python3
"""VORTEX CLIENT main-menu art generator.

Renders every texture the Vortex-Client-Pack ships for the in-game title screen, so the
menu art is authored once and verified visually instead of being guessed at runtime on a
phone:

  assets/vortex_menu/menu_red_forest.webp     wide red-forest scene (panorama + menu bg)
  assets/vortex_menu/logo_modern.png          256x64  (1.19.4+ LogoRenderer sheet)
  assets/vortex_menu/logo_legacy.png          256x256 (1.16.5-1.19.2 split UV sheet)
  assets/vortex_menu/button.png               200x20  vanilla widget/button sprite
  assets/vortex_menu/button_highlighted.png   200x20  hover state
  assets/vortex_menu/button_disabled.png      200x20  disabled state
  assets/vortex_menu/widgets_legacy.png       256x256 legacy gui/widgets.png sheet
  assets/vortex_menu/panorama_overlay.png     16x128  readability scrim over the panorama

Usage:  python3 tools/vortex_menu_art.py [source_scene.png]
The source scene defaults to design/vortex-red-menu/forest_raw.png.
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app_pojavlauncher/src/main/assets/vortex_menu"
DEFAULT_SRC = ROOT / "design/vortex-red-menu/forest_source.jpg"
if not DEFAULT_SRC.is_file():
    DEFAULT_SRC = ROOT / "design/vortex-red-menu/forest_raw.png"

# Palette (spec: dark red #A90016, black #090909, accent #FF2525, white text)
BLACK = (9, 9, 9)
DEEP = (74, 2, 9)
RED = (169, 0, 22)
RED_HOVER = (226, 31, 61)
ACCENT = (255, 37, 37)
WHITE = (255, 255, 255)

# 5x7 pixel font — the "Minecraft menu" lettering used for VORTEX / CLIENT EDITION.
GLYPHS = {
    "A": ("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
    "B": ("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
    "C": ("01110", "10001", "10000", "10000", "10000", "10001", "01110"),
    "D": ("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
    "E": ("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
    "F": ("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
    "G": ("01110", "10001", "10000", "10111", "10001", "10001", "01111"),
    "H": ("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
    "I": ("11111", "00100", "00100", "00100", "00100", "00100", "11111"),
    "J": ("00111", "00010", "00010", "00010", "00010", "10010", "01100"),
    "K": ("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
    "L": ("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
    "M": ("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
    "N": ("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
    "O": ("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    "P": ("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
    "Q": ("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
    "R": ("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
    "S": ("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
    "T": ("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
    "U": ("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
    "V": ("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
    "W": ("10001", "10001", "10001", "10101", "10101", "11011", "10001"),
    "X": ("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
    "Y": ("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
    "Z": ("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
    "0": ("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    "1": ("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    "2": ("01110", "10001", "00001", "00110", "01000", "10000", "11111"),
    "3": ("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    "4": ("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    "5": ("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    "6": ("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    "7": ("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    "8": ("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    "9": ("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    " ": ("00000", "00000", "00000", "00000", "00000", "00000", "00000"),
    ".": ("00000", "00000", "00000", "00000", "00000", "01100", "01100"),
    "-": ("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
    "!": ("00100", "00100", "00100", "00100", "00100", "00000", "00100"),
    "?": ("01110", "10001", "00001", "00110", "00100", "00000", "00100"),
}


def text_width(text: str, px: int, tracking: int = 1) -> int:
    return len(text) * (5 * px + tracking * px) - tracking * px


def draw_pixel_text(
    img: Image.Image,
    text: str,
    x: int,
    y: int,
    px: int,
    fill,
    tracking: int = 1,
    shadow: int = 1,
    shadow_fill=(0, 0, 0, 210),
) -> int:
    """Blits 5x7 pixel text; fill may be a solid colour or a vertical (top, bottom) pair."""
    draw = ImageDraw.Draw(img)
    gradient = fill if (isinstance(fill, tuple) and len(fill) == 2 and isinstance(fill[0], tuple)) else None
    solid = None if gradient else (tuple(fill) if len(fill) == 4 else tuple(fill) + (255,))
    height = 7 * px
    cursor = x
    for ch in text.upper():
        rows = GLYPHS.get(ch, GLYPHS["?"])
        for ry, row in enumerate(rows):
            for rx, bit in enumerate(row):
                if bit != "1":
                    continue
                if shadow:
                    draw.rectangle(
                        [cursor + rx * px + shadow, y + ry * px + shadow,
                         cursor + rx * px + px - 1 + shadow, y + ry * px + px - 1 + shadow],
                        fill=shadow_fill,
                    )
        for ry, row in enumerate(rows):
            for rx, bit in enumerate(row):
                if bit != "1":
                    continue
                if gradient is not None:
                    t = ry / 6.0
                    colour = tuple(
                        int(gradient[0][i] + (gradient[1][i] - gradient[0][i]) * t) for i in range(3)
                    ) + (255,)
                else:
                    colour = solid
                draw.rectangle(
                    [cursor + rx * px, y + ry * px, cursor + rx * px + px - 1, y + ry * px + px - 1],
                    fill=colour,
                )
        cursor += 5 * px + tracking * px
    return height


# ─────────────────────────────────────────────────────────────────────────────
# Background scene
# ─────────────────────────────────────────────────────────────────────────────
def make_scene(src: Path) -> Image.Image:
    scene = Image.open(src).convert("RGB")
    # Cover-crop to 16:9 so the panorama projection never stretches.
    target = 16 / 9
    w, h = scene.size
    if w / h < target:
        new_h = int(w / target)
        top = max(0, (h - new_h) // 2)
        scene = scene.crop((0, top, w, top + new_h))
    else:
        new_w = int(h * target)
        left = max(0, (w - new_w) // 2)
        scene = scene.crop((left, 0, left + new_w, h))
    scene = scene.resize((1920, 1080), Image.LANCZOS)

    # Atmospheric grade: deepen the blacks, protect a warm crimson mid, cinematic vignette.
    px = scene.load()
    for y in range(1080):
        for x in range(1920):
            r, g, b = px[x, y]
            lum = (r * 299 + g * 587 + b * 114) // 1000
            r = int(r * 1.06)
            g = int(g * 0.86)
            b = int(b * 0.94)
            if lum < 26:
                r, g, b = int(r * 0.55), int(g * 0.5), int(b * 0.55)
            px[x, y] = (min(255, max(0, r)), min(255, max(0, g)), min(255, max(0, b)))

    vignette = Image.new("L", scene.size, 0)
    vd = ImageDraw.Draw(vignette)
    for i in range(220):
        alpha = int(150 * (i / 220.0) ** 1.8)
        vd.rectangle([i, i, 1919 - i, 1079 - i], outline=255 - alpha)
    dark = Image.new("RGB", scene.size, (4, 3, 5))
    scene = Image.composite(scene, dark, vignette.point(lambda v: 255 - int((255 - v) * 0.55)))

    # Soft dark scrim behind the button column so white labels always stay readable.
    scrim = Image.new("RGBA", scene.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(scrim)
    sd.rounded_rectangle([760, 300, 1160, 830], radius=60, fill=(6, 2, 4, 96))
    scrim = scrim.filter(ImageFilter.GaussianBlur(70))
    scene = Image.alpha_composite(scene.convert("RGBA"), scrim)

    # Fade the top and bottom edges so the panorama seam never shows.
    edge = Image.new("RGBA", scene.size, (0, 0, 0, 0))
    ed = ImageDraw.Draw(edge)
    for i in range(140):
        a = int(90 * (1 - i / 140.0))
        ed.rectangle([0, i, 1919, i], fill=(3, 1, 2, a))
        ed.rectangle([0, 1079 - i, 1919, 1079 - i], fill=(3, 1, 2, a))
    scene = Image.alpha_composite(scene, edge)
    return scene.convert("RGB")


# ─────────────────────────────────────────────────────────────────────────────
# Logo
# ─────────────────────────────────────────────────────────────────────────────
def render_banner(logical_width: int, scale: int = 4) -> Image.Image:
    """256x44 (or 274x44) logical logo box at `scale`x, glowing crimson VORTEX wordmark."""
    w, h = logical_width * scale, 44 * scale
    banner = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    word_px = 4 * scale
    word = "VORTEX"
    word_w = text_width(word, word_px, tracking=1)
    sub_px = 2 * scale
    sub = "CLIENT EDITION"
    sub_w = text_width(sub, sub_px, tracking=0)
    total_h = 7 * word_px + 2 * scale + 7 * sub_px
    word_y = (h - total_h) // 2
    word_x = (w - word_w) // 2
    sub_y = word_y + 7 * word_px + 2 * scale
    sub_x = (w - sub_w) // 2

    # Glow: the word blurred twice in accent red, then a tighter hot core.
    for radius, alpha in ((5 * scale, 190), (2 * scale, 230)):
        glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        draw_pixel_text(glow, word, word_x, word_y, word_px, (255, 37, 37, alpha), shadow=0)
        banner = Image.alpha_composite(banner, glow.filter(ImageFilter.GaussianBlur(radius)))

    draw_pixel_text(
        banner, word, word_x, word_y, word_px,
        ((255, 96, 96), (169, 0, 22)), shadow=1, shadow_fill=(20, 0, 4, 235),
    )
    # Metallic top highlight on the upper two pixel rows of every glyph column.
    hi = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw_pixel_text(hi, word, word_x, word_y - scale // 2, word_px, (255, 168, 168, 60), shadow=0)
    banner = Image.alpha_composite(banner, hi)

    draw_pixel_text(banner, sub, sub_x, sub_y, sub_px, (255, 255, 255), shadow=1)
    return banner


def make_logo_modern() -> Image.Image:
    """1.19.4+: 256x64 sheet, LogoRenderer blits UV (0,0,256,44)."""
    scale = 4
    sheet = Image.new("RGBA", (256 * scale, 64 * scale), (0, 0, 0, 0))
    sheet.alpha_composite(render_banner(256, scale), (0, 0))
    return sheet


def make_logo_legacy() -> Image.Image:
    """1.16.5-1.19.2: 256x256 sheet; left half at (0,0,155,44), right half at (0,45,155,44)."""
    scale = 4
    sheet = Image.new("RGBA", (256 * scale, 256 * scale), (0, 0, 0, 0))
    banner = render_banner(274, scale)
    left_w, right_w, row_h = 155 * scale, 119 * scale, 44 * scale
    sheet.alpha_composite(banner.crop((0, 0, left_w, row_h)), (0, 0))
    sheet.alpha_composite(banner.crop((left_w, 0, left_w + right_w, row_h)), (0, 45 * scale))
    return sheet


# ─────────────────────────────────────────────────────────────────────────────
# Buttons / widgets
# ─────────────────────────────────────────────────────────────────────────────
#: Glass alpha for the widget faces: the red-forest panorama must stay visible through every
#: button (reference menu look), while white labels keep their contrast.
BUTTON_FILL_ALPHA = 132
BUTTON_BORDER_ALPHA = 214


def make_button(state: str) -> Image.Image:
    """200x20 semi-transparent button sprite at its NATIVE size.

    Minecraft's nine-slice widgets expect exactly this size (border 3); 4x art rendered as
    magenta missing-texture bars. The face is intentionally translucent so the panorama shows
    through the button, like the reference red-forest menu.
    """
    w, h = 200, 20
    if state == "disabled":
        top, bottom, border = (52, 52, 52), (36, 36, 36), (26, 26, 26)
        fill_alpha, inner = 96, None
    elif state == "highlighted":
        top, bottom, border = (232, 46, 70), (176, 8, 26), (255, 82, 100)
        fill_alpha, inner = 168, (255, 150, 158)
    else:
        top, bottom, border = (150, 10, 24), (104, 2, 12), (58, 0, 8)
        fill_alpha, inner = BUTTON_FILL_ALPHA, (206, 34, 48)

    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for row in range(h):
        t = row / (h - 1)
        colour = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (fill_alpha,)
        draw.line([(0, row), (w - 1, row)], fill=colour)
    # 3-texel border on every side (matches the vanilla nine_slice `border`). The rim stays more
    # opaque than the face so the button keeps a crisp outline over the busy panorama.
    draw.rectangle([0, 0, w - 1, h - 1], outline=border + (BUTTON_BORDER_ALPHA,), width=3)
    if inner is not None:
        draw.line([(3, 3), (w - 4, 3)], fill=inner + (120,))
        draw.line([(3, h - 4), (w - 4, h - 4)], fill=(0, 0, 0, 130))
    return img


def make_tab(state: str) -> Image.Image:
    """130x24 tab sprite (vanilla border: left 2, top 2, right 2, bottom 0)."""
    w, h = 130, 24
    if state == "selected":
        top, bottom, border = (150, 10, 24), (104, 2, 12), (58, 0, 8)
        edge = (232, 46, 70)
        fill_alpha = BUTTON_FILL_ALPHA
    elif state == "highlighted":
        top, bottom, border = (196, 24, 42), (132, 4, 16), (255, 82, 100)
        edge = (255, 120, 130)
        fill_alpha = 168
    else:
        top, bottom, border = (64, 60, 66), (38, 34, 40), (26, 24, 28)
        edge = (92, 86, 94)
        fill_alpha = 110
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for row in range(h):
        t = row / (h - 1)
        colour = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (fill_alpha,)
        draw.line([(0, row), (w - 1, row)], fill=colour)
    draw.line([(0, 0), (w - 1, 0)], fill=edge + (BUTTON_BORDER_ALPHA,), width=2)
    draw.line([(0, 0), (0, h - 1)], fill=edge + (BUTTON_BORDER_ALPHA,), width=2)
    draw.line([(w - 2, 0), (w - 2, h - 1)], fill=edge + (BUTTON_BORDER_ALPHA,), width=2)
    draw.line([(0, h - 1), (w - 1, h - 1)], fill=border + (BUTTON_BORDER_ALPHA,), width=2)
    return img


def make_widgets_legacy() -> Image.Image:
    """1.16.5-1.20.1 gui/widgets.png (256x256) at NATIVE scale.

    Vanilla blits these rows by exact UV, so every piece sits at its vanilla offset:
      (0,0) 182x22 hotbar, (0,22) 24x24 selection, (0,46) 200x20 button,
      (0,66) highlighted, (0,86) disabled, (0,106) slider, (0,126) slider highlighted.
    Shipping this sheet at 4x put the art nowhere near those UVs, which is why the legacy
    title screen drew magenta buttons.
    """
    sheet = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(sheet)

    # Hotbar: dark crimson glass with a bright hairline.
    draw.rectangle([0, 0, 181, 21], fill=(22, 8, 12, BUTTON_FILL_ALPHA))
    draw.rectangle([0, 0, 181, 21], outline=(196, 30, 48, BUTTON_BORDER_ALPHA), width=1)
    # Selection highlight (24x24) with a hot inner glow.
    draw.rectangle([0, 22, 23, 45], outline=(255, 92, 102, BUTTON_BORDER_ALPHA), width=1)
    draw.rectangle([2, 24, 21, 43], fill=(255, 37, 37, 52))

    def blit(state: str, y: int) -> None:
        sheet.alpha_composite(make_button(state), (0, y))

    blit("disabled", 46)
    blit("normal", 66)
    blit("highlighted", 86)
    blit("normal", 106)
    blit("highlighted", 126)
    blit("disabled", 146)
    return sheet


def make_overlay() -> Image.Image:
    """16x128 vanilla panorama overlay replacement: soft dark scrim, top + bottom heavier."""
    img = Image.new("RGBA", (16, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for y in range(128):
        t = abs(y - 64) / 64.0
        alpha = int(24 + 92 * (t ** 1.5))
        draw.line([(0, y), (15, y)], fill=(4, 2, 4, alpha))
    return img


def make_preview(scene: Image.Image, logo: Image.Image) -> Image.Image:
    """What the player actually sees: scene + logo + classic 2x2 button stack."""
    preview = scene.resize((1920, 1080), Image.LANCZOS).convert("RGBA")
    logo_scaled = logo.crop((0, 0, logo.width, 44 * 4)).resize((960, 165), Image.LANCZOS)
    preview.alpha_composite(logo_scaled, (480, 150))

    btn_w, btn_h, gap = 700, 74, 12
    x = (1920 - btn_w) // 2
    y = 380
    rows = [["SINGLEPLAYER"], ["MULTIPLAYER"], ["MINECRAFT REALMS", "MODS"], ["OPTIONS...", "QUIT GAME"]]
    for row in rows:
        if len(row) == 1:
            img = make_button("normal").resize((btn_w, btn_h), Image.NEAREST)
            preview.alpha_composite(img, (x, y))
            label = row[0]
            tw = text_width(label, 5, tracking=1)
            draw_pixel_text(preview, label, x + (btn_w - tw) // 2, y + (btn_h - 35) // 2, 5, (255, 255, 255))
            y += btn_h + gap
        else:
            half = (btn_w - 16) // 2
            for i, label in enumerate(row):
                bx = x + i * (half + 16)
                state = "normal"
                img = make_button(state).resize((half, btn_h), Image.NEAREST)
                preview.alpha_composite(img, (bx, y))
                tw = text_width(label, 4, tracking=1)
                draw_pixel_text(preview, label, bx + (half - tw) // 2, y + (btn_h - 28) // 2, 4, (255, 255, 255))
            y += btn_h + gap
    return preview.convert("RGB")


def main() -> None:
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SRC
    OUT.mkdir(parents=True, exist_ok=True)
    scene = make_scene(src)
    scene.save(OUT / "menu_red_forest.webp", "WEBP", quality=82, method=6)

    logo_modern = make_logo_modern()
    logo_modern.save(OUT / "logo_modern.png")
    make_logo_legacy().save(OUT / "logo_legacy.png")
    for state in ("normal", "highlighted", "disabled"):
        name = "button" if state == "normal" else f"button_{state}"
        make_button(state).save(OUT / f"{name}.png")
    for state in ("normal", "highlighted", "selected"):
        name = "tab" if state == "normal" else f"tab_{state}"
        make_tab(state).save(OUT / f"{name}.png")
    make_widgets_legacy().save(OUT / "widgets_legacy.png")
    make_overlay().save(OUT / "panorama_overlay.png")

    design = ROOT / "design/vortex-red-menu"
    design.mkdir(parents=True, exist_ok=True)
    make_preview(scene, logo_modern).resize((1600, 900), Image.LANCZOS).save(
        design / "preview_main_menu.jpg", "JPEG", quality=92, optimize=True
    )
    (OUT / "splashes.txt").write_text(
        "\n".join(
            [
                "VORTEX CLIENT!",
                "Crimson & Obsidian!",
                "Maximum Mobile FPS!",
                "Waving 3D Animated Capes!",
                "Powered by Vortex Launcher!",
                "Red Forest Edition!",
            ]
        )
        + "\n",
        encoding="utf-8",
    )

    for f in sorted(OUT.iterdir()):
        print(f"{f.name:28} {f.stat().st_size / 1024:8.1f} KB")


if __name__ == "__main__":
    main()
