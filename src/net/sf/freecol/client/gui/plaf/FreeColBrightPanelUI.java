package net.sf.freecol.client.gui.plaf;

import         javax.swing.plaf.basic.BasicPanelUI;
import         javax.swing.plaf.*;
import         javax.swing.*;
import java.awt.Image;

import javax.swing.UIManager;


/**
* Draws the image "BackgroundImage2" from the defaults table as a tiled
* background image.
*/
public class FreeColBrightPanelUI extends BasicPanelUI {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static FreeColBrightPanelUI sharedInstance = new FreeColBrightPanelUI();
    
    

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
        if (c.isOpaque()) {
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
        }
    }

}
