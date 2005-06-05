
package net.sf.freecol.client.gui;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.PathNode;



/**
* Listens to mouse buttons being pressed at the
* level of the Canvas.
*/
public final class CanvasMouseListener implements MouseListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final Canvas canvas;
    private final GUI               gui;

    /**
    * The constructor to use.
    * @param g The GUI that holds information such as screen resolution.
    * @param uih The handler for the user's input.
    */
    public CanvasMouseListener(Canvas canvas, GUI g) {
        this.canvas = canvas;
        gui = g;
    }

    /**
    * Invoked when a mouse button was clicked.
    * @param e The MouseEvent that holds all the information.
    */
    public void mouseClicked(MouseEvent e) {
        if (!e.getComponent().isEnabled()) {
            return;
        }

        /*
        This doesn't seem to work, weird...
        if (e.isPopupTrigger()) {
            userInputHandler.popupRequested(gui.convertToMapCoordinates(e.getX(), e.getY()), e.getX(), e.getY());
        } else {
            userInputHandler.tileSelected(gui.convertToMapCoordinates(e.getX(), e.getY()));
        }*/

        if (e.getButton() == MouseEvent.BUTTON1) {
            gui.setSelectedTile(gui.convertToMapCoordinates(e.getX(), e.getY()));
            canvas.requestFocus();
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            canvas.showTilePopup(gui.convertToMapCoordinates(e.getX(), e.getY()), e.getX(), e.getY());
        }
    }

    /**
    * Invoked when the mouse enters the component.
    * @param e The MouseEvent that holds all the information.
    */
    public void mouseEntered(MouseEvent e) {
        // Ignore for now.
    }

    /**
    * Invoked when the mouse exits the component.
    * @param e The MouseEvent that holds all the information.
    */
    public void mouseExited(MouseEvent e) {
        // Ignore for now.
    }

    /**
    * Invoked when a mouse button was pressed.
    * @param e The MouseEvent that holds all the information.
    */
    public void mousePressed(MouseEvent e) {
        // Ignore for now.
    }

    /**
    * Invoked when a mouse button was released.
    * @param e The MouseEvent that holds all the information.
    */
    public void mouseReleased(MouseEvent e) {
        if (gui.getDragPath() != null) {
            // A mouse drag has ended (see CanvasMouseMotionListener).

            PathNode temp = gui.getDragPath();

            gui.stopDrag();
            
            // Move the unit:
            if (temp != null && temp.getTurns() == 0 
                    && gui.getActiveUnit().getMoveType(temp.getDirection()) != Unit.ILLEGAL_MOVE) {
                canvas.getClient().getInGameController().moveActiveUnit(temp.getDirection());
                temp = temp.next;
            }
            while (temp != null && temp.getTurns() == 0) {
                if (gui.getActiveUnit() == null) {
                    break;
                }
                if (gui.getActiveUnit().getMoveType(temp.getDirection()) != Unit.MOVE) {
                    break;
                }
                canvas.getClient().getInGameController().moveActiveUnit(temp.getDirection());
                temp = temp.next;
            }
        } else if (gui.isDragStarted()) {
            gui.stopDrag();
        }
    }
}
