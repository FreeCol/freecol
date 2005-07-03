

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.FreeColClient;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;


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

    
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getCanvas().showEuropePanel();
    }
}
