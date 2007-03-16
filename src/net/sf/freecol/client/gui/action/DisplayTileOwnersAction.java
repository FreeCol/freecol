package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;

/**
 * 
 */
public class DisplayTileOwnersAction extends SelectableAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(DisplayTileOwnersAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2006-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "displayTileOwnersAction";


    /**
     * Creates this action
     * 
     * @param freeColClient The main controller object for the client.
     */
    DisplayTileOwnersAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.displayTileOwners", null, KeyStroke.getKeyStroke('W', Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return true if this action should be enabled.
     */
    protected boolean shouldBeEnabled() {
        return true;
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return
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
        freeColClient.getGUI().setDisplayTileOwners(((JCheckBoxMenuItem) e.getSource()).isSelected());
        freeColClient.getCanvas().refresh();
    }
}
