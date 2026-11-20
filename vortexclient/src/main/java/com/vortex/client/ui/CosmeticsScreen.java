package com.vortex.client.ui;

import com.vortex.client.VortexClientApp;
import com.vortex.client.game.MinecraftLauncher;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Cosmetics: cape/style selection saved to the instance's
 * config/vortex-client-cosmetics.properties (a real file Vortex mods read).
 */
public class CosmeticsScreen extends JPanel implements VortexClientApp.Navigable {

    private static final String[] CAPES = {
        "None", "Crimson Sovereign", "Vortex Ember", "Void Walker", "Frost Bite",
        "Golden Era", "Neon Drift", "Phantom Wave", "Obsidian Core"
    };

    private final VortexClientApp app;
    private final JComboBox<String> capeBox = new JComboBox<>(CAPES);
    private final JComboBox<String> styleBox = new JComboBox<>(new String[]{
        "Classic", "Slim (Alex)", "Wide (Steve)"
    });
    private final JLabel status = new JLabel(" ");

    public CosmeticsScreen(VortexClientApp app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        Theme.VButton back = new Theme.VButton("‹ Menu", false);
        back.addActionListener(e -> app.navigate("menu"));
        top.add(back, BorderLayout.WEST);
        JLabel title = new JLabel("COSMETICS");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.font(24f, Font.BOLD));
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        form.add(row("Cape", capeBox));
        form.add(row("Skin style", styleBox));
        form.add(Box.createVerticalStrut(12));

        Theme.VButton save = new Theme.VButton("SAVE COSMETICS", true);
        save.setMaximumSize(new Dimension(240, 44));
        save.setAlignmentX(Component.LEFT_ALIGNMENT);
        save.addActionListener(e -> {
            File mc = new File(app.config.get("mcDir",
                MinecraftLauncher.defaultMinecraftDir().getAbsolutePath()));
            File cfgDir = new File(mc, "config");
            cfgDir.mkdirs();
            File out = new File(cfgDir, "vortex-client-cosmetics.properties");
            try (java.io.PrintWriter w = new java.io.PrintWriter(out)) {
                w.println("cape=" + capeBox.getSelectedItem());
                w.println("style=" + styleBox.getSelectedItem());
                w.println("username=" + app.config.get("username", "Player"));
            } catch (Exception ex) {
                status.setText("Save failed: " + ex.getMessage());
                status.setForeground(Theme.RED);
                return;
            }
            app.config.set("cape", String.valueOf(capeBox.getSelectedItem()));
            app.config.set("skinStyle", String.valueOf(styleBox.getSelectedItem()));
            app.config.save();
            status.setText("Saved to config/vortex-client-cosmetics.properties");
            status.setForeground(new Color(0x3DDC84));
        });
        form.add(save);
        form.add(Box.createVerticalStrut(8));
        status.setForeground(Theme.TEXT2);
        status.setFont(Theme.font(12f, Font.PLAIN));
        form.add(status);
        add(form, BorderLayout.CENTER);
    }

    private JComponent row(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel l = new JLabel(label);
        l.setForeground(Theme.TEXT2);
        l.setFont(Theme.font(12f, Font.BOLD));
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    @Override public void onShown() {
        capeBox.setSelectedItem(app.config.get("cape", "None"));
        styleBox.setSelectedItem(app.config.get("skinStyle", "Classic"));
        status.setText("Cosmetics apply to the instance on its next launch.");
        status.setForeground(Theme.TEXT2);
    }
}
