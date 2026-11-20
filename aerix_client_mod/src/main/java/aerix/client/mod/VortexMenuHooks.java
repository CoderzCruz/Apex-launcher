package aerix.client.mod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Properties;

/**
 * Vortex title-screen hooks (pure reflection, zero Minecraft compile-time dependencies).
 *
 * Intermediary ids verified against FabricMC/yarn 1.21.11 and stable across the
 * 1.19.4 -> 26.x line (the same strategy AerixClientMod uses for capes):
 *  - MinecraftClient class_310 (getInstance method_1551, currentScreen field_1755)
 *  - TitleScreen    class_442 (splash field_2586, children field_22786)
 *  - SplashTextRenderer class_8519 (text field_44666 -> Component class_2561)
 *  - Component      class_2561 (literal method_43470 -> MutableText class_5250)
 *  - ButtonWidget   class_4185 (builder method_46430, Builder.position method_46433,
 *                    Builder.build method_46431, onPress interface class_4185$class_4241)
 *  - Screen.addDrawableChild method_37063
 *
 * The VORTEX RED FOREST main menu itself is pure resource-pack texture work (shipped by
 * AerixClientPackGenerator from assets/vortex_menu/): logo sheet, red-forest cubemap panorama,
 * nine-slice crimson buttons and splashes. Nothing here replaces Minecraft's screens, so every
 * vanilla button (Singleplayer, Multiplayer, Realms, Mods, Options, Quit) keeps its own action.
 *
 * While a TitleScreen is open this hook only does two cosmetic things:
 *  - repaints the yellow splash crimson ("VORTEX CLIENT!"),
 *  - adds the "VORTEX FULLBRIGHT" button above the title buttons, which flips
 *    config/aerix_live.properties gamma (1.0 <-> 100.0); the animator worker hot-applies it.
 */
public final class VortexMenuHooks {

    private static final String MINECRAFT = "net.minecraft.class_310";
    private static final String TITLE = "net.minecraft.class_442";
    private static final String BUTTON = "net.minecraft.class_4185";
    private static final String ON_PRESS = "net.minecraft.class_4185$class_4241";
    private static final String TEXT = "net.minecraft.class_2561";
    private static final String WIDGET_ARG = "net.minecraft.class_364";
    private static final String SPLASH = "net.minecraft.class_8519";

    private static final String SECTION = "\u00a7";

    private static volatile boolean started = false;
    private static Object lastTitleScreen = null;

    private VortexMenuHooks() {
    }

