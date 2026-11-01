package net.kdt.pojavlaunch.tasks;


import static net.kdt.pojavlaunch.Architecture.archAsString;
import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncAssetManager {

    /** Reports overall boot progress, 0..100, from a worker thread. */
    public interface BootCallback {
        void onStep(int percent);
    }

    /** How long the splash waits for the unpacking before letting the launcher through anyway. */
    private static final long BOOT_TIMEOUT_MS = 15000L;

    private AsyncAssetManager(){}

    /**
     * Runs the startup unpacking in the order the launcher needs it, reporting real progress so the
     * splash screen can show it, then hands control back on the main thread exactly once. A watchdog
     * releases the splash even if a copy wedges, so a slow disk can never trap the user.
     */
    public static void boot(Context ctx, BootCallback callback, Runnable done) {
        final Handler main = new Handler(Looper.getMainLooper());
        final AtomicBoolean finished = new AtomicBoolean(false);
        final Runnable release = () -> {
            if (!finished.compareAndSet(false, true)) return;
            main.post(done);
        };
        main.postDelayed(release, BOOT_TIMEOUT_MS);
        sExecutorService.execute(() -> {
            try {
                report(callback, 10);
                ProgressLayout.setProgress(ProgressLayout.EXTRACT_COMPONENTS, 0);
                tryUnpackComponent(ctx, "caciocavallo", false);
                report(callback, 26);
                tryUnpackComponent(ctx, "caciocavallo17", false);
                report(callback, 42);
                tryUnpackComponent(ctx, "security", true);
                report(callback, 56);
                tryUnpackComponent(ctx, "forge_installer", true);
                report(callback, 70);
                tryUnpackComponent(ctx, "authlib-injector", true);
                ProgressLayout.clearProgress(ProgressLayout.EXTRACT_COMPONENTS);
                report(callback, 82);

                ProgressLayout.setProgress(ProgressLayout.EXTRACT_SINGLE_FILES, 0);
                try {
                    Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, false);
                    report(callback, 88);
                    Tools.copyAssetFile(ctx, "launcher_profiles.json", Tools.DIR_GAME_NEW, false);
                    report(callback, 94);
                    Tools.copyAssetFile(ctx, "resolv.conf", Tools.DIR_DATA, false);
                } catch (IOException e) {
                    Log.e("AsyncAssetManager", "Failed to unpack critical components !");
                }
                ProgressLayout.clearProgress(ProgressLayout.EXTRACT_SINGLE_FILES);
                report(callback, 100);
            } catch (Throwable t) {
                Log.e("AsyncAssetManager", "Boot sequence failed", t);
            }
            release.run();
        });
    }

    private static void report(BootCallback callback, int percent) {
        if (callback == null) return;
        try {
            callback.onStep(percent);
        } catch (RuntimeException ignored) {
            // A splash that has already gone away must not break the boot.
        }
    }

    /**
     * Attempt to install the java 8 runtime, if necessary
     * @param am App context
     */
    public static void unpackRuntime(AssetManager am) {
        /* Check if JRE is included */
        String rt_version = null;
        String current_rt_version = MultiRTUtils.readInternalRuntimeVersion("Internal");
        try {
            rt_version = Tools.read(am.open("components/jre/version"));
        } catch (IOException e) {
            Log.e("JREAuto", "JRE was not included on this APK.", e);
        }
        String exactJREName = MultiRTUtils.getExactJreName(8);
        if(current_rt_version == null && exactJREName != null && !exactJREName.equals("Internal")/*this clause is for when the internal runtime is goofed*/) return;
        if(rt_version == null) return;
        if(rt_version.equals(current_rt_version)) return;

        // Install the runtime in an async manner, hope for the best
        String finalRt_version = rt_version;
        sExecutorService.execute(() -> {

            try {
                MultiRTUtils.installRuntimeNamedBinpack(
                        am.open("components/jre/universal.tar.xz"),
                        am.open("components/jre/bin-" + archAsString(Tools.DEVICE_ARCHITECTURE) + ".tar.xz"),
                        "Internal", finalRt_version);
                MultiRTUtils.postPrepare("Internal");
            }catch (IOException e) {
                Log.e("JREAuto", "Internal JRE unpack failed", e);
            }
        });
    }

    /** Unpack single files, with no regard to version tracking */
    public static void unpackSingleFiles(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_SINGLE_FILES, 0);
        sExecutorService.execute(() -> {
            try {
                Tools.copyAssetFile(ctx, "default.json", Tools.CTRLMAP_PATH, false);
                Tools.copyAssetFile(ctx, "launcher_profiles.json", Tools.DIR_GAME_NEW, false);
                Tools.copyAssetFile(ctx,"resolv.conf",Tools.DIR_DATA, false);
            } catch (IOException e) {
                Log.e("AsyncAssetManager", "Failed to unpack critical components !");
            }
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_SINGLE_FILES);
        });
    }

    public static void unpackComponents(Context ctx){
        ProgressLayout.setProgress(ProgressLayout.EXTRACT_COMPONENTS, 0);
        sExecutorService.execute(() -> {
            tryUnpackComponent(ctx, "caciocavallo", false);
            tryUnpackComponent(ctx, "caciocavallo17", false);

            tryUnpackComponent(ctx, "security", true);
            tryUnpackComponent(ctx, "forge_installer", true);
            tryUnpackComponent(ctx, "authlib-injector", true);
            ProgressLayout.clearProgress(ProgressLayout.EXTRACT_COMPONENTS);
        });
    }

    private static String readInstalledComponentVersion(File componentRoot) {
        File localVersionFile = new File(componentRoot, "version");
        try(FileInputStream fileInputStream = new FileInputStream(localVersionFile)) {
            return IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
        }catch (IOException ignored) {}
        return null;
    }

    private static String readBuiltinComponentVersion(AssetManager assetManager, String componentName) {
        String componentVersionLocation = "components/"+componentName+"/version";
        try (InputStream inputStream = assetManager.open(componentVersionLocation)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }catch (IOException ignored) {}
        return null;
    }

    private static void tryUnpackComponent(Context ctx, String component, boolean privateDirectory) {
        try {
            unpackComponent(ctx, component, privateDirectory);
        }catch (IOException e) {
            Log.e("AssetUnpacker", "Failed to unpack component "+component, e);
        }
    }

    private static void unpackComponent(Context ctx, String component, boolean privateDirectory) throws IOException {
        AssetManager am = ctx.getAssets();
        String rootDir = privateDirectory ? Tools.DIR_DATA : Tools.DIR_GAME_HOME;
        File componentTarget = new File(rootDir, component);
        String installedVersion = readInstalledComponentVersion(componentTarget);
        String builtinVersion = readBuiltinComponentVersion(am, component);
        if(installedVersion != null && installedVersion.equals(builtinVersion)) {
            Log.i("AssetUnpacker", "Component "+component+" is up-to-date, continuing...");
            return;
        }
        Log.i("AssetUnpacker", "Updating "+component);

        if(componentTarget.exists()) {
            FileUtils.deleteDirectory(componentTarget);
        }
        if(!componentTarget.mkdirs()) {
            throw new IOException("Failed to create directory for "+component);
        }

        String componentSource = "components/" + component;

        String[] fileList = am.list(componentSource);
        for (String fileName : fileList) {
            if(fileName.equals("version")) continue;
            String sourcePath = componentSource + "/" + fileName;
            Tools.copyAssetFile(ctx, sourcePath, componentTarget.getAbsolutePath(), true);
        }

        // Always write the version file separately after extracting everything else, to improve
        // reliability.
        Tools.write(new File(componentTarget, "version"), builtinVersion);
    }

    public static void extractDefaultSettings(Context context, File gamedir)  {
        try {
            String gameDirPath = gamedir.getAbsolutePath();
            Tools.copyAssetFile(context, "options.txt", gameDirPath, false);
        }catch (IOException e) {
            Tools.showError(context, e);
        }
    }
}
