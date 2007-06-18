package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Unit;

/**
 * An action for assigning a trade route to the currently selected unit.
 */
public class AssignTradeRouteAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(AssignTradeRouteAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "assignTradeRouteAction";


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    AssignTradeRouteAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.orders.assignTradeRoute", null, KeyStroke.getKeyStroke('A', 0));
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "assignTradeRouteAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if there is a carrier active
     */
    protected boolean shouldBeEnabled() {
        if (super.shouldBeEnabled()) {
            GUI gui = getFreeColClient().getGUI();
            if (gui != null) {
                Unit unit = getFreeColClient().getGUI().getActiveUnit();
                return (unit != null && unit.isCarrier());
            }
        }
        return false;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        Unit unit = getFreeColClient().getGUI().getActiveUnit();
        if (unit != null) {
            getFreeColClient().getInGameController().assignTradeRoute(unit);
        }
    }
}
