/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourDetailPanel extends ReportPanel implements ActionListener {
    
    private Player player;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportLabourDetailPanel(Canvas parent) {
        super(parent, Messages.message("report.labour.details"));
        player = parent.getClient().getMyPlayer();
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize(JPanel detailPanel, UnitType unitType) {
        reportPanel.setLayout(new HIGLayout(new int[] {0, 12, 0}, new int[] {0, 12, 0}));

        reportPanel.add(createUnitLabel(unitType), higConst.rc(1, 1, "t"));
        reportPanel.add(detailPanel, higConst.rc(1, 3));
        reportPanel.add(new JLabel(Messages.message("report.labour.canTrain")),
                        higConst.rc(3, 3, "l"));
    }

    private JLabel createUnitLabel(UnitType unitType) {
        Role role = Role.DEFAULT;
        if (unitType.hasAbility("model.ability.expertPioneer")) {
            role = Role.PIONEER;
        }
        JLabel unitLabel = new JLabel(getLibrary().getUnitImageIcon(unitType, role));
        return unitLabel;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals("-1")) {
            super.actionPerformed(event);
        } else if (command.equals(player.getEurope().getName())) {
            getCanvas().showEuropePanel();
        } else {
            getCanvas().showColonyPanel(player.getColony(command));
        }
    }
}
