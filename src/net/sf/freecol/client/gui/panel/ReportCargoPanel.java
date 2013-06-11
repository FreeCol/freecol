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
import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Cargo Report.
 */
public final class ReportCargoPanel extends ReportUnitPanel {

    /**
     * Creates a cargo report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     */
    public ReportCargoPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, "reportCargoAction.name", false);
    }


    protected void addREFUnits() {}

    protected void addOwnUnits() {
        reportPanel.add(localizedLabel(StringTemplate.template("report.military.forces")
                                       .addStringTemplate("%nation%", player.getNationName())),
                        "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");
        
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isAvailableTo(player)
                && (unitType.canCarryUnits() || unitType.canCarryGoods())) {
                AbstractUnit unit = new AbstractUnit(unitType, Role.DEFAULT, getCount("carriers", unitType));
                reportPanel.add(createUnitTypeLabel(unit), "sg");
            }
        }
    }

    protected void gatherData() {
        for (Unit unit : player.getUnits()) {
            if (unit.isCarrier()) {
                addUnit(unit, "carriers");
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
