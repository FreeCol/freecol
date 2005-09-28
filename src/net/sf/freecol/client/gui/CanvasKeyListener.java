
package net.sf.freecol.client.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.common.model.Map;


/**
* Listens to keys being pressed at the level of the Canvas.
*/
public final class CanvasKeyListener implements KeyListener {
    private static final Logger logger = Logger.getLogger(CanvasKeyListener.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final Canvas            parent;
    private final InGameController inGameController;

    //private KeyEvent keyEvent;

    
    /**
    * The constructor to use.
    * @param parent The container of all child panels.
    */
    public CanvasKeyListener(Canvas parent, InGameController inGameController) {
        this.parent = parent;
        this.inGameController = inGameController;
    }


    /**
    * Invoked when a key has been pressed.
    * @param e The event that holds key-information.
    */
    public void keyPressed(KeyEvent e) {
        if (parent.getGUI().isDragStarted()) {
            parent.getGUI().stopDrag();
        }

        if (e.isShiftDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_D) {
            inGameController.setInDebugMode(!FreeCol.isInDebugMode());
            return;
        }

        if (e.getModifiers() != 0) {
            return;
        }

        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                //main menu
                break;
            case KeyEvent.VK_NUMPAD1:
            case KeyEvent.VK_END:
                inGameController.moveActiveUnit(Map.SW);
                break;
            case KeyEvent.VK_NUMPAD2:
            case KeyEvent.VK_KP_DOWN:
            case KeyEvent.VK_DOWN:
                inGameController.moveActiveUnit(Map.S);
                break;
            case KeyEvent.VK_NUMPAD3:
            case KeyEvent.VK_PAGE_DOWN:
                inGameController.moveActiveUnit(Map.SE);
                break;
            case KeyEvent.VK_NUMPAD4:
            case KeyEvent.VK_KP_LEFT:
            case KeyEvent.VK_LEFT:
                inGameController.moveActiveUnit(Map.W);
                break;
            case KeyEvent.VK_NUMPAD5:
            case KeyEvent.VK_C:
                inGameController.centerActiveUnit();
                break;
            case KeyEvent.VK_NUMPAD6:
            case KeyEvent.VK_KP_RIGHT:
            case KeyEvent.VK_RIGHT:
                inGameController.moveActiveUnit(Map.E);
                break;
            case KeyEvent.VK_NUMPAD7:
            case KeyEvent.VK_HOME:
                inGameController.moveActiveUnit(Map.NW);
                break;
            case KeyEvent.VK_NUMPAD8:
            case KeyEvent.VK_KP_UP:
            case KeyEvent.VK_UP:
                inGameController.moveActiveUnit(Map.N);
                break;
            case KeyEvent.VK_NUMPAD9:
            case KeyEvent.VK_PAGE_UP:
                inGameController.moveActiveUnit(Map.NE);
                break;
            case KeyEvent.VK_S:
                /*
                if (parent.getGUI().getActiveUnit() != null) {
                    parent.getClient().getInGameController().changeState(parent.getGUI().getActiveUnit(), Unit.SENTRY);
                }
                */
                break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
                //mapControls.zoomIn();
                //parent.refresh();
                break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_UNDERSCORE:
                //mapControls.zoomOut();
                //parent.refresh();
                break;
            default:
                logger.info("The typed key (" + e.getKeyCode() + ") doesn't have a function yet.");
        }
    }

    
    /**
    * Invoked when a key has been released.
    * @param e The event that holds key-information.
    */
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            parent.getGUI().setDragPath(null);
        }
    }
    
    /**
    * Invoked when a key has been typed.
    * @param e The event that holds key-information.
    */
    public void keyTyped(KeyEvent e) {
        // Ignore for now
    }
}
