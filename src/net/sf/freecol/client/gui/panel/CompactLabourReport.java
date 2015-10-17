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

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;


/**
 * This panel displays the Labour Report.
 */
public final class CompactLabourReport extends ReportPanel {

    private final int COLONY_COLUMN = 0;
    private final int UNIT_TYPE_COLUMN = 1;
    private final int WORKING_COLUMN = 2;
    private final int BUILDING_COLUMN = 3;
    private final int COLONIST_COLUMN = 4;
    private final int COLONIST_SUMMARY_COLUMN = 5;
    private final int PRODUCTION_SYMBOL_COLUMN = 6;
    private final int PRODUCTION_COLUMN = 7;
    private final int PRODUCTION_SUMMARY_COLUMN = 8;
    private final int NETPRODUCTION_SUMMARY_COLUMN = 9;

    private static final int COLUMNS = 10;

    private LabourData labourData;

    private final LabourData.UnitData unitData;

    private boolean showProduction;
    private boolean showNetProduction;
    private boolean showProductionSymbols;

    private boolean showBuildings;

    private final JPanel headerRow = new MigPanel("ReportPanelUI");


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public CompactLabourReport(FreeColClient freeColClient) {
        this(freeColClient, null);

        labourData = new LabourData(getFreeColClient());
    }

    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param data The <code>UnitData</code> to display in this report.
     */
    public CompactLabourReport(FreeColClient freeColClient,
                               LabourData.UnitData data) {
        super(freeColClient, ((data == null) ? "reportLabourAction"
                : "report.labour.details"));

        this.unitData = data;

        headerRow.setBorder(new EmptyBorder(20, 20, 0, 20));
        headerRow.setOpaque(true);
        scrollPane.setColumnHeaderView(headerRow);
    }


    @Override
    public JButton createColonyButton(final Colony colony) {
        String text = colony.getName();
        if (!unitData.isSummary()) {
            /*
            int unitIndex = unitData.getUnitType().getIndex();
            
            int skillLevel = Unit.getSkillLevel(unitIndex);
            if (skillLevel <= 0 && skillLevel > -2) {
            //settlers and servants can be trained anywwhere a farmer can
            unitIndex = Unit.EXPERT_FARMER;
            }
            */

            if (colony.canTrain(unitData.getUnitType())) {
                text = text + "*";
            }
        }

        return createButton(text, (ActionEvent ae) -> {
                getGUI().showColonyPanel(colony, null);
        });
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


        String layoutConstraints = "fill, insets 0, gap 0 0";
        StringBuilder widths = new StringBuilder();
        widths.append("[175, fill]");
        widths.append(isOverview() || !unitData.isSummary() ? "[150, fill]" : "[0!]");
        widths.append("[150, fill]");
        widths.append(showBuildings ? "[130, fill]" : "[0!]");
        widths.append("[26, fill][33, fill]");
        widths.append(showProductionSymbols ? "[50, fill]" : "[0!]");
        widths.append(showProduction ? "[30, fill][40, fill]" : "[0!][0!]");
        widths.append(showNetProduction ? "[40, fill]" : "[0!]");
        String columnConstraints = widths.toString();
        String rowConstraints = "[fill]";

        headerRow.setLayout(new MigLayout(layoutConstraints, columnConstraints, rowConstraints));
        reportPanel.setLayout(new MigLayout(layoutConstraints, columnConstraints, rowConstraints));

        addHeader();

        if (isOverview()) {
            addUnitTypes();
        } else {
            addLocations();
        }
    }

    protected Border createBorder() {
        return new EmptyBorder(0, 20, 20, 20);
    }

