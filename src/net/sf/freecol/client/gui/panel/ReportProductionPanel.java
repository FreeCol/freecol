/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TypeCountMap;


/**
 * This panel displays the ContinentalCongress Report.
 */
public final class ReportProductionPanel extends ReportPanel {

    private static final int NUMBER_OF_GOODS = 4;
    private final JComboBox[] boxes = new JComboBox[NUMBER_OF_GOODS];
    private final JButton selectButton;
    private final JLabel selectLabel;
    private final List<GoodsType> goodsTypes;

    /**
     * The constructor that will add the items to this panel.
     * @param freeColClient
     *
     * @param gui The parent of this panel.
     */
    public ReportProductionPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, Messages.message("reportProductionAction.name"));

        // TODO: can we extend this to cover farmed goods?
        goodsTypes = new ArrayList<GoodsType>();
        List<String> goodsNames = new ArrayList<String>();
        goodsNames.add(Messages.message("nothing"));
        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            if (!goodsType.isFarmed()) {
                goodsTypes.add(goodsType);
                goodsNames.add(Messages.message(goodsType.getNameKey()));
            }
        }

        String[] model = goodsNames.toArray(new String[goodsTypes.size() + 1]);
        for (int index = 0; index < NUMBER_OF_GOODS; index++) {
            boxes[index] = new JComboBox(model);
        }

        selectLabel = new JLabel(Messages.message("report.production.selectGoods"));
        selectButton = new JButton(Messages.message("report.production.update"));
        selectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    update();
                }
            });

        reportPanel.setLayout(new MigLayout("gap 0 0", "[fill]", "[fill]"));
        update();
    }

    private void update() {

        List<GoodsType> selectedTypes = new ArrayList<GoodsType>();

        reportPanel.removeAll();

        reportPanel.add(selectLabel, "span, split " + (NUMBER_OF_GOODS + 2));

        for (int index = 0; index < NUMBER_OF_GOODS; index++) {
            reportPanel.add(boxes[index]);
            int selectedIndex = boxes[index].getSelectedIndex();
            if (selectedIndex > 0) {
                selectedTypes.add(goodsTypes.get(selectedIndex - 1));
            }
        }

        reportPanel.add(selectButton, "wrap 20");

        if (!selectedTypes.isEmpty()) {
            Player player = getMyPlayer();

            TypeCountMap<BuildingType> buildingCount = new TypeCountMap<BuildingType>();
            List<List<BuildingType>> basicBuildingTypes = new ArrayList<List<BuildingType>>();
            for (GoodsType goodsType : selectedTypes) {
                List<BuildingType> buildingTypes = new ArrayList<BuildingType>();
                for (BuildingType buildingType : getSpecification().getBuildingTypeList()) {
                    if (goodsType.equals(buildingType.getProducedGoodsType())
                        || !buildingType.getModifierSet(goodsType.getId()).isEmpty()) {
                        BuildingType firstLevel = buildingType.getFirstLevel();
                        if (!buildingTypes.contains(firstLevel)) {
                            buildingTypes.add(firstLevel);
                        }
                    }
                }
                basicBuildingTypes.add(buildingTypes);
            }

            JLabel newLabel;

            // labels
            newLabel = new JLabel(Messages.message("Colony"));
            newLabel.setBorder(FreeColPanel.TOPLEFTCELLBORDER);
            reportPanel.add(newLabel, "newline 20");

            for (int index = 0; index < selectedTypes.size(); index++) {
                newLabel = localizedLabel(selectedTypes.get(index).getNameKey());
                newLabel.setBorder(FreeColPanel.TOPCELLBORDER);
                reportPanel.add(newLabel);

                for (BuildingType buildingType : basicBuildingTypes.get(index)) {
                    newLabel = localizedLabel(buildingType.getNameKey());
                    newLabel.setBorder(FreeColPanel.TOPCELLBORDER);
                    reportPanel.add(newLabel);
                }
            }


            int[] totalProduction = new int[selectedTypes.size()];
            for (Colony colony : getSortedColonies()) {
                // colonyButton
                JButton colonyButton = getLinkButton(colony.getName(), null, colony.getId());
                colonyButton.setBorder(FreeColPanel.LEFTCELLBORDER);
                colonyButton.addActionListener(this);
                reportPanel.add(colonyButton, "newline");

                // production
                for (int index = 0; index < selectedTypes.size(); index++) {
                    GoodsType goodsType = selectedTypes.get(index);
                    int newValue = colony.getNetProductionOf(goodsType);
                    totalProduction[index] += newValue;
                    Goods goods = new Goods(colony.getGame(), colony, goodsType, newValue);
                    GoodsLabel goodsLabel = new GoodsLabel(goods, getGUI());
                    goodsLabel.setHorizontalAlignment(JLabel.LEADING);
                    goodsLabel.setBorder(FreeColPanel.CELLBORDER);
                    reportPanel.add(goodsLabel);

                    for (BuildingType buildingType : basicBuildingTypes.get(index)) {
                        Building building = colony.getBuilding(buildingType);
                        if (building == null) {
                            newLabel = new JLabel();
                            newLabel.setBorder(FreeColPanel.CELLBORDER);
                            reportPanel.add(newLabel);
                        } else {
                            buildingCount.incrementCount(building.getType(), 1);
                            BuildingPanel buildingPanel =
                                new BuildingPanel(getFreeColClient(), building, getGUI());
                            buildingPanel.setBorder(FreeColPanel.CELLBORDER);
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
