"""Checks public branding and package configuration without an Android SDK."""
from pathlib import Path
import struct
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher"
RES = APP / "src/main/res"


class BrandingIdentitySmoke(unittest.TestCase):
    def test_application_ids_and_provider_authorities(self):
        gradle = (APP / "build.gradle").read_text()
        self.assertIn('applicationId "net.kdt.pojavlaunch"', gradle)
        for package in ("net.kdt.pojavlaunch", "net.kdt.pojavlaunch.debug", "net.kdt.pojavlaunch.pub"):
            self.assertIn("'application_package', '" + package + "'", gradle)
        for authority in ("net.kdt.pojavlaunch.scoped.gamefolder",
                          "net.kdt.pojavlaunch.scoped.gamefolder.debug",
                          "net.kdt.pojavlaunch.pub.scoped.gamefolder"):
            self.assertIn("'storageProviderAuthorities', '" + authority + "'", gradle)
        self.assertIn("namespace 'net.kdt.pojavlaunch'", gradle)

    def test_existing_signing_and_debug_version_configuration(self):
        gradle = (APP / "build.gradle").read_text()
        self.assertIn('storeFile file("debug.keystore")', gradle)
        self.assertIn('storeFile file("aerix_release.p12")', gradle)
        self.assertIn("signingConfig signingConfigs.customDebug", gradle)
        self.assertIn("signingConfig signingConfigs.aerixRelease", gradle)
        self.assertIn('onVariants(selector().withBuildType("debug"))', gradle)
        self.assertIn("output.versionCode.set(AERIX_DEBUG_VERSION_CODE)", gradle)

    def test_previous_apps_can_still_be_discovered(self):
        manifest = ET.parse(APP / "src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        packages = {p.get(ns + "name") for p in manifest.findall("./queries/package")}
        self.assertTrue({"net.kdt.pojavlaunch", "net.kdt.pojavlaunch.debug", "net.kdt.pojavlaunch.pub"} <= packages)
        activity = (APP / "src/main/java/net/kdt/pojavlaunch/LauncherActivity.java").read_text()
        self.assertIn("s.equals(getPackageName())", activity)
        self.assertIn("R.string.migration_notice", activity)

    def test_localized_app_labels_and_notifications(self):
        count = 0
        visible_keys = {"app_name", "app_short_name", "error_fatal", "lazy_service_default_title",
                        "storage_required", "modpack_install_notification_title", "notification_permission_dialog_text",
                        "forge_dl_no_installer", "create_profile_modded_versions"}
        for file in RES.glob("values*/strings.xml"):
            root = ET.parse(file).getroot()
            for item in root.findall("string"):
                if item.get("name") not in visible_keys:
                    continue
                text = "".join(item.itertext())
                self.assertNotRegex(text.lower(), r"pojav", f"{file}: {item.get('name')}")
            count += 1
        self.assertGreaterEqual(count, 50)
        strings = ET.parse(RES / "values/strings.xml").getroot()
        self.assertEqual(strings.find("string[@name='lazy_service_default_title']").text,
                         "Apex Foreground Service")

    def test_no_loading_screen(self):
        themes = (RES / "values/aerix_themes.xml").read_text()
        splash = themes.split('Aerix.SplashScreen', 1)[1].split('</style>', 1)[0]
        # The entry window shows the plain canvas, not a splash image or progress screen.
        self.assertIn("@color/aerix_canvas", splash)
        self.assertNotIn("@drawable/aerix_splash", splash)

    def test_notification_icon_is_used_and_valid_png(self):
        for service in ("GameService", "ProgressService"):
            code = (APP / f"src/main/java/net/kdt/pojavlaunch/services/{service}.java").read_text()
            self.assertIn("setSmallIcon(R.drawable.notif_icon)", code)
            self.assertIn("R.string.lazy_service_default_title", code)
        data = (RES / "drawable-nodpi/notif_icon.png").read_bytes()
        self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n")
        self.assertEqual(struct.unpack(">II", data[16:24]), (96, 96))


    def test_about_tab_uses_the_apex_mark_and_is_paused(self):
        """The About tab shows the current logo and every action says it is under construction."""
        settings = (APP / "src/main/java/net/kdt/pojavlaunch/aerix/ui/screens/SettingsScreen.kt").read_text()
        about = settings.split("private fun AboutInfoTabContent(", 1)[1]
        # New mark, not the leftover Aerix file.
        self.assertIn("R.drawable.ic_apex_mark", about)
        self.assertNotIn("R.drawable.ic_aerix_mark", about)
        # Nothing opens a link or the update dialog from here any more.
        self.assertNotIn("PojavNavigator.openUrl", about)
        self.assertNotIn("PojavNavigator.openDiscord", about)
        self.assertNotIn("showUpdateDialog = true", about)
        for label in ("Check for Updates", "Project Link", "Discord", "GitHub profile"):
            self.assertIn('AboutPillButton("%s", accent, enabled = false)' % label, about)
        self.assertEqual(about.count("underConstruction(activity)"), 4)
        self.assertIn("private fun underConstruction(", settings)
        self.assertIn("Under Construction", settings)
        # The owner card sits at the bottom, with the Discord mark and the Cruz name.
        self.assertIn('text = "Cruz"', about)
        self.assertIn("Owner", about)
        self.assertIn("R.drawable.ic_discord", about)
        self.assertIn("R.drawable.apex_contrib_cruz", about)

if __name__ == "__main__":
    unittest.main()
