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
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

    // The number of columns for military reports
    private static final int militaryColumns = 9;

    // The number of columns for naval and cargo reports
    private static final int carrierColumns = 7;

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
     * Where to put the detailPanel.
     */
    private int detailPanelRow;

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
    private final JPanel detailPanel = new JPanel();

    private final Player player;

    /**
     * Records the number of units of each type.
     */
    private HashMap<UnitType, Integer> soldiers = new HashMap<UnitType, Integer>();
    private HashMap<UnitType, Integer> dragoons = new HashMap<UnitType, Integer>();
    private HashMap<UnitType, Integer> others = new HashMap<UnitType, Integer>();

    private static final UnitType defaultType =
        FreeCol.getSpecification().getUnitType("model.unit.freeColonist");

    /**
     * Records the total cargo capacity of the fleet (currently unused).
     */
    int capacity = 0;

    private JButton detailButton;

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
        detailPanel.setOpaque(false);
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

        detailButton = new JButton(Messages.message("details"));
        detailButton.setActionCommand("details");
        detailButton.addActionListener(this);

        detailPanelRow = row;
        add(detailButton, higConst.rc(row, labelColumn, "tl"));
    }

    private void showDetails() {

        remove(detailButton);
        detailPanel.removeAll();

        int[] widths;
        int[] heights;
        int rows = 0;
        for (ArrayList<Unit> units : locations.values()) {
            if (units == null || units.isEmpty()) {
                if (!ignoreEmptyLocations) {
                    rows++;
                }
            } else if (reportType == ReportType.MILITARY) {
                rows += units.size() / militaryColumns;
                if (units.size() % militaryColumns != 0) {
                    rows++;
                }
            } else {
                int cargoColumns = carrierColumns - 1;
                for (Unit unit : units) {
                    int cargo = unit.getGoodsCount() + unit.getUnitCount();
                    if (cargo == 0) {
                        rows++;
                    } else {
                        rows += cargo / cargoColumns;
                        if (cargo % cargoColumns != 0) {
                            rows++;
                        }
                    }
                }
            }
        }
        widths = new int[2 + (reportType == ReportType.MILITARY ? militaryColumns : carrierColumns)];
        heights = new int[rows];

        widths[1] = separator; // margin
        detailPanel.setLayout(new HIGLayout(widths, heights));

        int row = 1;
        // colonies first, sorted according to user preferences
        for (String locationName : colonyNames) {
            row = handleLocation(locationName, true, row);
        }

        // Europe next
        if (player.getEurope() != null) {
            String europeName = player.getEurope().getLocationName();
            row = handleLocation(europeName, true, row);
            otherNames.remove(europeName);
        }

        // finally all other locations, sorted alphabetically
        Collections.sort(otherNames);
        for (String locationName : otherNames) {
            row = handleLocation(locationName, false, row);
        }

        add(detailPanel, higConst.rc(detailPanelRow, labelColumn));
        revalidate();
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
        locations = new HashMap<String, ArrayList<Unit>>();
        colonies = player.getColonies();
        Collections.sort(colonies, parent.getClient().getClientOptions().getColonyComparator());
        colonyNames = new ArrayList<String>();
        for (Colony colony : colonies) {
            colonyNames.add(colony.getName());
        }
        otherNames = new ArrayList<String>();

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
        List<AbstractUnit> navalUnits = new ArrayList<AbstractUnit>();
        List<AbstractUnit> landUnits = new ArrayList<AbstractUnit>();
        if (refUnits != null) {
            NodeList childElements = refUnits.getChildNodes();
            for (int index = 0; index < childElements.getLength(); index++) {
                AbstractUnit unit = new AbstractUnit();
                unit.readFromXMLElement((Element) childElements.item(index));
                if (unit.getUnitType().hasAbility("model.ability.navalUnit")) {
                    navalUnits.add(unit);
                } else {
                    landUnits.add(unit);
                }
            }
        }

        JPanel refPanel = null;
        if (reportType == ReportType.NAVAL) {
            refPanel = new JPanel(new GridLayout(1, navalUnits.size()));
            for (AbstractUnit unit : navalUnits) {
                refPanel.add(createUnitTypeLabel(unit.getUnitType(), Role.DEFAULT, unit.getNumber()));
            }
        } else if (reportType == ReportType.MILITARY) {
            refPanel = new JPanel(new GridLayout(1, landUnits.size()));
            for (AbstractUnit unit : landUnits) {
                refPanel.add(createUnitTypeLabel(unit.getUnitType(), unit.getRole(), unit.getNumber()));
            }
        }
        if (refPanel != null) {
            refPanel.setOpaque(false);
            String refName = Messages.message(player.getNation().getRefId() + ".name");
            refPanel.setBorder(BorderFactory.createTitledBorder(refName));
        }
        return refPanel;
    }

    private JPanel createUnitPanel() {
        JPanel unitPanel = new JPanel();
        List<AbstractUnit> units = new ArrayList<AbstractUnit>();
        switch (reportType) {
        case CARGO:
            for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
                if (unitType.isAvailableTo(player) && 
                    (unitType.canCarryUnits() || unitType.canCarryGoods())) {
                    units.add(new AbstractUnit(unitType, Role.DEFAULT, getCount(others, unitType)));
                }
            }
            break;
        case NAVAL:
            for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
                if (unitType.isAvailableTo(player) &&
                    unitType.hasAbility("model.ability.navalUnit")) {
                    units.add(new AbstractUnit(unitType, Role.DEFAULT, getCount(others, unitType)));
                }
            }
            break;
        case MILITARY:
            List<AbstractUnit> dragoonUnits = new ArrayList<AbstractUnit>();
            List<AbstractUnit> soldierUnits = new ArrayList<AbstractUnit>();
            for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
                if (unitType.isAvailableTo(player) &&
                    !unitType.hasAbility("model.ability.navalUnit") && 
                    (unitType.hasAbility("model.ability.expertSoldier") ||
                     unitType.getOffence() > 0)) {
                    if (unitType.hasAbility("model.ability.canBeEquipped")) {
                        dragoonUnits.add(new AbstractUnit(unitType, Role.DRAGOON, getCount(dragoons, unitType)));
                        soldierUnits.add(new AbstractUnit(unitType, Role.SOLDIER, getCount(soldiers, unitType)));
                    } else {
                        units.add(new AbstractUnit(unitType, Role.DEFAULT, getCount(others, unitType)));
                    }
                }
            }
            dragoonUnits.add(new AbstractUnit(defaultType, Role.DRAGOON, getCount(dragoons, defaultType)));
            soldierUnits.add(new AbstractUnit(defaultType, Role.SOLDIER, getCount(soldiers, defaultType)));
            units.addAll(dragoonUnits);
            units.addAll(soldierUnits);
        }
        if (unitPanel != null) {
            unitPanel.setOpaque(false);
            unitPanel.setBorder(BorderFactory.createTitledBorder(player.getNationAsString()));
            unitPanel.setLayout(new GridLayout(1, units.size()));
            for (AbstractUnit unit : units) {
                unitPanel.add(createUnitTypeLabel(unit));
            }
        }
        return unitPanel;
    }


    private int handleLocation(String location, boolean makeButton, int row) {
        List<Unit> unitList = locations.get(location);
        if ((unitList == null || unitList.isEmpty()) && ignoreEmptyLocations) {
            return row;
        } else {
            if (makeButton) {
                JButton locationButton = FreeColPanel.getLinkButton(location, null, location);
                locationButton.addActionListener(this);
                detailPanel.add(locationButton, higConst.rc(row, labelColumn, "lt"));
            } else {
                JLabel locationLabel = new JLabel(location);
                detailPanel.add(locationLabel, higConst.rc(row, labelColumn));
            }
            if (unitList != null) {
                Collections.sort(unitList, reportPanel.getUnitTypeComparator());
                if (reportType == ReportType.MILITARY) {
                    int column = unitColumn - 1;
                    for (Unit unit : unitList) {
                        UnitLabel unitLabel = new UnitLabel(unit, parent, true);
                        if (unit.getDestination() != null) {
                            String destination = unit.getDestination().getLocationName();
                            unitLabel.setToolTipText("<html>" + unitLabel.getToolTipText() + "<br>" +
                                                     Messages.message("goingTo", "%location%", destination) +
                                                     "</html>");
                        }
                        if (column == 2 + militaryColumns) {
                            column = unitColumn;
                            row++;
                        } else {
                            column++;
                        }
                        detailPanel.add(unitLabel, higConst.rc(row, column));
                    }
                } else {
                    // one row for each unit
                    int column = unitColumn;
                    for (Unit unit : unitList) {
                        UnitLabel unitLabel = new UnitLabel(unit, parent, true);
                        if (unit.getDestination() != null) {
                            String messageID = unit.isNaval() ? "sailingTo" : "goingTo";
                            String destination = unit.getDestination().getLocationName();
                            unitLabel.setToolTipText("<html>" + unitLabel.getToolTipText() + "<br>" + 
                                                     Messages.message(messageID, "%location%",
                                                                      destination) +
                                                     "</html>");
                        }
                        // this is necessary because UnitLabel deselects carriers
                        unitLabel.setSelected(true);
                        detailPanel.add(unitLabel, higConst.rc(row, unitColumn));
                    
                        for (Goods goods : unit.getGoodsList()) {
                            GoodsLabel goodsLabel = new GoodsLabel(goods, reportPanel.getCanvas());
                            if (column == 2 + carrierColumns) {
                                column = unitColumn + 1;
                                row++;
                            } else {
                                column++;
                            }
                            detailPanel.add(goodsLabel, higConst.rc(row, column));
                        }
                        for (Unit unitLoaded : unit.getUnitList()) {
                            UnitLabel unitLoadedLabel = new UnitLabel(unitLoaded,
                                                                      reportPanel.getCanvas(), true);
                            if (column == 2 + carrierColumns) {
                                column = unitColumn + 1;
                                row++;
                            } else {
                                column++;
                            }
                            detailPanel.add(unitLoadedLabel, higConst.rc(row, column));
                        }
                    }
                }
            }
            return row + 1;
        }
    }

    private JLabel createUnitTypeLabel(AbstractUnit unit) {
        return createUnitTypeLabel(unit.getUnitType(), unit.getRole(), unit.getNumber());
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
