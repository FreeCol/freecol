

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;


/**
* An action for initiating chatting.
* @see net.sf.freecol.client.gui.panel.MapControls
*/
public class ChatAction extends FreeColAction {
    private static final Logger logger = Logger.getLogger(ChatAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = ChatAction.class.toString();


    /**
     * Creates a new <code>ChatAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    ChatAction(FreeColClient freeColClient) {
    	super(freeColClient, "menuBar.game.chat", null, KeyStroke.getKeyStroke('T', InputEvent.CTRL_MASK));
    }


    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the mapboard is selected.
     */
    protected boolean shouldBeEnabled() { 
        return super.shouldBeEnabled()  
                && getFreeColClient().getCanvas() != null
                && (!getFreeColClient().getCanvas().isShowingSubPanel()
                        || getFreeColClient().getGame() != null
                           && getFreeColClient().getGame().getCurrentPlayer() != getFreeColClient().getMyPlayer());
    }
    
    /**
    * Returns the id of this <code>Option</code>.
    * @return "chatAction"
    */
    public String getId() {
        return ID;
    }

    
    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        getFreeColClient().getCanvas().showChatPanel();
    }
}
