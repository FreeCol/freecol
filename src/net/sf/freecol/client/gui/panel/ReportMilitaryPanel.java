/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Military Report.
 */
public final class ReportMilitaryPanel extends ReportUnitPanel {

    /*
     * In the future, we should consider all possible combinations of
     * military equipment, rather than fixed roles.

     public static final Comparator<EquipmentType> equipmentTypeComparator =
     new Comparator<EquipmentType>() {
     public int compare(EquipmentType type1, EquipmentType type2) {
     return type2.getId().compareTo(type1.getId());
     }
     };

     public class EquippedUnitType {
     private final UnitType unitType;
     private final List<EquipmentType> equipment;

     public EquippedUnitType(UnitType unitType, List<EquipmentType> equipment) {
     this.unitType = unitType;
     this.equipment = new ArrayList<EquipmentType>(equipment);
     Collections.sort(this.equipment, equipmentTypeComparator);
     }
     }

     private Map<EquippedUnitType, Integer> unitMap;
    */

    /**
     * The constructor that will add the items to this panel.
     * @param freeColClient
     * @param gui The parent of this panel.
     */
    public ReportMilitaryPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, "reportMilitaryAction.name", true);
    }

    protected void addREFUnits() {
        reportPanel.add(new JLabel(Messages.message(player.getNation().getRefNation().getId() + ".name")),
                        "span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> refUnits = getController().getREFUnits();
        if (refUnits != null) {
            for (AbstractUnit unit : refUnits) {
                if (!unit.getUnitType(getSpecification()).hasAbility(Ability.NAVAL_UNIT)) {
                    reportPanel.add(createUnitTypeLabel(unit), "sg");
                }
            }
        }
    }

    protected void addOwnUnits() {
        reportPanel.add(localizedLabel(StringTemplate.template("report.military.forces")
                                       .addStringTemplate("%nation%", player.getNationName())),
                        "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> units = new ArrayList<AbstractUnit>();
        List<AbstractUnit> scoutUnits = new ArrayList<AbstractUnit>();
        List<AbstractUnit> dragoonUnits = new ArrayList<AbstractUnit>();
        List<AbstractUnit> soldierUnits = new ArrayList<AbstractUnit>();
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isAvailableTo(player) &&
                !unitType.hasAbility(Ability.NAVAL_UNIT) &&
                (unitType.hasAbility(Ability.EXPERT_SOLDIER) ||
                 unitType.getOffence() > 0)) {
                if (unitType.hasAbility("model.ability.canBeEquipped")) {
                    scoutUnits.add(new AbstractUnit(unitType, Role.SCOUT, getCount("scouts", unitType)));
                    dragoonUnits.add(new AbstractUnit(unitType, Role.DRAGOON, getCount("dragoons", unitType)));
                    soldierUnits.add(new AbstractUnit(unitType, Role.SOLDIER, getCount("soldiers", unitType)));
                } else {
                    units.add(new AbstractUnit(unitType, Role.DEFAULT, getCount("others", unitType)));
                }
            }
        }
        UnitType defaultType = getSpecification().getDefaultUnitType();
        dragoonUnits.add(new AbstractUnit(defaultType, Role.DRAGOON, getCount("dragoons", defaultType)));
        soldierUnits.add(new AbstractUnit(defaultType, Role.SOLDIER, getCount("soldiers", defaultType)));
        scoutUnits.add(new  AbstractUnit(defaultType, Role.SCOUT, getCount("scouts", defaultType)));
        units.addAll(dragoonUnits);
        units.addAll(soldierUnits);
        units.addAll(scoutUnits);

        for (AbstractUnit unit : units) {
            reportPanel.add(createUnitTypeLabel(unit), "sg");
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

    protected void gatherData() {
        UnitType defaultType = getSpecification().getDefaultUnitType();
        for (Unit unit : player.getUnits()) {
            if (unit.isOffensiveUnit() && !unit.isNaval()) {
                UnitType unitType = defaultType;
                if (unit.getType().getOffence() > 0 ||
                    unit.hasAbility(Ability.EXPERT_SOLDIER)) {
                    unitType = unit.getType();
                }
                String key = unit.getRole() == Role.DRAGOON
                    ? "dragoons"
                    : unit.getRole() == Role.SOLDIER
                    ? "soldiers"
                    : "others";
                addUnit(unit, key);
            }
        }
    }

}
