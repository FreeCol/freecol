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

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.UnitType;


/**
 * The panel that allows a user to choose which unit will emigrate from Europe.
 */
public final class EmigrationDialog extends FreeColChoiceDialog<Integer> {

    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param europe The <code>Europe</code> where we can find the
     *     units that are prepared to emigrate.
     * @param foy Is this emigration due to a fountain of youth?
     */
    public EmigrationDialog(FreeColClient freeColClient, JFrame frame,
            Europe europe, boolean foy) {
        super(freeColClient, frame);

        final ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        final List<UnitType> recruitables
            = new ArrayList<>(europe.getRecruitables());

        JTextArea header
            = Utility.localizedTextArea("emigrationDialog.chooseImmigrant");
        if (foy) {
            header.insert(Messages.message(LostCityRumour.RumourType.FOUNTAIN_OF_YOUTH.getDescriptionKey())
                          + "\n\n", 0);
        }

        MigPanel panel = new MigPanel(new MigLayout("wrap 1", "[fill]", ""));
        panel.add(header, "wrap 20");
        panel.setSize(panel.getPreferredSize());

        List<ChoiceItem<Integer>> c = choices();
        int i = Europe.MigrationType.getDefaultSlot();
        UnitType u0 = recruitables.remove(0);
        c.add(new ChoiceItem<>(Messages.getName(u0), i++)
            .defaultOption().setIcon(new ImageIcon(lib.getSmallUnitImage(u0))));
        for (UnitType ut : recruitables) {
            c.add(new ChoiceItem<>(Messages.getName(ut), i++)
                .setIcon(new ImageIcon(lib.getSmallUnitImage(ut))));
        }

        initializeChoiceDialog(frame, false, panel, null, null, c);
    }
}
