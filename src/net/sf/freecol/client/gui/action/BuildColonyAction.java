

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Unit;


/**
* An action for using the active unit to build a colony.
*/
public class BuildColonyAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(BuildColonyAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "buildColonyAction";

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    BuildColonyAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.7", null, KeyStroke.getKeyStroke('B', 0));
        putValue(BUTTON_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_BUILD, 0));
        putValue(BUTTON_ROLLOVER_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_BUILD, 1));
        putValue(BUTTON_PRESSED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_BUILD, 2));
        putValue(BUTTON_DISABLED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_BUILD, 3));
    }
    
    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active unit or 
     *      the active unit cannot build a colony, and <code>true</code>
     *      otherwise.
     */
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) {
            return false;
        }
        Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
        return selectedOne != null && selectedOne.getTile() != null && selectedOne.canBuildColony();
    }
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return 
    */
    public String getId() {
        return ID;
    }


    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().buildColony();
    }
}
