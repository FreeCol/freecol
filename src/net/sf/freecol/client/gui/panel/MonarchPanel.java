/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.StringTemplate;


/**
 * This panel is used to show monarch actions.
 */
public final class MonarchPanel extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(MonarchPanel.class.getName());

 
    /**
     * Creates a panel to handle monarch interactions.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param action The <code>MonarchAction</code> the monarch is performing.
     * @param template The <code>StringTemplate</code> describing the action.
     */
    public MonarchPanel(FreeColClient freeColClient,
                        MonarchAction action, StringTemplate template) {
        super(freeColClient, new MigLayout("wrap 2, insets 10", "[]20[]"));

        String messageId = "model.monarch.action." + action.toString();
        String yesId = messageId + ".yes";
        String noId = messageId + ".no";

        if (!Messages.containsKey(messageId)) {
            logger.warning("Unrecognized monarch action: " + action);
            return;
        }

        JLabel header = new JLabel(Messages.message("aMessageFromTheCrown"));
        header.setFont(mediumHeaderFont);
        add(header, "span, align center, wrap 20");

        Nation nation = getMyPlayer().getNation();
        add(new JLabel(getLibrary().getMonarchImageIcon(nation)));
        add(getDefaultTextArea((template == null)
                ? Messages.message(messageId)
                : Messages.message(new StringTemplate(messageId, template))));

        boolean haveOK = false;
        if (Messages.containsKey(yesId)) {
            okButton.setText(Messages.message(yesId));
            haveOK = true;
        }
        if (!Messages.containsKey(noId)) noId = "close";
        cancelButton.setText(Messages.message(noId));
        if (haveOK) {
            if (action == MonarchAction.RAISE_TAX_ACT
                || action == MonarchAction.RAISE_TAX_WAR) {
                add(okButton, "newline 20, span, tag ok, split 3");
                JButton helpButton = new JButton(Messages.message("help"));
                helpButton.setActionCommand(HELP);
                helpButton.addActionListener(this);
                add(helpButton, "tag help");
            } else {
                add(okButton, "newline 20, span, tag ok, split 2");
            }
            add(cancelButton, "tag cancel");
        } else {
            add(cancelButton, "newline 20, span, tag cancel");
        }
        setSize(getPreferredSize());
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            setResponse(Boolean.TRUE);
        } else if (CANCEL.equals(command)) {
            setResponse(Boolean.FALSE);
        } else if (HELP.equals(command)) {
            getGUI().showColopediaPanel("colopedia.concepts.taxes");
        } else {
            super.actionPerformed(event);
        }
    }
}
