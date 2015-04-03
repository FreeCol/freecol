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
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;


/**
 * Utility methods for Look-and-Feel classes.
 */
public final class LAFUtilities {

    private static final int AA_TEXT_SIZE = 16;

    /**
     * Modifies the given graphics object with any relevant
     * {@link JComponent#getClientProperty(Object) client property}
     * from the given component.
     *
     * Currently only <code>RenderingHints.KEY_TEXT_ANTIALIASING</code>
     * is supported. Antialiasing is enabled explicitly if the text size
     * is larger or equal to 16.
     *
     * @param g The graphics object to be updated.
     * @param c The component to get the properties from.
     */
    public static void setProperties(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g;
        if (c.getFont().getSize() >= AA_TEXT_SIZE) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        Object textAA = c.getClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING);
        if (textAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAA);
        }
    }
}
