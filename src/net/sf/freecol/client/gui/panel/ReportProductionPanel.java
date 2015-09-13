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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.TypeCountMap;


/**
 * This panel displays the ContinentalCongress Report.
 */
public final class ReportProductionPanel extends ReportPanel {

    /** The number of selection boxes. */
    private static final int NUMBER_OF_GOODS = 4;

    /** The goods types available for selection. */
    private final List<GoodsType> goodsTypes;

    /** The boxes with which to select goods types for display. */
    private final List<JComboBox<String>> boxes = new ArrayList<>();


    /**
     * The constructor that will add the items to this panel.
     *
     * FIXME: can we extend this to cover farmed goods?
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportProductionPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportProductionAction");

        this.goodsTypes = new ArrayList<>();

        List<String> goodsNames = new ArrayList<>();
        goodsNames.add(Messages.message("nothing"));
        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            if (!goodsType.isFarmed()) {
                this.goodsTypes.add(goodsType);
                goodsNames.add(Messages.getName(goodsType));
            }
        }
        String[] model = goodsNames.toArray(new String[0]);
        for (int index = 0; index < NUMBER_OF_GOODS; index++) {
            JComboBox<String> newBox = new JComboBox<>(model);
            newBox.setSelectedIndex(0);
            this.boxes.add(newBox);
        }

        reportPanel.setLayout(new MigLayout("gap 0 0", "[fill]", "[fill]"));
        update();
    }


    private void update() {
        reportPanel.removeAll();
        JLabel selectLabel = Utility.localizedLabel("report.production.selectGoods");
        reportPanel.add(selectLabel);

        JButton selectButton = Utility.localizedButton("report.production.update");
        selectButton.addActionListener((ActionEvent ae) -> {
                update();
            });
        reportPanel.add(selectButton, "wrap");

        List<GoodsType> selectedTypes = new ArrayList<>();
        for (int index = 0; index < NUMBER_OF_GOODS; index++) {
            JComboBox<String> box = this.boxes.get(index);
            reportPanel.add(box);
            int selectedIndex = box.getSelectedIndex();
            if (selectedIndex > 0) {
                selectedTypes.add(this.goodsTypes.get(selectedIndex - 1));
            }
        }
        if (!selectedTypes.isEmpty()) {
            TypeCountMap<BuildingType> buildingCount
                = new TypeCountMap<>();
            List<List<BuildingType>> basicBuildingTypes = new ArrayList<>();
            for (GoodsType goodsType : selectedTypes) {
                List<BuildingType> buildingTypes = new ArrayList<>();
                for (BuildingType buildingType
                         : getSpecification().getBuildingTypeList()) {
                    if (goodsType.equals(buildingType.getProducedGoodsType())
                        || buildingType.hasModifier(goodsType.getId())) {
                        BuildingType firstLevel = buildingType.getFirstLevel();
                        if (!buildingTypes.contains(firstLevel)) {
                            buildingTypes.add(firstLevel);
                        }
                    }
                }
                basicBuildingTypes.add(buildingTypes);
            }

            // labels
            JLabel newLabel;
            newLabel = Utility.localizedLabel("Colony");
            newLabel.setBorder(Utility.TOPLEFTCELLBORDER);
            reportPanel.add(newLabel, "newline 20");

            for (int index = 0; index < selectedTypes.size(); index++) {
                newLabel = Utility.localizedLabel(selectedTypes.get(index));
                newLabel.setBorder(Utility.TOPCELLBORDER);
                reportPanel.add(newLabel);

                for (BuildingType buildingType : basicBuildingTypes.get(index)) {
                    newLabel = Utility.localizedLabel(buildingType);
                    newLabel.setBorder(Utility.TOPCELLBORDER);
                    reportPanel.add(newLabel);
                }
            }

            int[] totalProduction = new int[selectedTypes.size()];
            for (Colony colony : getFreeColClient().getMySortedColonies()) {
                // colonyButton
                JButton colonyButton = Utility.getLinkButton(colony.getName(),
                    null, colony.getId());
                colonyButton.setBorder(Utility.LEFTCELLBORDER);
                colonyButton.addActionListener(this);
                reportPanel.add(colonyButton, "newline");

                // production
                for (int index = 0; index < selectedTypes.size(); index++) {
                    GoodsType goodsType = selectedTypes.get(index);
                    int newValue = colony.getNetProductionOf(goodsType);
                    totalProduction[index] += newValue;
                    Goods goods = new Goods(colony.getGame(), colony, goodsType, newValue);
                    GoodsLabel goodsLabel = new GoodsLabel(getGUI(), goods);
                    goodsLabel.setHorizontalAlignment(JLabel.LEADING);
                    goodsLabel.setBorder(Utility.CELLBORDER);
                    reportPanel.add(goodsLabel);

                    for (BuildingType buildingType : basicBuildingTypes.get(index)) {
                        Building building = colony.getBuilding(buildingType);
                        if (building == null) {
                            newLabel = new JLabel();
                            newLabel.setBorder(Utility.CELLBORDER);
                            reportPanel.add(newLabel);
                        } else {
                            buildingCount.incrementCount(building.getType(), 1);
                            BuildingPanel buildingPanel =
                                new BuildingPanel(getFreeColClient(), building);
                            buildingPanel.setBorder(Utility.CELLBORDER);
                            buildingPanel.initialize();
                            reportPanel.add(buildingPanel);
                        }
                    }
                }
            }

        }
        revalidate();
        repaint();
    }
}
