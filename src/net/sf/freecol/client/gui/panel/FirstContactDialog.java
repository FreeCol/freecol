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

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Dialog to display on first contact with a native player.
 */
public class FirstContactDialog extends FreeColConfirmDialog {

    private static final String BASE_KEY = "firstContactDialog.meeting.";
    private static final String IMAGE_BASE_KEY = "image.flavor.event.meeting.";
    private static final String NATIVES_KEY = "natives";
    private static final String TUTORIAL_KEY = BASE_KEY + NATIVES_KEY + ".tutorial";


    /**
     * Create an FirstContactDialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param player The <code>Player</code> making contact.
     * @param other The <code>Player</code> to contact.
     * @param tile An optional <code>Tile</code> on offer.
     * @param settlementCount The number of settlements the other
     *     player has.
     */
    public FirstContactDialog(FreeColClient freeColClient, JFrame frame,
            Player player, Player other, Tile tile, int settlementCount) {
        super(freeColClient, frame);

        MigPanel panel
            = new MigPanel(new MigLayout("wrap 1", "[center]", "[]20"));
        panel.setOpaque(false);

        String headerKey = BASE_KEY + other.getNation().getSuffix();
        String imageKey = IMAGE_BASE_KEY + other.getNationResourceKey();
        if (!Messages.containsKey(headerKey)) {
            headerKey = BASE_KEY + NATIVES_KEY;
            imageKey = IMAGE_BASE_KEY + NATIVES_KEY;
        }
        JLabel header = Utility.localizedHeaderLabel(headerKey,
            SwingConstants.LEADING, FontLibrary.FontSize.MEDIUM);
        JLabel image
            = new JLabel(new ImageIcon(ResourceManager.getImage(imageKey)));
        image.setOpaque(false);

        JTextArea tutorial = null;
        if (!player.hasContactedIndians() && freeColClient.tutorialMode()) {
            tutorial = Utility.localizedTextArea(TUTORIAL_KEY);
        }

        String messageId = (tile != null)
            ? "firstContactDialog.welcomeOffer.text"
            : "firstContactDialog.welcomeSimple.text";
        String type = ((IndianNationType)other.getNationType())
            .getSettlementTypeKey(true);
        JTextArea text = Utility.localizedTextArea(StringTemplate
            .template(messageId)
            .addStringTemplate("%nation%", other.getNationLabel())
            .addName("%camps%", Integer.toString(settlementCount))
            .add("%settlementType%", type));

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

        ImageIcon icon = new ImageIcon(
            getImageLibrary().getMiscIconImage(other.getNation()));
        initializeConfirmDialog(frame, false, panel, icon, "yes", "no");
    }
}
