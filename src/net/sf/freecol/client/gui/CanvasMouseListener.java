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

    private final Canvas canvas;


    /**
     * Create a new canvas mouse listener.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param canvas The {@code Canvas} to listen on.
     */
    public CanvasMouseListener(FreeColClient freeColClient, Canvas canvas) {
        super(freeColClient);

        this.canvas = canvas;
    }


    // Interface MouseListener

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent e) {
        if (!e.getComponent().isEnabled()) return;

        // Only process clicks on Button1
        if (e.getButton() != MouseEvent.BUTTON1) return;

        final Tile tile = canvas.convertToMapTile(e.getX(), e.getY());
        if (tile == null) return;

        // Only process events that could not have been gotos.
        final Unit unit = getGUI().getActiveUnit();
        if (e.getClickCount() > 1
            || unit == null
            || unit.getTile() == tile
            || !canvas.isDrag(e.getX(), e.getY())) {
            if (tile.hasSettlement()) {
                getGUI().showTileSettlement(tile);
            } else if (unit == null || unit.getTile() != tile) {
                Unit other = tile.getFirstUnit();
                if (other != null && getMyPlayer().owns(other)) {
                    getGUI().setActiveUnit(other);
                    getGUI().setViewMode(GUI.MOVE_UNITS_MODE);
                } else {
                    getGUI().setSelectedTile(tile);
                    getGUI().setViewMode(GUI.VIEW_TERRAIN_MODE);
                }
            } else {
                getGUI().setSelectedTile(tile);
                getGUI().setViewMode(GUI.VIEW_TERRAIN_MODE);
            }
        }
    }

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

        final Tile tile = canvas.convertToMapTile(e.getX(), e.getY());
        final Unit unit = getGUI().getActiveUnit();
        final int me = (e.isPopupTrigger()) ? MouseEvent.BUTTON3
            : e.getButton();
        if (canvas.isGotoStarted()) canvas.stopGoto();
        switch (me) {
        case MouseEvent.BUTTON1: // Drag and selection
            // Enable dragging with button 1
            // @see CanvasMouseMotionListener#mouseDragged
            canvas.setDragPoint(e.getX(), e.getY());
            canvas.requestFocus();
            break;
        case MouseEvent.BUTTON2: // Immediate goto
            if (tile != null && unit != null) {
                canvas.startGoto();
                getGUI().updateGotoPath(tile);
                getGUI().traverseGotoPath();
            }
            break;
        case MouseEvent.BUTTON3: // Immediate tile popup
            canvas.showTilePopup(tile, new Point(e.getX(), e.getY()));
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

        // Only process clicks on Button1
        if (e.getButton() != MouseEvent.BUTTON1) return;

        // Handle goto immediately on release.  Do not try to do this in
        // mouseClicked as long presses will not generate calls to it.
        if (canvas.isGotoStarted()) {
            getGUI().traverseGotoPath();
        }
    }
}
