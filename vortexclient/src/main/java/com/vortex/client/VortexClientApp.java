package com.vortex.client;

import com.vortex.client.data.ClientConfig;
import com.vortex.client.ui.MainMenuScreen;
import com.vortex.client.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * VORTEX CLIENT — standalone desktop Minecraft client shell.
 * One JFrame, a CardLayout screen registry, shared config. Every screen is a real
 * functional surface (no placeholder buttons) navigating through navigate(id).
 */
public class VortexClientApp extends JFrame {

    public static final String CLIENT_VERSION = "1.0.0";

    private static VortexClientApp instance;

    public final ClientConfig config = ClientConfig.load();
    private final CardLayout cards = new CardLayout();
    private final JPanel screenHost = new JPanel(cards);
    private long tick = 0;
    private Timer particleTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            instance = new VortexClientApp();
            instance.setVisible(true);
        });
    }

    public static VortexClientApp get() { return instance; }

    public VortexClientApp() {
        super("Vortex Client");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new Dimension(940, 620));
        setLocationRelativeTo(null);
        setBackground(Theme.BG);
        getContentPane().setBackground(Theme.BG);
        screenHost.setBackground(Theme.BG);
        add(screenHost, BorderLayout.CENTER);

        // Screen registry — every spec screen is registered here.
        screenHost.add(new MainMenuScreen(this), "menu");
        screenHost.add(new com.vortex.client.ui.SingleplayerScreen(this), "singleplayer");
        screenHost.add(new com.vortex.client.ui.MultiplayerScreen(this), "multiplayer");
        screenHost.add(new com.vortex.client.ui.ModsScreen(this), "mods");
        screenHost.add(new com.vortex.client.ui.CosmeticsScreen(this), "cosmetics");
        screenHost.add(new com.vortex.client.ui.PerformanceScreen(this), "performance");
        screenHost.add(new com.vortex.client.ui.SettingsScreen(this), "settings");
        screenHost.add(new com.vortex.client.ui.AccountsScreen(this), "accounts");

        // One shared animation clock for particles/transitions (low-end friendly:15fps).
        particleTimer = new Timer(66, e -> {
            tick++;
            Component[] comps = screenHost.getComponents();
            if (comps.length > 0 && comps[0] instanceof Repaintable) {
                ((Repaintable) comps[0]).onTick(tick);
            }
            screenHost.repaint();
        });
        particleTimer.start();

        navigate("menu");
    }

    public void navigate(String screenId) {
        cards.show(screenHost, screenId);
        // Let the screen run its own enter logic.
        Component[] comps = screenHost.getComponents();
        for (Component c : comps) {
            if (c instanceof Navigable) ((Navigable) c).onShown();
        }
        screenHost.repaint();
    }

    /** Screens that react to the shared tick (particles etc). */
    public interface Repaintable { void onTick(long tick); }

    /** Screens that react to navigation. */
    public interface Navigable { void onShown(); }

    public long tick() { return tick; }
}
