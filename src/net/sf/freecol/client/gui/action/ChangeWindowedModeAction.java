package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import javax.swing.KeyStroke;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;

/**
 * An action for chosing the next unit as the active unit.
 */
public class ChangeWindowedModeAction extends SelectableAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(QuitAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision: 1945 $";

    public static final String ID = "changeWindowedModeAction";


    /**
     * Creates a new <code>ChangeWindowedModeAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    ChangeWindowedModeAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.view.changeWindowedModeAction", null, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_MASK));
    }
    
    /**
     * Updates the "enabled"-status 
     */
    @Override
    public void update() {
        selected = !getFreeColClient().isWindowed();
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "changeWindowedModeAction"
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
        getFreeColClient().changeWindowedMode(!getFreeColClient().isWindowed());
    }
}
