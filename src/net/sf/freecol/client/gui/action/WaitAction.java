

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.FreeColClient;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;


/**
* An action for chosing the next unit as the active unit.
*/
public class WaitAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(WaitAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "waitAction";


    /**
    * Creates a new <code>WaitAction</code>.
    */
    WaitAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.0", null, KeyEvent.VK_W, KeyStroke.getKeyStroke('W', 0));
    }

    
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "waitAction"
    */
    public String getId() {
        return ID;
    }


    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().nextActiveUnit();
    }
}
