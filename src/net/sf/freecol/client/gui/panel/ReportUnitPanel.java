package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Element;

import cz.autel.dmi.HIGLayout;
import cz.autel.dmi.HIGConstraints;


/**
 * This panel displays the Naval Report.
 */
public final class ReportUnitPanel extends JPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    // The HIGLayout widths.
    private static final int[] widths = new int[] {0, 12, 0};
    // The HIGLayout heights.
    private static int[] heights;
    // The column for location labels.
    private static final int labelColumn = 1;
    // The column for unit panels.
    private static final int unitColumn = 3;
    // The extra rows needed (one row for REF, one separator).
    private static final int extraRows = 2;
    // The height of the separator row.
    private static final int separator = 12;

    /**
     * Whether this is a naval unit report.
     */
    private boolean isNaval;

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
    private List<Settlement> colonies;
    private final ReportPanel reportPanel;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportUnitPanel(boolean isNaval, boolean ignoreEmptyLocations, Canvas parent, ReportPanel reportPanel) {
        this.isNaval = isNaval;
        this.ignoreEmptyLocations = ignoreEmptyLocations;
        this.parent = parent;
        this.reportPanel = reportPanel;
        heights = null;
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();

        locations = new HashMap<String, ArrayList<Unit>>();
        colonies = player.getSettlements();
        Collections.sort(colonies, parent.getClient().getClientOptions().getColonyComparator());
        ArrayList<String> colonyNames = new ArrayList();
        Iterator colonyIterator = colonies.iterator();
        String colonyName;
        while (colonyIterator.hasNext()) {
            colonyName = ((Colony) colonyIterator.next()).getName();
            colonyNames.add(colonyName);
        }

        ArrayList<String> otherNames = new ArrayList<String>();


        // Display Panel
        removeAll();

        // reset row
        row = 1;
        // reset location index
        locationIndex = 0;

        // total cargo capacity of fleet (currently unused)
        int capacity = 0;

        Iterator<Unit> units = player.getUnitIterator();
        while (units.hasNext()) {
            Unit unit = units.next();
            int type = unit.getType();
            String locationName = null;

            if (isNaval && unit.isNaval()) {

                capacity += unit.getInitialSpaceLeft();
                Location location = unit.getLocation();
                if (unit.getState() == Unit.TO_AMERICA) {
                    locationName = Messages.message("goingToAmerica");
                } else if (unit.getState() == Unit.TO_EUROPE) {
                    locationName = Messages.message("goingToEurope");
                } else if (unit.getDestination() != null) {
                    locationName = Messages.message("sailingTo", new String[][] {{"%location%", unit.getDestination().getLocationName()}});
                } else {
                    locationName = location.getLocationName();
                } 
            } else if (!isNaval && (unit.getType() == Unit.ARTILLERY ||
                                    unit.getType() == Unit.DAMAGED_ARTILLERY ||
                                    unit.isArmed())) {

                Location location = unit.getLocation();
                if (unit.getDestination() != null) {
                    locationName = Messages.message("goingTo", new String[][] {{"%location%", unit.getDestination().getLocationName()}});
                } else {
                    locationName = location.getLocationName();
                } 
            }

            if (locationName != null) {
                ArrayList<Unit> unitList = locations.get(locationName);
                if (unitList == null) {
                    unitList = new ArrayList<Unit>();
                    locations.put(locationName, unitList);
                }
                unitList.add(unit);
                if (!(colonyNames.contains(locationName) ||
                      otherNames.contains(locationName))) {
                    otherNames.add(locationName);
                }
            }
        }

        heights = new int[colonies.size() + otherNames.size() + extraRows];
        heights[1] = separator;

        setLayout(new HIGLayout(widths, heights));

        // REF
        Element refUnits = parent.getClient().getInGameController().getREFUnits();
        int artillery = Integer.parseInt(refUnits.getAttribute("artillery"));
        int menOfWar = Integer.parseInt(refUnits.getAttribute("menOfWar"));
        int dragoons = Integer.parseInt(refUnits.getAttribute("dragoons"));
        int infantry = Integer.parseInt(refUnits.getAttribute("infantry"));

        JPanel refPanel;
        if (isNaval) {
            refPanel = new JPanel(new GridLayout(0, 7));
            for (int count = 0; count < menOfWar; count++) {
                refPanel.add(reportPanel.buildUnitLabel(ImageLibrary.MAN_O_WAR, 0.66f));
            }
        } else {
            int[] refUnitCounts = new int[] {artillery, dragoons, infantry};
            int[] libraryUnitType = new int[] {
                ImageLibrary.ARTILLERY,
                ImageLibrary.KINGS_CAVALRY,
                ImageLibrary.KINGS_REGULAR };
            refPanel = new JPanel(new GridLayout(0, 12));
            for (int index = 0; index < refUnitCounts.length; index++) {
                for (int count = 0; count < refUnitCounts[index]; count++) {
                    refPanel.add(reportPanel.buildUnitLabel(libraryUnitType[index], 0.66f));
                }
            }
        }
        refPanel.setBorder(BorderFactory.createTitledBorder(player.getREFPlayer().getNationAsString()));
        add(refPanel, higConst.rcwh(row, labelColumn, widths.length, 1));
        row++;

        // add separator with strut
        add(Box.createHorizontalStrut(500), higConst.rc(row, unitColumn));
        row++;

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

    }

    private void handleLocation(String location, boolean makeButton) {
        List unitList = locations.get(location);
        if (!(unitList == null && ignoreEmptyLocations)) {
            if (makeButton) {
                JButton locationButton = new JButton(location);
                locationButton.setActionCommand(String.valueOf(locationIndex));
                locationButton.addActionListener(this);
                add(locationButton, higConst.rc(row, labelColumn));
            } else {
                JLabel locationLabel = new JLabel(location);
                add(locationLabel, higConst.rc(row, labelColumn));
            }
            if (unitList != null) {
                JPanel unitPanel = new JPanel(new GridLayout(0, 7));
                Collections.sort(unitList, reportPanel.getUnitTypeComparator());
                Iterator<Unit> unitIterator = unitList.iterator();
                while (unitIterator.hasNext()) {
                    UnitLabel unitLabel = new UnitLabel(unitIterator.next(), parent, true);
                    // this is necessary because UnitLabel deselects carriers
                    unitLabel.setSelected(true);
                    unitPanel.add(unitLabel);
                }
                add(unitPanel, higConst.rc(row, unitColumn, "l"));
            }
            row++;
        }
        locationIndex++;
    }


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        int action = Integer.valueOf(command).intValue();
        if (action == ReportPanel.OK) {
            reportPanel.actionPerformed(event);
        } else if (action < colonies.size()) {
            parent.showColonyPanel((Colony) colonies.get(action));
        } else if (action == colonies.size()) {
            parent.showEuropePanel();
        }

    }
}

