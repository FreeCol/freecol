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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
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
        super(freeColClient, "reportMilitaryAction.name", true);
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
        for (Unit unit : getMyPlayer().getUnits()) {
            if (unit.isOffensiveUnit() && !unit.isNaval()) {
                String key = "model.role.dragoon".equals(unit.getRole().getId())
                    ? "dragoons"
                    : "model.role.soldier".equals(unit.getRole().getId())
                    ? "soldiers"
                    : "others";
                addUnit(unit, key);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void addREFUnits() {
        final Specification spec = getSpecification();
        final Nation refNation = getMyPlayer().getNation().getREFNation();

        reportPanel.add(localizedLabel(refNation.getNameKey()),
                        "span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> refUnits = getController().getREFUnits();
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
        final Role defaultRole = spec.getRole("model.role.default");
        final UnitType defaultType = spec.getDefaultUnitType();
        
        StringTemplate t;
        t = StringTemplate.template("report.military.forces")
            .addStringTemplate("%nation%", player.getNationName());
        reportPanel.add(localizedLabel(t), "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> units = new ArrayList<AbstractUnit>();
        List<AbstractUnit> scoutUnits = new ArrayList<AbstractUnit>();
        List<AbstractUnit> dragoonUnits = new ArrayList<AbstractUnit>();
        List<AbstractUnit> soldierUnits = new ArrayList<AbstractUnit>();
        for (UnitType unitType : spec.getUnitTypeList()) {
            if (unitType.isAvailableTo(player) &&
                !unitType.hasAbility(Ability.NAVAL_UNIT) &&
                (unitType.hasAbility(Ability.EXPERT_SOLDIER) ||
                 unitType.getOffence() > 0)) {
                if (unitType.hasAbility(Ability.CAN_BE_EQUIPPED)) {
                    scoutUnits.add(new AbstractUnit(unitType,
                            "model.role.scout", getCount("scouts", unitType)));
                    dragoonUnits.add(new AbstractUnit(unitType,
                            "model.role.dragoon", getCount("dragoons", unitType)));
                    soldierUnits.add(new AbstractUnit(unitType,
                            "model.role.soldier", getCount("soldiers", unitType)));
                } else {
                    units.add(new AbstractUnit(unitType,
                            "model.role.default", getCount("others", unitType)));
                }
            }
        }
        dragoonUnits.add(new AbstractUnit(defaultType,
                "model.role.dragoon", getCount("dragoons", defaultType)));
        soldierUnits.add(new AbstractUnit(defaultType,
                "model.role.soldier", getCount("soldiers", defaultType)));
        scoutUnits.add(new  AbstractUnit(defaultType,
                "model.role.scout", getCount("scouts", defaultType)));
        units.addAll(dragoonUnits);
        units.addAll(soldierUnits);
        units.addAll(scoutUnits);

        for (AbstractUnit unit : units) {
            reportPanel.add(createUnitTypeLabel(unit), "sg");
        }
    }
}
