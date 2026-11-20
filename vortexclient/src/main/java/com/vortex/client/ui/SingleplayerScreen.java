package com.vortex.client.ui;

import com.vortex.client.VortexClientApp;
import com.vortex.client.game.MinecraftLauncher;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** Real saves list from the configured .minecraft/saves + launch + open folder. */
public class SingleplayerScreen extends JPanel implements VortexClientApp.Navigable {

    private final VortexClientApp app;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JLabel status = new JLabel(" ");

    public SingleplayerScreen(VortexClientApp app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        Theme.VButton back = new Theme.VButton("‹ Menu", false);
        back.addActionListener(e -> app.navigate("menu"));
        top.add(back, BorderLayout.WEST);
        JLabel title = new JLabel("SINGLEPLAYER");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.font(24f, Font.BOLD));
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        status.setForeground(Theme.TEXT2);
        status.setFont(Theme.font(12f, Font.PLAIN));
        add(status, BorderLayout.NORTH);

        list.setBackground(Theme.CARD);
        list.setForeground(Theme.TEXT);
        list.setSelectionBackground(Theme.RED);
        list.setSelectionForeground(Color.WHITE);
        list.setFont(Theme.font(14f, Font.PLAIN));
        add(new JScrollPane(list) {{
            setOpaque(false); getViewport().setOpaque(false); setBackground(Theme.BG);
        }}, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(btn("PLAY", true, () -> {
            if (!MinecraftLauncher.isConfigured(app.config)) {
                status.setText("Set up Minecraft folder + version in Settings first.");
                return;
            }
            try {
                MinecraftLauncher.launch(app.config, null);
                status.setText("Launching Minecraft — worlds are inside the game menu.");
            } catch (Exception ex) {
                status.setText("Launch failed: " + ex.getMessage());
            }
        }));
        actions.add(btn("REFRESH WORLDS", false, this::refresh));
        actions.add(btn("OPEN SAVES FOLDER", false, () -> {
            File saves = new File(mcDir(), "saves");
            saves.mkdirs();
            try {
                Desktop.getDesktop().open(saves);
            } catch (Exception ex) {
                status.setText(saves.getAbsolutePath());
            }
        }));
        add(actions, BorderLayout.SOUTH);
    }

    private File mcDir() {
        return new File(app.config.get("mcDir",
            MinecraftLauncher.defaultMinecraftDir().getAbsolutePath()));
    }

    private Theme.VButton btn(String text, boolean primary, Runnable action) {
        Theme.VButton b = new Theme.VButton(text, primary);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void refresh() {
        model.clear();
        File saves = new File(mcDir(), "saves");
        File[] worlds = saves.listFiles(File::isDirectory);
        if (worlds != null) {
            for (File w : worlds) model.addElement(w.getName());
        }
        status.setText(model.isEmpty()
            ? "No worlds found in " + saves.getAbsolutePath()
            : model.size() + " world(s) — select in-game after launch");
    }

    @Override public void onShown() { refresh(); }
}
