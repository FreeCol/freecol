/**
 *  Copyright (C) 2002-2011  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;


/**
 * This is the panel that pops up when the user wants to send a message to the
 * other players. There is no close button because it closes as soon as the user
 * presses enter in the textfield.
 */
public final class ChatPanel extends FreeColPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(ChatPanel.class.getName());

    public static final int    CHAT = 1;

    private final JTextField        field;

    /**
    * The constructor that will add the items to this panel.
     * @param freeColClient 
    * 
    * @param parent The parent of this panel.
    */
    public ChatPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui);

        JLabel label = new JLabel("Message: ");

        field = new JTextField("", 40);

        setLayout(new BorderLayout(10, 10));

        field.setActionCommand(String.valueOf(CHAT));

        field.addActionListener(this);

        add(label);
        add(field);

        //setFocusable(false);
        label.setFocusable(false);
        field.setFocusable(true);

        setSize(getPreferredSize());
    }

    
    
    

    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case CHAT:
                    String message = getChatText();
                    getController().sendChat(message);
                    getGUI().displayChatMessage(message, false);
                    getCanvas().remove(this);
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
    @Override
    public void requestFocus() {
        field.requestFocus();
    }
}
