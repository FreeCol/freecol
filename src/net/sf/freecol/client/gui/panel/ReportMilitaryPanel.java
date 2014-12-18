/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
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


    @Override
    public Dimension getMinimumSize() {
        return new Dimension(750, 600);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }


    // Implement ReportUnitPanel

    /**
     * {@inheritDoc}
     */
    protected void gatherData() {
        final List<Role> militaryRoles = getSpecification().getMilitaryRoles();
        for (Unit unit : getMyPlayer().getUnits()) {
            if (unit.isNaval()) continue;
            Role role = unit.getRole();
            if (militaryRoles.contains(role)
                || unit.hasAbility(Ability.EXPERT_SOLDIER)
                || unit.isOffensiveUnit()) {
                addUnit(unit, role.getId());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void addREFUnits() {
        final Specification spec = getSpecification();
        final Nation refNation = getMyPlayer().getNation().getREFNation();

        reportPanel.add(GUI.localizedLabel(refNation.getNameKey()),
                        "span, split 2");
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
    protected void addOwnUnits() {
        final Specification spec = getSpecification();
        final Player player = getMyPlayer();
        final UnitType defaultType = spec.getDefaultUnitType();
        List<Role> militaryRoles = spec.getMilitaryRoles();
        // default role is valid because of artillery and disarmed experts
        militaryRoles.add(spec.getDefaultRole());

        reportPanel.add(GUI.localizedLabel(StringTemplate
                .template("report.military.forces")
                .addStringTemplate("%nation%", player.getNationName())),
            "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> units = new ArrayList<>();
        for (Role r : militaryRoles) {
            for (UnitType unitType : spec.getUnitTypeList()) {
                if (unitType.isAvailableTo(player)
                    && !unitType.isNaval()
                    && (unitType.hasAbility(Ability.EXPERT_SOLDIER)
                        || unitType.isOffensive())) {
                    int count = getCount(r.getId(), unitType);
                    if (count > 0) {
                        units.add(new AbstractUnit(unitType, r.getId(), count));
                    }
                }
            }
        }
        for (AbstractUnit au : units) {
            reportPanel.add(createUnitTypeLabel(au), "sg");
        }
    }
}
