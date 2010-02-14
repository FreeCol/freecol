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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Naval Report.
 */
public final class ReportNavalPanel extends ReportPanel {

    private List<String> colonyNames;
    private List<String> otherNames;

    /**
     * Records the number of units of each type.
     */
    private TypeCountMap<UnitType> navalUnits = new TypeCountMap<UnitType>();

    private Map<String, ArrayList<Unit>> locations;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportNavalPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.naval"));

        gatherData();

        Player player = getMyPlayer();

        reportPanel.setLayout(new MigLayout("fillx, wrap 12", "", ""));

        reportPanel.add(new JLabel(Messages.message(player.getNation().getRefNation().getId() + ".name")),
                        "span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> refUnits = getController().getREFUnits();
        if (refUnits != null) {
            for (AbstractUnit unit : refUnits) {
                if (unit.getUnitType().hasAbility("model.ability.navalUnit")) {
                    reportPanel.add(createUnitTypeLabel(unit.getUnitType(), unit.getRole(), unit.getNumber()),
                                    "sg");
                }
            }
        }

        reportPanel.add(new JLabel(Messages.message("report.military.forces", "%nation%",
                                                    player.getNationAsString())),
                        "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> ships = new ArrayList<AbstractUnit>();
        for (UnitType unitType : Specification.getSpecification().getUnitTypeList()) {
            if (unitType.isAvailableTo(player) && unitType.hasAbility("model.ability.navalUnit")) {
                ships.add(new AbstractUnit(unitType, Role.DEFAULT, navalUnits.getCount(unitType)));
            }
        }

        for (AbstractUnit unit : ships) {
            reportPanel.add(createUnitTypeLabel(unit), "sg");
        }

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "newline, span, growx, wrap 40");

        // colonies first, sorted according to user preferences
        for (String locationName : colonyNames) {
            handleLocation(locationName, true);
        }

        // Europe next
        if (player.getEurope() != null) {
            String europeName = player.getEurope().getName();
            handleLocation(europeName, true);
            otherNames.remove(europeName);
        }

        // finally all other locations, sorted alphabetically
        Collections.sort(otherNames);
        for (String locationName : otherNames) {
            handleLocation(locationName, false);
        }

        revalidate();
        repaint();
    }


    private void gatherData() {
        locations = new HashMap<String, ArrayList<Unit>>();
        Player player = getMyPlayer();
        List<Colony> colonies = player.getColonies();
        Collections.sort(colonies, getClient().getClientOptions().getColonyComparator());
        colonyNames = new ArrayList<String>();
        for (Colony colony : colonies) {
            colonyNames.add(colony.getName());
        }
        otherNames = new ArrayList<String>();
        if (player.getEurope() != null) {
            otherNames.add(player.getEurope().getName());
        }

        for (Unit unit : player.getUnits()) {
            if (unit.isNaval()) {
                navalUnits.incrementCount(unit.getType(), 1);
            
                String locationName = Messages.message(unit.getLocation().getLocationName());
                if (unit.getState() == UnitState.TO_AMERICA) {
                    locationName = Messages.message("goingToAmerica");
                } else if (unit.getState() == UnitState.TO_EUROPE) {
                    locationName = Messages.message("goingToEurope");
                }
            
                ArrayList<Unit> unitList = locations.get(locationName);
                if (unitList == null) {
                    unitList = new ArrayList<Unit>();
                    locations.put(locationName, unitList);
                }
                unitList.add(unit);
                if (!(colonyNames.contains(locationName) || otherNames.contains(locationName))) {
                    otherNames.add(locationName);
                }
            }
        }
    }

    private void handleLocation(String location, boolean makeButton) {
        List<Unit> unitList = locations.get(location);
        
        // Do not show locations without units
        if (unitList == null) {
        	return;
        }
        
        JComponent component;
        if (makeButton) {
            JButton button = FreeColPanel.getLinkButton(location, null, location);
            button.addActionListener(this);
            component = button;
        } else {
            component = new JLabel(location);
        }
        reportPanel.add(component, "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        Collections.sort(unitList, ReportPanel.getUnitTypeComparator());
        for (Unit unit : unitList) {
        	UnitLabel unitLabel = new UnitLabel(unit, getCanvas(), true);
        	if (unit.getDestination() != null) {
                    String destination = Messages.message(unit.getDestination().getLocationName());
                    unitLabel.setToolTipText("<html>" + unitLabel.getToolTipText() + "<br>" +
                                             Messages.message("goingTo", "%location%", destination) +
                                             "</html>");
        	}
        	// this is necessary because UnitLabel deselects carriers
        	unitLabel.setSelected(true);
        	reportPanel.add(unitLabel, "newline, sg");
        	for (Goods goods : unit.getGoodsList()) {
        		GoodsLabel goodsLabel = new GoodsLabel(goods, getCanvas());
        		reportPanel.add(goodsLabel);
        	}
        	for (Unit unitLoaded : unit.getUnitList()) {
        		UnitLabel unitLoadedLabel = new UnitLabel(unitLoaded, getCanvas(), true);
        		reportPanel.add(unitLoadedLabel);
        	}
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

}

