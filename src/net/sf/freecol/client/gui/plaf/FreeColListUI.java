package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         java.awt.*;

import javax.swing.ImageIcon;


public class FreeColListUI extends BasicListUI {


    public static ComponentUI createUI(JComponent c) {
        return new FreeColListUI();
    }


    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void paint(Graphics g, JComponent c) {
        int width = c.getWidth();
        int height = c.getHeight();

        ImageIcon tempImage = (ImageIcon) UIManager.get("BackgroundImage2");

        if (tempImage != null) {
            for (int x=0; x<width; x+=tempImage.getIconWidth()) {
                for (int y=0; y<height; y+=tempImage.getIconHeight()) {
                    g.drawImage(tempImage.getImage(), x, y, null);
                }
            }
        } else {
            g.setColor(c.getBackground());
            g.fillRect(0, 0, width, height);
        }
        
        super.paint(g, c);
    }

}
