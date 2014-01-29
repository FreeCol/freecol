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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.StringTemplate;

import net.miginfocom.swing.MigLayout;


/**
 * This panel is used to show monarch actions.
 *
 * Generally monarch actions require a choice to accept or reject, but
 * some do not.  Therefore the value of the dialog is boolean, but
 * there may not be a meaningful accept option in some cases.  This
 * prevents just extending FreeColConfirmDialog. 
 */
public final class MonarchDialog extends FreeColDialog<Boolean> {

    /**
     * Creates a dialog to handle monarch interactions.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param action The <code>MonarchAction</code> the monarch is performing.
     * @param template The <code>StringTemplate</code> describing the action.
     * @param monarchKey The resource key for the monarch image.
     */
    public MonarchDialog(FreeColClient freeColClient,
                         MonarchAction action, StringTemplate template,
                         String monarchKey) {
        super(freeColClient);

        final ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        final Nation nation = freeColClient.getMyPlayer().getNation();
        final String messageId = "model.monarch.action." + action.toString();
        if (!Messages.containsKey(messageId)) {
            throw new IllegalStateException("Unrecognized monarch action: "
                + action);
        }
        String yesId = messageId + ".yes";
        if (!Messages.containsKey(yesId)) yesId = null;        
        String noId = messageId + ".no";
        if (!Messages.containsKey(noId)) noId = "close";

        String hdrKey = (action == MonarchAction.HESSIAN_MERCENARIES)
            ? "monarchDialog.header.fromHessians"
            : "monarchDialog.header.fromCrown";
        String hdr = Messages.message(hdrKey);
        JTextArea header = GUI.getDefaultTextArea(hdr);
        header.setFont(GUI.MEDIUM_HEADER_FONT);

        MigPanel panel = new MigPanel(new MigLayout("wrap 2, insets 10",
                                                    "[]20[]"));
        panel.add(header, "span, align center, wrap 20");
        if (action == MonarchAction.RAISE_TAX_ACT
            || action == MonarchAction.RAISE_TAX_WAR) {
            JButton helpButton = new JButton(Messages.message("help"));
            helpButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        getGUI().showColopediaPanel("colopedia.concepts.taxes");
                    }
                });
            panel.add(helpButton, "tag help");
        }
        JTextArea text = GUI.getDefaultTextArea((template == null)
            ? Messages.message(messageId)
            : Messages.message(new StringTemplate(messageId, template)),
            30);
        panel.add(text);
        panel.setSize(panel.getPreferredSize());

        List<ChoiceItem<Boolean>> c = choices();
        if (yesId != null) {
            c.add(new ChoiceItem<Boolean>(Messages.message(yesId), Boolean.TRUE)
                .okOption());
        }
        c.add(new ChoiceItem<Boolean>(Messages.message(noId), Boolean.FALSE)
            .cancelOption().defaultOption());

        initialize(DialogType.QUESTION, false, panel,
                   lib.getMiscImageIcon(monarchKey), c);
    }
}
