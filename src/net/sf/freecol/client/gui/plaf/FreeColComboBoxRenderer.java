package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.BasicComboBoxRenderer;
import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         java.awt.*;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;


public class FreeColComboBoxRenderer implements ListCellRenderer {

    private final SelectedComponent SELECTED_COMPONENT = new SelectedComponent();
    private final NormalComponent NORMAL_COMPONENT = new NormalComponent();


    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
        JLabel c;

        if (isSelected) {
            c = SELECTED_COMPONENT;
        } else {
            c = NORMAL_COMPONENT;
        }

        c.setForeground(list.getForeground());
        c.setFont(list.getFont());

        if (value instanceof Icon) {
            c.setIcon((Icon) value);
        } else {
            c.setText((value == null) ? "" : value.toString());
        }

        return c;
    }


    public class SelectedComponent extends JLabel {

        public SelectedComponent() {
            setOpaque(false);
        }

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
    }
    

    public class NormalComponent extends JLabel {

        public NormalComponent() {
            setOpaque(false);
        }
    }
    

    public static class UIResource extends FreeColComboBoxRenderer implements javax.swing.plaf.UIResource {
    }

}
