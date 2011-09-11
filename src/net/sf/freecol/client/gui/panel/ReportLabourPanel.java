/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel {

    private Map<UnitType, Map<Location, Integer>> data;
    private TypeCountMap<UnitType> unitCount;
    private List<Colony> colonies;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportLabourPanel(Canvas parent) {
        super(parent, Messages.message("reportLabourAction.name"));
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
            if (location.getSettlement() != null) {
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

    private JPanel createUnitDetails(UnitType unitType, ReportLabourDetailPanel report) {

        JPanel detailPanel = new JPanel(new MigLayout("wrap 7", "[]30[][]30[][]30[][]", ""));
        detailPanel.setOpaque(false);

        Role role = Role.DEFAULT;
        if (unitType.hasAbility(Ability.EXPERT_PIONEER)) {
            role = Role.PIONEER;
        } else if (unitType.hasAbility(Ability.EXPERT_MISSIONARY)) {
            role = Role.MISSIONARY;
        }

        // summary
        detailPanel.add(new JLabel(getLibrary().getUnitImageIcon(unitType, role)), "spany");
        detailPanel.add(localizedLabel(unitType.getNameKey()));
        detailPanel.add(new JLabel(String.valueOf(unitCount.getCount(unitType))), "wrap 10");
        boolean canTrain = false;
        Map<Location, Integer> unitLocations = data.get(unitType);
        for (Colony colony : colonies) {
            if (unitLocations.get(colony) != null) {
                String colonyName = colony.getName();
                if (colony.canTrain(unitType)) {
                    canTrain = true;
                    colonyName += "*";
                }
                JButton colonyButton = getLinkButton(colonyName, null, colony.getId());
                colonyButton.addActionListener(report);
                detailPanel.add(colonyButton);
                JLabel countLabel = new JLabel(unitLocations.get(colony).toString());
                countLabel.setForeground(LINK_COLOR);
                detailPanel.add(countLabel);
            }
        }
        for (Entry<Location, Integer> entry : unitLocations.entrySet()) {
            if (!(entry.getKey() instanceof Colony)) {
                String locationName = Messages.message(entry.getKey().getLocationName());
                JButton linkButton = getLinkButton(locationName, null, entry.getKey().getId());
                linkButton.addActionListener(report);
                detailPanel.add(linkButton);
                JLabel countLabel = new JLabel(entry.getValue().toString());
                countLabel.setForeground(LINK_COLOR);
                detailPanel.add(countLabel);
            }
        }
        if (canTrain) {
            detailPanel.add(new JLabel(Messages.message("report.labour.canTrain")),
                            "newline 20, span");
        }
        return detailPanel;
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
            ReportLabourDetailPanel details = new ReportLabourDetailPanel(getCanvas());
            details.setDetailPanel(createUnitDetails(unitType, details));
            getCanvas().addAsFrame(details);
            details.requestFocus();
        }
    }
}
