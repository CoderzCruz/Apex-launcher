"""Check the Rich Presence image points to the shipped raven logo in both link modes."""
import hashlib
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
base = root / 'app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/discord'
src = (base / 'AerixDiscordPresence.kt').read_text()
url = re.search(r'const val AERIX_LOGO_URL\s*=\s*"([^"]+)"', src).group(1)
assert url == ('https://raw.githubusercontent.com/CoderzCruz/Apex-launcher/'
               'arena/a426cc08-apex-launcher/assets/aerix-logo.png')
logo = (root / 'assets/aerix-logo.png').read_bytes()
assert hashlib.sha256(logo).hexdigest() == '8f492acd702c325fea0d6c01f0796703a9a2b05ee367e5e4f2f4fbd29aa5d01e'
assert logo == (root / 'app_pojavlauncher/src/main/res/drawable-nodpi/aerix_logo.png').read_bytes()
activity = src.split('private fun sendActivity()', 1)[1].split('private fun send(frame:', 1)[0]
assets, links = activity.split('val assets = JSONObject()', 1)[1].split('if (clickable) {', 1)
assert 'assets.put("large_image", AERIX_LOGO_URL)' in assets
assert 'assets.put("small_image", currentSkinAvatarUrl() ?: AERIX_LOGO_URL)' in assets, "skin badge with logo fallback"
assert 'clickable && inGameMode && isValidHttpAssetUrl(it)' in assets
assert 'activity.put("assets", assets)' in links
preview = (base / 'AerixDiscordPresenceCard.kt').read_text().split('fun DiscordRichPresenceLivePreview', 1)[1]
assert 'painterResource(AerixPreferences.logoSkin)' in preview, "card follows the logo setting"
assert 'https://discord.gg/wpjAVkf2V' in src, "our Discord invite"
assert '"About Vortex"' in preview and '"Join Vortex Discord"' in preview, "renamed buttons"
assert 'Website will Not Ready' in preview, "about button shows not-ready toast"
assert 'badgeFace' in preview, "skin replaces the badge logo"
assert 'via Vortex Launcher' in preview, "via line renamed"
assert 'mc-heads.net/avatar/' in src, "real card badge uses the skin face"
# Backgrounding (opening Discord) keeps the card: clear only on a real finish.
stopped = src.split('fun onLauncherStopped(', 1)[1].split('fun prepareHandoffToGame', 1)[0]
assert 'isFinishing != true) return' in stopped, "background switch keeps the card"
assert 'clearAndDetach(app, onlyIfExited = true)' in stopped, "real exit still clears"
assert 'if (handoffToGameInProgress) return' in stopped
assert 'onAppSwipedFromRecents' in src, "recents swipe still clears"
assert 'onProcessExiting' in src, "process exit still clears"

# Minecraft game profile in-game: catalog first, retried every session.
prefs2 = (root / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt").read_text()
assert "discordGameProfileMode = p.getBoolean(KEY_DISCORD_GAME_PROFILE_MODE, true)" in prefs2, "game profile on by default"
assert "MINECRAFT_PRIMARY_APP_ID" in src, "Minecraft catalog id"
assert src.count("rejectedAppIds.clear()") >= 2, "catalog retried each game session"
print('Discord presence logo wiring passed')
