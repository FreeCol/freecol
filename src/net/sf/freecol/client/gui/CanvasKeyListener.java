
package net.sf.freecol.client.gui;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.control.InGameController;

import net.sf.freecol.client.control.*;

/**
* Listens to keys being pressed at the level of the Canvas.
*/
public final class CanvasKeyListener implements KeyListener {
    private static final Logger logger = Logger.getLogger(CanvasKeyListener.class.getName());
    
    private final Canvas            parent;
    private final InGameController inGameController;
    private final MapControls       mapControls;

    private KeyEvent keyEvent;

    /**
    * The constructor to use.
    * @param parent The container of all child panels.
    * @param userInputHandler The handler for the user's input.
    * @param mapControls The gui components that are used to interact with the map
    * and the objects that are located on the map.
    */
    public CanvasKeyListener(Canvas parent, InGameController inGameController, MapControls mapControls) {
        this.parent = parent;
        this.inGameController = inGameController;
        this.mapControls = mapControls;
    }


    /**
    * Invoked when a key has been pressed.
    * @param e The event that holds key-information.
    */
    public void keyPressed(KeyEvent e) {
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
            case KeyEvent.VK_W: //Unit wait:
                inGameController.nextActiveUnit();
                break;
            case KeyEvent.VK_SPACE:
                inGameController.skipActiveUnit();
                break;
            case KeyEvent.VK_ENTER:
                inGameController.endTurn();
                break;
            case KeyEvent.VK_F:
                if (parent.getGUI().getActiveUnit() != null) {
                    parent.getClient().getInGameController().changeState(parent.getGUI().getActiveUnit(), Unit.FORTIFY);
                }
                break;
            case KeyEvent.VK_S:
                if (parent.getGUI().getActiveUnit() != null) {
                    parent.getClient().getInGameController().changeState(parent.getGUI().getActiveUnit(), Unit.SENTRY);
                }
                break;
            case KeyEvent.VK_B:
                inGameController.buildColony();
                break;
            case KeyEvent.VK_P:
                if (parent.getGUI().getActiveUnit() != null) {
                    parent.getClient().getInGameController().changeState(parent.getGUI().getActiveUnit(), Unit.PLOW);
                }
                break;
            case KeyEvent.VK_R:
                if (parent.getGUI().getActiveUnit() != null) {
                    parent.getClient().getInGameController().changeState(parent.getGUI().getActiveUnit(), Unit.BUILD_ROAD);
                }
                break;
            case KeyEvent.VK_E:
                parent.showEuropePanel();
                break;
            case KeyEvent.VK_T:
                parent.showChatPanel();
                break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
                mapControls.zoomIn();
                parent.refresh();
                break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_UNDERSCORE:
                mapControls.zoomOut();
                parent.refresh();
                break;
            default:
                logger.warning("The typed key (" + e.getKeyCode() + ") doesn't have a function yet.");
        }
    }

    
    /**
    * Invoked when a key has been released.
    * @param e The event that holds key-information.
    */
    public void keyReleased(KeyEvent e) {
        // Ignore for now
    }
    
    /**
    * Invoked when a key has been typed.
    * @param e The event that holds key-information.
    */
    public void keyTyped(KeyEvent e) {
        // Ignore for now
    }
}
