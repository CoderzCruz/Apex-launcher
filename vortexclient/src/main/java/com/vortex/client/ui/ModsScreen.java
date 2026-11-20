package com.vortex.client.ui;

import com.vortex.client.VortexClientApp;
import com.vortex.client.game.MinecraftLauncher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Real mod manager: lists <mcDir>/mods/*.jar, toggles = rename to .jar.disabled (persists). */
public class ModsScreen extends JPanel implements VortexClientApp.Navigable {

    private enum Category { ALL, COMBAT, VISUALS, MOVEMENT, HUD, UTILITY, PERFORMANCE }

    private final VortexClientApp app;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final Theme.VField search = new Theme.VField("Search mods");
    private final JLabel status = new JLabel(" ");
    private Category category = Category.ALL;
    private List<File> jars = new ArrayList<>();

    public ModsScreen(VortexClientApp app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        Theme.VButton back = new Theme.VButton("‹ Menu", false);
        back.addActionListener(e -> app.navigate("menu"));
        top.add(back, BorderLayout.WEST);
        JLabel title = new JLabel("MODS");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.font(24f, Font.BOLD));
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Category chips + search
        JPanel filters = new JPanel(new BorderLayout(8, 0));
        filters.setOpaque(false);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        for (Category c : Category.values()) {
            Theme.VButton chip = new Theme.VButton(c.name(), false);
            chip.addActionListener(e -> { category = c; refresh(); });
            chips.add(chip);
        }
        filters.add(chips, BorderLayout.CENTER);
        search.setMaximumSize(new Dimension(200, 34));
        search.setPreferredSize(new Dimension(200, 34));
        search.addActionListener(e -> refresh());
        filters.add(search, BorderLayout.EAST);
        add(filters, BorderLayout.NORTH);

        // SPACE toggles the selected mod's jar <-> jar.disabled (persists on disk).
        list.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    int i = list.getSelectedIndex();
                    if (i >= 0 && i < jars.size()) {
                        File f = jars.get(i);
                        if (f.getName().endsWith(".jar.disabled")) {
                            f.renameTo(new File(f.getParent(), f.getName().substring(0, f.getName().length() - 9)));
                        } else {
                            f.renameTo(new File(f.getParent(), f.getName() + ".disabled"));
                        }
                        refresh();
                    }
                }
            }
        });

        list.setBackground(Theme.CARD);
        list.setForeground(Theme.TEXT);
        list.setSelectionBackground(Theme.RED);
        list.setSelectionForeground(Color.WHITE);
        list.setFont(Theme.font(14f, Font.PLAIN));
        add(new JScrollPane(list) {{
            setOpaque(false); getViewport().setOpaque(false); setBackground(Theme.BG);
        }}, BorderLayout.CENTER);

        status.setForeground(Theme.TEXT2);
        status.setFont(Theme.font(12f, Font.PLAIN));
        add(status, BorderLayout.SOUTH);
    }

    private File modsDir() {
        return new File(mcDir(), "mods");
    }

    private File mcDir() {
        return new File(app.config.get("mcDir",
            MinecraftLauncher.defaultMinecraftDir().getAbsolutePath()));
    }

    private String categoryOf(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("sodium") || n.contains("lithium") || n.contains("ferrite")
            || n.contains("immediatelyfast") || n.contains("entityculling")
            || n.contains("dynamic-fps") || n.contains("modernfix") || n.contains("krypton"))
            return Category.PERFORMANCE.name();
        if (n.contains("capes") || n.contains("cosmetic") || n.contains("skin")
            || n.contains("iris") || n.contains("shader") || n.contains("sodium"))
            return Category.VISUALS.name();
        if (n.contains("keystroke") || n.contains("hud") || n.contains("fps")
            || n.contains("cps") || n.contains("minihud"))
            return Category.HUD.name();
        if (n.contains("pvp") || n.contains("combat") || n.contains("aim")
            || n.contains("knockback") || n.contains("hit"))
            return Category.COMBAT.name();
        if (n.contains("move") || n.contains("sprint") || n.contains("step"))
            return Category.MOVEMENT.name();
        if (n.contains("util") || n.contains("qol") || n.contains("inventory"))
            return Category.UTILITY.name();
        return Category.UTILITY.name();
    }

    private void refresh() {
        model.clear();
        jars.clear();
        File dir = modsDir();
        File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".jar")
            || n.toLowerCase(Locale.ROOT).endsWith(".jar.disabled"));
        if (files != null) {
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            for (File f : files) {
                boolean enabled = f.getName().toLowerCase(Locale.ROOT).endsWith(".jar");
                String base = enabled ? f.getName() : f.getName().substring(0, f.getName().length() - 9);
                if (q.isEmpty() || base.toLowerCase(Locale.ROOT).contains(q)) {
                    if (category != Category.ALL && !categoryOf(base).equals(category.name())) continue;
                    jars.add(f);
                    model.addElement((enabled ? "● " : "○ ") + base.replace(".jar", ""));
                }
            }
        }
        status.setText(jars.isEmpty()
            ? "No mods in " + dir.getAbsolutePath() + " — drop jars there and refresh."
            : jars.size() + " mod(s) · select + press SPACE to enable/disable");
    }

    @Override public void onShown() { refresh(); }
}
