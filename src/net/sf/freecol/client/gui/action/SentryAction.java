
package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Unit;

/**
* An action to set sentry state to the active unit.
*/
public class SentryAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SentryAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2006-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    public static final String ID = "sentryAction";
    
    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    public SentryAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.3", null, KeyStroke.getKeyStroke('S', 0));
        putValue(BUTTON_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_SENTRY, 0));
        putValue(BUTTON_ROLLOVER_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_SENTRY, 1));
        putValue(BUTTON_PRESSED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_SENTRY, 2));
        putValue(BUTTON_DISABLED_IMAGE, freeColClient.getImageLibrary().getUnitButtonImageIcon(ImageLibrary.UNIT_BUTTON_SENTRY, 3));        
    }
    
    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active unit.
     */
    protected boolean shouldBeEnabled() { 
        return super.shouldBeEnabled() 
                && getFreeColClient().getGUI().getActiveUnit() != null;
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
    public void actionPerformed(ActionEvent actionEvent) {
        getFreeColClient().getInGameController().changeState(getFreeColClient().getGUI().getActiveUnit(), Unit.SENTRY);
    }
    
}
