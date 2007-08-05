

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;


/**
* An action for editing trade routes.
*/
public class TradeRouteAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TradeRouteAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "tradeRouteAction";

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    TradeRouteAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.tradeRoutes", null, KeyEvent.VK_NUMBER_SIGN);
    }
    
    
    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if there is an active unit.
     */
    protected boolean shouldBeEnabled() { 
        return true;
    }
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "tradeRouteAction"
    */
    public String getId() {
        return ID;
    }


    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getCanvas().showTradeRouteDialog(null);
    }
}
