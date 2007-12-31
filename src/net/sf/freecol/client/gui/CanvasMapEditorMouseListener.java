/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Position;

/**
 * Listens to mouse buttons being pressed at the level of the Canvas.
 */
public final class CanvasMapEditorMouseListener implements MouseListener {
    private static final Logger logger = Logger.getLogger(CanvasMapEditorMouseListener.class.getName());




    private final Canvas canvas;

    private final GUI gui;

    /**
     * The constructor to use.
     * 
     * @param canvas The component this object gets created for.
     * @param g The GUI that holds information such as screen resolution.
     */
    public CanvasMapEditorMouseListener(Canvas canvas, GUI g) {
        this.canvas = canvas;
        gui = g;
    }

    /**
     * This method can be called to make sure the map is loaded
     * There is no point executing mouse events if the map is not loaded
     */
    private Map getMap() {
        Map map = null;
        if (canvas.getClient().getGame() != null)
            map = canvas.getClient().getGame().getMap();
        return map;
    }
    
    /**
     * Invoked when a mouse button was clicked.
     * 
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseClicked(MouseEvent e) {
        if (getMap() == null) {
            return;
        }
        try {
            if (e.getClickCount() > 1) {
                Position p = gui.convertToMapCoordinates(e.getX(), e.getY());
                gui.showColonyPanel(p);
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
        if (getMap() == null) {
            return;
        }
        try {
            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                //canvas.showTilePopup(gui.convertToMapCoordinates(e.getX(), e.getY()), e.getX(), e.getY());
                Position p = gui.convertToMapCoordinates(e.getX(), e.getY());
                gui.setSelectedTile(p, true);
            } else if (e.getButton() == MouseEvent.BUTTON1) {
                if (gui.getFocus() != null) {
                    Position p = gui.convertToMapCoordinates(e.getX(), e.getY());
                    Tile t = getMap().getTile(p);
                    if (t == null) {
                        // can happen on the edges of the map
                        return;
                    }
                    canvas.getClient().getMapEditorController().transform(t);
                    canvas.refresh();
                    canvas.requestFocus();
                }
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
        if (getMap() == null) {
            return;
        }
        try {
            if (gui.getDragPath() != null) {
                // A mouse drag has ended (see CanvasMouseMotionListener).

                PathNode temp = gui.getDragPath();

                gui.stopDrag();

                // Move the unit:
                Unit unit = gui.getActiveUnit();
                canvas.getClient().getInGameController().setDestination(unit, temp.getLastNode().getTile());
                if (canvas.getClient().getGame().getCurrentPlayer() == canvas.getClient().getMyPlayer()) {
                    canvas.getClient().getInGameController().moveToDestination(unit);
                }
            } else if (gui.isDragStarted()) {
                gui.stopDrag();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mouseReleased!", ex);
        }
    }
}
