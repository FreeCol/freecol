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
        implements ActionListener, MouseListener {

    private static final Logger logger = Logger.getLogger(CanvasMouseListener.class.getName());

    private static final int doubleClickDelay = 200; // Milliseconds

    private final Canvas canvas;

    private final Timer doubleClickTimer = new Timer(doubleClickDelay, this);

    private int centerX, centerY;


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


    /**
     * If a goto order is underway, perform it.
     *
     * @return True if a goto was underway.
     */
    private boolean flushGoto() {
        if (canvas.isGotoStarted()) {
            PathNode path = canvas.getGotoPath();
            canvas.stopGoto();
            if (path != null) { // Move the unit
                getFreeColClient().getInGameController()
                    .goToTile(getGUI().getActiveUnit(),
                                path.getLastNode().getTile());
            }
            return true;
        }
        return false;
    }

    /**
     * Perform a goto order to the given tile with the active unit, if
     * possible.
     *
     * @param tile The {@code Tile} to go to.
     */
    private void immediateGoto(Tile tile) {
        Unit unit;
        PathNode path;
        if (tile != null
                && (unit = getGUI().getActiveUnit()) != null
                && unit.getTile() != tile
                && (path = unit.findPath(tile)) != null) {
            canvas.startGoto();
            canvas.setGotoPath(path);
            flushGoto();
        }
    }

    private void immediatePopup(Tile tile, int x, int y) {
        if (canvas.isGotoStarted()) canvas.stopGoto();
        canvas.showTilePopup(tile, x, y);
    }

    private void immediateSettlement(Tile tile) {
        if (tile.hasSettlement()) {
            getFreeColClient().getGUI().showSettlement(tile.getSettlement());
        }
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        doubleClickTimer.stop();
        Tile tile = canvas.convertToMapTile(centerX, centerY);
        if (tile == null) return;
        if (flushGoto()) return;

        switch (canvas.getViewMode()) {
        case GUI.MOVE_UNITS_MODE:
            // Clear goto order when active unit is on the tile, else
            // try to display a settlement.
            Unit unit = getGUI().getActiveUnit();
            if (unit != null && unit.getTile() == tile) {
                igc().clearGotoOrders(unit);
                canvas.updateCurrentPathForActiveUnit();
            } else {
                immediateSettlement(tile);
            }
            break;
        case GUI.VIEW_TERRAIN_MODE: default:
            break;
        }
        getGUI().setSelectedTile(tile);
    }


    // Interface MouseListener

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent e) {
        if (!e.getComponent().isEnabled()) return;

        Tile tile;
        if (e.getClickCount() > 1
            && (tile = canvas.convertToMapTile(e.getX(), e.getY())) != null) {
            immediateSettlement(tile);
        } else {
            canvas.requestFocus();
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
        final int me = (e.isPopupTrigger()) ? MouseEvent.BUTTON3
            : e.getButton();
        switch (me) {
        case MouseEvent.BUTTON1:
            // Record initial click point for purposes of dragging,
            // @see CanvasMouseMotionListener#mouseDragged.
            canvas.setDragPoint(e.getX(), e.getY());

            // New click sequence? Remember position to use when timer expires
            if (!doubleClickTimer.isRunning()) {
                centerX = e.getX();
                centerY = e.getY();
            }
            doubleClickTimer.start();
            canvas.requestFocus();
            break;
        case MouseEvent.BUTTON2:
            immediateGoto(tile);
            break;
        case MouseEvent.BUTTON3:
            immediatePopup(tile, e.getX(), e.getY());
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
    }
}
