package com.vortex.client.ui;

import com.vortex.client.VortexClientApp;

import javax.swing.*;
import java.awt.*;

/** Real performance settings — persisted and applied at launch (JVM args + options.txt). */
public class PerformanceScreen extends JPanel implements VortexClientApp.Navigable {

    private final VortexClientApp app;
    private final JSlider ram = new JSlider(512, 8192, 2048);
    private final JSlider fps = new JSlider(30, 260, 120);
    private final JSlider renderDist = new JSlider(2, 32, 8);
    private final JComboBox<String> graphics = new JComboBox<>(new String[]{"FAST", "FANCY", "FABULOUS"});
    private final JComboBox<String> particles = new JComboBox<>(new String[]{"ALL", "DECREASED", "MINIMAL"});
    private final JCheckBox vsync = new JCheckBox("VSync");
    private final JLabel status = new JLabel(" ");

    public PerformanceScreen(VortexClientApp app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        Theme.VButton back = new Theme.VButton("‹ Menu", false);
        back.addActionListener(e -> app.navigate("menu"));
        top.add(back, BorderLayout.WEST);
        JLabel title = new JLabel("PERFORMANCE");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.font(24f, Font.BOLD));
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        form.add(slider("RAM allocation (MB)", ram, v -> ram.setBorder(BorderFactory.createTitledBorder(v + " MB"))));
        ram.setBorder(BorderFactory.createTitledBorder(ram.getValue() + " MB"));
        form.add(slider("FPS limit", fps, v -> fps.setBorder(BorderFactory.createTitledBorder(v + " FPS"))));
        fps.setBorder(BorderFactory.createTitledBorder(fps.getValue() + " FPS"));
        form.add(slider("Render distance (chunks)", renderDist,
            v -> renderDist.setBorder(BorderFactory.createTitledBorder(v + " chunks"))));
        renderDist.setBorder(BorderFactory.createTitledBorder(renderDist.getValue() + " chunks"));

        form.add(row("Graphics", graphics));
        form.add(row("Particles", particles));
        JPanel vsyncRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        vsyncRow.setOpaque(false);
        vsync.setForeground(Theme.TEXT);
        vsync.setBackground(Theme.BG);
        vsyncRow.add(vsync);
        form.add(vsyncRow);
        form.add(Box.createVerticalStrut(10));

        Theme.VButton save = new Theme.VButton("SAVE PERFORMANCE", true);
        save.setMaximumSize(new Dimension(260, 44));
        save.setAlignmentX(Component.LEFT_ALIGNMENT);
        save.addActionListener(e -> {
            app.config.setInt("ramMb", ram.getValue());
            app.config.setInt("fpsCap", fps.getValue());
            app.config.setInt("renderDistance", renderDist.getValue());
            app.config.set("graphics", String.valueOf(graphics.getSelectedItem()));
            app.config.set("particles", String.valueOf(particles.getSelectedItem()));
            app.config.setBool("vsync", vsync.isSelected());
            app.config.save();
            status.setText("Saved — applied on next launch (JVM -Xmx + options.txt).");
            status.setForeground(new Color(0x3DDC84));
        });
        form.add(save);
        form.add(Box.createVerticalStrut(8));
        status.setForeground(Theme.TEXT2);
        status.setFont(Theme.font(12f, Font.PLAIN));
        form.add(status);

        JLabel note = new JLabel("No guaranteed-FPS claims — values depend on your device.");
        note.setForeground(Theme.TEXT2);
        note.setFont(Theme.font(11f, Font.ITALIC));
        form.add(note);

        add(form, BorderLayout.CENTER);
    }

    private JComponent slider(String label, JSlider s, java.util.function.IntConsumer onChange) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        JLabel l = new JLabel(label);
        l.setForeground(Theme.TEXT2);
        l.setFont(Theme.font(12f, Font.BOLD));
        p.add(l, BorderLayout.NORTH);
        s.addChangeListener(e -> onChange.accept(s.getValue()));
        p.add(s, BorderLayout.CENTER);
        return p;
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
        ram.setValue(app.config.getInt("ramMb", 2048));
        fps.setValue(app.config.getInt("fpsCap", 120));
        renderDist.setValue(app.config.getInt("renderDistance", 8));
        graphics.setSelectedItem(app.config.get("graphics", "FAST"));
        particles.setSelectedItem(app.config.get("particles", "MINIMAL"));
        vsync.setSelected(app.config.getBool("vsync", false));
        status.setText("Ready.");
        status.setForeground(Theme.TEXT2);
    }
}
