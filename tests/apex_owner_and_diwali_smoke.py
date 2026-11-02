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
# A foreign account must never publish, and status must say so.
send = discord.split("private fun sendActivity()", 1)[1]
assert "if (foreignAccount) return" in send.split("val usingMinecraftProfile", 1)[0]
assert "not the owner" in discord
assert "KEY_DISCORD_OWNER_ID" in prefs and "discordOwnerId" in prefs
assert "Owner Discord account" in CARD.read_text()

# ── Diwali page ─────────────────────────────────────────────────────────────
diwali = DIWALI.read_text()
for phrase in ("A NEW BEGINNING IS COMING", "APEX LAUNCHER 2.0 IS COMING SOON",
               "20 NOVEMBER 2026", "SAVE THE DATE", "The Next Generation Begins"):
    assert phrase in diwali, phrase
assert "fun DiwaliSpecialScreen(" in diwali

client = CLIENT.read_text()
assert "showDiwali" in client and "DiwaliSpecialScreen(onClose" in client
assert 'text = ApexDiwali.TITLE' in client

# ── CEO entry ───────────────────────────────────────────────────────────────
contrib = CONTRIB.read_text()
assert 'name = "YadavBoy"' in contrib and 'role = "CEO"' in contrib
assert "R.drawable.apex_contrib_yadavboy" in contrib
data = AVATAR.read_bytes()
assert data[:8] == b"\x89PNG\r\n\x1a\n"
assert struct.unpack(">II", data[16:24]) == (512, 512)

print("Owner lock, Diwali page and CEO entry passed")
