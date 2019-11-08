/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Point;
import java.util.logging.Logger;

import javax.swing.Timer;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Listens to mouse buttons being pressed at the level of the Canvas.
 */
public final class CanvasMouseListener extends FreeColClientHolder
    implements MouseListener {

    private static final Logger logger = Logger.getLogger(CanvasMouseListener.class.getName());


    /**
     * Create a new mouse listener.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     */
    public CanvasMouseListener(FreeColClient freeColClient) {
        super(freeColClient);
    }


    // Interface MouseListener

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent e) { /* Ignore for now. */ }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(MouseEvent e) { /* Ignore for now. */ }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent e) {
        if (!e.getComponent().isEnabled()) return;

        if (e.isPopupTrigger()) {
            getGUI().showTilePopup(e.getX(), e.getY());
            return;
        }

        switch (e.getButton()) {
        case MouseEvent.BUTTON1: // Drag and selection
            // Enable dragging with button 1
            // @see CanvasMouseMotionListener#mouseDragged
            getGUI().prepareDrag(e.getX(), e.getY());
            break;
        case MouseEvent.BUTTON2: // Immediate goto
            getGUI().performGoto(e.getX(), e.getY());
            break;
        case MouseEvent.BUTTON3: // Immediate tile popup
            getGUI().showTilePopup(e.getX(), e.getY());
            break;
        default:
            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent e) {
        if (!e.getComponent().isEnabled()) return;

        // Only process release of Button1 for drag-and-release gotos
        if (e.getButton() != MouseEvent.BUTTON1) return;

        // Handle goto on release, following updates in
        // @see CanvasMouseMotionListener#mouseDragged.
        //
        // Do not try to do this in mouseClicked as long presses will
        // not generate calls there.
        getGUI().traverseGotoPath();
    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent e) {
        if (!e.getComponent().isEnabled()) return;

        // Only process clicks on Button1
        if (e.getButton() != MouseEvent.BUTTON1) return;

        getGUI().clickAt(e.getClickCount(), e.getX(), e.getY());
    }
}
