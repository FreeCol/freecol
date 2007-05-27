package net.sf.freecol.client.gui.plaf;

import java.awt.Image;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;


/**
* Draws the image "BackgroundImage" from the defaults table as a tiled
* background image.
*/
public class FreeColPanelUI extends BasicPanelUI {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static FreeColPanelUI sharedInstance = new FreeColPanelUI();
    
    private FreeColPanelUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
        if (c.isOpaque()) {
            int width = c.getWidth();
            int height = c.getHeight();

            Image tempImage = (Image) UIManager.get("BackgroundImage");
            
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
