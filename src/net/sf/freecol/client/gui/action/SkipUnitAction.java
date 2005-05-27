

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.FreeColClient;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;


/**
* An action for skipping the active unit.
*/
public class SkipUnitAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(SkipUnitAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "skipUnitAction";



    /**
    * Creates a new <code>SkipUnitAction</code>.
    */
    SkipUnitAction(FreeColClient freeColClient) {
        super(freeColClient, "unit.state.1", null, KeyEvent.VK_S, KeyStroke.getKeyStroke(' ', 0));
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
    * @return "skipUnitAction"
    */
    public String getId() {
        return ID;
    }



    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getInGameController().skipActiveUnit();
    }
}
