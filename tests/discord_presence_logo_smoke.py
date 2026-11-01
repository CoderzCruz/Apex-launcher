"""Check the Rich Presence image points to the shipped logo in both link modes."""
import hashlib
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
base = root / 'app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/discord'
src = (base / 'AerixDiscordPresence.kt').read_text()
# The card artwork is the asset uploaded to the fork's own Discord application. Mobile clients
# resolve application asset keys only, so a bare image URL is never used here.
assert 'const val APEX_LOGO_ASSET = "apex_logo"' in src
assert 'raw.githubusercontent.com' not in src.split('private fun sendActivity()', 1)[1]
logo = (root / 'assets/apex-logo.png').read_bytes()
assert logo[:4] == b'\x89PNG'
assert len(logo) > 10_000
activity = src.split('private fun sendActivity()', 1)[1].split('private fun send(frame:', 1)[0]
assets, links = activity.split('val assets = JSONObject()', 1)[1].split('if (clickable && APEX_WEBSITE_URL.isNotBlank()) {', 1)
assert 'assets.put("large_image", APEX_LOGO_ASSET)' in assets
assert 'assets.put("small_image", APEX_LOGO_ASSET)' in assets
# No upstream links or buttons on the card.
assert 'AERIX_WEBSITE_URL' not in links and 'AERIX_DISCORD_URL' not in links
assert 'clickable && inGameMode && isValidHttpAssetUrl(it)' in assets
assert 'activity.put("assets", assets)' in links
preview = (base / 'AerixDiscordPresenceCard.kt').read_text().split('fun DiscordRichPresenceLivePreview', 1)[1]
# The fork's preview draws its own mark, not the upstream logo.
assert 'painterResource(R.drawable.ic_apex_mark)' in preview
assert 'painterResource(R.drawable.aerix_logo)' not in preview
assert 'About Apex' in preview and 'Join Apex Discord' in preview
print('Discord presence logo wiring passed')
