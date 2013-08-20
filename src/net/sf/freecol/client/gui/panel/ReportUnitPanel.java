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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays a unit Report.
 */
public abstract class ReportUnitPanel extends ReportPanel {

    /**
     * Units in Europe.
     */
    private List<Unit> inEurope = new ArrayList<Unit>();

    /**
     * Units in colonies.
     */
    private Map<Colony, List<Unit>> inColonies
        = new HashMap<Colony, List<Unit>>();

    /**
     * Units in other locations.
     */
    private Map<String, List<Unit>> inLocations
        = new HashMap<String, List<Unit>>();

    /**
     * Records the number of units of each type.
     */
    private Map<String, TypeCountMap<UnitType>> units
        = new HashMap<String, TypeCountMap<UnitType>>();

    /**
     * Whether to show colonies even if no selected units are present.
     */
    private boolean showColonies = false;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param key the report name key
     * @param showColonies whether to show colonies with no selected units
     */
    public ReportUnitPanel(FreeColClient freeColClient, String key,
                           boolean showColonies) {
        super(freeColClient, Messages.message(key));

        this.showColonies = showColonies;
        reportPanel.setLayout(new MigLayout("fillx, wrap 12", "", ""));

        gatherData();
        addREFUnits();
        addOwnUnits();

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "newline, span, growx, wrap 40");

        // colonies first, sorted according to user preferences
        for (Colony colony : getSortedColonies()) {
            handleLocation(colony, colony.getName(), inColonies.get(colony));
        }

        // Europe next
        Europe europe = getMyPlayer().getEurope();
        if (europe != null) {
            handleLocation(europe, Messages.message(europe.getNameKey()), inEurope);
        }

        // finally all other locations, sorted alphabetically
        List<String> otherNames = new ArrayList<String>(inLocations.keySet());
        Collections.sort(otherNames);
        for (String locationName : otherNames) {
            handleLocation(null, locationName, inLocations.get(locationName));
        }

        revalidate();
        repaint();
    }

    protected int getCount(String key, UnitType type) {
        TypeCountMap<UnitType> map = units.get(key);
        if (map == null) {
            return 0;
        } else {
            return map.getCount(type);
        }
    }

    protected void incrementCount(String key, UnitType type, int number) {
        TypeCountMap<UnitType> map = units.get(key);
        if (map == null) {
            map = new TypeCountMap<UnitType>();
            units.put(key, map);
        }
        map.incrementCount(type, number);
    }

    protected void addUnit(Unit unit, String key) {
        if (unit.getLocation() == null) {
            // this should never happen
            return;
        } else if (unit.isInEurope()) {
            inEurope.add(unit);
        } else {
            Colony colony = unit.getLocation().getColony();
            if (colony == null) {
                String locationName = getLocationNameFor(unit);
                List<Unit> unitList = inLocations.get(locationName);
                if (unitList == null) {
                    unitList = new ArrayList<Unit>();
                    inLocations.put(locationName, unitList);
                }
                unitList.add(unit);
            } else {
                List<Unit> unitList = inColonies.get(colony);
                if (unitList == null) {
                    unitList = new ArrayList<Unit>();
                    inColonies.put(colony, unitList);
                }
                unitList.add(unit);
            }
        }
        incrementCount(key, unit.getType(), 1);
    }

    protected abstract void gatherData();

    protected abstract void addREFUnits();

    protected abstract void addOwnUnits();

    protected void handleLocation(Location location, String locationName, List<Unit> unitList) {
        if ((unitList == null || unitList.isEmpty()) && !showColonies) {
            return;
        }

        JComponent component;
        if (location == null) {
            component = new JLabel(locationName);
        } else {
            JButton button = getLinkButton(locationName, null, location.getId());
            button.addActionListener(this);
            component = button;
        }
        reportPanel.add(component, "newline, span, split 2");

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        if (unitList == null || unitList.isEmpty()) {
            reportPanel.add(new JLabel(Messages.message("none")), "sg");
        } else {
            Collections.sort(unitList, ReportPanel.getUnitTypeComparator());
            for (Unit unit : unitList) {
                JButton unitButton = getUnitButton(unit);
                if (unit.isCarrier()) {
                    reportPanel.add(unitButton, "newline, sg");
                    for (Goods goods : unit.getGoodsList()) {
                        GoodsLabel goodsLabel = new GoodsLabel(goods, getGUI());
                        reportPanel.add(goodsLabel);
                    }
                    for (Unit unitLoaded : unit.getUnitList()) {
                        UnitLabel unitLoadedLabel = new UnitLabel(getFreeColClient(), unitLoaded, true);
                        reportPanel.add(unitLoadedLabel);
                    }
                } else {
                    reportPanel.add(unitButton, "sg");
                }
            }
        }
    }

    protected JButton getUnitButton(Unit unit) {
        ImageIcon icon = getLibrary().getUnitImageIcon(unit.getType(),
                                                       unit.getRole().getId());
        JButton button = getLinkButton("", icon, unit.getLocation().getId());
        button.addActionListener(this);
        String toolTip = Messages.message(Messages.getLabel(unit));
        if (unit.getDestination() != null) {
            String type = unit.isPerson()
                ? "person"
                : unit.isNaval()
                ? "ship"
                : "other";
            toolTip += "\n"
                + Messages.message(StringTemplate.template("goingTo")
                                   .addName("%type%", type)
                                   .addStringTemplate("%location%", unit.getDestination()
                                                      .getLocationNameFor(getMyPlayer())));
        }
        button.setToolTipText(toolTip);
        return button;
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
