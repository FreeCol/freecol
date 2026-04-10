/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.client.gui.dialog;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.DialogHandler;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.FreeColButton.ButtonStyle;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;


/**
 * Dialog to display on first contact with a native player.
 */
public class FirstContactDialog extends FreeColPanel {

    private static final String BASE_KEY = "firstContactDialog.meeting.";
    private static final String NATIVES = "natives";
    private static final String TUTORIAL_KEY = BASE_KEY + NATIVES + ".tutorial";
    
    
    /**
     * Create an FirstContactDialog.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param player The {@code Player} making contact.
     * @param other The {@code Player} to contact.
     * @param tile An optional {@code Tile} on offer.
     * @param settlementCount The number of settlements the other
     *     player has.
     */
    public FirstContactDialog(FreeColClient freeColClient, Player player, Player other,
            Tile tile, int settlementCount, DialogHandler<Boolean> handler) {
        super(freeColClient, null, new MigLayout("wrap 1, fill", "[center]", "[]unrel[]unrel[growprio 200]unrel[]"));

        String headerKey = BASE_KEY + other.getNation().getSuffix();
        if (!Messages.containsKey(headerKey)) {
            headerKey = BASE_KEY + NATIVES;
        }
        final JLabel header = Utility.localizedHeaderLabel(headerKey, SwingConstants.LEADING, Utility.FONTSPEC_SUBTITLE);
        final JLabel image = new JLabel(new ImageIcon(ImageLibrary.getMeetingImage(other)));
        image.setOpaque(false);

        final String messageId = (tile != null) ? "firstContactDialog.welcomeOffer.text" : "firstContactDialog.welcomeSimple.text";
        final String type = ((IndianNationType)other.getNationType()).getSettlementTypeKey(true);
        final JTextArea text = Utility.localizedTextArea(StringTemplate
            .template(messageId)
            .addStringTemplate("%nation%", other.getNationLabel())
            .addName("%camps%", Integer.toString(settlementCount))
            .add("%settlementType%", type));

        // Resize the text areas to better match the image.
        final int columns = (int) Math.floor(text.getColumns() * image.getPreferredSize().getWidth() / text.getPreferredSize().getWidth());
        text.setColumns(columns);
        text.setSize(text.getPreferredSize());

        add(header);
        add(image);
        add(text, "grow, wmin 100");
        
        okButton = Utility.localizedButton("yes").withButtonStyle(ButtonStyle.IMPORTANT);
        okButton.addActionListener(ae -> {
            getGUI().removeComponent(this);
            handler.handle(Boolean.TRUE);
        });
        add(okButton, "newline, split 3, tag ok");
        
        setEscapeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                okButton.doClick();
            }
        });
        
        final JButton cancelButton = Utility.localizedButton("no");
        cancelButton.addActionListener(ae -> {
            getGUI().removeComponent(this);
            handler.handle(Boolean.FALSE);
        });
        add(cancelButton, "tag cancel");
        
        final JButton helpButton = Utility.localizedButton("help");
        helpButton.addActionListener(ae -> {
            getGUI().showInformationPanel(TUTORIAL_KEY); 
        });
        add(helpButton, "tag help");
    }
}
