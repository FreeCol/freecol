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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;

import org.w3c.dom.Element;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Naval Report.
 */
public final class ReportUnitPanel extends JPanel implements ActionListener {

    // The column for location labels.
    private static final int labelColumn = 1;

    // The column for unit panels.
    private static final int unitColumn = 3;
    
    // The extra rows needed (one for summary, one separator).
    private static final int extraRows = 2;

    // The height of the separator row.
    private static final int separator = 12;

    /** The type of a unit report. */
    public static enum ReportType { MILITARY, NAVAL, CARGO }
    
    /**
     * Whether this is a naval, military or cargo unit report.
     */
    private ReportType reportType;

    /**
     * Whether to display empty locations.
     */
    private boolean ignoreEmptyLocations;

    /**
     * The current HIGLayout row.
     */
    private int row = 1;

    private int locationIndex = 0;

    /**
     * The main data structure.
     */
    private HashMap<String, ArrayList<Unit>> locations;

    private static final HIGConstraints higConst = new HIGConstraints();

    private Canvas parent;

    private List<Colony> colonies;
    private ArrayList<String> colonyNames;
    private ArrayList<String> otherNames;

    private final ReportPanel reportPanel;

    private final Player player;

    /**
     * Records the number of units of each type.
     */
    private HashMap<UnitType, Integer> soldiers = new HashMap<UnitType, Integer>();
    private HashMap<UnitType, Integer> dragoons = new HashMap<UnitType, Integer>();
    private HashMap<UnitType, Integer> others = new HashMap<UnitType, Integer>();

    private static final UnitType defaultType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");

    /**
     * Records the total cargo capacity of the fleet (currently
     * unused).
     */
    int capacity = 0;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportUnitPanel(ReportType type, boolean ignoreEmptyLocations, Canvas parent, ReportPanel reportPanel) {
        this.reportType = type;
        this.ignoreEmptyLocations = ignoreEmptyLocations;
        this.parent = parent;
        this.reportPanel = reportPanel;
        player = parent.getClient().getMyPlayer();
        setOpaque(false);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        gatherData();

        int rowsREF = (reportType == ReportType.CARGO ? 0 : 1);
        int[] widths = new int[] {0};
        int[] heights = new int[3 + rowsREF];
        heights[1 + rowsREF] = separator;

        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        if (reportType != ReportType.CARGO) {
            add(createREFPanel(), higConst.rc(row, labelColumn));
            row++;
        }
        add(createUnitPanel(), higConst.rc(row, labelColumn));
        row += 2;

        JButton detailButton = new JButton(Messages.message("details"));
        detailButton.setActionCommand("details");
        detailButton.addActionListener(this);

        add(detailButton, higConst.rc(row, labelColumn, "tl"));
    }

    private void prepareData() {
        locations = new HashMap<String, ArrayList<Unit>>();
        colonies = player.getColonies();
        Collections.sort(colonies, parent.getClient().getClientOptions().getColonyComparator());
        colonyNames = new ArrayList<String>();
        Iterator<Colony> colonyIterator = colonies.iterator();
        String colonyName;
        while (colonyIterator.hasNext()) {
            colonyName = (colonyIterator.next()).getName();
            colonyNames.add(colonyName);
        }
        otherNames = new ArrayList<String>();
    }

    private void showDetails() {

        int rowsREF = (reportType == ReportType.CARGO ? 0 : 1);
        int[] widths = new int[] { 0, 12, 0 };
        int[] heights = new int[colonies.size() + otherNames.size() + extraRows + rowsREF];
        heights[1 + rowsREF] = separator;

        // reset row
        row = 1;

        removeAll();
        setLayout(new HIGLayout(widths, heights));

        // REF
        if (reportType != ReportType.CARGO) {
            add(createREFPanel(), higConst.rcwh(row, labelColumn, widths.length, 1));
            row++;
        }
        add(createUnitPanel(), higConst.rcwh(row, labelColumn, widths.length, 1));
        row += 2;

        JButton detailButton = new JButton(Messages.message("details"));
        detailButton.setActionCommand("details");
        detailButton.addActionListener(this);
        
        // reset location index
        locationIndex = 0;

        // colonies first, sorted according to user preferences
        Iterator<String> locationIterator = colonyNames.iterator();
        while (locationIterator.hasNext()) {
            handleLocation(locationIterator.next(), true);
        }

        // Europe next
        if (player.getEurope() != null) {
            if (locations.get(player.getEurope().getLocationName()) != null) {
                handleLocation(player.getEurope().getLocationName(), true);
            }
            otherNames.remove(player.getEurope().getLocationName());
        }

        // finally all other locations, sorted alphabetically
        Collections.sort(otherNames);
        locationIterator = otherNames.iterator();
        while (locationIterator.hasNext()) {
            handleLocation(locationIterator.next(), false);
        }

        revalidate();
        //repaint();
    }

