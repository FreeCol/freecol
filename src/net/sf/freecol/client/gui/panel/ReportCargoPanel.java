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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Cargo Report.
 */
public final class ReportCargoPanel extends ReportPanel {

    private List<String> colonyNames;
    private List<String> otherNames;

    /**
     * Records the number of units of each type.
     */
    private TypeCountMap<UnitType> carriers = new TypeCountMap<UnitType>();

    private Map<String, ArrayList<Unit>> locations;

    /**
     * Records the total cargo capacity of the fleet (currently unused).
     */
    int capacity = 0;


    /**
     * The constructor that will add the items to this panel.
     * @param freeColClient 
     * @param parent The parent of this panel.
     */
    public ReportCargoPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, Messages.message("reportCargoAction.name"));

        gatherData();

        Player player = getMyPlayer();

        reportPanel.setLayout(new MigLayout("fillx, wrap 12", "", ""));

        reportPanel.add(localizedLabel(StringTemplate.template("report.military.forces")
                                       .addStringTemplate("%nation%", player.getNationName())),
                        "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> cargoTypes = new ArrayList<AbstractUnit>();
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.isAvailableTo(player)
                && (unitType.canCarryUnits() || unitType.canCarryGoods())) {
                cargoTypes.add(new AbstractUnit(unitType, Role.DEFAULT, carriers.getCount(unitType)));
            }
        }

        for (AbstractUnit unit : cargoTypes) {
            reportPanel.add(createUnitTypeLabel(unit), "sg");
        }

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "newline, span, growx, wrap 40");

        // colonies first, sorted according to user preferences
        for (String locationName : colonyNames) {
            handleLocation(locationName, true);
        }

        // Europe next
        if (player.getEurope() != null) {
            String europeName = Messages.message(player.getEurope().getNameKey());
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
        List<Colony> colonies = getSortedColonies();
        colonyNames = new ArrayList<String>();
        for (Colony colony : colonies) {
            colonyNames.add(colony.getName());
        }
        otherNames = new ArrayList<String>();
        if (player.getEurope() != null) {
            otherNames.add(Messages.message(player.getEurope().getNameKey()));
        }

        for (Unit unit : player.getUnits()) {
            if (unit.isCarrier()) {
                carriers.incrementCount(unit.getType(), 1);
                capacity += unit.getType().getSpace();

                String locationName = getLocationNameFor(unit);

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

        if (unitList == null) {
            reportPanel.add(new JLabel(Messages.message("none")), "sg");
        } else {
            Collections.sort(unitList, ReportPanel.getUnitTypeComparator());
            for (Unit unit : unitList) {
                UnitLabel unitLabel = new UnitLabel(getFreeColClient(), unit, getGUI(), true);
                if (unit.getDestination() != null) {
                    String destination = Messages.message(unit.getDestination().getLocationNameFor(getMyPlayer()));
                    unitLabel.setToolTipText("<html>" + unitLabel.getToolTipText() + "<br>" +
                                             Messages.message(StringTemplate.template("goingTo")
                                                              .addName("%location%", destination))
                                             + "</html>");
                }
                // this is necessary because UnitLabel deselects carriers
                unitLabel.setSelected(true);
                reportPanel.add(unitLabel, "newline, sg");
                for (Goods goods : unit.getGoodsList()) {
                    GoodsLabel goodsLabel = new GoodsLabel(goods, getGUI());
                    reportPanel.add(goodsLabel);
                }
                for (Unit unitLoaded : unit.getUnitList()) {
                    UnitLabel unitLoadedLabel = new UnitLabel(getFreeColClient(), unitLoaded, getGUI(), true);
                    reportPanel.add(unitLoadedLabel);
                }
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
