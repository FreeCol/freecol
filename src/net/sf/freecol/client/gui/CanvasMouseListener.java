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

package net.sf.freecol.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Timer;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * Listens to mouse buttons being pressed at the level of the Canvas.
 */
public final class CanvasMouseListener implements ActionListener, MouseListener {

    private static final Logger logger = Logger.getLogger(CanvasMouseListener.class.getName());

    private static final int doubleClickDelay = 200; // Milliseconds

    private final FreeColClient freeColClient;

    private final Canvas canvas;

    private final Timer doubleClickTimer = new Timer(doubleClickDelay,this);

    private int centerX, centerY;


    /**
     * Create a new canvas mouse listener.
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     * @param canvas The component this object gets created for.
     */
    public CanvasMouseListener(FreeColClient freeColClient, Canvas canvas) {
        this.freeColClient = freeColClient;
        this.canvas = canvas;
    }

    /**
     * Invoked when a mouse button was clicked.
     *
     * @param e The MouseEvent that holds all the information.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        try {
            if (e.getClickCount() > 1) {
                Tile tile = canvas.convertToMapTile(e.getX(), e.getY());
                Colony colony = tile.getColony();
                if (colony != null) {
                    if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
                        canvas.showForeignColony(colony);
                    } else {
                        canvas.showColonyPanel(colony, null);
                    }
                }
            } else {
                canvas.requestFocus();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mouseClicked!", ex);
        }
    }

    /**
     * Invoked when the mouse enters the component.
     *
     * @param e The MouseEvent that holds all the information.
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        // Ignore for now.
    }

    /**
     * Invoked when the mouse exits the component.
     *
     * @param e The MouseEvent that holds all the information.
     */
    @Override
    public void mouseExited(MouseEvent e) {
        // Ignore for now.
    }

    /**
     * Invoked when a mouse button was pressed.
     *
     * @param e The MouseEvent that holds all the information.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (!e.getComponent().isEnabled()) return;

        int me = e.getButton();
        if (e.isPopupTrigger()) me = MouseEvent.BUTTON3;
        Tile tile = canvas.convertToMapTile(e.getX(), e.getY());

        switch (me) {
        case MouseEvent.BUTTON1:
            // Record initial click point for purposes of dragging
            canvas.setDragPoint(e.getX(), e.getY());
            if (canvas.isGotoStarted()) {
                PathNode path = canvas.getGotoPath();
                if (path != null) {
                    canvas.stopGoto();
                    // Move the unit
                    freeColClient.getInGameController()
                        .goToTile(canvas.getActiveUnit(),
                            path.getLastNode().getTile());
                }
            } else if (doubleClickTimer.isRunning()) {
                doubleClickTimer.stop();
            } else {
                centerX = e.getX();
                centerY = e.getY();
                doubleClickTimer.start();
            }
            canvas.requestFocus();
            break;
        case MouseEvent.BUTTON2:
            if (tile != null) {
                Unit unit = canvas.getActiveUnit();
                if (unit != null && unit.getTile() != tile) {
                    PathNode dragPath = unit.findPath(tile);
                    canvas.startGoto();
                    canvas.setGotoPath(dragPath);
                }
            }
            break;
        case MouseEvent.BUTTON3:
            // Cancel goto if one is active
            if (canvas.isGotoStarted()) canvas.stopGoto();
            canvas.showTilePopup(tile, e.getX(), e.getY());
            break;
        default:
            break;
        }
    }

    /**
     * Invoked when a mouse button was released.
     *
     * @param e The MouseEvent that holds all the information.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        try {
            if (canvas.getGotoPath() != null) { // A mouse drag has ended.
                PathNode temp = canvas.getGotoPath();
                canvas.stopGoto();

                freeColClient.getInGameController()
                    .goToTile(canvas.getActiveUnit(),
                              temp.getLastNode().getTile());

            } else if (canvas.isGotoStarted()) {
                canvas.stopGoto();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mouseReleased!", ex);
        }
    }
 

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        doubleClickTimer.stop();
        Tile tile=canvas.convertToMapTile(centerX, centerY);
        if(canvas.getViewMode() == GUI.MOVE_UNITS_MODE) {
            // Clear goto order when active unit is on the tile
            Unit unit=canvas.getActiveUnit();
            if(unit != null && unit.getTile() == tile) {
                freeColClient.getInGameController().clearGotoOrders(unit);
                canvas.updateCurrentPathForActiveUnit();
            } else {
                if (tile != null && tile.hasSettlement()) {
                    freeColClient.getGUI().showSettlement(tile.getSettlement());
                    return;
                }
            }
        }
        freeColClient.getGUI().setSelectedTile(tile);
    }
}
