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
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.DialogHandler;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.LostCityRumour;


/**
 * The panel that allows a user to choose which unit will emigrate from Europe.
 */
public final class EmigrationDialog extends FreeColPanel {

    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param europe The {@code Europe} where we can find the
     *     units that are prepared to emigrate.
     * @param foy Is this emigration due to a fountain of youth?
     */
    public EmigrationDialog(FreeColClient freeColClient,  Europe europe, boolean foy, DialogHandler<Integer> handler) {
        super(freeColClient, null, new MigLayout("wrap 1", "[fill]", ""));
        
        final List<AbstractUnit> recruitables = new ArrayList<>(europe.getExpandedRecruitables(false));
        final JTextArea header = Utility.localizedTextArea("emigrationDialog.chooseImmigrant");
        if (foy) {
            header.insert(Messages.message(LostCityRumour.RumourType.FOUNTAIN_OF_YOUTH.getDescriptionKey()) + "\n\n", 0);
        }

        add(header, "wrap 20");
        
        int i = Europe.MigrationType.getDefaultSlot();
        for (AbstractUnit au : recruitables) {
            final JButton unitChoiceButton = new JButton(Messages.message(au.getSingleLabel()), new ImageIcon(getSmallAbstractUnitImage(au)));
            final int index = i;
            unitChoiceButton.addActionListener(ae -> {
                getGUI().removeComponent(EmigrationDialog.this);
                handler.handle(index);
            });
            add(unitChoiceButton);
            okButton = unitChoiceButton;
            i++;
        }
        
        setEscapeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ignore.
            }
        });
    }
}
