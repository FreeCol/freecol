package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private int[] unitCount;
    private Vector<HashMap<String, Integer>> unitLocations;
    private ArrayList<String> locationNames = new ArrayList<String>();

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportLabourPanel(Canvas parent) {
        super(parent, Messages.message("report.labour"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        // Count Units
        unitCount = new int[Unit.UNIT_COUNT];
        unitLocations = new Vector<HashMap<String,Integer>>(Unit.UNIT_COUNT);
        for (int index = 0; index < Unit.UNIT_COUNT; index++) {
            unitLocations.set(index,new HashMap<String, Integer>());
        }
        Player player = getCanvas().getClient().getMyPlayer();
        Iterator<Unit> units = player.getUnitIterator();

        ArrayList<Colony> colonies = new ArrayList<Colony>();
        for(Settlement s : player.getSettlements()) {
            if (s instanceof Colony) {
                colonies.add((Colony) s);
            } else {
                throw new RuntimeException("Invalid type in array returned by getSettlements()");
            }
        }
        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());
        Iterator<Colony> colonyIterator = colonies.iterator();
        locationNames = new ArrayList<String>();
        locationNames.add(Messages.message("report.atSea"));
        locationNames.add(Messages.message("report.onLand"));
        if (player.getEurope() != null)
            locationNames.add(player.getEurope().toString());
        while (colonyIterator.hasNext()) {
            locationNames.add(colonyIterator.next().getName());
        }

        while (units.hasNext()) {
            Unit unit = units.next();
            int type = unit.getType();
            Location location = unit.getLocation();
            String locationName = null;

            if (location instanceof WorkLocation) {
                locationName = ((WorkLocation) location).getColony().getName();
            } else if (location instanceof Europe) {
                locationName = player.getEurope().toString();
            } else if (location instanceof Tile &&
                       ((Tile) location).getSettlement() != null) {
                locationName = ((Colony) ((Tile) location).getSettlement()).getName();
            } else if (location instanceof Unit) {
                locationName = Messages.message("report.atSea");
            } else {
                locationName = Messages.message("report.onLand");
            }

            if (locationName != null) {
                if (unitLocations.get(type).containsKey(locationName)) {
                    int oldValue = unitLocations.get(type).get(locationName).intValue();
                    unitLocations.get(type).put(locationName, new Integer(oldValue + 1));
                } else {
                    unitLocations.get(type).put(locationName, new Integer(1));
                }
            }

            unitCount[unit.getType()]++;
        }

        // Display Panel
        reportPanel.removeAll();
        int margin = 30;
        int[] widths = new int[] {0, 5, 0, margin, 0, 5, 0, margin, 0, 5, 0};
        int[] heights = new int[] {0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0};
        reportPanel.setLayout(new HIGLayout(widths, heights));

        int[][] unitTypes = new int[][] {
            {Unit.FREE_COLONIST, Unit.INDENTURED_SERVANT, Unit.PETTY_CRIMINAL},
            {Unit.INDIAN_CONVERT, Unit.EXPERT_FARMER, Unit.EXPERT_FISHERMAN},
            {Unit.MASTER_SUGAR_PLANTER, Unit.MASTER_DISTILLER, Unit.EXPERT_LUMBER_JACK},
            {Unit.MASTER_CARPENTER, Unit.MASTER_TOBACCO_PLANTER, Unit.MASTER_TOBACCONIST},
            {Unit.EXPERT_FUR_TRAPPER, Unit.MASTER_FUR_TRADER, Unit.MASTER_COTTON_PLANTER},
            {Unit.MASTER_WEAVER, Unit.EXPERT_ORE_MINER, Unit.MASTER_BLACKSMITH},
            {Unit.MASTER_GUNSMITH, Unit.EXPERT_SILVER_MINER, Unit.HARDY_PIONEER},
            {Unit.VETERAN_SOLDIER, Unit.SEASONED_SCOUT, Unit.JESUIT_MISSIONARY},
            {Unit.ELDER_STATESMAN, Unit.FIREBRAND_PREACHER, -1}
        };

        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 3; column++) {
                int tools = 0;
                if (unitTypes[row][column] < 0) {
                    continue;
                } else if (unitTypes[row][column] == Unit.HARDY_PIONEER) {
                    tools = 20;
                }
                int imageType = ImageLibrary.getUnitGraphicsType(unitTypes[row][column], false, false, tools, false);
                reportPanel.add(buildUnitLabel(imageType, 1f),
                                higConst.rc(2 * row + 1, 4 * column + 1));
                reportPanel.add(buildUnitReport(unitTypes[row][column]),
                                higConst.rc(2 * row + 1, 4 * column + 3));
            }
        }

        reportPanel.doLayout();
    }

    private JPanel buildUnitReport(int unit) {

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);

        int[] widths = {0, 5, 0};
        int[] heights = null;
        int colonyColumn = 1;
        int countColumn = 3;
        int keys = 0;
        if (unitLocations.get(unit) != null) {
            keys = unitLocations.get(unit).size();
        }
        if (keys == 0) {
            heights = new int[] {0};
        } else {
            heights = new int[keys + 2];
            for (int index = 0; index < heights.length; index++) {
                heights[index] = 0;
            }
            heights[1] = 5;
        }

        textPanel.setLayout(new HIGLayout(widths, heights));

        // summary
        int row = 1;
        JLabel unitLabel = new JLabel(Unit.getName(unit)); 
        textPanel.add(unitLabel, higConst.rc(row, colonyColumn));

        if (unitCount[unit] == 0) {
            unitLabel.setForeground(Color.GRAY);
        } else {
            textPanel.add(new JLabel(String.valueOf(unitCount[unit])),
                          higConst.rc(row, countColumn));

            row = 3;
            Iterator<String> locationIterator = locationNames.iterator();
            while (locationIterator.hasNext()) {
                String name = locationIterator.next();
                if (unitLocations.get(unit).get(name) != null) {
                    JLabel colonyLabel = new JLabel(name);
                    colonyLabel.setForeground(Color.GRAY);
                    textPanel.add(colonyLabel, higConst.rc(row, colonyColumn));
                    JLabel countLabel = new JLabel(unitLocations.get(unit).get(name).toString());
                    countLabel.setForeground(Color.GRAY);
                    textPanel.add(countLabel, higConst.rc(row, countColumn));
                    row++;
                }
            }
        }
        return textPanel;
    }
}
