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

import java.util.List;

import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Nation;
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
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportMilitaryPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportMilitaryAction", true);
    }


    private boolean reportable(UnitType unitType) {
        return !unitType.isNaval()
            && unitType.isAvailableTo(getMyPlayer())
            && (unitType.hasAbility(Ability.EXPERT_SOLDIER)
                || unitType.isOffensive());
    }

    private boolean reportable(Unit unit) {
        return !unit.isNaval()
            && (unit.hasAbility(Ability.EXPERT_SOLDIER)
                || unit.isOffensiveUnit());
    }

    private void tryUnitRole(UnitType unitType, String roleId) {
        int count = getCount(roleId, unitType);
        if (count > 0) {
            AbstractUnit au = new AbstractUnit(unitType, roleId, count);
            reportPanel.add(createUnitTypeLabel(au), "sg");
        }
    }


    // Implement ReportUnitPanel

    /**
     * {@inheritDoc}
     */
    @Override
    protected void gatherData() {
        for (Unit unit : getMyPlayer().getUnits()) {
            if (reportable(unit)) {
                addUnit(unit, unit.getRole().getId());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addREFUnits() {
        final Specification spec = getSpecification();
        final Nation refNation = getMyPlayer().getNation().getREFNation();

        reportPanel.add(Utility.localizedLabel(refNation), "span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> refUnits = igc().getREFUnits();
        if (refUnits != null) {
            for (AbstractUnit au : refUnits) {
                if (!au.getType(spec).isNaval()) {
                    reportPanel.add(createUnitTypeLabel(au), "sg");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addOwnUnits() {
        final Specification spec = getSpecification();
        final Player player = getMyPlayer();
        final UnitType defaultType = spec.getDefaultUnitType(player);

        reportPanel.add(Utility.localizedLabel(player.getForcesLabel()),
            "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        // Report unit types that are inherently reportable, and units
        // with military roles.
        List<Role> militaryRoles = getSpecification().getMilitaryRoles();
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (reportable(unitType)) {
                tryUnitRole(unitType, Specification.DEFAULT_ROLE_ID);
            }
            for (Role r : militaryRoles) {
                tryUnitRole(unitType, r.getId());
            }
        }
    }
}
