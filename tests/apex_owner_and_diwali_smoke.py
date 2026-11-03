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
owner = (APP / "java/net/kdt/pojavlaunch/aerix/bridge/ApexOwner.kt").read_text()
settings = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt").read_text()

# The card itself is public: image, invite and website are always shown.
for field in ("Card image", "Discord invite link", "Website link"):
    assert field in card, field
# ...but each of them, and both API keys, is owner-only.
assert card.count("editable = owner") == 3, "all three branding fields must be owner-locked"
assert card.count("ApexOwner.LOCK_HINT") >= 3
assert "ApexOwner.verify(" in card and "ApexOwner.forget()" in card
# No global unlock switch left that anybody could flip.
assert "Lock branding to the owner" not in card

# Owner identity: remembered verification wins even without Discord running.
assert "fun isOwner(): Boolean" in owner
assert "AerixPreferences.ownerVerified || AerixDiscordPresence.isOwnerAccount()" in owner
assert "fun verify(candidate: String): Boolean" in owner
assert "ownerVerified" in prefs and "KEY_OWNER_VERIFIED" in prefs

# Storage refuses the five fields unless the owner is verified, so a patched UI cannot help.
guarded = prefs.split("if (!ownerVerified) return")
assert len(guarded) == 6, "invite, website, card image, both keys and the AI key setter"
for setter in ("saveDiscordAssetName", "saveDiscordInviteUrl", "saveDiscordWebsiteUrl",
               "saveCurseForgeApiKey", "saveGoogleAiKey"):
    assert setter in prefs

# The two keys are hidden behind dots for everyone else.
assert "maskKey(" in settings and "editable = ownerAccess" in settings
assert 'ApexOwner.LOCK_HINT' in settings

# ── Diwali page ─────────────────────────────────────────────────────────────
diwali = DIWALI.read_text()
for phrase in ("A NEW BEGINNING IS COMING", "APEX LAUNCHER 2.0 IS COMING SOON",
               "20 NOVEMBER 2026", "SAVE THE DATE", "The Next Generation Begins"):
    assert phrase in diwali, phrase
assert "fun DiwaliSpecialScreen(" in diwali

# The page is alive: a real countdown to the launch, not a static date string.
assert "fun millisUntilRelease(" in diwali
assert "mutableLongStateOf" in diwali and "delay(1_000L)" in diwali
assert "CountdownCell(" in diwali
assert "JourneyCard(" in diwali and "FeatureGrid(" in diwali
# Drawn celebration instead of plain background.
assert "BoxScope.DiwaliNightSky(" in diwali and "drawCircle(" in diwali
# Real actions: a calendar reminder and a share sheet.
assert "CalendarContract.EXTRA_EVENT_BEGIN_TIME" in diwali
assert "Intent.ACTION_SEND" in diwali and "fun shareText()" in diwali
assert "fun addToCalendar(" in diwali and "fun shareAnnouncement(" in diwali
# Premium pass: foil sheen over the hero, an ornament rule, candle-glow numerals.
assert "BoxScope.DiwaliGoldSheen(" in diwali and diwali.count("DiwaliGoldSheen(") >= 2
assert "fun DiwaliDivider(" in diwali and diwali.count("DiwaliDivider(") >= 2
# Two more real options: copy the announcement, open the project page.
assert "fun copyAnnouncement(" in diwali and "ClipData.newPlainText" in diwali
assert "fun openGitHub(" in diwali and "Intent.ACTION_VIEW" in diwali
# Everything on the page animates: staggered entrances, popping digits, a sweeping
# journey bar, a rule that draws itself, a breathing footer and a halo on the date pill.
assert "private fun Modifier.entrance(" in diwali
assert diwali.count(".entrance(") >= 11, "every section staggered in"
assert "pop.animateTo(" in diwali, "countdown digits pop on each tick"
assert "fill.animateTo(" in diwali, "the journey bar sweeps from zero"
assert "grow.animateTo(" in diwali, "the ornament rule draws itself"
assert "animateFloatAsState(" in diwali and "graphicsLayer" in diwali
assert "ambientColor = festivalAmber" in diwali, "the date pill breathes a halo"

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
