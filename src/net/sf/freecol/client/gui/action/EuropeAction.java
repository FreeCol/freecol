

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;


/**
* An action for displaying the {@link net.sf.freecol.client.gui.panel.EuropePanel}.
*/
public class EuropeAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(EuropeAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "europeAction";


    /**
    * Creates a new <code>EuropeAction</code>.
    */
    EuropeAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.europe", null, KeyEvent.VK_E, KeyStroke.getKeyStroke('E', 0));
    }
    

    /**
    * Returns the id of this <code>Option</code>.
    * @return "europeAction"
    */
    public String getId() {
        return ID;
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the player has access to Europe.
     */
    protected boolean shouldBeEnabled() { 
        return super.shouldBeEnabled() 
                && getFreeColClient().getMyPlayer() != null 
                && getFreeColClient().getMyPlayer().getEurope() != null;
    }    
    
    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getCanvas().showEuropePanel();
    }
}