    /**
     * adds the header rows
     */
    private void addHeader() {

        JLabel empty = new JLabel();
        empty.setBorder(Utility.TOPLEFTCELLBORDER);
        headerRow.add(empty, "cell " + COLONY_COLUMN + " 1");

        if (isOverview() || !unitData.isSummary()) {
            JLabel unitType = Utility.localizedLabel("unitType");
            unitType.setBorder(Utility.TOPCELLBORDER);
            headerRow.add(unitType, "cell " + UNIT_TYPE_COLUMN + " 1");
        }

        JLabel workingAs = Utility.localizedLabel("report.labour.workingAs");
        workingAs.setBorder(Utility.TOPCELLBORDER);
        headerRow.add(workingAs, "cell " + WORKING_COLUMN + " 1");

        if (showBuildings) {
            JLabel building = Utility.localizedLabel("building");
            building.setBorder(Utility.TOPCELLBORDER);
            headerRow.add(building, "cell " + BUILDING_COLUMN + " 1");
        }

        JLabel colonists = Utility.localizedLabel("colonists");
        colonists.setBorder(Utility.TOPCELLBORDER);
        headerRow.add(colonists, "cell " + COLONIST_COLUMN + " 1 2 1");

        if (isOverview()) {
            JLabel production = Utility.localizedLabel("report.labour.production");
            production.setBorder(Utility.TOPCELLBORDER);
            headerRow.add(production, "cell " + PRODUCTION_SYMBOL_COLUMN + " 1 " + (COLUMNS - PRODUCTION_SYMBOL_COLUMN) + " 1");
        } else if (showProduction) {
            LabourData.UnitData unit = unitData;
            GoodsType goods = unit.getExpertProduction();

            ImageLibrary lib = getImageLibrary();
            JLabel production = new JLabel(new ImageIcon(lib.getIconImage(goods)));
            production.setBorder(Utility.TOPCELLBORDER);

            headerRow.add(production, "cell " + PRODUCTION_SYMBOL_COLUMN + " 1 " + (COLUMNS - PRODUCTION_SYMBOL_COLUMN + (showNetProduction && goods.isStoredAs() ? 1 : 0)) + " 1");

            if (showNetProduction && goods.isStoredAs()) {
                JLabel netProduction = new JLabel(new ImageIcon(lib.getIconImage(goods.getStoredAs())));
                netProduction.setBorder(Utility.TOPCELLBORDER);
                headerRow.add(netProduction, "cell " + NETPRODUCTION_SUMMARY_COLUMN + " 1");
            }
        }

        if (!isSummary()) {
            ImageIcon icon = new ImageIcon(
                getImageLibrary().getUnitImage(unitData.getUnitType()));
            header.setIcon(icon);
            header.setIconTextGap(20);
        }
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
        WorkLocation productionWL = null;

        if (!allColonists) {
            workingAs = Messages.message(unitType.getWorkingAsKey());
            if (colony != null) {
                GoodsType expert = unitType.getExpertProduction();
                if (expert != null) {
                    productionWL = colony.getWorkLocationForProducing(expert);
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

        if (showBuildings) {
            if (productionWL != null && row > buildingStartRow) {
                JLabel buildingLabel = Utility.localizedLabel(productionWL.getLabel());
                buildingLabel.setBorder(Utility.CELLBORDER);
                reportPanel.add(buildingLabel, "cell " + BUILDING_COLUMN + " " + buildingStartRow + " 1 " + (row - buildingStartRow));
                buildingStartRow = row;
            } else {
                reportPanel.add(createEmptyLabel(), "cell " + BUILDING_COLUMN + " " + buildingStartRow + " 1 " + (row - buildingStartRow));
            }
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

        WorkLocation school = (colony != null && data.isTraining())
            ? colony.getWorkLocationWithAbility(Ability.TEACH)
            : null;

        if (showBuildings && school != null && row > buildingStartRow) {
            reportPanel.add(createEmptyLabel(), "cell " + BUILDING_COLUMN
                + " " + buildingStartRow + " 1 " + (row - buildingStartRow));
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
                         Messages.message(StringTemplate
                             .template("report.labour.learning")
                             .addName("%unit%", data.getUnitData().getUnitName())),
                         data.getOtherStudents(),
                         0, row);
        }

        int studentCount = data.getStudents();
        if (studentCount > 0) {
            if (allColonists) {
                addRow(data, null, Messages.message("report.labour.sutdent"), createNonCountedLabel(studentCount), 0, row);
            } else {
                Set<UnitType> resultOfTraining = new LinkedHashSet<>();
                if (colony != null) {
                    for (Unit teacher : colony.getTeachers()) {
                        Unit student = teacher.getStudent();
                        if (student != null && student.getType() == unitType) {
                            resultOfTraining.add(Unit.getUnitTypeTeaching(teacher.getType(), student.getType()));
                        }
                    }
                }

                String student = resultOfTraining.size() == 1 ?
                    Messages.message(StringTemplate
                        .template("report.labour.learning")
                        .addName("%unit%", resultOfTraining.iterator().next())) :
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
            JLabel buildingLabel = new JLabel((school == null) ? ""
                : Messages.message(school.getLabel()));
            buildingLabel.setBorder(Utility.CELLBORDER);
            reportPanel.add(buildingLabel, "cell " + BUILDING_COLUMN
                + " " + buildingStartRow
                + " 1 " + (row - buildingStartRow));
        }

        if (data.getUnitData().showProduction() && row > notProducingStartRow) {
            reportPanel.add(createEmptyLabel(), "cell " + PRODUCTION_COLUMN
                + " " + notProducingStartRow
                + " 1 " + (row - notProducingStartRow));
        }

        return row;
    }

    private void addLocations() {
        LabourData.LocationData unitTotal = unitData.getTotal();

        int row = 1;
        JLabel summaryLabel = Utility.localizedLabel("report.labour.summary");
        summaryLabel.setBorder(Utility.LEFTCELLBORDER);
        reportPanel.add(summaryLabel, "cell " + COLONY_COLUMN + " " + row
            + " 1 " + unitTotal.getRowCount());

        row = addLocationData(unitTotal, null, row);

        for (Colony colony : getFreeColClient().getMySortedColonies()) {
            LabourData.LocationData colonyData
                = unitData.getDetails().get(colony);
            if (colonyData != null) {
                reportPanel.add(createColonyButton(colony),
                    "cell " + COLONY_COLUMN + " " + row
                    + " 1 " + colonyData.getRowCount());
                row = addLocationData(colonyData, colony, row);
            }
        }
        LabourData.LocationData europe = unitData.getUnitsInEurope();
        if (europe.getRowCount() > 0) {
            String nam = Messages.getName(getMyPlayer().getEurope());
            JButton button = createButton(nam,
                (ActionEvent ae) -> { getGUI().showEuropePanel(); });
            reportPanel.add(button, "cell " + COLONY_COLUMN + " " + row
                + " 1 " + europe.getRowCount());
            row = addLocationData(europe, null, row);
        }
        row = addNonLinkedLocation(unitData.getUnitsOnLand(),
                                   "report.labour.onLand", row);
        row = addNonLinkedLocation(unitData.getUnitsAtSea(),
                                   "report.labour.atSea", row);

        reportPanel.add(Utility.localizedLabel("report.labour.canTrain"),
                        "cell 1 " + row + " " + COLUMNS + " 1");
    }

    /*
     * distributes {@code value}, amount the number of {@code pocketCount}
     *
     * @param value
     * @param pocketCount
     * @return distribution of {@code value}
     */
    /*
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
     */

    private void addLocationSummary(LabourData.LocationData data, int row) {
        int rows = data.getRowCount();

        JLabel colonistsLabel = createNumberLabel(data.getTotalColonists(), null);
        if (data.getUnitData().isSummary()) {
            if (isOverview()) {
                reportPanel.add(createEmptyLabel(), "cell " + UNIT_TYPE_COLUMN + " " + row + " 1 " + rows);
            }
        } else {
            Utility.localizeToolTip(colonistsLabel, StringTemplate
                .template("report.labour.unitTotal.tooltip")
                .addName("%unit%", data.getUnitData().getUnitName()));
        }

        reportPanel.add(colonistsLabel, "cell " + COLONIST_SUMMARY_COLUMN + " " + row + " 1 " + data.getRowCount());

        if (showProduction && !data.getUnitData().showProduction()) {
            reportPanel.add(createEmptyLabel(), "cell " + PRODUCTION_SYMBOL_COLUMN + " " + row + " 4 " + rows);
            return;
        }

        if (showProduction) {
            JLabel productionLabel = createNumberLabel(data.getTotalProduction(), "report.labour.potentialProduction.tooltip");
            if (!data.isTotal() && data.getTotalProduction() == 0) {
                productionLabel.setText("");
            }

            reportPanel.add(productionLabel, "cell " + PRODUCTION_SUMMARY_COLUMN + " " + row + " 1 " + rows);
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
            reportPanel.add(netProductionLabel, "cell " + NETPRODUCTION_SUMMARY_COLUMN + " " + row + " 1 " + rows);
        }

        if (showProductionSymbols) {
            JLabel icon = new JLabel();
            icon.setBorder(Utility.CELLBORDER);
            GoodsType goods = data.getUnitData().getExpertProduction();
            if (goods != null) {
                icon.setIcon(new ImageIcon(getImageLibrary().getIconImage(goods)));
            }
            reportPanel.add(icon, "cell " + PRODUCTION_SYMBOL_COLUMN + " " + row + " 1 " + rows);
        }
    }

    private int addNonLinkedLocation(LabourData.LocationData data, String messageKey, int row) {
        int rows = data.getRowCount();
        if (rows > 0) {
            JLabel label = Utility.localizedLabel(messageKey);
            label.setBorder(Utility.LEFTCELLBORDER);
            label.setForeground(Color.GRAY);
            reportPanel.add(label, "cell " + COLONY_COLUMN + " " + row + " 1 " + rows);
            return addLocationData(data, null, row);
        }
        return row;
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
            typeLabel.setBorder(Utility.CELLBORDER);
            reportPanel.add(typeLabel, "cell " + UNIT_TYPE_COLUMN + " " + row);
        }

        JLabel activityLabel = new JLabel(activity);
        activityLabel.setBorder(Utility.CELLBORDER);
        reportPanel.add(activityLabel, "cell " + WORKING_COLUMN + " " + row);

        reportPanel.add(colonistLabel, "cell " + COLONIST_COLUMN + " " + row);

        if (data.getUnitData().showProduction() && production > 0) {
            reportPanel.add(createNumberLabel(production, "report.labour.potentialProduction.tooltip"), "cell " + PRODUCTION_COLUMN + " " + row);
        }
    }

    private void addUnitTypes() {
        int row = 1;

        JButton allColonistsButton = createUnitNameButton(Messages.message("report.labour.allColonists"), labourData.getSummary());
        reportPanel.add(allColonistsButton, "cell " + COLONY_COLUMN + " " + row + " 1 " + labourData.getSummary().getUnitSummaryRowCount());

        row = addLocationData(labourData.getSummary().getTotal(), null, row);

        for (UnitType unitType : LabourData.getLabourTypes(getMyPlayer())) {
            LabourData.UnitData unitData = labourData.getUnitData(unitType);

            JButton unitButton = createUnitNameButton(unitData.getUnitName(), unitData);
            int rows = unitData.getUnitSummaryRowCount();
            reportPanel.add(unitButton, "cell " + COLONY_COLUMN + " " + row + " 1 " + rows);

            if (unitData.hasDetails()) {
                row = addLocationData(unitData.getTotal(), null, row);
            } else {
                unitButton.setEnabled(false);
                unitButton.setDisabledIcon(unitButton.getIcon());
                unitButton.setForeground(Color.GRAY);

                reportPanel.add(createEmptyLabel(), "cell " + UNIT_TYPE_COLUMN + " " + row + " " + (COLUMNS - 1) + " 1");
                row++;
            }
        }
    }

    private JButton createButton(String name, ActionListener listener) {
        JButton button = new JButton(name);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setOpaque(false);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.setForeground(Utility.LINK_COLOR);
        button.setBorder(Utility.LEFTCELLBORDER);
        button.addActionListener(listener);
        return button;
    }

    private JLabel createEmptyLabel() {
        JLabel empty = new JLabel();
        empty.setBorder(Utility.CELLBORDER);
        return empty;
    }

    private JLabel createNonCountedLabel(int otherAmateurs) {
        JLabel label = createNumberLabel(otherAmateurs,
                                         "report.labour.notCounted.tooltip");
        label.setForeground(Color.GRAY);
        return label;
    }

    private JLabel createNumberLabel(int number, String toolTipKey) {
        JLabel label = new JLabel(String.valueOf(number));
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        label.setBorder(Utility.CELLBORDER);
        if (toolTipKey != null) Utility.localizeToolTip(this, toolTipKey);
        return label;
    }

    private JButton createUnitNameButton(String name,
                                         LabourData.UnitData unitData) {
        JButton button = createButton(name, (ActionEvent ae) -> {
                getGUI().showCompactLabourReport(unitData);
            });

        if (!unitData.isSummary()) {
            button.setIcon(new ImageIcon(
                getImageLibrary().getUnitImage(unitData.getUnitType())));
        }

        return button;
    }

    private GoodsType getGoodsType() {
        return (isSummary()) ? null
            : unitData.getUnitType().getExpertProduction();
    }

    /**
     * Is this an overview/location summary?
     *
     * @return True if this is the location summary.
     */
    private boolean isOverview() {
        return unitData == null;
    }

    /**
     * Is this a summary?
     *
     * @return True if this is any sort of summary.
     */
    private boolean isSummary() {
        return isOverview() || unitData.isSummary();
    }
}
