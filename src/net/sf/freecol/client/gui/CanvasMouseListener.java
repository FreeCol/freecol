/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;


/**
 * Listens to mouse buttons being pressed at the level of the Canvas.
 */
public final class CanvasMouseListener implements ActionListener, MouseListener {

    private static final Logger logger = Logger.getLogger(CanvasMouseListener.class.getName());

    private final Canvas canvas;

    private final MapViewer mapViewer;

    private final int doubleClickDelay = 200; // Milliseconds
    private Timer doubleClickTimer = new Timer(doubleClickDelay,this);

    private int centerX, centerY;

    private FreeColClient freeColClient;

    /**
     * The constructor to use.
     *
     * @param canvas The component this object gets created for.
     * @param mapViewer The GUI that holds information such as screen resolution.
     */
    public CanvasMouseListener(FreeColClient freeColClient, Canvas canvas, MapViewer mapViewer) {
        this.freeColClient = freeColClient;
        this.canvas = canvas;
        this.mapViewer = mapViewer;
    }

    /**
     * Invoked when a mouse button was clicked.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseClicked(MouseEvent e) {
        try {
            if (e.getClickCount() > 1) {
                Tile tile = mapViewer.convertToMapTile(e.getX(), e.getY());
                if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
                    if (tile.hasSettlement()) {
                        canvas.showForeignColony(tile.getSettlement());
                    }
                } else {
                    canvas.showColonyPanel(tile);
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
    public void mouseEntered(MouseEvent e) {
        // Ignore for now.
    }

    /**
     * Invoked when the mouse exits the component.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseExited(MouseEvent e) {
        // Ignore for now.
    }

    /**
     * Invoked when a mouse button was pressed.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mousePressed(MouseEvent e) {
        if (!e.getComponent().isEnabled()) {
            return;
        }
        try {
            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                // Cancel goto if one is active
                if (mapViewer.isGotoStarted()) {
                    mapViewer.stopGoto();
                }

                canvas.showTilePopup(mapViewer.convertToMapTile(e.getX(), e.getY()), e.getX(), e.getY());
            } else if (e.getButton() == MouseEvent.BUTTON2) {
                Tile tile = mapViewer.convertToMapTile(e.getX(), e.getY());
                if (tile != null) {
                    Unit unit = mapViewer.getActiveUnit();
                    if (unit != null && unit.getTile() != tile) {
                        PathNode dragPath = unit.findPath(tile);
                        mapViewer.startGoto();
                        mapViewer.setGotoPath(dragPath);
                    }
                }
            } else if (e.getButton() == MouseEvent.BUTTON1) {
                // Record initial click point for purposes of dragging
                mapViewer.setDragPoint(e.getX(), e.getY());
                if (mapViewer.isGotoStarted()) {
                    PathNode path = mapViewer.getGotoPath();
                    if (path != null) {
                        mapViewer.stopGoto();
                        // Move the unit
                        freeColClient.getInGameController()
                            .goToTile(mapViewer.getActiveUnit(),
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
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mousePressed!", ex);
        }
    }

    /**
     * Invoked when a mouse button was released.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseReleased(MouseEvent e) {
        try {
            if (mapViewer.getGotoPath() != null) { // A mouse drag has ended.
                PathNode temp = mapViewer.getGotoPath();
                mapViewer.stopGoto();

                freeColClient.getInGameController()
                    .goToTile(mapViewer.getActiveUnit(),
                              temp.getLastNode().getTile());

            } else if (mapViewer.isGotoStarted()) {
                mapViewer.stopGoto();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mouseReleased!", ex);
        }
    }
 

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        doubleClickTimer.stop();
        mapViewer.setSelectedTile(mapViewer.convertToMapTile(centerX, centerY),
            true);
    }
}
