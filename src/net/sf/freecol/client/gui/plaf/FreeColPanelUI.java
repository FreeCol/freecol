package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.BasicPanelUI;
import         javax.swing.plaf.basic.*;
import         javax.swing.plaf.*;
import         javax.swing.*;
import         java.awt.*;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.UIManager;


/**
* Draws the image "BackgroundImage" from the defaults table as a tiled
* background image.
*/
public class FreeColPanelUI extends BasicPanelUI {

    private static FreeColPanelUI sharedInstance = new FreeColPanelUI();
    
    

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
