package com.vortex.client.ui;

import com.vortex.client.VortexClientApp;

import javax.swing.*;
import java.awt.*;

/** VORTEX client-style main menu: header, hero, action grid, character pane, footer. */
public class MainMenuScreen extends JPanel implements VortexClientApp.Repaintable, VortexClientApp.Navigable {

    private final VortexClientApp app;
    private JLabel userLabel;

    public MainMenuScreen(VortexClientApp app) {
        this.app = app;
        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG);

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 8, 22));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        JLabel logo = new JLabel("VORTEX");
        logo.setForeground(Theme.RED);
        logo.setFont(Theme.font(22f, Font.BOLD));
        JLabel logoSub = new JLabel("CLIENT");
        logoSub.setForeground(Theme.TEXT);
        logoSub.setFont(Theme.font(22f, Font.BOLD));
        brand.add(logo); brand.add(logoSub);
        header.add(brand, BorderLayout.WEST);

        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        account.setOpaque(false);
        userLabel = new JLabel();
        userLabel.setForeground(Theme.TEXT);
        userLabel.setFont(Theme.font(13f, Font.BOLD));
        account.add(userLabel);

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.CARD); g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.RED); g2.setStroke(new BasicStroke(1.6f));
                g2.drawOval(1, 1, getWidth() - 2, getHeight() - 2);
                String name = app.config.get("username", "Player");
                g2.setColor(Theme.TEXT); g2.setFont(Theme.font(14f, Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                String initials = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                g2.drawString(initials, (getWidth() - fm.stringWidth(initials)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        avatar.setPreferredSize(new Dimension(34, 34));
        account.add(avatar);

        Theme.VButton settingsBtn = new Theme.VButton("⚙", false);
        settingsBtn.setPreferredSize(new Dimension(40, 32));
        settingsBtn.addActionListener(e -> app.navigate("settings"));
        account.add(settingsBtn);
        header.add(account, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Center: hero + actions | character pane ──
        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 22, 6, 22));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel hero = new JLabel("VORTEX");
        hero.setForeground(Color.WHITE);
        hero.setFont(Theme.font(54f, Font.BOLD));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(hero);
        left.add(Strut.v(6));

        JLabel welcome = new JLabel();
        welcome.setForeground(Theme.TEXT2);
        welcome.setFont(Theme.font(15f, Font.PLAIN));
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(welcome);
        left.add(Strut.v(26));

        String[][] actions = {
            {"SINGLEPLAYER", "singleplayer"},
            {"MULTIPLAYER", "multiplayer"},
            {"MODS", "mods"},
            {"SETTINGS", "settings"},
            {"COSMETICS", "cosmetics"},
        };
        for (String[] action : actions) {
            Theme.VButton b = new Theme.VButton(action[0], action[0].equals("SINGLEPLAYER"));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(340, 46));
            b.setPreferredSize(new Dimension(340, 46));
            b.addActionListener(e -> app.navigate(action[1]));
            left.add(b);
            left.add(Strut.v(9));
        }

        // Extra navigation from the spec's screen list.
        JPanel extra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        extra.setOpaque(false);
        Theme.VButton perf = new Theme.VButton("PERFORMANCE", false);
        perf.addActionListener(e -> app.navigate("performance"));
        Theme.VButton accounts = new Theme.VButton("ACCOUNTS", false);
        accounts.addActionListener(e -> app.navigate("accounts"));
        extra.add(perf); extra.add(accounts);
        left.add(extra);

        center.add(left, BorderLayout.WEST);

        // Right: character preview pane (stylised blocky figure over the dark scene).
        JPanel character = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Dark environment panel.
                g2.setPaint(new GradientPaint(0, 0, new Color(0x141414), 0, h, Theme.BG));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(new Color(0x242424));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                // Blocky character (silhouette art, centred).
                int cx = w / 2, cy = h / 2 + 10, u = Math.max(6, Math.min(w, h) / 16);
                g2.setColor(new Color(0x2E2E33));
                g2.fillRect(cx - u, cy - u * 6, u * 2, u * 2);            // head
                g2.fillRect(cx - u, cy - u * 4, u * 2, u * 4);            // body
                g2.fillRect(cx - u * 2, cy - u * 4, u, u * 3);            // arms
                g2.fillRect(cx + u, cy - u * 4, u, u * 3);
                g2.fillRect(cx - u, cy, u, u * 4);                        // legs
                g2.fillRect(cx, cy, u, u * 4);
                g2.setColor(Theme.RED);
                g2.fillRect(cx - u, cy - u * 6, u * 2, u);                // head accent
                g2.setColor(Theme.TEXT2);
                g2.setFont(Theme.font(11f, Font.PLAIN));
                FontMetrics fm = g2.getFontMetrics();
                String caption = "Player preview";
                g2.drawString(caption, (w - fm.stringWidth(caption)) / 2, h - 16);
            }
        };
        character.setPreferredSize(new Dimension(300, 0));
        center.add(character, BorderLayout.EAST);

        add(center, BorderLayout.CENTER);

        // ── Footer ──
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 22, 14, 22));
        JLabel versionLeft = new JLabel();
        versionLeft.setForeground(Theme.TEXT2);
        versionLeft.setFont(Theme.font(11f, Font.PLAIN));
        footer.add(versionLeft, BorderLayout.WEST);
        JLabel status = new JLabel();
        status.setForeground(Theme.RED);
        status.setFont(Theme.font(11f, Font.BOLD));
        footer.add(status, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        this.versionLeft = versionLeft;
        this.statusLabel = status;
        this.welcomeLabel = welcome;
    }

    private final JLabel versionLeft;
    private final JLabel statusLabel;
    private final JLabel welcomeLabel;

    @Override public void onShown() {
        String name = app.config.get("username", "Player");
        userLabel.setText(name);
        welcomeLabel.setText("Welcome back, " + name);
        String mcDir = app.config.get("mcDir", "");
        String mcVersion = app.config.get("mcVersion", "");
        versionLeft.setText("Minecraft " + (mcVersion.isEmpty() ? "-" : mcVersion)
            + "  ·  Vortex Client " + VortexClientApp.CLIENT_VERSION);
        boolean configured = !mcDir.isEmpty() && new java.io.File(mcDir).isDirectory();
        statusLabel.setText(configured ? "● READY" : "○ SET UP IN SETTINGS");
        statusLabel.setForeground(configured ? new Color(0x3DDC84) : Theme.RED);
    }

    @Override public void onTick(long tick) { repaint(); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Theme.paintBackground(g, this, app.tick());
    }

    private static final class Strut {
        static Component v(int h) { return Box.createVerticalStrut(h); }
    }
}
