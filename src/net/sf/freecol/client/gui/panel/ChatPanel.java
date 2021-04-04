/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;


/**
 * This is the panel that pops up when the user wants to send a
 * message to the other players. There is no close button because it
 * closes as soon as the user presses enter in the textfield.
 */
public final class ChatPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(ChatPanel.class.getName());

    public static final int CHAT = 1;

    private final JTextField field;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ChatPanel(FreeColClient freeColClient) {
        super(freeColClient, null, new BorderLayout(10, 10));

        JLabel label = Utility.localizedLabel("chatPanel.message");
        add(label, BorderLayout.PAGE_START);
        label.setFocusable(false);

        this.field = new JTextField("", 40);
        this.field.setActionCommand(String.valueOf(CHAT));
        this.field.addActionListener(this);
        add(this.field, BorderLayout.CENTER);
        this.field.setFocusable(true);

        setSize(getPreferredSize());
    }


    /**
     * Requests that the chat textfield in this chat panel gets the focus.
     */
    @Override
    public void requestFocus() {
        this.field.requestFocus();
    }

    /**
     * Gets the chat message that the user has entered and clears the
     * chat textfield to make room for a new message.
     *
     * @return The chat message.
     */
    public String getChatText() {
        String message = this.field.getText();
        this.field.setText("");
        return message;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        try {
            switch (Integer.parseInt(command)) {
            case CHAT:
                String message = getChatText();
                igc().chat(message);
                getGUI().removeComponent(this);
                break;
            default:
                super.actionPerformed(ae);
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid ActionEvent, not a number: " + command);
        }
    }
}
