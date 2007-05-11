package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.MainPanel;

/**
 * Returns to the <code>MainPanel</code>.
 * All in-game components are removed.
 * 
 * @see MainPanel
 */
public class ShowMainAction extends FreeColAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ShowMainAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "showMainAction";


    /**
     * Creates this action.
     * 
     * @param freeColClient The main controller object for the client.
     */
    ShowMainAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.returnToMain", null, null);
    }

    /**
     * Checks if this action should be enabled.
     * @return <code>true</code>
     */
    protected boolean shouldBeEnabled() {
        return true;
    }

    /**
     * Returns the id of this <code>Option</code>.
     * @return The <code>String</code>: "showMainAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        if (!getFreeColClient().getCanvas().showConfirmDialog("stopCurrentGame.text", "stopCurrentGame.yes", "stopCurrentGame.no")) {
            return;
        }
        
        getFreeColClient().getCanvas().removeInGameComponents();
        getFreeColClient().setMapEditor(false);
        getFreeColClient().setGame(null);
        getFreeColClient().getCanvas().returnToTitle();
    }
}
