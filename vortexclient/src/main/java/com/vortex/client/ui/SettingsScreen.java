package com.vortex.client.ui;

import com.vortex.client.VortexClientApp;
import com.vortex.client.game.MinecraftLauncher;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** Settings: Minecraft folder, installed version, java, username, resolution + save. */
public class SettingsScreen extends JPanel implements VortexClientApp.Navigable {

    private final VortexClientApp app;
    private final Theme.VField mcDirField = new Theme.VField("Path to .minecraft folder");
    private final Theme.VField javaField = new Theme.VField("Java executable (blank = bundled JRE)");
    private final Theme.VField userField = new Theme.VField("Username");
    private final Theme.VField resField = new Theme.VField("Resolution e.g. 1280x720");
    private final JComboBox<String> versionBox = new JComboBox<>();
    private final JLabel status = new JLabel(" ");

    public SettingsScreen(VortexClientApp app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(navBack(), BorderLayout.WEST);
        JLabel title = new JLabel("SETTINGS");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.font(24f, Font.BOLD));
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

        form.add(labeled("Minecraft folder", mcDirField));
        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            String cur = mcDirField.getText();
            if (!cur.isEmpty()) chooser.setCurrentDirectory(new File(cur));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                mcDirField.setText(chooser.getSelectedFile().getAbsolutePath());
                refreshVersions();
            }
        });
        form.add(withButton(mcDirField, browse));
        form.add(labeled("Minecraft version (installed)", versionBox));
        form.add(labeled("Java executable", javaField));
        form.add(labeled("Username", userField));
        form.add(labeled("Resolution", resField));
        form.add(Box.createVerticalStrut(14));

        Theme.VButton save = new Theme.VButton("SAVE SETTINGS", true);
        save.setMaximumSize(new Dimension(240, 44));
        save.setAlignmentX(Component.LEFT_ALIGNMENT);
        save.addActionListener(e -> {
            app.config.set("mcDir", mcDirField.getText().trim());
            app.config.set("javaPath", javaField.getText().trim());
            app.config.set("username", userField.getText().trim());
            app.config.set("resolution", resField.getText().trim());
            Object v = versionBox.getSelectedItem();
            if (v != null) app.config.set("mcVersion", String.valueOf(v));
            app.config.save();
            status.setText("Saved.");
            status.setForeground(new Color(0x3DDC84));
        });
        form.add(save);
        form.add(Box.createVerticalStrut(8));

        status.setForeground(Theme.TEXT2);
        status.setFont(Theme.font(12f, Font.PLAIN));
        form.add(status);
        add(form, BorderLayout.CENTER);
    }

    private void refreshVersions() {
        versionBox.removeAllItems();
        File mc = new File(mcDirField.getText().trim().isEmpty()
            ? MinecraftLauncher.defaultMinecraftDir().getAbsolutePath()
            : mcDirField.getText().trim());
        for (String v : MinecraftLauncher.installedVersions(mc)) versionBox.addItem(v);
        String saved = app.config.get("mcVersion", "");
        if (!saved.isEmpty()) versionBox.setSelectedItem(saved);
    }

    @Override public void onShown() {
        mcDirField.setText(app.config.get("mcDir",
            MinecraftLauncher.defaultMinecraftDir().getAbsolutePath()));
        javaField.setText(app.config.get("javaPath", ""));
        userField.setText(app.config.get("username", "Player"));
        resField.setText(app.config.get("resolution", "1280x720"));
        refreshVersions();
        status.setText(MinecraftLauncher.isConfigured(app.config)
            ? "Ready — use the Main Menu to play." : "Pick an installed Minecraft version.");
        status.setForeground(MinecraftLauncher.isConfigured(app.config)
            ? new Color(0x3DDC84) : Theme.RED);
    }

    private JComponent navBack() {
        Theme.VButton back = new Theme.VButton("‹ Menu", false);
        back.addActionListener(e -> app.navigate("menu"));
        return back;
    }

    private JComponent labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        JLabel l = new JLabel(label);
        l.setForeground(Theme.TEXT2);
        l.setFont(Theme.font(12f, Font.BOLD));
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JComponent withButton(JComponent field, JButton button) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        p.add(field, BorderLayout.CENTER);
        p.add(button, BorderLayout.EAST);
        return p;
    }
}
