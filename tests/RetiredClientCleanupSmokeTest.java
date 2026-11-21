import java.lang.reflect.Method;

/**
 * Retiring the Vortex Client must not leave its title-screen pack behind in options.txt.
 * Compiled against the plain-Java helper, so the CI needs no Android SDK for this check.
 */
public class RetiredClientCleanupSmokeTest {
    private static Method drop;

    public static void main(String[] args) throws Exception {
        Class<?> owner = Class.forName("net.kdt.pojavlaunch.aerix.bridge.ClientPackResidue");
        drop = owner.getDeclaredMethod("dropPackEntry", String.class);

        expect("resourcePacks:[\"vanilla\",\"file/Vortex-Main-Menu.zip\"]", "resourcePacks:[\"vanilla\"]");
        expect("resourcePacks:[\"file/Vortex-Main-Menu.zip\",\"vanilla\"]", "resourcePacks:[\"vanilla\"]");
        expect("resourcePacks:[\"file/Vortex-Main-Menu.zip\"]", "resourcePacks:[]");
        expect("resourcePacks:[]", "resourcePacks:[]");
        expect("resourcePacks:[\"vanilla\"]", "resourcePacks:[\"vanilla\"]");
        expect("resourcePacks:[\"vanilla\",\"file/Vortex-Main-Menu.zip\",\"file/Fresh.zip\"]",
                "resourcePacks:[\"vanilla\",\"file/Fresh.zip\"]");
        expect("resourcePacks:[\"file/A.zip\",\"file/Vortex-Main-Menu.zip.disabled\"]",
                "resourcePacks:[\"file/A.zip\"]");
        // Non-pack lines are never touched, even when they mention the file name.
        expect("resourcePacks:[\"vanilla\"]", "resourcePacks:[\"vanilla\"]");

        System.out.println("RetiredClientCleanup: options.txt pack removal passed");
    }

    private static void expect(String input, String wanted) throws Exception {
        String got = (String) drop.invoke(null, input);
        if (!wanted.equals(got)) {
            throw new AssertionError("options line\n  in:  " + input + "\n  got: " + got + "\n  want:" + wanted);
        }
    }
}
