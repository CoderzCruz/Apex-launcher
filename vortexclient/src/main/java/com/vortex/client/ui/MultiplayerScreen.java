package com.vortex.client.ui;

import com.vortex.client.VortexClientApp;
import com.vortex.client.game.MinecraftLauncher;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Server list: add/edit/delete/direct-connect + real status ping (latency + online count). */
public class MultiplayerScreen extends JPanel implements VortexClientApp.Navigable {

    private final VortexClientApp app;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final List<String[]> servers = new ArrayList<>();
    private final JLabel status = new JLabel(" ");
    private final ExecutorService pinger = Executors.newFixedThreadPool(4);

    public MultiplayerScreen(VortexClientApp app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        Theme.VButton back = new Theme.VButton("‹ Menu", false);
        back.addActionListener(e -> app.navigate("menu"));
        top.add(back, BorderLayout.WEST);
        JLabel title = new JLabel("MULTIPLAYER");
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
            setOpaque(false); getViewport().setOpaque(false); setBackground(Theme.BG);
        }}, BorderLayout.CENTER);

        status.setForeground(Theme.TEXT2);
        status.setFont(Theme.font(12f, Font.PLAIN));
        add(status, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(btn("＋ ADD SERVER", true, () -> {
            String name = JOptionPane.showInputDialog(this, "Server name:");
            if (name == null || name.isBlank()) return;
            String ip = JOptionPane.showInputDialog(this, "Server address (host:port):");
            if (ip == null || ip.isBlank()) return;
            servers.add(new String[]{name.trim(), ip.trim()});
            app.config.saveServers(servers);
            refresh();
        }));
        actions.add(btn("DIRECT CONNECT", false, () -> {
            String ip = JOptionPane.showInputDialog(this, "Address to join once:");
            if (ip == null || ip.isBlank()) return;
            launch(ip);
        }));
        actions.add(btn("EDIT", false, () -> {
            int i = list.getSelectedIndex();
            if (i < 0 || i >= servers.size()) { status.setText("Select a server first."); return; }
            String name = JOptionPane.showInputDialog(this, "Server name:", servers.get(i)[0]);
            if (name == null) return;
            String ip = JOptionPane.showInputDialog(this, "Address:", servers.get(i)[1]);
            if (ip == null) return;
            servers.set(i, new String[]{name.trim(), ip.trim()});
            app.config.saveServers(servers);
            refresh();
        }));
        actions.add(btn("DELETE", false, () -> {
            int i = list.getSelectedIndex();
            if (i < 0 || i >= servers.size()) return;
            servers.remove(i);
            app.config.saveServers(servers);
            refresh();
        }));
        actions.add(btn("REFRESH (PING)", false, this::pingAll));
        actions.add(btn("JOIN SELECTED", true, () -> {
            int i = list.getSelectedIndex();
            if (i < 0 || i >= servers.size()) { status.setText("Select a server first."); return; }
            launch(servers.get(i)[1]);
        }));
        add(actions, BorderLayout.SOUTH);
    }

    private Theme.VButton btn(String text, boolean primary, Runnable action) {
        Theme.VButton b = new Theme.VButton(text, primary);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void launch(String address) {
        if (!MinecraftLauncher.isConfigured(app.config)) {
            status.setText("Set up Minecraft folder + version in Settings first.");
            return;
        }
        try {
            MinecraftLauncher.launch(app.config, address);
            status.setText("Launching with " + address + " …");
        } catch (Exception ex) {
            status.setText("Launch failed: " + ex.getMessage());
        }
    }

    private void refresh() {
        model.clear();
        for (String[] s : servers) model.addElement(s[0] + "   —   " + s[1]);
        status.setText(servers.size() + " server(s) · select and REFRESH to ping");
    }

    private void pingAll() {
        status.setText("Pinging…");
        for (int i = 0; i < servers.size(); i++) {
            final int idx = i;
            final String hostPort = servers.get(i)[1];
            pinger.submit(() -> {
                String result = ping(hostPort);
                SwingUtilities.invokeLater(() -> {
                    if (idx < servers.size()) {
                        servers.set(idx, new String[]{servers.get(idx)[0], servers.get(idx)[1]});
                        model.set(idx, servers.get(idx)[0] + "   —   " + servers.get(idx)[1] + "   " + result);
                    }
                });
            });
        }
    }

    /**
     * Real Minecraft server-list ping: handshake (proto79) → status request →
     * read the JSON (players online) and measure round-trip latency.
     */
    private static String ping(String hostPort) {
        String host = hostPort;
        int port = 25565;
        int colon = hostPort.lastIndexOf(':');
        if (colon > 0) {
            host = hostPort.substring(0, colon);
            try { port = Integer.parseInt(hostPort.substring(colon + 1)); } catch (NumberFormatException ignored) {}
        }
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2500);
            socket.setSoTimeout(2500);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
            // Handshake: packetId0 + proto ver79 + address + port + next(1)
            out.write(0x00);
            writeVarInt(out, 79);
            writeVarInt(out, hostBytes.length);
            out.write(hostBytes);
            out.writeShort(port);
            writeVarInt(out, 1);
            out.write(0x00); // status state
            out.flush();
            // Status request
            out.write(0x00);
            out.flush();
            readVarInt(in);                 // packet id
            int len = readVarInt(in);
            byte[] payload = new byte[len];
            in.readFully(payload);
            long latency = System.currentTimeMillis() - start;
            String json = new String(payload, StandardCharsets.UTF_8);
            String online = "?";
            int playersIdx = json.indexOf("\"online\":");
            if (playersIdx >= 0) {
                int s = playersIdx + 9, e = s;
                while (e < json.length() && Character.isDigit(json.charAt(e))) e++;
                online = json.substring(s, e);
            }
            return "● " + online + " online · " + latency + "ms";
        } catch (Exception ex) {
            return "○ offline (" + ex.getClass().getSimpleName() + ")";
        }
    }

    private static void writeVarInt(DataOutputStream out, int value) throws Exception {
        while ((value & ~0x7F) != 0) { out.writeByte((value & 0x7F) | 0x80); value >>>= 7; }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws Exception {
        int value = 0, position = 0, current;
        while (true) {
            current = in.readByte();
            value |= (current & 0x7F) << position;
            if ((current & 0x80) == 0) break;
            position += 7;
            if (position >= 32) throw new RuntimeException("VarInt too big");
        }
        return value;
    }

    @Override public void onShown() {
        servers.clear();
        servers.addAll(app.config.servers());
        refresh();
    }
}
