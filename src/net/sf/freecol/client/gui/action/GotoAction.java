package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;

/**
 * An action for making a unit move to a specific location. This action first
 * displays a panel from which the player can choose a location the unit should
 * move towards.
 */
public class GotoAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GotoAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "gotoAction";


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    GotoAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.orders.goto", null, KeyStroke.getKeyStroke('G', 0));
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the mapboard is selected.
     */
    protected boolean shouldBeEnabled() {
        // super.shouldBeEnabled();
        return getFreeColClient().getCanvas() != null && !getFreeColClient().getCanvas().isShowingSubPanel();
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "gotoAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        Unit unit = getFreeColClient().getGUI().getActiveUnit();
        if (unit != null) {
            getFreeColClient().getInGameController().selectDestination(unit);
        }
    }
}
