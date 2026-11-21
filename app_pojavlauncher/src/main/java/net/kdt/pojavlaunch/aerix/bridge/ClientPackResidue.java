package net.kdt.pojavlaunch.aerix.bridge;

/**
 * Pure string helpers for removing the retired Vortex Client title-screen pack from a Minecraft
 * {@code options.txt}. Kept free of Android types so the CI smoke test can compile and run it on a
 * plain JVM.
 */
public final class ClientPackResidue {
    /** The pack the client used to generate and enable in every instance. */
    public static final String MENU_PACK_NAME = "Vortex-Main-Menu.zip";

    private ClientPackResidue() {}

    /**
     * Rebuilds one {@code resourcePacks:[...]} line without our pack. The value is a comma
     * separated list of quoted names (no Minecraft pack name can contain a comma), so every other
     * pack keeps its place and the list never keeps a dangling comma.
     */
    public static String dropPackEntry(String line) {
        if (line == null) return null;
        int open = line.indexOf('[');
        int close = line.lastIndexOf(']');
        if (open < 0 || close <= open) return line;
        String prefix = line.substring(0, open + 1);
        String inside = line.substring(open + 1, close);
        String suffix = line.substring(close);
        StringBuilder kept = new StringBuilder();
        for (String entry : inside.split(",")) {
            String value = entry.trim();
            if (value.isEmpty() || value.contains(MENU_PACK_NAME)) continue;
            if (kept.length() > 0) kept.append(',');
            kept.append(value);
        }
        return prefix + kept + suffix;
    }

    /** True when this {@code options.txt} line lists the retired pack. */
    public static boolean mentions(String line) {
        return line != null && line.trim().startsWith("resourcePacks:") && line.contains(MENU_PACK_NAME);
    }
}
