/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.StringTemplate;


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
     * @param frame The owner frame.
     * @param action The <code>MonarchAction</code> the monarch is performing.
     * @param template The <code>StringTemplate</code> describing the action.
     * @param monarchKey The resource key for the monarch image.
     */
    public MonarchDialog(FreeColClient freeColClient, JFrame frame,
            MonarchAction action, StringTemplate template, String monarchKey) {
        super(freeColClient, frame);

        final ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        final String messageId = action.getTextKey();
        if (!Messages.containsKey(messageId)) {
            throw new IllegalStateException("Unrecognized monarch action: "
                + action);
        }
        String yesId = action.getYesKey();
        if (!Messages.containsKey(yesId)) yesId = null;        
        String noId = action.getNoKey();
        if (!Messages.containsKey(noId)) noId = "close";

        String hdrKey = action.getHeaderKey();
        if (!Messages.containsKey(hdrKey)) {
            hdrKey = "monarchDialog.default";
        }
        JLabel header = Utility.localizedHeaderLabel(hdrKey,
            SwingConstants.LEADING, FontLibrary.FontSize.MEDIUM);

        MigPanel panel = new MigPanel(new MigLayout("wrap 2, insets 10",
                                                    "[]20[]"));
        panel.add(header, "span, align center, wrap 20");
        if (action == MonarchAction.RAISE_TAX_ACT
            || action == MonarchAction.RAISE_TAX_WAR) {
            JButton helpButton = Utility.localizedButton("help");
            helpButton.addActionListener((ActionEvent ae) -> {
                    getGUI().showColopediaPanel("colopedia.concepts.taxes");
                });
            panel.add(helpButton, "tag help");
        }
        JTextArea text = (template == null)
            ? Utility.localizedTextArea(messageId, 30)
            : Utility.localizedTextArea(StringTemplate.copy(messageId, template), 30);
        panel.add(text);
        panel.setSize(panel.getPreferredSize());

        List<ChoiceItem<Boolean>> c = choices();
        if (yesId != null) {
            c.add(new ChoiceItem<>(Messages.message(yesId), Boolean.TRUE)
                .okOption());
        }
        c.add(new ChoiceItem<>(Messages.message(noId), Boolean.FALSE)
            .cancelOption().defaultOption());

        initializeDialog(frame, DialogType.QUESTION, false, panel,
                         new ImageIcon(lib.getMiscImage(monarchKey)), c);
    }
}
