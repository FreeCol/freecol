package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;

import javax.swing.ImageIcon;


/**
* Draws the image "BackgroundImage" from the defaults table as a tiled
* background image.
*/
public class FreeColMenuBarUI extends BasicMenuBarUI {

    private static FreeColMenuBarUI sharedInstance = new FreeColMenuBarUI();
    


    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
        if (c.isOpaque()) {
            int width = c.getWidth();
            int height = c.getHeight();

            ImageIcon tempImage = (ImageIcon) UIManager.get("BackgroundImage");

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
