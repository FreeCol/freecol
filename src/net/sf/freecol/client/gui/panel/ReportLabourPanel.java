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

import cz.autel.dmi.HIGLayout;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel implements ActionListener {

    private static final int HEADER_ROWS = 1;

    private int COLONY_COLUMN = 1;
    private int UNIT_TYPE_COLUMN = 2;
    private int WORKING_COLUMN = 3;
    private int BUILDING_COLUMN = 4;
    private int COLONIST_COLUMN = 5;
    private int COLONIST_SUMMARY_COLUMN = 6;
    private int PRODUCTION_SYMBOL_COLUMN = 7;
    private int PRODUCTION_COLUMN = 8;
    private int PRODUCTION_SUMMARY_COLUMN = 9;
    private int NETPRODUCTION_SUMMARY_COLUMN = 10;

    private static final int COLUMNS = 10;

    private LabourData labourData;

    private LabourData.UnitData unitData;

    private Player player;

    private boolean showProduction;
    private boolean showNetProduction;
    private boolean showProductionSymbols;

    private boolean showBuildings;

    private final JPanel headerRow = new JPanel();

    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public ReportLabourPanel(Canvas parent) {
        this(parent, null);

        this.labourData = new LabourData(getCanvas().getClient());
    }

    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    private ReportLabourPanel(Canvas parent, LabourData.UnitData data) {
        super(parent, data == null ? Messages.message("report.labour") : Messages.message("report.labour.details"));
        this.unitData = data;
        player = parent.getClient().getMyPlayer();

        headerRow.setBorder(new EmptyBorder(20, 20, 0, 20));
        scrollPane.setColumnHeaderView(headerRow);
    }

    @Override
    protected Border createBorder() {
        return new EmptyBorder(0, 20, 20, 20);
    }


    /**
     * @return if this is the location summary, grouped by unit type
     */
    private boolean isOverview() {
        return unitData == null;
    }

    /**
     * @return if we are any summary
     */
    private boolean isSummary() {
        return isOverview() || unitData.isSummary();
    }

    private ImageIcon getUnitIcon(UnitType unit) {
        Unit.Role role = Unit.Role.DEFAULT;
        if (unit.hasAbility("model.ability.expertPioneer")) {
            role = Unit.Role.PIONEER;
        } else if (unit.hasAbility("model.ability.expertMissionary")) {
            role = Unit.Role.MISSIONARY;
        }

        return getLibrary().getUnitImageIcon(unit, role);
    }

    @Override
    public void initialize() {
        if (isOverview()) {
            showProduction = true;
            showNetProduction = true;
            showProductionSymbols = true;
            showBuildings = false;
        } else {
            showProduction = unitData.showProduction();
            showNetProduction = unitData.showNetProduction();
            showProductionSymbols = false;

            GoodsType expertProduction = getGoodsType();
            showBuildings = (expertProduction != null && !expertProduction.isFarmed()) || unitData.getTotal().isTraining();
        }


        int[] widths = new int[COLUMNS];
        widths[COLONY_COLUMN - 1] = 160;
        widths[UNIT_TYPE_COLUMN - 1] = isOverview() || !unitData.isSummary() ? 150 : 0;
        widths[WORKING_COLUMN - 1] = 170;
        widths[BUILDING_COLUMN - 1] = showBuildings ? 130 : 0;
        widths[COLONIST_COLUMN - 1] = 26;
        widths[COLONIST_SUMMARY_COLUMN - 1] = 33;
        widths[PRODUCTION_COLUMN - 1] = showProduction ? 30 : 0;
        widths[PRODUCTION_SUMMARY_COLUMN - 1] = showProduction ? 40 : 0;
        widths[NETPRODUCTION_SUMMARY_COLUMN - 1] = showNetProduction ? 40 : 0;
        widths[PRODUCTION_SYMBOL_COLUMN - 1] = showProductionSymbols ? 50 : 0;

        headerRow.setLayout(new HIGLayout(widths, new int[HEADER_ROWS]));

        addHeader();

        if (isOverview()) {
            int rows = labourData.getSummary().getTotal().getRowCount();

            for (UnitType unitType : LabourData.getLabourTypes(player)) {
                rows += labourData.getUnitData(unitType).getUnitSummaryRowCount();
            }

            reportPanel.setLayout(new HIGLayout(widths, new int[rows]));

            addUnitTypes();
        } else {
            int footerRows = 1;
            int rows =
                footerRows +
                    unitData.getTotal().getRowCount() +
                    unitData.getUnitsInEurope().getRowCount() +
                    unitData.getUnitsAtSea().getRowCount() +
                    unitData.getUnitsOnLand().getRowCount();

            for (LabourData.LocationData colonyData : unitData.getDetails().values()) {
                rows += colonyData.getRowCount();
            }
            reportPanel.setLayout(new HIGLayout(widths, new int[rows]));

            addLocations();
        }
    }

    private void addUnitTypes() {
        int row = 1;

        JButton allColonistsButton = createUnitNameButton(Messages.message("report.labour.allColonists"), labourData.getSummary());
        reportPanel.add(allColonistsButton,
                        higConst.rcwh(row, COLONY_COLUMN, 1, labourData.getSummary().getUnitSummaryRowCount()));

        row = addLocationData(labourData.getSummary().getTotal(), null, row);

        int minHeight = allColonistsButton.getPreferredSize().height;
        HIGLayout higLayout = (HIGLayout) reportPanel.getLayout();

        for (UnitType unitType : LabourData.getLabourTypes(player)) {
            LabourData.UnitData unitData = labourData.getUnitData(unitType);

            JButton unitButton = createUnitNameButton(unitData.getUnitName(), unitData);
            int rows = unitData.getUnitSummaryRowCount();
            ensureHeight(higLayout, unitButton.getPreferredSize().height, row, rows, minHeight);
            reportPanel.add(unitButton, higConst.rcwh(row, COLONY_COLUMN, 1, rows));

            if (unitData.hasDetails()) {
                row = addLocationData(unitData.getTotal(), null, row);
            } else {
                unitButton.setEnabled(false);
                unitButton.setDisabledIcon(unitButton.getIcon());
                unitButton.setForeground(Color.GRAY);

                reportPanel.add(createEmptyLabel(), higConst.rcwh(row, UNIT_TYPE_COLUMN, COLUMNS - 1, 1));
                row++;
            }
        }
    }

    /**
     * ensures that {@code rows} rows, starting from {@code startRow}, are at least {@code height} heigh
     *
     * @param higLayout
     * @param height    the height we need
     * @param startRow  from this row on
     * @param rows      number of rows where the height has to be met
     * @param minHeight minimum height for each row
     */
    private void ensureHeight(HIGLayout higLayout, int height, int startRow, int rows, int minHeight) {
        for (int delta : distribute(height, rows)) {
            higLayout.setRowHeight(startRow, Math.max(delta, minHeight));
            startRow++;
        }
    }

    /**
     * distributes {@code value}, amount the number of {@code pocketCount}
     *
     * @param value
     * @param pocketCount
     * @return
     */
    private int[] distribute(int value, int pocketCount) {
        int[] pockets = new int[pocketCount];

        int pocketIndex = 0;
        for (int i = value; i > 0; i--) {
            pockets[pocketIndex]++;

            pocketIndex++;
            pocketIndex = pocketIndex % pocketCount;
        }
        return pockets;
    }

    private JLabel createEmptyLabel() {
        JLabel empty = new JLabel("");
        empty.setBorder(CELLBORDER);
        return empty;
    }

    private void addLocations() {
        LabourData.LocationData unitTotal = unitData.getTotal();

        int row = 1;
        JLabel summaryLabel = new JLabel(Messages.message("report.labour.summary"));
        summaryLabel.setBorder(LEFTCELLBORDER);
        reportPanel.add(summaryLabel, higConst.rcwh(row, COLONY_COLUMN, 1, unitTotal.getRowCount()));

        row = addLocationData(unitTotal, null, row);

        List<Colony> colonies = player.getColonies();

        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        for (Colony colony : colonies) {
            LabourData.LocationData colonyData = unitData.getDetails().get(colony);
            if (colonyData != null) {
                reportPanel.add(createColonyButton(colony),
                                higConst.rcwh(row, COLONY_COLUMN, 1, colonyData.getRowCount()));
                row = addLocationData(colonyData, colony, row);
            }
        }
        LabourData.LocationData europe = unitData.getUnitsInEurope();
        if (europe.getRowCount() > 0) {
            JButton button = createButton(player.getEurope().getName(), new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getCanvas().showEuropePanel();
                }
            });
            reportPanel.add(button, higConst.rcwh(row, COLONY_COLUMN, 1, europe.getRowCount()));
            row = addLocationData(europe, null, row);
        }
        row = addNonLinkedLocation(unitData.getUnitsOnLand(), "report.onLand", row);
        row = addNonLinkedLocation(unitData.getUnitsAtSea(), "report.atSea", row);

        reportPanel.add(new JLabel(Messages.message("report.labour.canTrain")), higConst.rcwh(row, 1, COLUMNS, 1));
    }

    private GoodsType getGoodsType() {
        return isSummary() ? null : unitData.getUnitType().getExpertProduction();
    }

    /**
     * adds the header rows
     *
     * @return next row index
     */
    private void addHeader() {
        int row = 1;

        JLabel empty = new JLabel("");
        empty.setBorder(TOPLEFTCELLBORDER);
        headerRow.add(empty, higConst.rc(row, COLONY_COLUMN));

        if (isOverview() || !unitData.isSummary()) {
            JLabel unitType = new JLabel(Messages.message("model.unit.type"));
            unitType.setBorder(TOPCELLBORDER);
            headerRow.add(unitType, higConst.rc(row, UNIT_TYPE_COLUMN));
        }

        JLabel workingAs = new JLabel(Messages.message("model.unit.workingAs"));
        workingAs.setBorder(TOPCELLBORDER);
        headerRow.add(workingAs, higConst.rc(row, WORKING_COLUMN));

        if (showBuildings) {
            JLabel building = new JLabel(Messages.message("building"));
            building.setBorder(TOPCELLBORDER);
            headerRow.add(building, higConst.rc(row, BUILDING_COLUMN));
        }

        JLabel colonists = new JLabel(Messages.message("colonists"));
        colonists.setBorder(TOPCELLBORDER);
        headerRow.add(colonists, higConst.rcwh(row, COLONIST_COLUMN, 2, 1));

        if (isOverview()) {
            JLabel production = new JLabel(Messages.message("report.production"));
            production.setBorder(TOPCELLBORDER);
            headerRow.add(production, higConst.rcwh(row, PRODUCTION_SYMBOL_COLUMN, 4, 1));
        } else if (showProduction) {
            LabourData.UnitData unit = unitData;
            GoodsType goods = unit.getExpertProduction();

            ImageLibrary imageLibrary = getCanvas().getGUI().getImageLibrary();
            JLabel production = new JLabel(imageLibrary.getGoodsImageIcon(goods));
            production.setBorder(TOPCELLBORDER);

            GoodsType storedAs = goods.getStoredAs();

            headerRow.add(production, higConst.rcwh(row, PRODUCTION_SYMBOL_COLUMN, storedAs == null ? 4 : 3, 1));

            if (showNetProduction && storedAs != null) {
                JLabel netProduction = new JLabel(imageLibrary.getGoodsImageIcon(storedAs));
                netProduction.setBorder(TOPCELLBORDER);
                headerRow.add(netProduction, higConst.rc(row, NETPRODUCTION_SUMMARY_COLUMN));
            }
        }

        if (!isSummary()) {
            ImageIcon icon = getUnitIcon(unitData.getUnitType());
            header.setIcon(icon);
            header.setIconTextGap(20);
        }
    }

    private int addNonLinkedLocation(LabourData.LocationData data, String messageKey, int row) {
        int rows = data.getRowCount();
        if (rows > 0) {
            JLabel label = new JLabel(Messages.message(messageKey));
            label.setBorder(LEFTCELLBORDER);
            label.setForeground(Color.GRAY);
            reportPanel.add(label, higConst.rcwh(row, COLONY_COLUMN, 1, rows));
            return addLocationData(data, null, row);
        }
        return row;
    }

    /**
     * add unit data for a given location
     *
     * @param data
     * @param row  starting row
     * @return next row to use
     */
    private int addLocationData(LabourData.LocationData data, Colony colony, int row) {
        boolean allColonists = data.getUnitData().isSummary();

        LabourData.UnitData unit = data.getUnitData();
        UnitType unitType = unit.getUnitType();
        String unitName = unit.getUnitName();

        String workingAs = null;
        Building productionBuilding = null;

        if (!allColonists) {
            workingAs = Messages.message(unitType.getId() + ".workingAs");
            if (colony != null) {
                GoodsType expert = unitType.getExpertProduction();
                if (expert != null) {
                    productionBuilding = colony.getBuildingForProducing(expert);
                }
            }
        }
        addLocationSummary(data, row);

        int buildingStartRow = row;

        //details

        int otherAmateurs = data.getOtherWorkingAmateurs().getColonists();
        if (!allColonists && otherAmateurs > 0) {
            addRow(data,
                   Messages.message("report.labour.otherUnitType"),
                   workingAs,
                   createNonCountedLabel(otherAmateurs),
                   data.getOtherWorkingAmateurs().getProduction(),
                   row);
            row++;
        }

        row = addRow(data,
                     unitName,
                     allColonists ? Messages.message("report.labour.expertsWorking") : workingAs,
                     data.getWorkingProfessionals().getColonists(),
                     data.getWorkingProfessionals().getProduction(),
                     row);

        int notProducingStartRow = row;

        if (showBuildings && productionBuilding != null && row > buildingStartRow) {
            JLabel buildingLabel = new JLabel(productionBuilding.getName());
            buildingLabel.setBorder(CELLBORDER);
            reportPanel.add(buildingLabel, higConst.rcwh(buildingStartRow, BUILDING_COLUMN, 1, row - buildingStartRow));
            buildingStartRow = row;
        }

        row = addRow(data,
                     unitName,
                     Messages.message(allColonists ?
                         "report.labour.amateursWorking" :
                         "report.labour.workingAsOther"),
                     data.getWorkingAmateurs(),
                     0, row);
        if (data.getNotWorking() > 0) {
            addRow(data,
                   unitName,
                   Messages.message("report.labour.notWorking"),
                   createNumberLabel(data.getNotWorking(), "report.labour.notWorking.tooltip"),
                   0, row);
            row++;
        }

        Building schoolhouse = colony != null && data.isTraining() ?
            colony.getBuildingWithAbility("model.ability.teach") : null;

        if (showBuildings && schoolhouse != null && row > buildingStartRow) {
            reportPanel.add(createEmptyLabel(),
                            higConst.rcwh(buildingStartRow, BUILDING_COLUMN, 1, row - buildingStartRow));
            buildingStartRow = row;
        }

        row = addRow(data,
                     unitName,
                     Messages.message("report.labour.teacher"),
                     data.getTeachers(),
                     0, row);
        if (!allColonists) {
            row = addRow(data,
                         data.getOtherStudentsName(),
                         Messages.message("report.labour.learning",
                                          "%unit%",
                                          data.getUnitData().getUnitName()),
                         data.getOtherStudents(),
                         0, row);
        }

        int studentCount = data.getStudents();
        if (studentCount > 0) {
            if (allColonists) {
                addRow(data, null, Messages.message("report.labour.sutdent"), createNonCountedLabel(studentCount), 0, row);
            } else {
                Set<UnitType> resultOfTraining = new LinkedHashSet<UnitType>();
                for (Unit teacher : colony.getTeachers()) {
                    Unit student = teacher.getStudent();
                    if (student != null && student.getType() == unitType) {
                        resultOfTraining.add(Unit.getUnitTypeTeaching(teacher.getType(), student.getType()));
                    }
                }

                String student = resultOfTraining.size() == 1 ?
                    Messages.message("report.labour.learning",
                                     "%unit%",
                                     resultOfTraining.iterator().next().getName()) :
                    Messages.message("report.labour.learningOther");
                addRow(data,
                       data.getUnitData().getUnitName(),
                       student,
                       createNumberLabel(-studentCount, "report.labour.subtracted.tooltip"),
                       0, row);
            }
            row++;
        }

        if (showBuildings && row > buildingStartRow) {
            JLabel buildingLabel = new JLabel(schoolhouse != null ? schoolhouse.getName() : "");
            buildingLabel.setBorder(CELLBORDER);
            reportPanel.add(buildingLabel, higConst.rcwh(buildingStartRow, BUILDING_COLUMN, 1, row - buildingStartRow));
        }

        if (data.getUnitData().showProduction() && row > notProducingStartRow) {
            reportPanel.add(createEmptyLabel(), higConst.rcwh(notProducingStartRow, PRODUCTION_COLUMN, 1, row - notProducingStartRow));
        }

        return row;
    }

    private void addLocationSummary(LabourData.LocationData data, int row) {
        int rows = data.getRowCount();

        JLabel colonistsLabel = createNumberLabel(data.getTotalColonists(), null);
        if (data.getUnitData().isSummary()) {
            if (isOverview()) {
                reportPanel.add(createEmptyLabel(), higConst.rcwh(row, UNIT_TYPE_COLUMN, 1, rows));
            }
        } else {
            colonistsLabel.setToolTipText(Messages.message("report.labour.unitTotal.tooltip", "%unit%", data.getUnitData().getUnitName()));
        }

        reportPanel.add(colonistsLabel, higConst.rcwh(row, COLONIST_SUMMARY_COLUMN, 1, data.getRowCount()));

        if (showProduction && !data.getUnitData().showProduction()) {
            reportPanel.add(createEmptyLabel(), higConst.rcwh(row, PRODUCTION_SYMBOL_COLUMN, 4, rows));
            return;
        }

        if (showProduction) {
            JLabel productionLabel = createNumberLabel(data.getTotalProduction(), "report.labour.potentialProduction.tooltip");
            if (!data.isTotal() && data.getTotalProduction() == 0) {
                productionLabel.setText("");
            }

            reportPanel.add(productionLabel, higConst.rcwh(row, PRODUCTION_SUMMARY_COLUMN, 1, rows));
        }

        if (showNetProduction) {
            int net = data.getNetProduction();
            JLabel netProductionLabel = createNumberLabel(net, "report.labour.netProduction.tooltip");

            if (!data.getUnitData().showNetProduction() || (!data.isTotal() && net == 0)) {
                netProductionLabel.setText("");
                netProductionLabel.setToolTipText("");
            } else if (net >= 0) {
                netProductionLabel.setText("+" + net);
            } else {
                netProductionLabel.setForeground(Color.RED);
            }
            reportPanel.add(netProductionLabel, higConst.rcwh(row, NETPRODUCTION_SUMMARY_COLUMN, 1, rows));
        }

        if (showProductionSymbols) {
            JLabel icon = new JLabel();
            icon.setBorder(CELLBORDER);
            GoodsType goods = data.getUnitData().getExpertProduction();
            if (goods != null) {
                ImageLibrary imageLibrary = getCanvas().getGUI().getImageLibrary();
                icon.setIcon(imageLibrary.getGoodsImageIcon(goods));
            }
            reportPanel.add(icon, higConst.rcwh(row, PRODUCTION_SYMBOL_COLUMN, 1, rows));
        }
    }

    private JLabel createNonCountedLabel(int otherAmateurs) {
        JLabel label = createNumberLabel(otherAmateurs, "report.labour.notCounted.tooltip");
        label.setForeground(Color.GRAY);
        return label;
    }

    private JLabel createNumberLabel(int number, String toolTipKey) {
        JLabel label = new JLabel(String.valueOf(number));
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        label.setBorder(CELLBORDER);
        if (toolTipKey != null) {
            label.setToolTipText(Messages.message(toolTipKey));
        }
        return label;
    }

    private int addRow(LabourData.LocationData data, String typeName, String activity, int colonists, int production, int row) {
        if (colonists > 0) {
            addRow(data, typeName, activity, createNumberLabel(colonists, null), production, row);
            row++;
        }
        return row;
    }

    private void addRow(LabourData.LocationData data, String typeName, String activity, JLabel colonistLabel, int production, int row) {
        if (!data.getUnitData().isSummary()) {
            JLabel typeLabel = new JLabel(typeName);
            typeLabel.setBorder(CELLBORDER);
            reportPanel.add(typeLabel, higConst.rc(row, UNIT_TYPE_COLUMN));
        }

        JLabel activityLabel = new JLabel(activity);
        activityLabel.setBorder(CELLBORDER);
        reportPanel.add(activityLabel, higConst.rc(row, WORKING_COLUMN));

        reportPanel.add(colonistLabel, higConst.rc(row, COLONIST_COLUMN));

        if (data.getUnitData().showProduction() && production > 0) {
            reportPanel.add(createNumberLabel(production, "report.labour.potentialProduction.tooltip"),
                            higConst.rc(row, PRODUCTION_COLUMN));
        }
    }

    private JButton createColonyButton(final Colony colony) {
        String text = colony.getName();
        if (!unitData.isSummary()) {
//            int unitIndex = unitData.getUnitType().getIndex();
//
//            int skillLevel = Unit.getSkillLevel(unitIndex);
//            if (skillLevel <= 0 && skillLevel > -2) {
//                //settlers and servants can be trained anywwhere a farmer can
//                unitIndex = Unit.EXPERT_FARMER;
//            }

            if (colony.canTrain(unitData.getUnitType())) {
                text = text + "*";
            }
        }

        return createButton(text, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getCanvas().showColonyPanel(colony);
            }
        });
    }

    private JButton createUnitNameButton(String name, final LabourData.UnitData unitData) {
        JButton button = createButton(name, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ReportLabourPanel details = new ReportLabourPanel(getCanvas(), unitData);
                details.initialize();
                getCanvas().addAsFrame(details);
                details.requestFocus();
            }
        });

        if (!unitData.isSummary()) {
            button.setIcon(getUnitIcon(unitData.getUnitType()));
        }

        return button;
    }

    private JButton createButton(String name, ActionListener listener) {
        JButton button = new JButton(name);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setOpaque(false);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.setForeground(LINK_COLOR);
        button.setBorder(LEFTCELLBORDER);
        button.addActionListener(listener);
        return button;
    }


}
