

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;


/**
* An action for clearing the active unit's orders.
*/
public class ClearOrdersAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(ClearOrdersAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = ClearOrdersAction.class.toString();


    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    ClearOrdersAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.orders.clearOrders", null, KeyStroke.getKeyStroke('L', 0));
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
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().clearOrders(getFreeColClient().getGUI().getActiveUnit());
    }
}
