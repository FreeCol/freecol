package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * An action for declaring independence.
 */
public class ReconnectAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ReconnectAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "reconnectAction";


    /**
     * Creates a new <code>DeclareIndependenceAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    ReconnectAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.reconnect", null, KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit
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
     * @return "declareIndependenceAction"
     */
    public String getId() {
        return ID;
    }

    public String getMenuItemText() {
        return Messages.message("menuBar.game.reconnect");
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        freeColClient.getConnectController().reconnect();
    }
}
