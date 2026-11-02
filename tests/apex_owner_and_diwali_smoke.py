#!/usr/bin/env python3
"""Smoke checks: the Discord card is owner-locked, Diwali page exists, YadavBoy is the CEO."""
import pathlib
import struct

ROOT = pathlib.Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
DISCORD = APP / "java/net/kdt/pojavlaunch/aerix/discord/AerixDiscordPresence.kt"
CARD = APP / "java/net/kdt/pojavlaunch/aerix/discord/AerixDiscordPresenceCard.kt"
PREFS = APP / "java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt"
DIWALI = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/DiwaliSpecialScreen.kt"
CLIENT = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/AerixClientScreen.kt"
CONTRIB = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/ContributionScreen.kt"
AVATAR = APP / "res/drawable-nodpi/apex_contrib_yadavboy.png"

discord = DISCORD.read_text()
prefs = PREFS.read_text()

# ── owner lock ──────────────────────────────────────────────────────────────
assert 'const val OWNER_DISCORD_ID = "1385646460774907914"' in discord
assert "fun readAccountFromFrame(" in discord
assert 'optJSONObject("user")' in discord
assert "foreignAccount" in discord
# The card publishes for every account; only the branding fields are owner-locked.
send = discord.split("private fun sendActivity()", 1)[1].split("val usingMinecraftProfile", 1)[0]
assert "if (foreignAccount) return" not in send, "the card must not be suppressed"
card = CARD.read_text()
assert "brandingUnlocked" in card and "Branding locked" in card
assert "Owner unlock" in card and "OWNER_DISCORD_ID" in card
assert "KEY_DISCORD_BRANDING_UNLOCKED" in prefs
assert "discordBrandingUnlocked" in prefs

# ── Diwali page ─────────────────────────────────────────────────────────────
diwali = DIWALI.read_text()
for phrase in ("A NEW BEGINNING IS COMING", "APEX LAUNCHER 2.0 IS COMING SOON",
               "20 NOVEMBER 2026", "SAVE THE DATE", "The Next Generation Begins"):
    assert phrase in diwali, phrase
assert "fun DiwaliSpecialScreen(" in diwali

# Diwali has its own rail destination and is gone from the Apex Client screen.
rail = (APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixNavigationRail.kt").read_text()
assert "DIWALI" in rail and "aerix_ic_diwali" in rail
screen = (APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixLauncherScreen.kt").read_text()
assert "LauncherSection.DIWALI ->" in screen
client = CLIENT.read_text()
assert "showDiwali" not in client and "DiwaliSpecialScreen" not in client, "Diwali must leave Apex Client"
assert 'text = ApexDiwali.TITLE' not in client

# ── CEO entry ───────────────────────────────────────────────────────────────
contrib = CONTRIB.read_text()
assert 'name = "YadavBoy"' in contrib and 'role = "CEO"' in contrib
assert "R.drawable.apex_contrib_yadavboy" in contrib
data = AVATAR.read_bytes()
assert data[:8] == b"\x89PNG\r\n\x1a\n"
assert struct.unpack(">II", data[16:24]) == (512, 512)

print("Owner lock, Diwali page and CEO entry passed")
