
package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.NumberFormatException;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.FreeColClient;


/**
 * This is the panel that pops up when the user wants to send a message to the
 * other players. There is no close button because it closes as soon as the user
 * presses enter in the textfield.
 */
public final class ChatPanel extends FreeColPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(ChatPanel.class.getName());

    public static final int    CHAT = 1;

    private final Canvas            parent;
    private final FreeColClient     freeColClient;
    private final JTextField        field;





    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public ChatPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;

        JLabel label = new JLabel("Message: ");

        field = new JTextField("");

        label.setSize(65, 20);
        field.setSize(355, 20);

        label.setLocation(10, 10);
        field.setLocation(85, 10);

        setLayout(null);

        field.setActionCommand(String.valueOf(CHAT));

        field.addActionListener(this);

        add(label);
        add(field);

        //setFocusable(false);
        label.setFocusable(false);
        field.setFocusable(true);

        setSize(450, 40);
    }

    
    
    

    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case CHAT:
                    String message = getChatText();
                    freeColClient.getInGameController().sendChat(message);
                    parent.displayChatMessage(message);
                    parent.remove(this);
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }


    /**
    * Returns the chat message that the user has entered and clears the chat
    * textfield to make room for a new message.
    * @return The chat message that the user has entered and clears the chat
    * textfield to make room for a new message.
    */
    public String getChatText() {
        String message = field.getText();
        field.setText("");
        return message;
    }


    /**
    * Requests that the chat textfield in this chat panel gets the focus.
    */
    public void requestFocus() {
        field.requestFocus();
    }
}
