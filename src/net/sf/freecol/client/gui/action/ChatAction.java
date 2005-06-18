

package net.sf.freecol.client.gui.action;

import net.sf.freecol.client.FreeColClient;

import javax.swing.AbstractButton;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;


/**
* An action for initiating chatting.
* @see MapControls
*/
public class ChatAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(ChatAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "chatAction";


    /**
    * Creates a new <code>ChatAction</code>
    */
    ChatAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.chat", null, KeyEvent.VK_T, KeyStroke.getKeyStroke('T', 0));
    }


    public void update() {
        super.update();
    }


    /**
    * Returns the id of this <code>Option</code>.
    * @return "chatAction"
    */
    public String getId() {
        return ID;
    }


    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getCanvas().showChatPanel();
    }
}
