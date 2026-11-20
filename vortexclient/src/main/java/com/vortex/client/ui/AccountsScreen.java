package com.vortex.client.ui;

import com.vortex.client.VortexClientApp;
import com.vortex.client.data.ClientConfig;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Offline accounts: add/remove/select. Microsoft login is an official-OAuth phase, not faked. */
public class AccountsScreen extends JPanel implements VortexClientApp.Navigable {

    private final VortexClientApp app;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);

    public AccountsScreen(VortexClientApp app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        Theme.VButton back = new Theme.VButton("‹ Menu", false);
        back.addActionListener(e -> app.navigate("menu"));
        top.add(back, BorderLayout.WEST);
        JLabel title = new JLabel("ACCOUNTS");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.font(24f, Font.BOLD));
        top.add(title, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        list.setBackground(Theme.CARD);
        list.setForeground(Theme.TEXT);
        list.setSelectionBackground(Theme.RED);
        list.setSelectionForeground(Color.WHITE);
        list.setFont(Theme.font(14f, Font.PLAIN));
        add(new JScrollPane(list) {{
            setOpaque(false);
            getViewport().setOpaque(false);
            setBackground(Theme.BG);
        }}, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        Theme.VButton add = new Theme.VButton("+ ADD OFFLINE ACCOUNT", true);
        add.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Username:", "Add account",
                JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.isBlank()) return;
            List<String[]> accounts = app.config.accounts();
            accounts.add(new String[]{name.trim(), ClientConfig.offlineUuid(name.trim())});
            app.config.saveAccounts(accounts);
            refresh();
        });
        Theme.VButton remove = new Theme.VButton("REMOVE", false);
        remove.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx < 0) return;
            List<String[]> accounts = app.config.accounts();
            if (idx < accounts.size()) { accounts.remove(idx); app.config.saveAccounts(accounts); }
            refresh();
        });
        Theme.VButton select = new Theme.VButton("USE SELECTED", false);
        select.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            List<String[]> accounts = app.config.accounts();
            if (idx >= 0 && idx < accounts.size()) {
                app.config.set("username", accounts.get(idx)[0]);
                app.config.save();
                JOptionPane.showMessageDialog(this,
                    "Active username: " + accounts.get(idx)[0]
                        + "\n(Offline profile — Microsoft sign-in is an official-OAuth phase).");
            }
        });
        actions.add(add); actions.add(select); actions.add(remove);
        add(actions, BorderLayout.SOUTH);
    }

    private void refresh() {
        model.clear();
        String active = app.config.get("username", "Player");
        for (String[] a : app.config.accounts()) {
            model.addElement((a[0].equals(active) ? "● " : "  ") + a[0] + "   [" + a[1].substring(0, 8) + "…]");
        }
        if (model.isEmpty()) model.addElement("  No accounts yet — add an offline username.");
    }

    @Override public void onShown() { refresh(); }
}
