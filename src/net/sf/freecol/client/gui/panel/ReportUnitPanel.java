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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Element;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Naval Report.
 */
public final class ReportUnitPanel extends JPanel implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    // The column for location labels.
    private static final int labelColumn = 1;

    // The column for unit panels.
    private static final int unitColumn = 3;
    
    // The extra rows needed (one for summary, one separator).
    private static final int extraRows = 2;

    // The height of the separator row.
    private static final int separator = 12;

    /** The type of a unit report. */
    public static final int MILITARY = 0, NAVAL = 1, CARGO = 2;
    
    /**
     * Whether this is a naval, military or cargo unit report.
     */
    private int reportType;

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
    private int[][] unitCounts = new int[Unit.UNIT_COUNT][2];

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
    public ReportUnitPanel(int type, boolean ignoreEmptyLocations, Canvas parent, ReportPanel reportPanel) {
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

        int rowsREF = (reportType == CARGO ? 0 : 1);
        int[] widths = new int[] {0};
        int[] heights = new int[3 + rowsREF];
        heights[1 + rowsREF] = separator;

        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        if (reportType != CARGO) {
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

        int rowsREF = (reportType == CARGO ? 0 : 1);
        int[] widths = new int[] { 0, 12, 0 };
        int[] heights = new int[colonies.size() + otherNames.size() + extraRows + rowsREF];
        heights[1 + rowsREF] = separator;

        // reset row
        row = 1;

        removeAll();
        setLayout(new HIGLayout(widths, heights));

        // REF
        if (reportType != CARGO) {
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

    private void gatherData() {
        prepareData();

        Iterator<Unit> units = player.getUnitIterator();
        while (units.hasNext()) {
            Unit unit = units.next();
            int type = unit.getType();
            String locationName = null;

            if (reportType == CARGO && unit.isCarrier()) {
                unitCounts[type][0]++;
                capacity += unit.getInitialSpaceLeft();
            } else if (reportType == NAVAL && unit.isNaval()) {
                unitCounts[type][0]++;
                capacity += unit.getInitialSpaceLeft();
            } else if (reportType == MILITARY) {
                switch (type) {
                case Unit.ARTILLERY:
                case Unit.DAMAGED_ARTILLERY:
                    unitCounts[type][0]++;
                    break;
                case Unit.VETERAN_SOLDIER:
                case Unit.COLONIAL_REGULAR:
                    if (unit.isArmed()) {
                        if (unit.isMounted()) {
                            unitCounts[type][1]++;
                        } else {
                            unitCounts[type][0]++;
                        }
                    } else {
                        continue;
                    }
                    break;
                default:
                    if (unit.isArmed()) {
                        if (unit.isMounted()) {
                            unitCounts[Unit.FREE_COLONIST][1]++;
                        } else {
                            unitCounts[Unit.FREE_COLONIST][0]++;
                        }
                    } else {
                        continue;
                    }
                    break;
                }                    
            } else {
                continue;
            }

            if (unit.getState() == Unit.TO_AMERICA) {
                locationName = Messages.message("goingToAmerica");
            } else if (unit.getState() == Unit.TO_EUROPE) {
                locationName = Messages.message("goingToEurope");
            } else {
                locationName = unit.getLocation().getLocationName();
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
        if (reportType == NAVAL) {
            int menOfWar = Integer.parseInt(refUnits.getAttribute("menOfWar"));
            refPanel = new JPanel(new GridLayout(1, 6));
            refPanel.add(createUnitTypeLabel(Unit.MAN_O_WAR, false, menOfWar));
        } else if (reportType == MILITARY) {
            int artillery = Integer.parseInt(refUnits.getAttribute("artillery"));
            int damagedArtillery = Integer.parseInt(refUnits.getAttribute("damagedArtillery"));
            int dragoons = Integer.parseInt(refUnits.getAttribute("dragoons"));
            int infantry = Integer.parseInt(refUnits.getAttribute("infantry"));
            refPanel = new JPanel(new GridLayout(1, 8));
            refPanel.add(createUnitTypeLabel(Unit.ARTILLERY, false, artillery));
            refPanel.add(createUnitTypeLabel(Unit.DAMAGED_ARTILLERY, false, damagedArtillery));
            refPanel.add(createUnitTypeLabel(Unit.KINGS_REGULAR, true, dragoons));
            refPanel.add(createUnitTypeLabel(Unit.KINGS_REGULAR, false, infantry));
        }
        if (refPanel != null) {
            refPanel.setOpaque(false);
            refPanel.setBorder(BorderFactory.createTitledBorder(player.getREFPlayer().getNationAsString()));
        }
        return refPanel;
    }

    private JPanel createUnitPanel() {
        JPanel unitPanel = null;
        if (reportType == CARGO) {
            int[] unitTypes = new int[] {
                Unit.WAGON_TRAIN, Unit.CARAVEL, Unit.MERCHANTMAN, Unit.GALLEON,
                Unit.FRIGATE, Unit.MAN_O_WAR, Unit.PRIVATEER
            };
            unitPanel = new JPanel(new GridLayout(1, unitTypes.length));
            for (int index = 0; index < unitTypes.length; index++) {
                int count = unitCounts[unitTypes[index]][0];
                unitPanel.add(createUnitTypeLabel(unitTypes[index], false, count));
            }
        } else if (reportType == NAVAL) {
            int[] unitTypes = new int[] {
                Unit.CARAVEL, Unit.MERCHANTMAN, Unit.GALLEON,
                Unit.FRIGATE, Unit.MAN_O_WAR, Unit.PRIVATEER
            };
            unitPanel = new JPanel(new GridLayout(1, unitTypes.length));
            for (int index = 0; index < unitTypes.length; index++) {
                int count = unitCounts[unitTypes[index]][0];
                unitPanel.add(createUnitTypeLabel(unitTypes[index], false, count));
            }
        } else if (reportType == MILITARY) {
            int[] unitTypes = new int[] {
                Unit.ARTILLERY, Unit.DAMAGED_ARTILLERY,
                Unit.COLONIAL_REGULAR, Unit.VETERAN_SOLDIER,
                Unit.FREE_COLONIST
            };
            unitPanel = new JPanel(new GridLayout(1, unitTypes.length));
            // artillery can not be mounted
            for (int index = 0; index < 2; index++) {
                int count = unitCounts[unitTypes[index]][0];
                unitPanel.add(createUnitTypeLabel(unitTypes[index], false, count));
            }
            // other units can be mounted
            for (int mounted = 1; mounted >= 0; mounted--) {
                for (int index = 2; index < unitTypes.length; index++) {
                    int count = unitCounts[unitTypes[index]][mounted];
                    unitPanel.add(createUnitTypeLabel(unitTypes[index], (mounted == 1), count));
                }
            }
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
                if (reportType == MILITARY) {
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

    private JLabel createUnitTypeLabel(int unitType, boolean mounted, int count) {
        int graphicsType = ImageLibrary.getUnitGraphicsType(unitType, true, mounted, 0, false);
        JLabel unitLabel = reportPanel.buildUnitLabel(graphicsType, 0.66f);
        unitLabel.setText(String.valueOf(count));
        if (count == 0) {
            unitLabel.setForeground(Color.GRAY);
        }
        switch (unitType) {
        case Unit.ARTILLERY:
        case Unit.DAMAGED_ARTILLERY:
        case Unit.CARAVEL:
        case Unit.MERCHANTMAN:
        case Unit.GALLEON:
        case Unit.FRIGATE:
        case Unit.MAN_O_WAR:
        case Unit.PRIVATEER:
        case Unit.WAGON_TRAIN:
            unitLabel.setToolTipText(Unit.getName(unitType));
            break;
        case Unit.VETERAN_SOLDIER:
            if (mounted) {
                unitLabel.setToolTipText(Messages.message("model.unit.veteranDragoon"));
            } else {
                unitLabel.setToolTipText(Messages.message("model.unit.veteranSoldier"));
            }
            break;
        case Unit.COLONIAL_REGULAR:
            if (mounted) {
                unitLabel.setToolTipText(Messages.message("model.unit.colonialCavalry"));
            } else {
                unitLabel.setToolTipText(Messages.message("model.unit.colonialRegular"));
            }
            break;
        case Unit.KINGS_REGULAR:
            if (mounted) {
                unitLabel.setToolTipText(Messages.message("model.unit.kingsCavalry"));
            } else {
                unitLabel.setToolTipText(Messages.message("model.unit.kingsRegular"));
            }
            break;
        default:
            if (mounted) {
                unitLabel.setToolTipText(Messages.message("model.unit.dragoon"));
            } else {
                unitLabel.setToolTipText(Messages.message("model.unit.soldier"));
            }
        }
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
