/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;

import net.sf.freecol.client.gui.ImageLibrary;


/**
 * Draw the "image.background.FreeColPanel" resource as a tiled background image.
 */
public class FreeColPanelUI extends BasicPanelUI {

    private static final FreeColPanelUI sharedInstance = new FreeColPanelUI();


    private FreeColPanelUI() {}

    public static ComponentUI createUI(@SuppressWarnings("unused") JComponent c) {
        return sharedInstance;
    }

    @Override
    public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
        if (c.isOpaque()) {
            ImageLibrary.drawTiledImage("image.background.FreeColPanel", g, c, null);
        }
    }
}
