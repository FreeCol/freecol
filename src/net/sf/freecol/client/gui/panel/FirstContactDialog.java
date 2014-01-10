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

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


/**
 * Dialog to display on first contact with a native player.
 */
public class FirstContactDialog extends FreeColConfirmDialog {

    private static String BASE_KEY = "event.meeting.";
    private static String IMAGE_BASE_KEY = "EventImage.meeting.";
    private static String NATIVES_KEY = "natives";
    private static String TUTORIAL_KEY = BASE_KEY + NATIVES_KEY + ".tutorial";


    /**
     * Create an FirstContactDialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param player The <code>Player</code> making contact.
     * @param other The <code>Player</code> to contact.
     * @param tile An optional <code>Tile</code> on offer.
     * @param settlementCount The number of settlements the other
     *     player has.
     */
    public FirstContactDialog(FreeColClient freeColClient, Player player,
                              Player other, Tile tile, int settlementCount) {
        super(freeColClient);

        MigPanel panel
            = new MigPanel(new MigLayout("wrap 1", "[center]", "[]20"));

        String headerKey = BASE_KEY + other.getNationNameKey();
        String imageKey = IMAGE_BASE_KEY + other.getNationNameKey();
        if (!Messages.containsKey(headerKey)) {
            headerKey = BASE_KEY + NATIVES_KEY;
            imageKey = IMAGE_BASE_KEY + NATIVES_KEY;
        }
        JLabel header = new JLabel(Messages.message(headerKey));
        header.setFont(GUI.MEDIUM_HEADER_FONT);
        JLabel image
            = new JLabel(new ImageIcon(ResourceManager.getImage(imageKey)));

        JTextArea tutorial = null;
        if (!player.hasContactedIndians() && freeColClient.getClientOptions()
            .getBoolean("model.option.guiShowTutorial")) {
            tutorial = GUI.getDefaultTextArea(Messages.message(TUTORIAL_KEY));
        }

        String messageId = (tile != null) ? "welcomeOffer.text"
            : "welcomeSimple.text";
        String type = ((IndianNationType)other.getNationType())
            .getSettlementTypeKey(true);
        StringTemplate template = StringTemplate.template(messageId)
            .addStringTemplate("%nation%", other.getNationName())
            .addName("%camps%", Integer.toString(settlementCount))
            .add("%settlementType%", type);
        JTextArea text = GUI.getDefaultTextArea(Messages.message(template));

        // Resize the text areas to better match the image.
        int columns = (int)Math.floor(text.getColumns()
            * image.getPreferredSize().getWidth()
            / text.getPreferredSize().getWidth());
        text.setColumns(columns);
        text.setSize(text.getPreferredSize());
        if (tutorial != null) {
            tutorial.setColumns(columns);
            tutorial.setSize(tutorial.getPreferredSize());
        }

        panel.add(header);
        panel.add(image);
        if (tutorial != null) panel.add(tutorial);
        panel.add(text);
        panel.setSize(panel.getPreferredSize());

        initialize(false, panel, getImageLibrary().getImageIcon(other, false),
                   "welcome.yes", "welcome.no");
    }
}
