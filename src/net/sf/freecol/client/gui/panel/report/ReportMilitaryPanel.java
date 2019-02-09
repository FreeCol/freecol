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
package net.sf.freecol.client.gui.panel.report;

import java.util.List;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Military Report.
 */
public final class ReportMilitaryPanel extends ReportUnitPanel {

    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportMilitaryPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportMilitaryAction", true);
    }


    /**
     * {@inheritDoc}
     */
    protected boolean isReportable(Unit unit) {
        return !unit.isNaval()
                && (unit.hasAbility(Ability.EXPERT_SOLDIER)
                    || unit.isOffensiveUnit());
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isReportable(UnitType unitType, Role role) {
        final Player player = getMyPlayer();
        if (!unitType.isAvailableTo(player) || unitType.isNaval()) return false;
        return unitType.isOffensive()
            || (role.isAvailableTo(player, unitType)
                && getSpecification().getMilitaryRolesList().contains(role));
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isReportableREF(AbstractUnit au) {
        return !au.getType(getSpecification()).isNaval();
    }
}