    public static void start() {
        if (started) return;
        started = true;
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }, "Vortex-Menu-Hooks");
        worker.setDaemon(true);
        worker.start();
    }

    private static void loop() {
        while (true) {
            try {
                Thread.sleep(500L);
                tickOnce();
            } catch (Throwable ignored) {
                // Version without these ids, or the screen changed mid-tick; try again next pass.
            }
        }
    }

    private static void tickOnce() throws Exception {
        Class<?> clsMc = Class.forName(MINECRAFT);
        Object mc = clsMc.getMethod("method_1551").invoke(null);
        if (mc == null) return;
        Field fScreen = findField(clsMc, "field_1755");
        if (fScreen == null) return;
        Object screen = fScreen.get(mc);
        Class<?> clsTitle = Class.forName(TITLE);
        if (screen == null || !clsTitle.isInstance(screen)) {
            lastTitleScreen = null;
            return;
        }

        // Vanilla re-rolls the splash from its supplier; repaint every pass so ours sticks.
        repaintSplash(screen, clsTitle);

        if (screen == lastTitleScreen) return;
        lastTitleScreen = screen;
        addVortexButton(mc, screen, clsTitle);
    }

    private static void repaintSplash(Object screen, Class<?> clsTitle) {
        try {
            Field fSplash = findField(clsTitle, "field_2586");
            if (fSplash == null) return;
            Object splash = fSplash.get(screen);
            if (splash == null) return;
            Field fText = findField(splash.getClass(), "field_44666");
            if (fText == null || !Modifier.isPublic(fText.getModifiers())) return;
            fText.set(splash, literal(SECTION + "c" + SECTION + "lVORTEX CLIENT!"));
        } catch (Throwable ignored) {
        }
    }

    private static void addVortexButton(final Object mc, final Object screen, Class<?> clsTitle) {
        try {
            Field fChildren = findField(clsTitle, "field_22786");
            if (fChildren == null) return;
            Object childrenRaw = fChildren.get(screen);
            if (!(childrenRaw instanceof List)) return;
            List<?> children = (List<?>) childrenRaw;

            Class<?> clsButton = Class.forName(BUTTON);
            Class<?> clsText = Class.forName(TEXT);
            Class<?> clsPress = Class.forName(ON_PRESS);
            Class<?> clsWidget = Class.forName(WIDGET_ARG);

            int baseX = Integer.MIN_VALUE;
            int minTopY = Integer.MAX_VALUE;
            for (Object child : children) {
                if (!clsButton.isInstance(child)) continue;
                Field fx = findField(child.getClass(), "field_22760");
                Field fy = findField(child.getClass(), "field_22761");
                if (fx == null || fy == null) continue;
                int x = fx.getInt(child);
                int y = fy.getInt(child);
                if (baseX == Integer.MIN_VALUE) baseX = x;
                if (y < minTopY) minTopY = y;
            }
            if (baseX == Integer.MIN_VALUE || minTopY == Integer.MAX_VALUE) return;

            Object literal = literal(SECTION + "c" + SECTION + "lVORTEX FULLBRIGHT");
            Object onPress = Proxy.newProxyInstance(
                    VortexMenuHooks.class.getClassLoader(),
                    new Class<?>[]{clsPress},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            if ("toString".equals(method.getName())) return "VortexFullbright";
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                            if ("equals".equals(method.getName())) return proxy == args[0];
                            toggleFullbright(mc);
                            return null;
                        }
                    });

            Object builder = clsButton.getMethod("method_46430", clsText, clsPress)
                    .invoke(null, literal, onPress);
            if (builder == null) return;
            Object positioned = builder.getClass().getMethod("method_46433", int.class, int.class)
                    .invoke(builder, baseX, minTopY - 26);
            Object button = (positioned != null ? positioned : builder).getClass()
                    .getMethod("method_46431").invoke(positioned != null ? positioned : builder);
            if (button == null) return;

            // Mutate the screen's children on the client thread, never from this worker.
            Method execute = mc.getClass().getMethod("execute", Runnable.class);
            final Object target = button;
            final Class<?> widgetArg = clsWidget;
            execute.invoke(mc, new Runnable() {
                @Override
                public void run() {
                    try {
                        screen.getClass().getMethod("method_37063", widgetArg).invoke(screen, target);
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
            // Pre-1.19.4 ids differ; skip the button on those versions.
        }
    }

    private static void toggleFullbright(Object mc) {
        try {
            Field fRunDir = findField(mc.getClass(), "field_1697");
            if (fRunDir == null) return;
            File runDir = (File) fRunDir.get(mc);
            if (runDir == null) return;
            File cfg = new File(runDir, "config/aerix_live.properties");
            File cfgDir = cfg.getParentFile();
            if (cfgDir != null && !cfgDir.isDirectory()) cfgDir.mkdirs();

            Properties props = new Properties();
            if (cfg.isFile()) {
                FileInputStream in = new FileInputStream(cfg);
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
            }
            double gamma = 1.0;
            try {
                gamma = Double.parseDouble(props.getProperty("gamma", "1.0").trim());
            } catch (NumberFormatException ignored) {
            }
            props.setProperty("gamma", gamma >= 10.0 ? "1.0" : "100.0");
            FileOutputStream out = new FileOutputStream(cfg);
            try {
                props.store(out, "Vortex live options");
            } finally {
                out.close();
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object literal(String raw) throws Exception {
        Class<?> clsText = Class.forName(TEXT);
        return clsText.getMethod("method_43470", String.class).invoke(null, raw);
    }

    private static Field findField(Class<?> cls, String name) {
        Class<?> cur = cls;
        while (cur != null && cur != Object.class) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }
}
