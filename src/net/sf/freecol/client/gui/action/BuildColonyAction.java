

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;


/**
* An action for using the active unit to build a colony.
*/
public class BuildColonyAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(BuildColonyAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "buildColonyAction";


    /**
    * Creates a new <code>BuildColonyAction</code>.
    */
    BuildColonyAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.7", null, KeyEvent.VK_B, KeyStroke.getKeyStroke('B', 0));
    }
    
    
    
    /**
    * Updates this action. If there is no active unit or the active unit cannot build a colony,
    * then <code>setEnabled(false)</code> gets called.
    */
    public void update() {
        super.update();
        
        Unit selectedOne = getFreeColClient().getGUI().getActiveUnit();
        if (selectedOne == null || selectedOne.getTile() == null || !selectedOne.canBuildColony()) {
            setEnabled(false);
        }
    }

    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "buildColonyAction"
    */
    public String getId() {
        return ID;
    }


    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().buildColony();
    }
}
