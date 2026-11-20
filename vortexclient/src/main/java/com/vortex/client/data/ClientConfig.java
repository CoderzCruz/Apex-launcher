package com.vortex.client.data;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Persistent client configuration (~/.vortexclient/vortex-client.properties).
 * Everything the app remembers: launch settings, servers, accounts, cosmetics,
 * performance values and mod-toggle overrides. No network, no telemetry.
 */
public final class ClientConfig {

    public static final File DIR = new File(System.getProperty("user.home"), ".vortexclient");
    public static final File FILE = new File(DIR, "vortex-client.properties");

    private final Properties props = new Properties();

    private ClientConfig() {}

    public static ClientConfig load() {
        ClientConfig c = new ClientConfig();
        if (FILE.isFile()) {
            try (InputStream in = new FileInputStream(FILE)) { c.props.load(in); }
            catch (IOException ignored) {}
        }
        return c;
    }

    public synchronized void save() {
        try {
            DIR.mkdirs();
            try (OutputStream out = new FileOutputStream(FILE)) {
                props.store(out, "Vortex Client configuration");
            }
        } catch (IOException ignored) {}
    }

    public synchronized String get(String key, String def) { return props.getProperty(key, def); }
    public synchronized void set(String key, String value) {
        if (value == null) props.remove(key); else props.setProperty(key, value);
    }
    public synchronized int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
    public synchronized void setInt(String key, int value) { props.setProperty(key, String.valueOf(value)); }
    public synchronized boolean getBool(String key, boolean def) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(def)));
    }
    public void setBool(String key, boolean value) { set(key, String.valueOf(value)); }

    // ── Servers: "name;;ip" lines ──────────────────────────────────────
    public synchronized List<String[]> servers() {
        List<String[]> out = new ArrayList<>();
        String raw = props.getProperty("servers", "");
        if (raw.isEmpty()) return out;
        for (String entry : raw.split("~~")) {
            String[] parts = entry.split(";;", -1);
            if (parts.length == 2) out.add(parts);
        }
        return out;
    }

    public synchronized void saveServers(List<String[]> servers) {
        StringBuilder sb = new StringBuilder();
        for (String[] s : servers) {
            if (sb.length() > 0) sb.append("~~");
            sb.append(s[0].replace("~~", " ").replace(";;", " ")).append(";;").append(s[1]);
        }
        props.setProperty("servers", sb.toString());
        save();
    }

    // ── Offline accounts: "name;uuid" lines ────────────────────────────
    public synchronized List<String[]> accounts() {
        List<String[]> out = new ArrayList<>();
        String raw = props.getProperty("accounts", "");
        for (String entry : raw.split("~~")) {
            String[] parts = entry.split(";", -1);
            if (parts.length == 2) out.add(parts);
        }
        return out;
    }

    public synchronized void saveAccounts(List<String[]> accounts) {
        StringBuilder sb = new StringBuilder();
        for (String[] a : accounts) {
            if (sb.length() > 0) sb.append("~~");
            sb.append(a[0]).append(';').append(a[1]);
        }
        props.setProperty("accounts", sb.toString());
        save();
    }

    /** Offline-mode UUID (v3, name-based) — same scheme Minecraft uses for offline profiles. */
    public static String offlineUuid(String name) {
        try {
            java.util.UUID id = java.util.UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return id.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
