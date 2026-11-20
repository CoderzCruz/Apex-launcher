package com.vortex.client.game;

import com.vortex.client.data.ClientConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Real vanilla launch: builds the classpath from an INSTALLED version's json
 * (libraries already on disk under the configured .minecraft) and starts the
 * official main class with ProcessBuilder. No downloads, no auth bypass —
 * offline profiles only (Microsoft login is a separate, official-OAuth phase).
 */
public final class MinecraftLauncher {

    private MinecraftLauncher() {}

    public static File defaultMinecraftDir() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return new File(appData != null ? appData : home + File.separator + "AppData/Roaming",
                ".minecraft");
        }
        return new File(home, ".minecraft");
    }

    public static boolean isConfigured(ClientConfig config) {
        File mc = new File(config.get("mcDir", defaultMinecraftDir().getAbsolutePath()));
        String version = config.get("mcVersion", "");
        return mc.isDirectory() && !version.isEmpty() &&
            new File(mc, "versions" + File.separator + version + File.separator + version + ".json").isFile();
    }

    /** Installed versions (directories that contain their own json). */
    public static List<String> installedVersions(File mcDir) {
        List<String> out = new ArrayList<>();
        File versions = new File(mcDir, "versions");
        File[] dirs = versions.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File d : dirs) {
                if (new File(d, d.getName() + ".json").isFile()) out.add(d.getName());
            }
        }
        Collections.sort(out, Collections.reverseOrder());
        return out;
    }

    /**
     * Launches Minecraft. @return the started process, or throws with a user-readable reason.
     */
    public static Process launch(ClientConfig config, String extraServer) throws Exception {
        File mcDir = new File(config.get("mcDir", defaultMinecraftDir().getAbsolutePath()));
        String versionId = config.get("mcVersion", "");
        if (versionId.isEmpty()) throw new IllegalStateException("No Minecraft version selected (Settings)");
        File jsonFile = new File(mcDir, "versions" + File.separator + versionId + File.separator + versionId + ".json");
        if (!jsonFile.isFile()) throw new IllegalStateException("Version not installed: " + versionId);

        JSONObject json = new JSONObject(readAll(jsonFile));
        String mainClass = json.optString("mainClass", "net.minecraft.client.main.Main");

        // Classpath: every library jar the json lists, resolved against the local maven-style tree.
        List<String> classpath = new ArrayList<>();
        JSONArray libs = json.optJSONArray("libraries");
        if (libs != null) {
            for (int i = 0; i < libs.length(); i++) {
                JSONObject lib = libs.getJSONObject(i);
                JSONObject downloads = lib.optJSONObject("downloads");
                JSONObject artifact = downloads != null ? downloads.optJSONObject("artifact") : null;
                String rel = artifact != null ? artifact.optString("path") : "";
                if (rel.isEmpty()) continue;
                File jar = new File(mcDir, "libraries" + File.separator + rel.replace('/', File.separatorChar));
                if (jar.isFile()) classpath.add(jar.getAbsolutePath());
            }
        }
        File clientJar = new File(mcDir, "versions" + File.separator + versionId
            + File.separator + versionId + ".jar");
        if (!clientJar.isFile()) {
            // Some installs keep the client jar under libraries/versions.
            File alt = new File(mcDir, "client" + File.separator + versionId + ".jar");
            if (alt.isFile()) clientJar = alt;
            else throw new IllegalStateException("Client jar missing for " + versionId);
        }
        classpath.add(clientJar.getAbsolutePath());

        String javaExe = config.get("javaPath", "");
        if (javaExe.isEmpty()) {
            javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator
                + (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java");
        }

        String username = config.get("username", "Player");
        String assetsDir = mcDir.getAbsolutePath();
        String assetIndex = json.optJSONObject("assetIndex") != null
            ? json.getJSONObject("assetIndex").optString("id", "legacy") : "legacy";

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        int ramMb = config.getInt("ramMb", 2048);
        cmd.add("-Xmx" + ramMb + "M");
        cmd.add("-Xms512M");
        String jvmArgs = config.get("jvmArgs", "").trim();
        if (!jvmArgs.isEmpty()) cmd.addAll(Arrays.asList(jvmArgs.split("\\s+")));
        cmd.add("-cp");
        cmd.add(String.join(File.pathSeparator, classpath));
        cmd.add(mainClass);
        cmd.add("--username"); cmd.add(username);
        cmd.add("--version"); cmd.add(versionId);
        cmd.add("--gameDir"); cmd.add(mcDir.getAbsolutePath());
        cmd.add("--assetsDir"); cmd.add(assetsDir);
        cmd.add("--assetIndex"); cmd.add(assetIndex);
        cmd.add("--uuid"); cmd.add(ClientConfig.offlineUuid(username));
        cmd.add("--accessToken"); cmd.add("0");
        cmd.add("--userType"); cmd.add("legacy");
        cmd.add("--versionType"); cmd.add("Vortex");
        String resolution = config.get("resolution", "");
        if (resolution.matches("\\d+x\\d+")) {
            String[] wh = resolution.split("x");
            cmd.add("--width"); cmd.add(wh[0]);
            cmd.add("--height"); cmd.add(wh[1]);
        }
        if (extraServer != null && !extraServer.isBlank()) {
            String[] hp = extraServer.split(":", 2);
            cmd.add("--server"); cmd.add(hp[0]);
            if (hp.length > 1) { cmd.add("--port"); cmd.add(hp[1]); }
        }

        File log = new File(com.vortex.client.data.ClientConfig.DIR, "last-launch.log");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(mcDir);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(log));
        return pb.start();
    }

    private static String readAll(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }
}
