package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private int[] unitCount, unitAtSea, unitOnLand, unitInEurope;
    private Vector<HashMap<Colony, Integer>> unitLocations;
    private List<Colony> colonies;

    private Player player;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportLabourPanel(Canvas parent) {
        super(parent, Messages.message("report.labour"));
        player = parent.getClient().getMyPlayer();
        colonies = player.getColonies();
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        gatherData();
        displayData();
    }

    public void gatherData() {

        // Count Units
        unitCount = new int[Unit.UNIT_COUNT];
        unitAtSea = new int[Unit.UNIT_COUNT];
        unitOnLand = new int[Unit.UNIT_COUNT];
        unitInEurope = new int[Unit.UNIT_COUNT];
        unitLocations = new Vector<HashMap<Colony,Integer>>(Unit.UNIT_COUNT);
        for (int index = 0; index < Unit.UNIT_COUNT; index++) {
            unitLocations.add(new HashMap<Colony, Integer>());
        }

        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        //locationNames.add(Messages.message("report.atSea"));
        //locationNames.add(Messages.message("report.onLand"));
        /*
        if (player.getEurope() != null)
            locationNames.add(player.getEurope().toString());
        while (colonyIterator.hasNext()) {
            String colonyName = colonyIterator.next().getName();
            locationNames.add(colonyName);
            locationNames.add(colonyName + "*");
        }
        */
        Iterator<Unit> units = player.getUnitIterator();
        while (units.hasNext()) {
            Unit unit = units.next();
            int type = unit.getType();
            Location location = unit.getLocation();

            unitCount[type]++;

            if (location instanceof WorkLocation) {
                incrementColonyCount(((WorkLocation) location).getColony(), type);
            } else if (location instanceof Europe) {
                unitInEurope[type]++;
            } else if (location instanceof Tile && ((Tile) location).getSettlement() != null) {
                incrementColonyCount((Colony) ((Tile) location).getSettlement(), type);
            } else if (location instanceof Unit) {
                unitAtSea[type]++;
            } else {
                unitOnLand[type]++;
            }

        }
    }

    private void incrementColonyCount(Colony colony, int type) {
        Integer count = unitLocations.get(type).get(colony);
        if (count == null) {
            unitLocations.get(type).put(colony, new Integer(1));
        } else {
            unitLocations.get(type).put(colony, new Integer(count.intValue() + 1));
        }
    }

    public void displayData() {

        // Display Panel
        //reportPanel.removeAll();
        int margin = 30;
        int[] widths = new int[] {0, 5, 0, margin, 0, 5, 0, margin, 0, 5, 0};
        int[] heights = new int[] {0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0, 12, 0};
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
                                higConst.rc(2 * row + 1, 4 * column + 1, "t"));
                reportPanel.add(buildUnitDetails(unitTypes[row][column]),
                                higConst.rc(2 * row + 1, 4 * column + 3));
            }
        }
        reportPanel.add(new JLabel(Messages.message("report.labour.canTrain")),
                        higConst.rcwh(heights.length, 1, widths.length, 1, "l"));
    }

    private JPanel buildUnitDetails(int unit) {

        HashMap<Colony, Integer> locations = unitLocations.get(unit);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);

        int[] widths = {0, 5, 0};
        int[] heights = null;
        int colonyColumn = 1;
        int countColumn = 3;

        int keys = 0;
        if (unitCount[unit] > 0) {
            if (locations != null) {
                keys = locations.size();
            }
            if (unitInEurope[unit] > 0) {
                keys++;
            }
            if (unitAtSea[unit] > 0) {
                keys++;
            }
            if (unitOnLand[unit] > 0) {
                keys++;
            }
        }

        if (keys == 0) {
            heights = new int[] {0};
        } else {
            heights = new int[keys + 2];
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
            for (Colony colony : colonies) {
                if (locations.get(colony) != null) {
                    textPanel.add(createColonyButton(colony, unit),
                                  higConst.rc(row, colonyColumn, "l"));
                    JLabel countLabel = new JLabel(locations.get(colony).toString());
                    countLabel.setForeground(LINK_COLOR);
                    textPanel.add(countLabel, higConst.rc(row, countColumn, "r"));
                    row++;
                }
            }
            if (unitInEurope[unit] > 0) {
                JButton button = new JButton(player.getEurope().getName());
                button.setMargin(new Insets(0,0,0,0));
                button.setOpaque(false);
                button.setForeground(LINK_COLOR);
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setActionCommand(player.getEurope().getName());
                button.addActionListener(this);
                textPanel.add(button, higConst.rc(row, colonyColumn, "l"));
                JLabel countLabel = new JLabel(String.valueOf(unitInEurope[unit]));
                countLabel.setForeground(LINK_COLOR);
                textPanel.add(countLabel, higConst.rc(row, countColumn, "r"));
                row++;
            }
            if (unitOnLand[unit] > 0) {
                JLabel onLandLabel = new JLabel(Messages.message("report.onLand"));
                onLandLabel.setForeground(Color.GRAY);
                textPanel.add(onLandLabel, higConst.rc(row, colonyColumn, "l"));
                JLabel countLabel = new JLabel(String.valueOf(unitOnLand[unit]));
                countLabel.setForeground(Color.GRAY);
                textPanel.add(countLabel, higConst.rc(row, countColumn, "r"));
                row++;
            }
            if (unitAtSea[unit] > 0) {
                JLabel atSeaLabel = new JLabel(Messages.message("report.atSea"));
                atSeaLabel.setForeground(Color.GRAY);
                textPanel.add(atSeaLabel, higConst.rc(row, colonyColumn, "l"));
                JLabel countLabel = new JLabel(String.valueOf(unitAtSea[unit]));
                countLabel.setForeground(Color.GRAY);
                textPanel.add(countLabel, higConst.rc(row, countColumn, "r"));
                row++;
            }
        }

        return textPanel;
    }

    private JButton createColonyButton(Colony colony, int unit) {

        JButton button = new JButton();
        if (colony.canTrain(unit)) {
            button.setText(colony.getName() + "*");
        } else {
            button.setText(colony.getName());
        }
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(colony.getName());
        button.addActionListener(this);
        return button;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals("-1")) {
            super.actionPerformed(event);
        } else if (command.equals(player.getEurope().getName())) {
            getCanvas().showEuropePanel();
        } else {
            getCanvas().showColonyPanel(player.getColony(command));
        }
    }
}
