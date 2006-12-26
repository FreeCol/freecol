

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;


/**
* An action for chosing the next unit as the active unit.
*/
public class ExecuteGotoOrdersAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(ExecuteGotoOrdersAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "executeGotoOrdersAction";


    /**
     * Creates a new <code>ExecuteGotoOrdersAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    ExecuteGotoOrdersAction(FreeColClient freeColClient) {
        super(freeColClient, "executeGotoOrders", null, KeyStroke.getKeyStroke('O', 0));
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
        getFreeColClient().getInGameController().executeGotoOrders();
    }
}
