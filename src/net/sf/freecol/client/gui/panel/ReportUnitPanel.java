/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays a unit Report.
 */
public abstract class ReportUnitPanel extends ReportPanel {

    /**
     * Units in Europe.
     */
    private final List<Unit> inEurope = new ArrayList<>();

    /**
     * Units in colonies.
     */
    private final Map<Colony, List<Unit>> inColonies = new HashMap<>();

    /**
     * Units in other locations.
     */
    private final Map<String, List<Unit>> inLocations = new HashMap<>();

    /**
     * Records the number of units of each type.
     */
    private final Map<String, TypeCountMap<UnitType>> units = new HashMap<>();

    /**
     * Whether to show colonies even if no selected units are present.
     */
    private boolean showColonies = false;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param key the report name key
     * @param showColonies Whether to show colonies with no selected units.
     */
    public ReportUnitPanel(FreeColClient freeColClient, String key,
                           boolean showColonies) {
        super(freeColClient, key);

        this.showColonies = showColonies;
        reportPanel.setLayout(new MigLayout("fillx, wrap 12", "", ""));

        gatherData();
        addREFUnits();
        addOwnUnits();

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                        "newline, span, growx, wrap 40");

        // Colonies first, sorted according to user preferences
        for (Colony colony : freeColClient.getMySortedColonies()) {
            handleLocation(colony, colony.getName(), inColonies.get(colony));
        }

        // Europe next
        Europe europe = getMyPlayer().getEurope();
        if (europe != null) {
            handleLocation(europe, Messages.getName(europe), inEurope);
        }

        // Finally all other locations, sorted alphabetically.
        List<String> otherNames = new ArrayList<>(inLocations.keySet());
        Collections.sort(otherNames);
        for (Entry<String, List<Unit>> e : mapEntriesByKey(inLocations)) {
            handleLocation(null, e.getKey(), e.getValue());
        }

        revalidate();
        repaint();
    }


    protected int getCount(String key, UnitType type) {
        TypeCountMap<UnitType> map = units.get(key);
        return (map == null) ? 0 : map.getCount(type);
    }

    protected void incrementCount(String key, UnitType type, int number) {
        TypeCountMap<UnitType> map = units.get(key);
        if (map == null) {
            map = new TypeCountMap<>();
            units.put(key, map);
        }
        map.incrementCount(type, number);
    }

    protected void addUnit(Unit unit, String key) {
        if (unit.getLocation() == null) {
            return; // Can not happen.
        } else if (unit.isInEurope()) {
            inEurope.add(unit);
        } else {
            Colony colony = unit.getLocation().getColony();
            if (colony == null) {
                String locationName = getLocationLabelFor(unit);
                List<Unit> unitList = inLocations.get(locationName);
                if (unitList == null) {
                    unitList = new ArrayList<>();
                    inLocations.put(locationName, unitList);
                }
                unitList.add(unit);
            } else {
                List<Unit> unitList = inColonies.get(colony);
                if (unitList == null) {
                    unitList = new ArrayList<>();
                    inColonies.put(colony, unitList);
                }
                unitList.add(unit);
            }
        }
        incrementCount(key, unit.getType(), 1);
    }

    protected void handleLocation(Location location, String locationName,
                                  List<Unit> unitList) {
        if ((unitList == null || unitList.isEmpty()) && !showColonies) {
            return;
        }

        JComponent component;
        if (location == null) {
            component = new JLabel(locationName);
        } else {
            JButton button = Utility.getLinkButton(locationName, null, location.getId());
            button.addActionListener(this);
            component = button;
        }
        reportPanel.add(component, "newline, span, split 2");

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        if (unitList == null || unitList.isEmpty()) {
            reportPanel.add(Utility.localizedLabel("none"), "sg");
        } else {
            for (Unit u : unitList.stream()
                     .sorted(Unit.typeRoleComparator).collect(Collectors.toList())) {
                JButton unitButton = getUnitButton(u);
                if (u.isCarrier()) {
                    reportPanel.add(unitButton, "newline, sg");
                    for (Goods goods : u.getGoodsList()) {
                        GoodsLabel goodsLabel = new GoodsLabel(getGUI(), goods);
                        reportPanel.add(goodsLabel);
                    }
                    for (Unit unitLoaded : u.getUnitList()) {
                        UnitLabel unitLoadedLabel
                            = new UnitLabel(getFreeColClient(), unitLoaded, true);
                        reportPanel.add(unitLoadedLabel);
                    }
                } else {
                    reportPanel.add(unitButton, "sg");
                }
            }
        }
    }

    protected JButton getUnitButton(Unit unit) {
        ImageIcon icon = new ImageIcon(getImageLibrary().getUnitImage(unit));
        JButton button = Utility.getLinkButton("", icon, unit.getLocation().getId());
        button.addActionListener(this);
        StringTemplate tip = StringTemplate.label("\n")
            .addStringTemplate(unit.getLabel());
        if (unit.getDestination() != null) {
            tip.addStringTemplate(unit.getDestinationLabel());
        }
        Utility.localizeToolTip(button, tip);
        return button;
    }


    // To be implemented by specific unit panels.
    
    /**
     * Gather the overall unit data, mostly by calling addUnit() above.
     */
    protected abstract void gatherData();

    /**
     * Add a section for the REF.
     */
    protected abstract void addREFUnits();

    /**
     * Add a section for specific unit types owned by the player.
     */
    protected abstract void addOwnUnits();
}
