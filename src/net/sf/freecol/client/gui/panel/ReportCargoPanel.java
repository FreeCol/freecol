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

import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Cargo Report.
 */
public final class ReportCargoPanel extends ReportUnitPanel {

    /**
     * Creates a cargo report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportCargoPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportCargoAction", false);
    }


    @Override
    protected void addREFUnits() {}

    @Override
    protected void addOwnUnits() {
        final Player player = getMyPlayer();
        reportPanel.add(Utility.localizedLabel(player.getForcesLabel()),
            "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");
        
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isAvailableTo(player)
                && (unitType.canCarryUnits() || unitType.canCarryGoods())) {
                AbstractUnit unit = new AbstractUnit(unitType,
                    Specification.DEFAULT_ROLE_ID,
                    getCount("carriers", unitType));
                reportPanel.add(createUnitTypeLabel(unit), "sg");
            }
        }
    }

    @Override
    protected void gatherData() {
        for (Unit unit : getMyPlayer().getUnits()) {
            if (unit.isCarrier()) {
                addUnit(unit, "carriers");
            }
        }
    }

}
