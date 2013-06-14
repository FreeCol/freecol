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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel {

    private Map<UnitType, Map<Location, Integer>> data;
    private TypeCountMap<UnitType> unitCount;
    private List<Colony> colonies;


    /**
     * The constructor that will add the items to this panel.
     */
    public ReportLabourPanel(FreeColClient freeColClient) {
        super(freeColClient, Messages.message("reportLabourAction.name"));

        colonies = getSortedColonies();
        gatherData();
        displayData();
    }

    public void gatherData() {
        data = new HashMap<UnitType, Map<Location, Integer>>();
        unitCount = new TypeCountMap<UnitType>();

        for (Unit unit : getMyPlayer().getUnits()) {
            UnitType type = unit.getType();
            unitCount.incrementCount(type, 1);
            Map<Location, Integer> unitMap = data.get(type);
            if (unitMap == null) {
                unitMap = new HashMap<Location, Integer>();
                data.put(type, unitMap);
            }

            Location location = unit.getLocation();
            if (location == null) {
                logger.warning("Unit has null location: " + unit.toString());
            } else if (location.getSettlement() != null) {
                location = location.getSettlement();
            } else if (unit.isInEurope()) {
                location = getMyPlayer().getEurope();
            } else if (location.getTile() != null) {
                location = location.getTile();
            }
            Integer count = unitMap.get(location);
            if (count == null) {
                unitMap.put(location, 1);
            } else {
                unitMap.put(location, count + 1);
            }
        }
    }

    public void displayData() {

        List<UnitType> unitTypes = getSpecification().getUnitTypeList();
        ArrayList<UnitType> colonists = new ArrayList<UnitType>();
        for (UnitType unitType : unitTypes) {
            if (unitType.hasSkill()) {
                colonists.add(unitType);
            }
        }

        reportPanel.setLayout(new MigLayout("wrap 9", "[]10[]10[]30[]10[]10[]30[]10[]10[]", ""));

        for (UnitType unitType : colonists) {
            if(!unitType.isAvailableTo(getMyPlayer())) {
                continue;
            }

            Role role = Role.DEFAULT;
            if (unitType.hasAbility(Ability.EXPERT_PIONEER)) {
                role = Role.PIONEER;
            } else if (unitType.hasAbility(Ability.EXPERT_MISSIONARY)) {
                role = Role.MISSIONARY;
            }

            int unitTypeCount = unitCount.getCount(unitType);
            if (unitTypeCount == 0) {
                JLabel unitIcon = new JLabel(getLibrary().getUnitImageIcon(unitType, role, true, 0.8));
                JLabel unitCount = new JLabel("0");
                JLabel unitName = localizedLabel(unitType.getNameKey());
                unitCount.setForeground(Color.GRAY);
                unitName.setForeground(Color.GRAY);
                reportPanel.add(unitIcon);
                reportPanel.add(unitCount);
                reportPanel.add(unitName);
            } else {
                JLabel unitIcon = new JLabel(getLibrary().getUnitImageIcon(unitType, role, false, 0.8));
                JLabel unitCount = new JLabel("" + unitTypeCount);
                JButton linkButton = getLinkButton(Messages.message(unitType.getNameKey()),
                                                   null, unitType.getId());
                linkButton.addActionListener(this);
                reportPanel.add(unitIcon);
                reportPanel.add(unitCount);
                reportPanel.add(linkButton);
            }
        }
    }

    /**
     * This function analyzes an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            super.actionPerformed(event);
        } else {
            UnitType unitType = getSpecification().getUnitType(command);
            getGUI().showReportLabourDetailPanel(unitType, data, unitCount, colonies);

        }
    }
}
