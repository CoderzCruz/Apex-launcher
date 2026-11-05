#!/usr/bin/env python3
"""Smoke checks for the Indian festivals added after launch: Lohri and Baisakhi.

Both are fixed-date on the Gregorian calendar (the platform only exposes a Chinese lunar
calendar, so lunisolar Hindu dates stay out), both greet in non-Chinese locales, and both
light real fireworks through the festival effect registry.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
FESTIVAL = APP / "java/net/kdt/pojavlaunch/aerix/festivals/Festival.kt"
EFFECTS = APP / "java/net/kdt/pojavlaunch/aerix/festivals/FestivalEffectType.kt"
STRINGS = APP / "res/values/aerix_strings.xml"

festival = FESTIVAL.read_text()

# ── Lohri: 13 January, fixed date ────────────────────────────────────────
assert "LOHRI(" in festival
lohri = festival[festival.index("LOHRI("):festival.index("BAISAKHI(")]
assert "date.checkDate(1, 13)" in lohri, "Lohri is 13 January"
assert "isChinese = false" in lohri and "isInternational = true" in lohri
assert "aerix_festivals_lohri" in lohri

# ── Baisakhi: 13-14 April, fixed date range ──────────────────────────────
baisakhi = festival[festival.index("BAISAKHI("):festival.index("QIXI(")]
assert "date.checkDateRange(4, 13..14)" in baisakhi, "Baisakhi is 13-14 April"
assert "isChinese = false" in baisakhi and "isInternational = true" in baisakhi
assert "aerix_festivals_baisakhi" in baisakhi

# ── Real fireworks on both days ──────────────────────────────────────────
effects = EFFECTS.read_text()
assert "Festival.LOHRI to listOf(FIREWORKS)" in effects
assert "Festival.BAISAKHI to listOf(FIREWORKS)" in effects

# ── Greetings exist for the festival chip ────────────────────────────────
strings = STRINGS.read_text()
assert '<string name="aerix_festivals_lohri">Happy Lohri!</string>' in strings
assert '<string name="aerix_festivals_baisakhi">Happy Baisakhi!</string>' in strings

print("Indian festivals: Lohri and Baisakhi dates, greetings and fireworks passed")
