package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         java.awt.*;


public class FreeColListUI extends BasicListUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColListUI();
    }


    public void installUI(JComponent c) {
        super.installUI(c);
        ((JList) c).setCellRenderer(createRenderer());
    }

    public void paint(Graphics g, JComponent c) {
        int width = c.getWidth();
        int height = c.getHeight();

        Image tempImage = (Image) UIManager.get("BackgroundImage2");

        if (tempImage != null) {
            for (int x=0; x<width; x+=tempImage.getWidth(null)) {
                for (int y=0; y<height; y+=tempImage.getHeight(null)) {
                    g.drawImage(tempImage, x, y, null);
                }
            }
        } else {
            g.setColor(c.getBackground());
            g.fillRect(0, 0, width, height);
        }
        
        super.paint(g, c);
    }

    protected ListCellRenderer createRenderer() {
        return new FreeColComboBoxRenderer.UIResource();
    }    
}
