"""Regression checks for the Social profile cosmetic entry points."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix"


class SocialCosmeticsSmoke(unittest.TestCase):
    def test_sidebar_uses_current_account_skin_and_revision(self):
        screen = (ROOT / "ui/screens/SocialsScreen.kt").read_text()
        sidebar = screen.split("private fun SocialSidebar(", 1)[1].split("private fun SocialRailItem(", 1)[0]
        self.assertIn("CurrentAccountHead(size = 32)", sidebar)
        head = screen.split("private fun CurrentAccountHead(", 1)[1].split("private val mcHeadCache", 1)[0]
        self.assertIn("PojavAccountBridge.current", head)
        self.assertIn("PojavAccountBridge.wardrobeRevision", head)
        self.assertIn("PojavAccountBridge.loadAccountFacePreview", head)
        self.assertNotIn("SocialHead(", head)

    def test_character_is_limited_to_own_profile_and_clipped(self):
        screen = (ROOT / "ui/screens/SocialProfileScreen.kt").read_text()
        card = screen.split("private fun ProfileCard(", 1)[1].split("private fun ProfileCharacter(", 1)[0]
        self.assertIn("if (isMe) {\n                ProfileCharacter(", card)
        self.assertIn("widthIn(max = 148.dp)", card)
        self.assertIn("height(130.dp)", card)
        character = screen.split("private fun ProfileCharacter(", 1)[1].split("private fun ProfileCosmeticsPage(", 1)[0]
        for feature in ("clipToBounds()", "detectDragGestures", "onClick = onClick", "AerixSkinPreview3D", "useClientCape = clientCape"):
            self.assertIn(feature, character)

    def test_skin_and_cape_actions_use_existing_account_paths(self):
        screen = (ROOT / "ui/screens/SocialProfileScreen.kt").read_text()
        page = screen.split("private fun ProfileCosmeticsPage(", 1)[1].split("private fun CountCell(", 1)[0]
        for method in ("equipSkinFromUri", "equipCapeFromUri", "canEquipCape", "syncCapeAssetsToAllClientInstances"):
            self.assertIn(method, page)
        self.assertIn("AerixClientStore.persist()", page)
        self.assertIn("BackHandler(enabled = view != ProfileView.CARD)", screen)

    def test_rendered_skin_and_cape_share_equipped_sources(self):
        bridge = (ROOT / "bridge/PojavAccountBridge.kt").read_text()
        face = bridge.split("fun loadAccountFacePreview(", 1)[1].split("fun loadAccountBodyPreview(", 1)[0]
        self.assertIn("resolveAccountSkinBytes(account)", face)
        self.assertIn("renderSkinFace(skin)", face)
        view = (ROOT / "components/AerixPlayerSkin.kt").read_text()
        self.assertIn("PojavAccountBridge.resolveAccountSkinBytes(account)", view)
        self.assertIn("PojavAccountBridge.resolveCapeBytes(account, includeOfficial = showOfficialCape)", view)
        self.assertIn("AerixClientCosmetics.resolveInGameCapeBytes(context)", view)

    def test_profiles_carry_their_logo_to_other_players(self):
        """A friend's profile must show the logo they uploaded, whatever the RPC returns."""
        backend = (ROOT / "social/SocialBackend.kt").read_text()
        # profile_card / find_player / find_by_code can omit the media columns.
        card = backend.split("suspend fun loadProfileCard(", 1)[1].split("suspend fun findPlayer(", 1)[0]
        self.assertIn("withMedia(it)", card)
        find = backend.split("suspend fun findPlayer(", 1)[1].split("fun inviteCode(", 1)[0]
        self.assertIn("withMedia(it)", find, "findPlayer must enrich too")
        by_code = backend.split("suspend fun findByCode(", 1)[1]
        self.assertIn("withMedia(it)", by_code.split("suspend fun claimUsername(", 1)[0])
        media = backend.split("private suspend fun withMedia(", 1)[1].split("suspend fun findPlayer(", 1)[0]
        for column in ("avatar_url", "banner_url", "skin_url", "cape_url"):
            self.assertIn(column, media)
        self.assertIn("profile.copy(", media)

        # Every avatar falls back to the player's Minecraft head instead of a generic icon.
        models = (ROOT / "social/SocialModels.kt").read_text()
        self.assertIn("val displayAvatarUrl: String?", models)
        profile = (ROOT / "social/SocialProfile.kt").read_text()
        self.assertIn("val displayAvatarUrl: String?", profile)
        self.assertIn("internal fun minecraftHeadUrl(", profile)
        self.assertIn("mc-heads.net/avatar/", profile)

        screen = (ROOT / "ui/screens/SocialProfileScreen.kt").read_text()
        self.assertIn("url = profile?.displayAvatarUrl", screen)
        self.assertIn("avatarUrl = friend.displayAvatarUrl", screen)
        self.assertIn("avatarUrl = person.displayAvatarUrl", screen)
        chat = (ROOT / "ui/screens/SocialChatViews.kt").read_text()
        self.assertIn("url = liveFriend.displayAvatarUrl", chat)


if __name__ == "__main__":
    unittest.main()
