

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Player;


/**
 * An action for declaring independence.
 */
public class DeclareIndependenceAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(DeclareIndependenceAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "declareIndependenceAction";
    
    /**
     * Creates a new <code>DeclareIndependenceAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    DeclareIndependenceAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.declareIndependence", null);
    }
    
    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the player can declare independence.
     */
    protected boolean shouldBeEnabled() { 
        Player p = getFreeColClient().getMyPlayer();
        return super.shouldBeEnabled() && p != null 
                && p.getRebellionState() == Player.REBELLION_PRE_WAR;
    }    
    
    /**
     * Returns the id of this <code>Option</code>.
     * @return "declareIndependenceAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().declareIndependence();
        update();
    }
}
