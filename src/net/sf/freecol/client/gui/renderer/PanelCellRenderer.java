package net.sf.freecol.client.gui.renderer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import javax.swing.JList;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.panel.MigPanel;


public abstract class PanelCellRenderer {

    private final JPanel normal = new MigPanel();
    private final JPanel selected = new MigPanel() {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                Composite oldComposite = g2d.getComposite();
                Color oldColor = g2d.getColor();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setComposite(oldComposite);
                g2d.setColor(oldColor);

                super.paintComponent(g);
            }
        };

    public JPanel getNormalPanel() {
        return normal;
    }

    public JPanel getSelectedPanel() {
        return selected;
    }

    public void setOpaque(boolean value) {
        normal.setOpaque(value);
        selected.setOpaque(value);
    }

    public void setLayout(LayoutManager layout) {
        normal.setLayout(layout);
        selected.setLayout(layout);
    }

    public JPanel getPanel(JList list, boolean isSelected) {
        JPanel panel = isSelected ? selected : normal;
        panel.removeAll();
        panel.setForeground(list.getForeground());
        panel.setFont(list.getFont());
        return panel;
    }

}