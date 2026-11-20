package com.vortex.client.ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/** VORTEX Premium design tokens + shared painters (black/red, angular, minimal glow). */
public final class Theme {
    public static final Color BG = new Color(0x090909);
    public static final Color SECONDARY = new Color(0x111111);
    public static final Color CARD = new Color(0x181818);
    public static final Color RED = new Color(0xE21F3D);
    public static final Color RED_HOVER = new Color(0xFF304F);
    public static final Color TEXT = Color.WHITE;
    public static final Color TEXT2 = new Color(0x858585);

    private Theme() {}

    /** Angular cut corner border (top-left + bottom-right notches). */
    public static class AngularBorder extends AbstractBorder {
        private final Color color; private final int cut; private final float thickness;
        public AngularBorder(Color color, int cut, float thickness) {
            this.color = color; this.cut = cut; this.thickness = thickness;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.setStroke(new BasicStroke(thickness));
            Path2D p = new Path2D.Float();
            p.moveTo(x + cut, y); p.lineTo(x + w, y); p.lineTo(x + w, y + h - cut);
            p.lineTo(x + w - cut, y + h); p.lineTo(x, y + h); p.lineTo(x, y + cut); p.closePath();
            g2.draw(p); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(cut + 4, cut + 4, cut + 4, cut + 4); }
    }

    /** Primary action button: charcoal → red on hover, angular border, pressed scale feel. */
    public static class VButton extends JButton {
        private boolean hover; private final boolean primary;
        public VButton(String text, boolean primary) {
            super(text); this.primary = primary;
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(primary ? Color.WHITE : Theme.TEXT);
            setFont(Theme.font(14f, Font.BOLD));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (primary) {
                g2.setColor(hover ? RED_HOVER : RED);
                g2.fillRoundRect(0, 0, w, h, 10, 10);
            } else {
                g2.setColor(hover ? new Color(0x222222) : CARD);
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(hover ? RED : new Color(0x2A2A2A));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            }
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Styled text field. */
    public static class VField extends JTextField {
        public VField(String placeholder) {
            setForeground(TEXT); setBackground(SECONDARY);
            setCaretColor(RED);
            setFont(Theme.font(14f, Font.PLAIN));
            setBorder(BorderFactory.createCompoundBorder(
                new AngularBorder(new Color(0x2A2A2A), 8, 1.2f),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { repaint(); }
            });
            putClientProperty("placeholder", placeholder);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(TEXT2);
                g2.setFont(getFont());
                g2.drawString((String) getClientProperty("placeholder"), 10, getHeight() / 2 + 5);
                g2.dispose();
            }
        }
    }

    public static Font font(float size, int style) {
        return new Font(Font.SANS_SERIF, style, (int) size);
    }

    /** Paint the background + faint red particles (called by screens). */
    public static void paintBackground(Graphics g, Component c, long tick) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(BG); g2.fillRect(0, 0, c.getWidth(), c.getHeight());
        // Subtle red light from the top-left (no excessive glow).
        g2.setPaint(new RadialGradientPaint(
            new Point(0, 0), Math.max(c.getWidth(), c.getHeight()),
            new float[]{0f, 1f}, new Color[]{new Color(0x1A0E11), new Color(0x090909)}));
        g2.fillRect(0, 0, c.getWidth(), c.getHeight());
        // Deterministic drifting particles (cheap, low-end friendly).
        g2.setColor(new Color(0xE2, 0x1F, 0x3D, 46));
        for (int i = 0; i < 18; i++) {
            int px = (int) ((i * 173 + tick * (6 + (i % 5))) % Math.max(1, c.getWidth()));
            int py = (int) ((i * 271 + tick * (4 + (i % 3))) % Math.max(1, c.getHeight()));
            g2.fillOval(px, py, 2 + (i % 3), 2 + (i % 3));
        }
    }
}
