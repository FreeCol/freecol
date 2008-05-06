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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;

import net.sf.freecol.common.resources.ResourceManager;


/**
* Sets the default opaque attribute to <i>false</i> and 
* uses a 10% black shading on the {@link #paintButtonPressed}.
*/
public class FreeColButtonUI extends MetalButtonUI {

    public static ComponentUI createUI(JComponent c) {
        return new FreeColButtonUI();
    }

    
    public void installUI(JComponent c) {
        super.installUI(c);
        
        c.setOpaque(false);
    }
    
    
    public void paint(Graphics g, JComponent b) {
        LAFUtilities.setProperties(g, b);
        
        if (b.isOpaque()) {
            int width = b.getWidth();
            int height = b.getHeight();
            
            Image tempImage = ResourceManager.getImage("BackgroundImage");
            
            if (tempImage != null) {
                for (int x=0; x<width; x+=tempImage.getWidth(null)) {
                    for (int y=0; y<height; y+=tempImage.getHeight(null)) {
                        g.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g.setColor(b.getBackground());
                g.fillRect(0, 0, width, height);
            }
        }
        super.paint(g, b);
        
        AbstractButton a = (AbstractButton) b;
        if (a.isRolloverEnabled()) {
            Point p = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(p, b);
            boolean rollover = b.contains(p);
            if (rollover) { 
                paintButtonPressed(g, (AbstractButton) b);
            }
        }
    }
    
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (b.isContentAreaFilled()) {
            Graphics2D g2d = (Graphics2D) g;
            Dimension size = b.getSize();
            Composite oldComposite = g2d.getComposite();
            Color oldColor = g2d.getColor();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, size.width, size.height);
            g2d.setComposite(oldComposite);
            g2d.setColor(oldColor);

        }
    }
}
