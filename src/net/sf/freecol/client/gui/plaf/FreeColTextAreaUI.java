package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;

import javax.swing.ImageIcon;


/**
* Draws the image "BackgroundImage2" from the defaults table as a tiled
* background image.
*/
public class FreeColTextAreaUI extends BasicTextAreaUI {

    public static ComponentUI createUI(JComponent c) {
        return new FreeColTextAreaUI();
    }

    public void paintBackground(java.awt.Graphics g) {
        JComponent c = getComponent();

        if (c.isOpaque()) {
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
        }

    }

}
