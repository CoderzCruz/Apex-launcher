package net.kdt.pojavlaunch.aerix.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;

import java.io.File;
import java.util.List;

/**
 * One-time sweep of the retired Vortex Client feature.
 *
 * The client used to drop a generated title-screen pack ({@code Vortex-Main-Menu.zip}) and a
 * client mod jar into every instance, and it switched that pack on inside {@code options.txt}.
 * Removing the code alone would leave those files behind, so an already-installed launcher would
 * keep showing the red menu and keep loading a mod whose classes no longer exist. This class
 * deletes them, drops them from {@code options.txt} and clears the client preferences once.
 *
 * Everything here is best-effort: a read-only game directory must never stop the launcher from
 * starting, and a second run does nothing.
 */
public final class RetiredClientCleanup {
    private static final String TAG = "RetiredClientCleanup";
    private static final String MARKER = "vortexClientRetiredCleanupDone";
    private static final String MENU_PACK_NAME = ClientPackResidue.MENU_PACK_NAME;
    private static final String[] RETIRED_MOD_JARS = {
            "aerix-client-animator.jar", "aerix-client-mod.jar", "vortex-client.jar"
    };

    private RetiredClientCleanup() {}

    /** Deletes the retired client files in every known instance, once per install. */
    public static void clean(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("pojavpref", Context.MODE_PRIVATE);
        if (prefs.getBoolean(MARKER, false)) return;

        try {
            List<Instance> instances = Instances.loadAllInstances();
            for (Instance instance : instances) cleanGameDir(instance.getGameDirectory());
        } catch (Throwable throwable) {
            Log.w(TAG, "Could not list instances while retiring the client files", throwable);
        }

        prefs.edit().putBoolean(MARKER, true).apply();
    }

    private static void cleanGameDir(File gameDir) {
        if (gameDir == null || !gameDir.isDirectory()) return;

        delete(new File(new File(gameDir, "resourcepacks"), MENU_PACK_NAME));
        File mods = new File(gameDir, "mods");
        for (String jar : RETIRED_MOD_JARS) delete(new File(mods, jar));

        // The pack was enabled through options.txt; drop it (and any duplicate) from the pack list
        // so Minecraft does not report a missing pack on every boot.
        stripFromOptions(new File(gameDir, "options.txt"));
    }

    private static void stripFromOptions(File options) {
        if (!options.isFile()) return;
        try {
            String text = Tools.read(options);
            if (text == null || !text.contains(ClientPackResidue.MENU_PACK_NAME)) return;
            StringBuilder rebuilt = new StringBuilder(text.length());
            for (String line : text.split("\n", -1)) {
                rebuilt.append(ClientPackResidue.mentions(line)
                        ? ClientPackResidue.dropPackEntry(line)
                        : line).append('\n');
            }
            String result = rebuilt.toString();
            // split(-1) kept one trailing empty element; drop it before writing back.
            if (result.endsWith("\n")) result = result.substring(0, result.length() - 1);
            Tools.write(options, result);
        } catch (Throwable throwable) {
            Log.w(TAG, "Could not clean " + options, throwable);
        }
    }

    private static void delete(File file) {
        try {
            if (file.isFile() && !file.delete()) Log.w(TAG, "Could not delete " + file);
        } catch (Throwable throwable) {
            Log.w(TAG, "Could not delete " + file, throwable);
        }
    }
}
