/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel implements ActionListener {
    
    private TypeCountMap<UnitType> unitCount, unitAtSea, unitOnLand, unitInEurope;
    private Map<UnitType, Map<Colony, Integer>> unitLocations;
    private List<Colony> colonies;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportLabourPanel(Canvas parent) {
        super(parent, Messages.message("report.labour"));
        colonies = getMyPlayer().getColonies();
        gatherData();
        displayData();
    }

    public void gatherData() {

        // Count Units
        unitCount = new TypeCountMap<UnitType>();
        unitAtSea = new TypeCountMap<UnitType>();
        unitOnLand = new TypeCountMap<UnitType>();
        unitInEurope = new TypeCountMap<UnitType>();
        unitLocations = new HashMap<UnitType, Map<Colony, Integer>>();
        for (UnitType type : Specification.getSpecification().getUnitTypeList()) {
            unitLocations.put(type, new HashMap<Colony, Integer>());
        }

        Collections.sort(colonies, getClient().getClientOptions().getColonyComparator());

        Iterator<Unit> units = getMyPlayer().getUnitIterator();
        while (units.hasNext()) {
            Unit unit = units.next();
            UnitType type = unit.getType();
            Location location = unit.getLocation();

            unitCount.incrementCount(type, 1);

            if (location instanceof WorkLocation) {
                incrementColonyCount(((WorkLocation) location).getColony(), type);
            } else if (location instanceof Europe) {
                unitInEurope.incrementCount(type, 1);
            } else if (location instanceof Tile && ((Tile) location).getSettlement() != null) {
                incrementColonyCount((Colony) ((Tile) location).getSettlement(), type);
            } else if (location instanceof Unit) {
                unitAtSea.incrementCount(type, 1);
            } else {
                unitOnLand.incrementCount(type, 1);
            }

        }
    }

    private void incrementColonyCount(Colony colony, UnitType type) {
        Integer count = unitLocations.get(type).get(colony);
        if (count == null) {
            unitLocations.get(type).put(colony, new Integer(1));
        } else {
            unitLocations.get(type).put(colony, new Integer(count.intValue() + 1));
        }
    }

    public void displayData() {

        List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypeList();
        ArrayList<UnitType> colonists = new ArrayList<UnitType>();
        for (UnitType unitType : unitTypes) {
            if (unitType.hasSkill()) {
                colonists.add(unitType);
            }
        }

        reportPanel.setLayout(new MigLayout("wrap 6", "[]30[]30[]", ""));
        
        for (UnitType unitType : colonists) {
            Role role = Role.DEFAULT;
            if (unitType.hasAbility("model.ability.expertPioneer")) {
                role = Role.PIONEER;
            } else if (unitType.hasAbility("model.ability.expertMissionary")) {
                role = Role.MISSIONARY;
            }
            
            int unitTypeCount = unitCount.getCount(unitType);
            if (unitTypeCount == 0) {
                reportPanel.add(createUnitTypeLabel(unitType, role, 0));
                JLabel unitName = localizedLabel(unitType.getNameKey());
                unitName.setForeground(Color.GRAY);
                reportPanel.add(unitName);
            } else {
                reportPanel.add(createUnitTypeLabel(unitType, role, unitTypeCount));
                JButton linkButton = getLinkButton(Messages.message(unitType.getNameKey()),
                                                   null, unitType.getId());
                linkButton.addActionListener(this);
                reportPanel.add(linkButton);
            }
        }
    }

    private JPanel createUnitDetails(UnitType unitType, ReportLabourDetailPanel report) {

        JPanel detailPanel = new JPanel(new MigLayout("wrap 7", "[]30[][]30[][]30[][]", ""));
        detailPanel.setOpaque(false);

        Role role = Role.DEFAULT;
        if (unitType.hasAbility("model.ability.expertPioneer")) {
            role = Role.PIONEER;
        } else if (unitType.hasAbility("model.ability.expertMissionary")) {
            role = Role.MISSIONARY;
        }
            
        // summary
        detailPanel.add(new JLabel(getLibrary().getUnitImageIcon(unitType, role)), "spany");
        detailPanel.add(localizedLabel(unitType.getNameKey()));
        detailPanel.add(new JLabel(String.valueOf(unitCount.getCount(unitType))), "wrap 10");
        boolean canTrain = false;
        for (Colony colony : colonies) {
            if (unitLocations.get(unitType).get(colony) != null) {
                String colonyName = colony.getName();
                if (colony.canTrain(unitType)) {
                    canTrain = true;
                    colonyName += "*";
                }
                JButton colonyButton = getLinkButton(colonyName, null, colony.getId());
                colonyButton.addActionListener(report);
                detailPanel.add(colonyButton);
                JLabel countLabel = new JLabel(unitLocations.get(unitType).get(colony).toString());
                countLabel.setForeground(LINK_COLOR);
                detailPanel.add(countLabel);
            }
        }
        if (unitInEurope.getCount(unitType) > 0) {
            JButton europeButton = getLinkButton(Messages.message(getMyPlayer().getEurope().getNameKey()),
                                                 null, getMyPlayer().getEurope().getId());
            europeButton.addActionListener(report);
            detailPanel.add(europeButton);
            JLabel countLabel = new JLabel(String.valueOf(unitInEurope.getCount(unitType)));
            countLabel.setForeground(LINK_COLOR);
            detailPanel.add(countLabel);
        }
        if (unitOnLand.getCount(unitType) > 0) {
            JLabel onLandLabel = new JLabel(Messages.message("report.onLand"));
            detailPanel.add(onLandLabel);
            JLabel countLabel = new JLabel(String.valueOf(unitOnLand.getCount(unitType)));
            detailPanel.add(countLabel); 
        }
        if (unitAtSea.getCount(unitType) > 0) {
            JLabel atSeaLabel = new JLabel(Messages.message("report.atSea"));
            detailPanel.add(atSeaLabel);
            JLabel countLabel = new JLabel(String.valueOf(unitAtSea.getCount(unitType)));
            detailPanel.add(countLabel);
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
            UnitType unitType = FreeCol.getSpecification().getUnitType(command);
            ReportLabourDetailPanel details = new ReportLabourDetailPanel(getCanvas());
            details.setDetailPanel(createUnitDetails(unitType, details));
            getCanvas().addAsFrame(details);
            details.requestFocus();
        }
    }
}
