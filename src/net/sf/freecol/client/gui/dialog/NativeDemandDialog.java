/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * This panel is used to show native demands at a colony.
 */
public final class NativeDemandDialog extends FreeColConfirmDialog {

    /**
     * Creates a dialog to handle native demands interactions.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param unit The demanding {@code Unit}.
     * @param colony The {@code Colony} being demanded of.
     * @param type The {@code GoodsType} demanded (may be null for gold).
     * @param amount The amount of goods demanded.
     */
    public NativeDemandDialog(FreeColClient freeColClient, JFrame frame,
                              Unit unit, Colony colony,
                              GoodsType type, int amount) {
        super(freeColClient, frame);

        final String nation = Messages.message(unit.getOwner().getNationLabel());
        StringTemplate template;
        String yes, no;
        if (type == null) {
            template = StringTemplate.template("indianDemand.gold.text")
                .addName("%nation%", nation)
                .addName("%colony%", colony.getName())
                .addAmount("%amount%", amount);
            yes = "accept";
            no = "indianDemand.gold.no";
        } else if (type.isFoodType()) {
            template = StringTemplate.template("indianDemand.food.text")
                .addName("%nation%", nation)
                .addName("%colony%", colony.getName())
                .addAmount("%amount%", amount);
            yes = "indianDemand.food.yes";
            no = "indianDemand.food.no";
        } else {
            template = StringTemplate.template("indianDemand.other.text")
                .addName("%nation%", nation)
                .addName("%colony%", colony.getName())
                .addAmount("%amount%", amount)
                .addNamed("%goods%", type);
            yes = "accept";
            no = "indianDemand.other.no";
        }

        JPanel panel = new MigPanel(new MigLayout("wrap 1, fill",
                                                  "[400, align center]"));
        JLabel header = Utility.localizedHeaderLabel(StringTemplate
                .template("nativeDemandDialog.name")
                .addName("%colony%", colony.getName()),
            SwingConstants.LEADING, FontLibrary.FontSize.BIG);
        panel.add(header);
        JTextArea text = Utility.localizedTextArea(template);
        panel.add(text);

        final ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        ImageIcon icon = new ImageIcon(lib.getSmallSettlementImage(colony));

        initializeConfirmDialog(frame, true, panel, icon, yes, no);
    }
}
