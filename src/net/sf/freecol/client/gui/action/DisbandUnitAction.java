

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;


/**
* An action for disbanding the active unit.
*/
public class DisbandUnitAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(DisbandUnitAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "disbandUnitAction";


    /**
    * Creates a new <code>DisbandUnitAction</code>.
    */
    DisbandUnitAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.8", null, KeyEvent.VK_D, KeyStroke.getKeyStroke('D', 0));
    }
    
    
    
    /**
    * Updates this action. If there is no active unit,
    * then <code>setEnabled(false)</code> gets called.
    */
    public void update() {
        super.update();
        
        if (getFreeColClient().getGUI().getActiveUnit() == null) {
            setEnabled(false);
        }
    }

    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "disbandUnitAction"
    */
    public String getId() {
        return ID;
    }


    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().disbandActiveUnit();
    }
}
