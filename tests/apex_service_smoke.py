#!/usr/bin/env python3
"""Guard rails for Apex Service, the in-app assistant.

It must stay a real assistant: a pinned conversation in Chats, mention handling inside shared
rooms, a Gemini brain when a key is saved, and a built-in knowledge base so the feature never
looks dead. The system prompt must keep the honest "no live voice chat" line.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix"

SERVICE = APP / "social/ApexService.kt"
BADGE = APP / "social/ApexBadge.kt"
PREFS = APP / "bridge/AerixPreferences.kt"
SETTINGS = APP / "ui/screens/SettingsScreen.kt"
CHATS = APP / "ui/screens/ChatsScreen.kt"
STRINGS = ROOT / "app_pojavlauncher/src/main/res/values/aerix_strings.xml"

service = SERVICE.read_text()
assert "object ApexService" in service
assert 'const val NAME = "Apex Service"' in service

# Addressable in a room, with the mention stripped before the brain sees it.
assert "fun mentionsUs(" in service
assert '"@apex"' in service and '"apex service"' in service, "the room trigger words must stay"
assert "private fun stripMention(" in service

# Real AI path.
assert "generativelanguage.googleapis.com" in service
assert "gemini-3.8-flash" in service, "current flash models must be in the fallback list"
assert "discoverModels(" in service, "model names retire, so the list must be discovered live"
assert "supportedGenerationMethods" in service and "generateContent" in service
assert "thinkingConfig" in service and '"low"' in service, "3.x thinking must not eat the answer budget"
assert 'setRequestProperty("x-goog-api-key"' in service
assert "suspend fun reply(" in service and "suspend fun ask(" in service

# Honest system prompt: never claims live voice chat exists.
assert "Live voice chat (a real voice channel) is NOT available yet" in service
assert "GPL-3.0 fork of PojavLauncher" in service, "author credit must stay in the prompt"

# Offline brain so the assistant answers without a key.
assert "localAnswer(" in service and "knowledge" in service
assert '"voice chat", "vc"' in service, "must answer VC questions honestly"

# Key storage + settings field.
prefs = PREFS.read_text()
assert 'const val KEY_GOOGLE_AI_KEY = "apexServiceAiKey"' in prefs
assert "var googleAiKey by mutableStateOf(" in prefs
assert "fun saveGoogleAiKey(" in prefs
settings = SETTINGS.read_text()
# The key field is owner-only now, so the card reads the effective (baked or saved) key.
assert "AerixPreferences.effectiveAiKey()" in settings
assert "AerixPreferences.saveGoogleAiKey(it)" in settings
assert "maskKey(" in settings, "a non-owner must never see the full key"
strings = STRINGS.read_text()
assert "aerix_settings_ai_key" in strings and "aerix_settings_ai_key_sub" in strings

# Assistant badge.
badge = BADGE.read_text()
assert 'SERVICE("BOT"' in badge
assert '"apex service" to ApexBadge.SERVICE' in badge

# Chats integration.
chats = CHATS.read_text()
assert "data object Service : ChatTarget" in chats
assert "private fun ServiceThread(" in chats
assert "private fun BotRow(" in chats
assert "ApexService.mentionsUs(" in chats, "rooms must detect the @apex mention"
assert "ApexService.reply(" in chats and "ApexService.ask(" in chats
assert 'items(botReplies, key = { "apex-service-" + it.first })' in chats
assert "ApexService.NAME" in chats and "ApexBadge.SERVICE" in chats

# The assistant's own page has its own look, not the player chat bubbles.
# The page follows the launcher accent, not a fixed colour of its own.
assert "private fun accentFor(): Color = AerixThemeManager.currentAccent()" in chats
assert "0xFF7C5CFF" not in chats and "serviceViolet" not in chats, "no hard-coded violet"

# The assistant wears its own emblem: header, pinned row, welcome card and every answer card.
import struct
assert "private fun ServiceLogo(" in chats
assert "R.drawable.apex_service_logo" in chats, "the pinned row must show the emblem"
assert chats.count("ServiceLogo(") >= 5, "header, welcome, answers and typing all wear it"
logo_path = ROOT / "app_pojavlauncher/src/main/res/drawable-nodpi/apex_service_logo.png"
assert logo_path.exists(), "the Apex Service emblem must ship with the app"
logo_bytes = logo_path.read_bytes()
assert logo_bytes[:8] == b"\x89PNG\r\n\x1a\n"
assert struct.unpack(">II", logo_bytes[16:24]) == (512, 512)
assert "private fun ServiceHero(" in chats and "private fun ServiceTurn(" in chats
assert "private fun UserTurn(" in chats
assert "private fun ServiceTyping(" in chats and "rememberInfiniteTransition" in chats
assert "private fun ServiceComposer(" in chats and "BasicTextField(" in chats
assert "SERVICE_PROMPTS" in chats, "quick prompts keep the first message easy"
assert "private fun ServicePromptStrip(" in chats
assert "ApexService.resetConversation()" in chats, "New chat must clear the thread"
# Service answers are cards, players keep the tailed bubble.
turn = chats.split("private fun ServiceTurn(", 1)[1].split("private fun ServiceTyping(", 1)[0]
assert "TailBubbleShape" not in turn, "an answer must not look like a chat bubble"

# Ship-with-build brain: the CI secret is baked in as a resource, never committed.
gradle = (ROOT / "app_pojavlauncher/build.gradle").read_text()
assert "def getServiceAiKey = {" in gradle
assert "resValue 'string', 'apex_service_ai_key', getServiceAiKey()" in gradle
assert "System.getenv(\"APEX_SERVICE_AI_KEY\")" in gradle
workflow = (ROOT / ".github/workflows/aerix-debug.yml").read_text()
assert "APEX_SERVICE_AI_KEY: ${{ secrets.APEX_SERVICE_AI_KEY }}" in workflow
assert "fun effectiveAiKey()" in prefs and "googleAiKey.ifBlank { bakedAiKey }" in prefs
assert "AerixPreferences.effectiveAiKey()" in service
# The secret itself must never appear in the tree.
# The prefix is assembled here so this guard file never contains the secret itself.
key_prefix = "AQ.Ab" + "8RN6"
for path in ROOT.rglob("*"):
    if path == Path(__file__) or not path.is_file():
        continue
    if path.suffix in {".kt", ".gradle", ".yml", ".py", ".xml", ".txt", ".properties"}:
        assert key_prefix not in path.read_text(errors="ignore"), f"AI key leaked into {path}"

assert "preferredModel" in service, "the model that answered must be reused"
assert "NON_CHAT_MARKERS" in service, "speech and image models must stay out of the list"
assert "lastModel" in service and "ApexService.lastModel" in chats

print("Apex Service wiring passed")
