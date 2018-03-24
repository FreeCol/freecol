/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

package net.sf.freecol.client.gui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Listens to the mouse being moved at the level of the Canvas.
 */
public final class CanvasMouseMotionListener extends AbstractCanvasListener
    implements MouseMotionListener {

    private static final Logger logger = Logger.getLogger(CanvasMouseMotionListener.class.getName());


    /**
     * Creates a new listener for mouse movement.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param canvas The {@code Canvas} to listen on.
     */
    public CanvasMouseMotionListener(FreeColClient freeColClient,
                                     Canvas canvas) {
        super(freeColClient, canvas);
    }


    /**
     * Does a mouse event update an existing goto?
     *
     * @param me The {@code MouseEvent} to check.
     * @return True if there is a goto underway.
     */
    private boolean updateGoto(MouseEvent me) {
        if (!canvas.isGotoStarted()) return false;
        
        getGUI().updateGotoPath(canvas.convertToMapTile(me.getX(), me.getY()));
        return true;
    }
        

    // Interface MouseMotionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent me) {
        performAutoScrollIfActive(me, true);

        updateGoto(me);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent me) {
        // getButton does not work here, TODO: find out why
        if ((me.getModifiers() & MouseEvent.BUTTON1_MASK)
            != MouseEvent.BUTTON1_MASK) return;
        performDragScrollIfActive(me);

        if (updateGoto(me)) {
            ; // Pass
        } else if (canvas.isDrag(me.getX(), me.getY())) {
            canvas.startGoto();
        }
    }
}
