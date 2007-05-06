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
        int[] widths = new int[] {
            0, 5, 0, 5, 0, margin,
            0, 5, 0, 5, 0, margin,
            0, 5, 0, 5, 0
        };
        int columnsPerUnit = 6;
        int buttonColumn = 1;
        int nameColumn = 3;
        int countColumn = 5;

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

        int[] heights = new int[2 * unitTypes.length - 1];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = 12;
        }

        reportPanel.setLayout(new HIGLayout(widths, heights));

        for (int row = 0; row < unitTypes.length; row++) {
            for (int column = 0; column < unitTypes[0].length; column++) {
                int tools = 0;
                if (unitTypes[row][column] < 0) {
                    continue;
                } else if (unitTypes[row][column] == Unit.HARDY_PIONEER) {
                    tools = 20;
                }

                reportPanel.add(createUnitLabel(unitTypes[row][column], tools),
                                higConst.rc(2 * row + 1, columnsPerUnit * column + buttonColumn, "t"));
                if (unitCount[unitTypes[row][column]] > 0) {
                    reportPanel.add(createUnitNameButton(unitTypes[row][column]),
                                    higConst.rc(2 * row + 1, columnsPerUnit * column + nameColumn, "tl"));
                    reportPanel.add(new JLabel(String.valueOf(unitCount[unitTypes[row][column]])),
                                    higConst.rc(2 * row + 1, columnsPerUnit * column + countColumn, "tr"));
                } else {
                    JLabel unitNameLabel = new JLabel(Unit.getName(unitTypes[row][column])); 
                    unitNameLabel.setForeground(Color.GRAY);
                    reportPanel.add(unitNameLabel,
                                    higConst.rc(2 * row + 1, columnsPerUnit * column + nameColumn, "tl"));
                }
            }
        }
    }
    

    private JButton createUnitNameButton(int unitType) {
        JButton button = new JButton(Unit.getName(unitType));
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(String.valueOf(unitType));
        button.addActionListener(this);
        return button;

    }

    private JLabel createUnitLabel(int unitType, int tools) {
        int imageType = ImageLibrary.getUnitGraphicsType(unitType, false, false, tools, false);
        JLabel unitLabel = new JLabel(getLibrary().getUnitImageIcon(imageType));
        return unitLabel;
    }

    private JPanel createUnitDetails(int unit, ReportLabourDetailPanel report) {

        int tools = 0;
        if (unit == Unit.HARDY_PIONEER) {
            tools = 20;
        }

        int maxColumns = 3;
        int columnsPerColumn = 4;
        int[] widths = new int[maxColumns * columnsPerColumn - 1];
        for (int index = 1; index < widths.length; index += 2) {
            widths[index] = 12;
        }
        int keys = 0;
        if (unitCount[unit] > 0) {
            if (unitLocations != null) {
                keys = unitLocations.get(unit).size();
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

        int numberOfRows = 6;
        if (keys > numberOfRows * maxColumns) {
            numberOfRows = keys / 3;
            if (keys % 3 > 0) {
                numberOfRows++;
            }
        }

        int[] heights = new int[numberOfRows + 2];
        heights[1] = 12;

        int colonyColumn = 1;
        int countColumn = 3;
        int startRow = 3;

        JPanel detailPanel = new JPanel(new HIGLayout(widths, heights));
        detailPanel.setOpaque(false);

        // summary
        int row = 1;
        detailPanel.add(new JLabel(Unit.getName(unit)),
                        higConst.rc(row, colonyColumn));
        detailPanel.add(new JLabel(String.valueOf(unitCount[unit])),
                        higConst.rc(row, countColumn));

        row = startRow;
        int column = 0;
        for (Colony colony : colonies) {
            if (unitLocations.get(unit).get(colony) != null) {
                detailPanel.add(createColonyButton(colony, unit, report),
                                higConst.rc(row, column * columnsPerColumn + colonyColumn, "l"));
                JLabel countLabel = new JLabel(unitLocations.get(unit).get(colony).toString());
                countLabel.setForeground(LINK_COLOR);
                detailPanel.add(countLabel, 
                                higConst.rc(row, column * columnsPerColumn + countColumn, "r"));
                if (row == heights.length) {
                    row = startRow;
                    column++;
                } else {
                    row++;
                }
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
            detailPanel.add(button,
                            higConst.rc(row, column * columnsPerColumn + colonyColumn, "l"));
            JLabel countLabel = new JLabel(String.valueOf(unitInEurope[unit]));
            countLabel.setForeground(LINK_COLOR);
            detailPanel.add(countLabel,
                            higConst.rc(row, column * columnsPerColumn + countColumn, "r"));
            if (row == heights.length) {
                row = startRow;
                column++;
            } else {
                row++;
            }
        }
        if (unitOnLand[unit] > 0) {
            JLabel onLandLabel = new JLabel(Messages.message("report.onLand"));
            onLandLabel.setForeground(Color.GRAY);
            detailPanel.add(onLandLabel,
                            higConst.rc(row, column * columnsPerColumn + colonyColumn, "l"));
            JLabel countLabel = new JLabel(String.valueOf(unitOnLand[unit]));
            countLabel.setForeground(Color.GRAY);
            detailPanel.add(countLabel,
                            higConst.rc(row, column * columnsPerColumn + countColumn, "r"));
            if (row == heights.length) {
                row = startRow;
                column++;
            } else {
                row++;
            }
        }
        if (unitAtSea[unit] > 0) {
            JLabel atSeaLabel = new JLabel(Messages.message("report.atSea"));
            atSeaLabel.setForeground(Color.GRAY);
            detailPanel.add(atSeaLabel,
                            higConst.rc(row, column * columnsPerColumn + colonyColumn, "l"));
            JLabel countLabel = new JLabel(String.valueOf(unitAtSea[unit]));
            countLabel.setForeground(Color.GRAY);
            detailPanel.add(countLabel, 
                            higConst.rc(row, column * columnsPerColumn + countColumn, "r"));
            if (row == heights.length) {
                row = startRow;
                column++;
            } else {
                row++;
            }
        }
        return detailPanel;
    }

    private JButton createColonyButton(Colony colony, int unit, ReportLabourDetailPanel report) {

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
        button.addActionListener(report);
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
        int action = Integer.parseInt(command);
        if (action == OK) {
            super.actionPerformed(event);
        } else if (action < unitCount.length) {
            ReportLabourDetailPanel details = new ReportLabourDetailPanel(getCanvas());
            JPanel detailPanel = createUnitDetails(action, details);
            details.initialize(detailPanel, action);
            getCanvas().addAsFrame(details);
            details.requestFocus();
        } else {
            logger.warning("Unknown action command " + command);
        }
    }
}
