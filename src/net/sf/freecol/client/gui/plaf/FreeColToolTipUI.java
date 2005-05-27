package net.sf.freecol.client.gui.plaf;

import javax.swing.plaf.basic.*;
import javax.swing.plaf.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.text.View;




/**
* Draws the image "BackgroundImage" from the defaults table as a tiled
* background image.
*/
public class FreeColToolTipUI extends BasicToolTipUI {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static FreeColToolTipUI sharedInstance = new FreeColToolTipUI();
    
    

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void paint(Graphics g, JComponent c) {


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

        // Copied from "BasicToolTipUI":
        Font font = c.getFont();
        FontMetrics metrics = g.getFontMetrics(font);
        Dimension size = c.getSize();

        g.setColor(c.getForeground());
        g.setFont(font);

        String tipText = ((JToolTip)c).getTipText();
        if (tipText == null) {
            tipText = "";
        }

        Insets insets = c.getInsets();
        Rectangle paintTextR = new Rectangle(
            insets.left,
            insets.top,
            size.width - (insets.left + insets.right),
            size.height - (insets.top + insets.bottom));

        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            v.paint(g, paintTextR);
        } else {
            g.drawString(tipText, paintTextR.x + 3,
                                  paintTextR.y + metrics.getAscent());
        }
    }

}
