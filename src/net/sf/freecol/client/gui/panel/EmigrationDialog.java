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

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;


/**
 * The panel that allows a user to choose which unit will emigrate from Europe.
 */
public final class EmigrationDialog extends FreeColChoiceDialog<Integer> {

    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param europe The <code>Europe</code> where we can find the
     *     units that are prepared to emigrate.
     * @param foy Is this emigration due to a fountain of youth?
     */
    public EmigrationDialog(FreeColClient freeColClient, Europe europe,
                            boolean foy) {
        super(freeColClient);

        final ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        final List<UnitType> recruitables
            = new ArrayList<UnitType>(europe.getRecruitables());

        String hdr = Messages.message("chooseImmigrant");
        JTextArea header = GUI.getDefaultTextArea(hdr);
        if (foy) {
            header.insert(Messages.message("lostCityRumour.fountainOfYouth")
                          + "\n\n", 0);
        }

        MigPanel panel = new MigPanel(new MigLayout("wrap 1", "[fill]", ""));
        panel.add(header, "wrap 20");
        panel.setSize(panel.getPreferredSize());

        List<ChoiceItem<Integer>> c = choices();
        int i = 1;
        UnitType u0 = recruitables.remove(0);
        c.add(new ChoiceItem<Integer>(Messages.message(u0.getNameKey()),
                new Integer(i++)).defaultOption()
            .setIcon(lib.getUnitImageIcon(u0, 0.66)));
        for (UnitType ut : recruitables) {
            c.add(new ChoiceItem<Integer>(Messages.message(ut.getNameKey()),
                    new Integer(i++))
                .setIcon(lib.getUnitImageIcon(ut, 0.66)));
        }

        initialize(false, panel, lib.getImageIcon(europe, true), null, c);
    }
}
