package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.sf.freecol.FreeCol;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
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
        int numberUnits = FreeCol.getSpecification().numberOfUnitTypes();
        unitCount = new int[numberUnits];
        unitAtSea = new int[numberUnits];
        unitOnLand = new int[numberUnits];
        unitInEurope = new int[numberUnits];
        unitLocations = new Vector<HashMap<Colony,Integer>>(numberUnits);
        for (int index = 0; index < numberUnits; index++) {
            unitLocations.add(new HashMap<Colony, Integer>());
        }

        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        Iterator<Unit> units = player.getUnitIterator();
        while (units.hasNext()) {
            Unit unit = units.next();
            int type = unit.getUnitType().getIndex();
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

        List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypeList();
        ArrayList<UnitType> colonists = new ArrayList<UnitType>();
        for (UnitType unitType : unitTypes) {
            if (unitType.hasSkill()) {
                colonists.add(unitType);
            }
        }
        int lines = (int) Math.ceil(colonists.size() / 3.0);
        int[] heights = new int[2 * lines - 1];
        for (int index = 1; index < lines; index += 2) {
            heights[index] = 12;
        }

        reportPanel.setLayout(new HIGLayout(widths, heights));
        
        int row = 0, column = 0;
        for (UnitType unitType : colonists) {
            int tools = 0;
            if (unitType.hasAbility("model.ability.expertPioneer")) {
                tools = 20;
            }
            
            reportPanel.add(createUnitLabel(unitType, tools),
                            higConst.rc(2 * row + 1, columnsPerUnit * column + buttonColumn, "t"));
            if (unitCount[unitType.getIndex()] > 0) {
                reportPanel.add(createUnitNameButton(unitType),
                                higConst.rc(2 * row + 1, columnsPerUnit * column + nameColumn, "tl"));
                reportPanel.add(new JLabel(String.valueOf(unitCount[unitType.getIndex()])),
                                higConst.rc(2 * row + 1, columnsPerUnit * column + countColumn, "tr"));
            } else {
                JLabel unitNameLabel = new JLabel(unitType.getName()); 
                unitNameLabel.setForeground(Color.GRAY);
                reportPanel.add(unitNameLabel,
                                higConst.rc(2 * row + 1, columnsPerUnit * column + nameColumn, "tl"));
            }
            
            column++;
            if (column == 3) {
                column = 0;
                row++;
            }
        }
    }
    

    private JButton createUnitNameButton(UnitType unitType) {
        JButton button = new JButton(unitType.getName());
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(String.valueOf(unitType.getIndex()));
        button.addActionListener(this);
        return button;

    }

    private JLabel createUnitLabel(UnitType unitType, int tools) {
        int imageType = ImageLibrary.getUnitGraphicsType(unitType.getIndex(), false, false, tools, false);
        JLabel unitLabel = new JLabel(getLibrary().getUnitImageIcon(imageType));
        return unitLabel;
    }

    private JPanel createUnitDetails(UnitType unitType, ReportLabourDetailPanel report) {
        int unitIndex = unitType.getIndex();
        int maxColumns = 3;
        int columnsPerColumn = 4;
        int[] widths = new int[maxColumns * columnsPerColumn - 1];
        for (int index = 1; index < widths.length; index += 2) {
            widths[index] = 12;
        }
        int keys = 0;
        if (unitCount[unitIndex] > 0) {
            if (unitLocations != null) {
                keys = unitLocations.get(unitIndex).size();
            }
            if (unitInEurope[unitIndex] > 0) {
                keys++;
            }
            if (unitAtSea[unitIndex] > 0) {
                keys++;
            }
            if (unitOnLand[unitIndex] > 0) {
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
        detailPanel.add(new JLabel(unitType.getName()),
                        higConst.rc(row, colonyColumn));
        detailPanel.add(new JLabel(String.valueOf(unitCount[unitIndex])),
                        higConst.rc(row, countColumn));

        row = startRow;
        int column = 0;
        for (Colony colony : colonies) {
            if (unitLocations.get(unitIndex).get(colony) != null) {
                detailPanel.add(createColonyButton(colony, unitType, report),
                                higConst.rc(row, column * columnsPerColumn + colonyColumn, "l"));
                JLabel countLabel = new JLabel(unitLocations.get(unitIndex).get(colony).toString());
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
        if (unitInEurope[unitIndex] > 0) {
            JButton button = new JButton(player.getEurope().getName());
            button.setMargin(new Insets(0,0,0,0));
            button.setOpaque(false);
            button.setForeground(LINK_COLOR);
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setActionCommand(player.getEurope().getName());
            button.addActionListener(this);
            detailPanel.add(button,
                            higConst.rc(row, column * columnsPerColumn + colonyColumn, "l"));
            JLabel countLabel = new JLabel(String.valueOf(unitInEurope[unitIndex]));
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
        if (unitOnLand[unitIndex] > 0) {
            JLabel onLandLabel = new JLabel(Messages.message("report.onLand"));
            onLandLabel.setForeground(Color.GRAY);
            detailPanel.add(onLandLabel,
                            higConst.rc(row, column * columnsPerColumn + colonyColumn, "l"));
            JLabel countLabel = new JLabel(String.valueOf(unitOnLand[unitIndex]));
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
        if (unitAtSea[unitIndex] > 0) {
            JLabel atSeaLabel = new JLabel(Messages.message("report.atSea"));
            atSeaLabel.setForeground(Color.GRAY);
            detailPanel.add(atSeaLabel,
                            higConst.rc(row, column * columnsPerColumn + colonyColumn, "l"));
            JLabel countLabel = new JLabel(String.valueOf(unitAtSea[unitIndex]));
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

    private JButton createColonyButton(Colony colony, UnitType unit, ReportLabourDetailPanel report) {

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
     * This function analyzes an event and calls the right methods to take care
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
            UnitType unitType = FreeCol.getSpecification().getUnitType(action);
            JPanel detailPanel = createUnitDetails(unitType, details);
            details.initialize(detailPanel, unitType);
            getCanvas().addAsFrame(details);
            details.requestFocus();
        } else {
            logger.warning("Unknown action command " + command);
        }
    }
}
