package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;

/**
 * 
 */
public class NewAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(NewAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "newAction";


    /**
     * Creates this action
     * 
     * @param freeColClient The main controller object for the client.
     */
    NewAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.new", null, KeyStroke.getKeyStroke('N', Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMask()));
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
     * @return "newAction"
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
        if (!freeColClient.isMapEditor()) {
            freeColClient.getCanvas().newGame();
        } else {
            freeColClient.getMapEditorController().newMap();
        }
    }
}
