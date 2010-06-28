/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.plaf;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.text.View;

import net.sf.freecol.client.gui.ImageLibrary;


/**
 * Draw the "background.FreeColToolTip" resource as a tiled background
 * image on tool tip popups.
 */
public class FreeColToolTipUI extends BasicToolTipUI {
    
    private static FreeColToolTipUI sharedInstance = new FreeColToolTipUI();
    

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void paint(Graphics g, JComponent c) {
        if (c.isOpaque()) {
            ImageLibrary.drawTiledImage("background.FreeColToolTip", g, c, null);
        }
         
        LAFUtilities.setProperties(g, c);

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
