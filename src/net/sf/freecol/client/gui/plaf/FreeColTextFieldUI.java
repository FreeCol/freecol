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

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;

import net.sf.freecol.client.gui.ImageLibrary;


/**
 * Provides a tiled background image "image.background.FreeColTextField" to
 * text fields.
 */
public class FreeColTextFieldUI extends BasicTextFieldUI {

    private final JComponent c;


    public FreeColTextFieldUI(JComponent c) {
        this.c = c;
    }

    public static ComponentUI createUI(JComponent c) {
        return new FreeColTextFieldUI(c);
    }

    @Override
    public void paintSafely(Graphics g) {
        LAFUtilities.setProperties(g, c);
        super.paintSafely(g);
    }

    @Override
    public void paintBackground(java.awt.Graphics g) {
        JComponent c = getComponent();

        if (c.isOpaque()) {
            ImageLibrary.drawTiledImage("image.background.FreeColTextField", g, c, null);
        }
    }

}
