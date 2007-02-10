
package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;


/**
* An action for changing view mode between move units mode and view terrain mode
* @see ViewMode
*/
public class ToggleViewModeAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ToggleViewModeAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "toggleViewModeAction";

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    ToggleViewModeAction(FreeColClient freeColClient) {
    	super(freeColClient, "menuBar.view.toggle", null,
                KeyStroke.getKeyStroke('V', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()|InputEvent.SHIFT_MASK));
    }

    /**
    * Returns the id of this <code>Option</code>.
    * @return 
    */
    public String getId() {
        return ID;
    }


    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getGUI().getViewMode().toggleViewMode();
    }
}
