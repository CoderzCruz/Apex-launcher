package aerix.client.mod;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * Roblox-style owner chat alert: polls the in-game ChatHud for new messages and, when one
 * contains the owner's name (config/vortex_owner_name.txt, written by the launcher at launch),
 * writes config/vortex_owner_alert.txt for the Android overlay to show as a banner.
 *
 * Everything is resolved dynamically (ChatHud by field type class_338 on the Minecraft client,
 * messages via the stable field_2061) so it keeps working across versions; text extraction
 * tries Component.getString (intermediary method_43494) and falls back to toString.
 */
public final class OwnerChatWatch {
    private static final String MC = "net.minecraft.class_310";
    private static final String CHAT_HUD = "net.minecraft.class_338";
    private static final String COMPONENT = "net.minecraft.class_2561";

    private static Field chatHudField;
    private static Field messagesField;
    private static Method getStringMethod;
    private static Field componentField;
    private static boolean resolved;

    private static String ownerName = "";
    private static long ownerNameCheckedAt = 0L;
    private static String lastSeen = "";

    private OwnerChatWatch() {
    }

    public static void tick(Object mc, File runDir) {
        try {
            if (mc == null || runDir == null) return;
            long now = System.currentTimeMillis();

            if (now - ownerNameCheckedAt > 2000L) {
                ownerNameCheckedAt = now;
                File nameFile = new File(runDir, "config/vortex_owner_name.txt");
                ownerName = nameFile.isFile()
                    ? new String(Files.readAllBytes(nameFile.toPath()), StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT)
                    : "";
            }
            if (ownerName.isEmpty()) return;

            if (!resolved) {
                resolved = true;
                resolve(mc.getClass());
            }
            if (chatHudField == null || messagesField == null) return;

            Object hud = chatHudField.get(mc);
            if (hud == null) return;
            Object listObj = messagesField.get(hud);
            if (!(listObj instanceof List)) return;
            List<?> messages = (List<?>) listObj;
            if (messages.isEmpty()) return;
            Object last = messages.get(messages.size() - 1);
            if (last == null) return;

            String text = extractText(last);
            if (text == null || text.isEmpty()) return;
            String key = text.toLowerCase(Locale.ROOT);
            if (key.equals(lastSeen)) return;
            lastSeen = key;
            if (!key.contains(ownerName)) return;

            File alert = new File(runDir, "config/vortex_owner_alert.txt");
            File parent = alert.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.write(alert.toPath(), (now + "\n" + text + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
            // Chat internals differ between versions; a miss just means no banner.
        }
    }

    private static void resolve(Class<?> mcClass) {
        try {
            Class<?> hudClass = Class.forName(CHAT_HUD);
            Class<?> compClass = Class.forName(COMPONENT);
            for (Class<?> c = mcClass; c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() == null || !f.getType().equals(hudClass)) continue;
                    f.setAccessible(true);
                    chatHudField = f;
                    break;
                }
                if (chatHudField != null) break;
            }
            messagesField = hudClass.getDeclaredField("field_2061");
            messagesField.setAccessible(true);
            try {
                getStringMethod = compClass.getMethod("method_43494");
            } catch (NoSuchMethodException ignored) {
                getStringMethod = null;
            }
            // ChatHudLine keeps its Component in a field of type class_2561 (name varies).
            try {
                Class<?> lineClass = Class.forName("net.minecraft.class_303");
                for (Field f : lineClass.getDeclaredFields()) {
                    if (f.getType() != null && compClass.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        componentField = f;
                        break;
                    }
                }
            } catch (Throwable ignored) {
                componentField = null;
            }
        } catch (Throwable ignored) {
            chatHudField = null;
        }
    }

    private static String extractText(Object line) {
        try {
            if (componentField != null) {
                Object comp = componentField.get(line);
                if (comp != null) {
                    if (getStringMethod != null) {
                        Object s = getStringMethod.invoke(comp);
                        if (s instanceof String && !((String) s).isEmpty()) return (String) s;
                    }
                    String s = String.valueOf(comp);
                    if (!s.isEmpty()) return s;
                }
            }
        } catch (Throwable ignored) {
        }
        return String.valueOf(line);
    }
}