    private int getCount(HashMap<UnitType, Integer> hash, UnitType unitType) {
        Integer count = hash.get(unitType);
        if (count != null) {
            return count.intValue();
        } else { 
            return 0;
        }
    }

    private void incrementCount(HashMap<UnitType, Integer> hash, UnitType unitType) {
        Integer count = hash.get(unitType);
        if (count == null) {
            hash.put(unitType, new Integer(1));
        } else {
            hash.put(unitType, new Integer(count.intValue() + 1));
        }
    }

    private void gatherData() {
        prepareData();
        for (Unit unit : player.getUnits()) {
            switch(reportType) {
            case CARGO:
                if (unit.isCarrier()) {
                    incrementCount(others, unit.getType());
                    capacity += unit.getType().getSpace();
                } else {
                    continue;
                }
                break;
            case NAVAL:
                if (unit.isNaval()) {
                    incrementCount(others, unit.getType());
                } else {
                    continue;
                }
                break;
            case MILITARY:
                if (unit.isOffensiveUnit() && !unit.isNaval()) {
                    UnitType unitType = defaultType;
                    if (unit.getType().getOffence() > 0 ||
                        unit.hasAbility("model.ability.expertSoldier")) {
                        unitType = unit.getType();
                    }
                    switch(unit.getRole()) {
                    case DRAGOON:
                        incrementCount(dragoons, unitType);
                        break;
                    case SOLDIER:
                        incrementCount(soldiers, unitType);
                        break;
                    default:
                        incrementCount(others, unitType);
                    }
                } else {
                    continue;
                }
                break;
            }

            String locationName = unit.getLocation().getLocationName();
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

    private JPanel createREFPanel() {
        Element refUnits = parent.getClient().getInGameController().getREFUnits();
        JPanel refPanel = null;
        if (reportType == ReportType.NAVAL) {
            int menOfWar = Integer.parseInt(refUnits.getAttribute("menOfWar"));
            refPanel = new JPanel(new GridLayout(1, 6));
            refPanel.add(createUnitTypeLabel(FreeCol.getSpecification().getUnitType("model.unit.manOWar"),
                                             Role.DEFAULT, menOfWar));
        } else if (reportType == ReportType.MILITARY) {
            int artillery = Integer.parseInt(refUnits.getAttribute("artillery"));
            int damagedArtillery = Integer.parseInt(refUnits.getAttribute("damagedArtillery"));
            int dragoons = Integer.parseInt(refUnits.getAttribute("dragoons"));
            int infantry = Integer.parseInt(refUnits.getAttribute("infantry"));
            refPanel = new JPanel(new GridLayout(1, 8));
            refPanel.add(createUnitTypeLabel(FreeCol.getSpecification().getUnitType("model.unit.artillery"),
                                             Role.DEFAULT, artillery));
            refPanel.add(createUnitTypeLabel(FreeCol.getSpecification().getUnitType("model.unit.damagedArtillery"),
                                             Role.DEFAULT, damagedArtillery));
            refPanel.add(createUnitTypeLabel(FreeCol.getSpecification().getUnitType("model.unit.kingsRegular"),
                                             Role.DRAGOON, dragoons));
            refPanel.add(createUnitTypeLabel(FreeCol.getSpecification().getUnitType("model.unit.kingsRegular"),
                                             Role.SOLDIER, infantry));
        }
        if (refPanel != null) {
            refPanel.setOpaque(false);
            refPanel.setBorder(BorderFactory.createTitledBorder(player.getREFPlayer().getNationAsString()));
        }
        return refPanel;
    }

    private JPanel createUnitPanel() {
        JPanel unitPanel = new JPanel(new GridLayout(1, 0));
        switch (reportType) {
        case CARGO:
            String[] abilities = new String[] {"model.ability.carryUnits", "model.ability.carryGoods"};
            for (UnitType unitType : FreeCol.getSpecification().getUnitTypesWithAnyAbility(abilities)) {
                unitPanel.add(createUnitTypeLabel(unitType, Role.DEFAULT, getCount(others, unitType)));
            }
            break;
        case NAVAL:
            for (UnitType unitType : FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.navalUnit")) {
                unitPanel.add(createUnitTypeLabel(unitType, Role.DEFAULT, getCount(others, unitType)));
            }
            break;
        case MILITARY:
            int otherCount = 0;
            int dragoonCount = 0;
            List<UnitType> unitTypes = new ArrayList<UnitType>();
            for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
                if (!unitType.hasAbility("model.ability.navalUnit") && 
                    (unitType.getOffence() > 0 ||
                     unitType.hasAbility("model.ability.expertSoldier"))) {
                    if (unitType.hasAbility("model.ability.canBeEquipped")) {
                        unitPanel.add(createUnitTypeLabel(unitType, Role.DRAGOON, getCount(dragoons, unitType)),
                                      otherCount + dragoonCount);
                        dragoonCount++;
                        unitPanel.add(createUnitTypeLabel(unitType, Role.SOLDIER, getCount(soldiers, unitType)), -1);
                    } else {
                        unitPanel.add(createUnitTypeLabel(unitType, Role.DEFAULT, getCount(others, unitType)),
                                      otherCount);
                        otherCount++;
                    }
                }
            }
            unitPanel.add(createUnitTypeLabel(defaultType, Role.DRAGOON, getCount(dragoons, defaultType)),
                          otherCount + dragoonCount);
            unitPanel.add(createUnitTypeLabel(defaultType, Role.SOLDIER, getCount(soldiers, defaultType)), -1);
        }
        if (unitPanel != null) {
            unitPanel.setOpaque(false);
            unitPanel.setBorder(BorderFactory.createTitledBorder(player.getNationAsString()));
        }
        return unitPanel;
    }


    private void handleLocation(String location, boolean makeButton) {
        List<Unit> unitList = locations.get(location);
        if (!(unitList == null && ignoreEmptyLocations)) {
            if (makeButton) {
                JButton locationButton = new JButton(location);
                locationButton.setMargin(new Insets(0,0,0,0));
                locationButton.setOpaque(false);
                locationButton.setForeground(FreeColPanel.LINK_COLOR);
                locationButton.setAlignmentY(0.8f);
                locationButton.setBorder(BorderFactory.createEmptyBorder());
                locationButton.setActionCommand(location);
                locationButton.addActionListener(this);
                add(locationButton, higConst.rc(row, labelColumn, "lt"));
            } else {
                JLabel locationLabel = new JLabel(location);
                add(locationLabel, higConst.rc(row, labelColumn));
            }
            if (unitList != null) {
                JPanel unitPanel = new JPanel();
                if (reportType == ReportType.MILITARY) {
                    unitPanel.setLayout(new GridLayout(0, 9));
                } else {
                    unitPanel.setLayout(new GridLayout(0, 7));
                }
                unitPanel.setOpaque(false);
                Collections.sort(unitList, reportPanel.getUnitTypeComparator());
                Iterator<Unit> unitIterator = unitList.iterator();
                while (unitIterator.hasNext()) {
                    Unit unit = unitIterator.next();
                    UnitLabel unitLabel = new UnitLabel(unit, parent, true);
                    if (unit.getDestination() != null) {
                        unitLabel.setToolTipText("<html>" + unitLabel.getToolTipText()
                                + "<br>" + Messages.message("goingTo", "%location%",
                                unit.getDestination().getLocationName()) + "</html>");
                    }
                    // this is necessary because UnitLabel deselects carriers
                    unitLabel.setSelected(true);
                    unitPanel.add(unitLabel);
                    
                    Iterator<Goods> goodsIterator = unit.getGoodsIterator();
                    while (goodsIterator.hasNext()) {
                        Goods goods = goodsIterator.next();
                        GoodsLabel goodsLabel = new GoodsLabel(goods, reportPanel.getCanvas());
                        unitPanel.add(goodsLabel, higConst.rc(row, 1));
                    }
                    List<Unit> loadedUnits = unit.getUnitList();
                    for (Unit unitLoaded : loadedUnits) {
                        UnitLabel unitLoadedLabel = new UnitLabel(unitLoaded,
                                reportPanel.getCanvas(), true);
                        unitPanel.add(unitLoadedLabel, higConst.rc(row, 1));
                    }
                }
                add(unitPanel, higConst.rc(row, unitColumn, "l"));
            }
            row++;
        }
        locationIndex++;
    }

    private JLabel createUnitTypeLabel(UnitType unitType, Role role, int count) {
        ImageIcon unitIcon = reportPanel.getLibrary().getUnitImageIcon(unitType, role, count == 0);
        JLabel unitLabel = new JLabel(reportPanel.getLibrary().getScaledImageIcon(unitIcon, 0.66f));
        unitLabel.setText(String.valueOf(count));
        if (count == 0) {
            unitLabel.setForeground(Color.GRAY);
        }
        unitLabel.setToolTipText(Unit.getName(unitType, role));
        return unitLabel;
    }


    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals("-1")) {
            reportPanel.actionPerformed(event);
        } else if (command.equals("details")) {
            showDetails();
        } else if (command.equals(player.getEurope().getName())) {
            parent.showEuropePanel();
        } else if (player.getColony(command) != null) {
            parent.showColonyPanel(player.getColony(command));
        } else {
            //logger.warning("Unknown action command: " + command);
        }
    }
}
