package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;

/**
 * An action for displaying an about box with version numbers.
 */
public class AboutAction extends FreeColAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(AboutAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "aboutAction";


    /**
     * Creates a new <code>AboutAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    AboutAction(FreeColClient freeColClient) {
        super(freeColClient, "FreeCol " + FreeCol.getVersion(), "FreeCol " + FreeCol.getVersion(), 0, null, false);
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "waitAction"
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
        // TODO: Create an About-panel:
        freeColClient.getCanvas().errorMessage(null, "http://www.freecol.org");
    }
}
