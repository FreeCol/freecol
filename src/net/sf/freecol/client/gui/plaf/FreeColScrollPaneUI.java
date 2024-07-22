/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

import net.sf.freecol.client.gui.FontLibrary;


/**
 * UI-class for scroll panes.
 */
public class FreeColScrollPaneUI extends BasicScrollPaneUI {

    public static ComponentUI createUI(@SuppressWarnings("unused") JComponent c) {
        return new FreeColScrollPaneUI();
    }


    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        c.setOpaque(false);
        
        if (c instanceof JScrollPane) {
            final JScrollPane pane = (JScrollPane) c;
            final int scrollUnitIncrement = (int) (FontLibrary.getFontScaling() * 16);
            updateUnitIncrement(pane.getHorizontalScrollBar(), scrollUnitIncrement);
            updateUnitIncrement(pane.getVerticalScrollBar(), scrollUnitIncrement);
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        LAFUtilities.setProperties(g, c);
        super.paint(g, c);
    }
    
    private void updateUnitIncrement(JScrollBar scrollBar, int increment) {
        if (scrollBar != null) {
            scrollBar.setUnitIncrement(increment);
        }
    }
}
