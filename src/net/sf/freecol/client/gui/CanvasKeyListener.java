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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.action.GotoTileAction;
import net.sf.freecol.common.model.Map.Direction;

/**
 * Listens to keys being pressed at the level of the Canvas.
 */
public final class CanvasKeyListener implements KeyListener {
    private static final Logger logger = Logger.getLogger(CanvasKeyListener.class.getName());




    private final Canvas parent;

    private final InGameController inGameController;


    /**
     * The constructor to use.
     * 
     * @param parent The container of all child panels.
     * @param inGameController The controller to be used for handling the
     *            events.
     */
    public CanvasKeyListener(Canvas parent, InGameController inGameController) {
        this.parent = parent;
        this.inGameController = inGameController;
    }

    /**
     * Invoked when a key has been pressed.
     * 
     * @param e The event that holds key-information.
     */
    public void keyPressed(KeyEvent e) {
        //Cancel any active goto operation when a key is pressed, unless the key is the Goto Tile action's key (let the action's own handler handle that)
        if (parent.getGUI().isGotoStarted() && 
                e.getKeyCode() != parent.getClient().getActionManager().getFreeColAction(GotoTileAction.id).getAccelerator().getKeyCode()) {
            parent.getGUI().stopGoto();
        }

        if (e.isShiftDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_D) {
            inGameController.setInDebugMode(!FreeCol.isInDebugMode());
            return;
        }

        if (e.getModifiers() != 0) {
            return;
        }

        try {
            int viewMode = parent.getGUI().getViewMode().getView();
            if (viewMode == ViewMode.MOVE_UNITS_MODE) {
                moveUnit(e);
            } else if (viewMode == ViewMode.VIEW_TERRAIN_MODE) {
                moveCursor(e);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception occurred while handling KeyEvent.", ex);
        }
    }

    private void moveCursor(KeyEvent e) {
        if (!parent.isMapboardActionsEnabled()) {
            return;
        }
        
        switch (e.getKeyCode()) {
        case KeyEvent.VK_ESCAPE:
            // main menu
            break;
        case KeyEvent.VK_NUMPAD1:
        case KeyEvent.VK_END:
            parent.getGUI().moveTileCursor(Direction.SW);
            break;
        case KeyEvent.VK_NUMPAD2:
        case KeyEvent.VK_KP_DOWN:
        case KeyEvent.VK_DOWN:
            parent.getGUI().moveTileCursor(Direction.S);
            break;
        case KeyEvent.VK_NUMPAD3:
        case KeyEvent.VK_PAGE_DOWN:
            parent.getGUI().moveTileCursor(Direction.SE);
            break;
        case KeyEvent.VK_NUMPAD4:
        case KeyEvent.VK_KP_LEFT:
        case KeyEvent.VK_LEFT:
            parent.getGUI().moveTileCursor(Direction.W);
            break;
        case KeyEvent.VK_NUMPAD5:
        case KeyEvent.VK_C:
            parent.getGUI().centerActiveUnit();
            break;
        case KeyEvent.VK_NUMPAD6:
        case KeyEvent.VK_KP_RIGHT:
        case KeyEvent.VK_RIGHT:
            parent.getGUI().moveTileCursor(Direction.E);
            break;
        case KeyEvent.VK_NUMPAD7:
        case KeyEvent.VK_HOME:
            parent.getGUI().moveTileCursor(Direction.NW);
            break;
        case KeyEvent.VK_NUMPAD8:
        case KeyEvent.VK_KP_UP:
        case KeyEvent.VK_UP:
            parent.getGUI().moveTileCursor(Direction.N);
            break;
        case KeyEvent.VK_NUMPAD9:
        case KeyEvent.VK_PAGE_UP:
            parent.getGUI().moveTileCursor(Direction.NE);
            break;
        case KeyEvent.VK_SPACE:
            TerrainCursor cursor = parent.getGUI().getCursor();
            parent.showTilePopup(parent.getGUI().getSelectedTile(), cursor.getCanvasX(), cursor.getCanvasY());
            break;
        default:
            logger.info("The typed key (" + e.getKeyCode() + ") doesn't have a function yet.");
        }
    }

    private void moveUnit(KeyEvent e) {
        if (!parent.isMapboardActionsEnabled()) {
            return;
        }
        
        switch (e.getKeyCode()) {
        case KeyEvent.VK_ESCAPE:
            // main menu
            break;
        case KeyEvent.VK_NUMPAD1:
        case KeyEvent.VK_END:
            inGameController.moveActiveUnit(Direction.SW);
            break;
        case KeyEvent.VK_NUMPAD2:
        case KeyEvent.VK_KP_DOWN:
        case KeyEvent.VK_DOWN:
            inGameController.moveActiveUnit(Direction.S);
            break;
        case KeyEvent.VK_NUMPAD3:
        case KeyEvent.VK_PAGE_DOWN:
            inGameController.moveActiveUnit(Direction.SE);
            break;
        case KeyEvent.VK_NUMPAD4:
        case KeyEvent.VK_KP_LEFT:
        case KeyEvent.VK_LEFT:
            inGameController.moveActiveUnit(Direction.W);
            break;
        case KeyEvent.VK_NUMPAD5:
        case KeyEvent.VK_C:
            parent.getGUI().centerActiveUnit();
            break;
        case KeyEvent.VK_NUMPAD6:
        case KeyEvent.VK_KP_RIGHT:
        case KeyEvent.VK_RIGHT:
            inGameController.moveActiveUnit(Direction.E);
            break;
        case KeyEvent.VK_NUMPAD7:
        case KeyEvent.VK_HOME:
            inGameController.moveActiveUnit(Direction.NW);
            break;
        case KeyEvent.VK_NUMPAD8:
        case KeyEvent.VK_KP_UP:
        case KeyEvent.VK_UP:
            inGameController.moveActiveUnit(Direction.N);
            break;
        case KeyEvent.VK_NUMPAD9:
        case KeyEvent.VK_PAGE_UP:
            inGameController.moveActiveUnit(Direction.NE);
            break;
        default:
            logger.info("The typed key (" + e.getKeyCode() + ") doesn't have a function yet.");
        }
    }

    /**
     * Invoked when a key has been released.
     * 
     * @param e The event that holds key-information.
     */
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            parent.getGUI().setGotoPath(null);
        }
    }

    /**
     * Invoked when a key has been typed.
     * 
     * @param e The event that holds key-information.
     */
    public void keyTyped(KeyEvent e) {
        // Ignore for now
    }
}
