

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;


/**
* An action for fortifying the active unit.
*/
public class FortifyAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(FortifyAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "fortifyAction";


    /**
    * Creates a new <code>FortifyAction</code>.
    */
    FortifyAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.2", null, KeyEvent.VK_F, KeyStroke.getKeyStroke('F', 0));
    }
    
    
    
    /**
    * Updates this action. If there is no active unit,
    * then <code>setEnabled(false)</code> gets called.
    */
    public void update() {
        super.update();
        
        if (getFreeColClient().getGUI().getActiveUnit() == null) {
            setEnabled(false);
        } else if (isEnabled()) {
            setEnabled(getFreeColClient().getGUI().getActiveUnit().checkSetState(Unit.FORTIFY));
        }
    }

    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "fortifyAction"
    */
    public String getId() {
        return ID;
    }


    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().changeState(getFreeColClient().getGUI().getActiveUnit(), Unit.FORTIFY);
    }
}
