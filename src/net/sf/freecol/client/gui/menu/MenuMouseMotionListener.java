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

package net.sf.freecol.client.gui.menu;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.AbstractCanvasListener;
import net.sf.freecol.client.gui.Canvas;


/**
 * This class is meant to make the autoscrolling work better, so that
 * you don't have to hover the mouse exactly one pixel below the menu
 * bar to make it scroll up.  This is the MouseMotionListener added to
 * the menu bar, allowing you to scroll by just moving the mouse all
 * the way to the top of the screen.
 * 
 * Note: This doesn't cause it to scroll down when you reach the
 * bottom of the menu bar, because the performAutoScrollIfActive will
 * compare the Y coordinate to the size of the entire canvas (which
 * should always be bigger).
 */
public class MenuMouseMotionListener extends AbstractCanvasListener
    implements MouseMotionListener {

    /**
     * Trivial constructor.
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     */
    public MenuMouseMotionListener(FreeColClient freeColClient, Canvas canvas) {
        super(freeColClient, canvas);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        //Do nothing
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        performAutoScrollIfActive(e);
    }
}
