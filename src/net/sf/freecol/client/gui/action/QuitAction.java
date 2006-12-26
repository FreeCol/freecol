

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;


/**
* An action for chosing the next unit as the active unit.
*/
public class QuitAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(QuitAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "quitAction";
    
    /**
     * Creates a new <code>WaitAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    QuitAction(FreeColClient freeColClient) {
    	super(freeColClient, "menuBar.game.quit", null, KeyStroke.getKeyStroke('Q', InputEvent.CTRL_MASK));
        putValue(BUTTON_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 0));
        putValue(BUTTON_ROLLOVER_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 1));
        putValue(BUTTON_PRESSED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 2));
        putValue(BUTTON_DISABLED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_WAIT, 3));
    }
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "waitAction"
    */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
    	getFreeColClient().getCanvas().quit();
    }
}
