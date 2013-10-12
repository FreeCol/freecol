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
import net.sf.freecol.common.model.Role;
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

        reportPanel.setLayout(new MigLayout("wrap 9", "[center]10[right]10[left]30"
                                            + "[center]10[right]10[left]30"
                                            + "[center]10[right]10[left]"));

        colonies = freeColClient.getMySortedColonies();
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

        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isPerson() && unitType.isAvailableTo(getMyPlayer())) {

                String roleId = "model.role.default";
                if (unitType.hasAbility(Ability.EXPERT_PIONEER)) {
                    roleId = "model.role.pioneer";
                } else if (unitType.hasAbility(Ability.EXPERT_MISSIONARY)) {
                    roleId = "model.role.missionary";
                }

                int unitTypeCount = unitCount.getCount(unitType);
                boolean greyscale = (unitTypeCount == 0);
                reportPanel.add(new JLabel(getLibrary().getUnitImageIcon(unitType, roleId, greyscale, 0.8)));
                reportPanel.add(new JLabel(Integer.toString(unitTypeCount)));
                if (unitTypeCount == 0) {
                    reportPanel.add(localizedLabel(unitType.getNameKey()));
                } else {
                    JButton linkButton = GUI.getLinkButton(Messages.message(unitType.getNameKey()),
                                                           null, unitType.getId());
                    linkButton.addActionListener(this);
                    reportPanel.add(linkButton);
                }
            }
        }
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
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
