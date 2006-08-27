
package net.sf.freecol.client.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Unit;



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
    * 
    * @param canvas The component this object gets created for.
    * @param g The GUI that holds information such as screen resolution.
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
        // Ignore for now.
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
        if (!e.getComponent().isEnabled()) {
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
            canvas.showTilePopup(gui.convertToMapCoordinates(e.getX(), e.getY()), e.getX(), e.getY());
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            gui.setSelectedTile(gui.convertToMapCoordinates(e.getX(), e.getY()), true);
            canvas.requestFocus();
        }
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
            Unit unit = gui.getActiveUnit();
            canvas.getClient().getInGameController().setDestination(unit, temp.getLastNode().getTile());
            canvas.getClient().getInGameController().moveToDestination(unit);
        } else if (gui.isDragStarted()) {
            gui.stopDrag();
        }
    }
}


