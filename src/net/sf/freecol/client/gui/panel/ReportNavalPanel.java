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

import java.awt.Dimension;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Naval Report.
 */
public final class ReportNavalPanel extends ReportUnitPanel {


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportNavalPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportNavalAction.name", false);
    }


    protected void addREFUnits() {
        final Player player = getMyPlayer();
        reportPanel.add(new JLabel(Messages.message(player.getNation().getREFNation().getId() + ".name")),
                        "span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> refUnits = getController().getREFUnits();
        if (refUnits != null) {
            for (AbstractUnit unit : refUnits) {
                if (unit.getUnitType(getSpecification()).hasAbility(Ability.NAVAL_UNIT)) {
                    reportPanel.add(createUnitTypeLabel(unit), "sg");
                }
            }
        }
    }

    protected void addOwnUnits() {
        final Player player = getMyPlayer();
        reportPanel.add(localizedLabel(StringTemplate.template("report.military.forces")
                                       .addStringTemplate("%nation%", player.getNationName())),
                        "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isAvailableTo(player) && unitType.hasAbility(Ability.NAVAL_UNIT)) {
                AbstractUnit unit = new AbstractUnit(unitType, Role.DEFAULT, getCount("naval", unitType));
                reportPanel.add(createUnitTypeLabel(unit), "sg");
            }
        }
    }

    protected void gatherData() {
        for (Unit unit : getMyPlayer().getUnits()) {
            if (unit.isNaval()) {
                addUnit(unit, "naval");
            }
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(750, 600);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

}

