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

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicListUI;

import net.sf.freecol.common.resources.ResourceManager;

/**
 * UI-class for lists.
 */
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

        Image tempImage = ResourceManager.getImage("BackgroundImage2");

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
         
        LAFUtilities.setProperties(g, c);
        super.paint(g, c);
    }

    protected ListCellRenderer createRenderer() {
        return new FreeColComboBoxRenderer.UIResource();
    }    
}
