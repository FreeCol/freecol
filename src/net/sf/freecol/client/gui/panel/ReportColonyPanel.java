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

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Colony Report.
 */
public final class ReportColonyPanel extends ReportPanel {



    private List<Colony> colonies;

    private final int ROWS_PER_COLONY = 4;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportColonyPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.colony"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    @Override
    public void initialize() {
        Player player = getCanvas().getClient().getMyPlayer();
        colonies = player.getColonies();

        // Display Panel
        
        int widths[] = new int[] {0};
        // If no colonies are defined, show an empty panel.
        int heights[] = null;
        if (colonies.size() == 0) {
        	heights = new int[0];
        } else {
        	heights = new int[colonies.size() * 2 - 1];
        }
        for (int i = 1; i < heights.length; i += 2) {
            heights[i] = 12;
        }
        reportPanel.setLayout(new HIGLayout(widths, heights));

        widths = new int[] {0};
        heights = new int[2 * ROWS_PER_COLONY - 1];
        for (int i = 1; i < heights.length; i += 2) {
            heights[i] = 5;
        }

        int panelColumn = 1;
        int colonyRow = 1;

        Border colonyBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINK_COLOR),
                                                                 BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());
        for (int colonyIndex = 0; colonyIndex < colonies.size(); colonyIndex++) {
            int row = 1;

            Colony colony = colonies.get(colonyIndex);
            JPanel colonyPanel = new JPanel(new HIGLayout(widths, heights));
            colonyPanel.setBorder(colonyBorder);

            colonyPanel.add(createColonyButton(colonyIndex), higConst.rc(row, panelColumn, "l"));
            row += 2;

            colonyPanel.add(createUnitPanel(colony), higConst.rc(row, panelColumn));
            row += 2;

            colonyPanel.add(createProductionPanel(colony), higConst.rc(row, panelColumn));
            row += 2;

            colonyPanel.add(createBuildingPanel(colony), higConst.rc(row, panelColumn));

            reportPanel.add(colonyPanel, higConst.rc(colonyRow, panelColumn));
            colonyRow += 2;
        }

    }


    private JPanel createUnitPanel(Colony colony) { 
        JPanel unitPanel = new JPanel(new GridLayout(0, 12));
        unitPanel.setOpaque(false);
        List<Unit> unitList = colony.getUnitList();
        Collections.sort(unitList, getUnitTypeComparator());
        for(Unit unit : unitList) {
            UnitLabel unitLabel = new UnitLabel(unit, getCanvas(), true, true);
            unitPanel.add(unitLabel);
        }
        unitList = colony.getTile().getUnitList();
        Collections.sort(unitList, getUnitTypeComparator());
        for(Unit unit : unitList) {
            UnitLabel unitLabel = new UnitLabel(unit, getCanvas(), true, true);
            unitPanel.add(unitLabel);
        }

        return unitPanel;
    }

    private JPanel createProductionPanel(Colony colony) {
        JPanel goodsPanel = new JPanel(new GridLayout(0, 10));
        goodsPanel.setOpaque(false);
        int netFood = colony.getFoodProduction() - colony.getFoodConsumption();
        if (netFood != 0) {
            ProductionLabel productionLabel = new ProductionLabel(Goods.FOOD, netFood, getCanvas());
            productionLabel.setStockNumber(colony.getFoodCount());
            goodsPanel.add(productionLabel);
        }
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            if (goodsType.isFoodType()) {
                continue;
            }
            int newValue = colony.getProductionNetOf(goodsType);
            int stockValue = colony.getGoodsCount(goodsType);
            if (newValue != 0 || stockValue > 0) {
                Building building = colony.getBuildingForProducing(goodsType);
                ProductionLabel productionLabel = new ProductionLabel(goodsType, newValue, getCanvas());
                if (building != null) {
                    productionLabel.setMaximumProduction(building.getMaximumProduction());
                }
                if (goodsType == Goods.HORSES) {
                    productionLabel.setMaxGoodsIcons(1);
                }
                productionLabel.setStockNumber(stockValue);   // Show stored items in ReportColonyPanel
                goodsPanel.add(productionLabel);
            }
        }
        return goodsPanel;
    }

    private JPanel createBuildingPanel(Colony colony) {
        JPanel buildingPanel = new JPanel(new GridLayout(0, 5, 12, 0));
        buildingPanel.setOpaque(false);

        for (Building building : colony.getBuildings()) {
            buildingPanel.add(new JLabel(building.getName()));
        }
        
        BuildableType currentType = colony.getCurrentlyBuilding();
        JLabel buildableLabel = new JLabel(currentType.getName());
        if (currentType == BuildableType.NOTHING) {
            buildableLabel.setForeground(Color.RED);
        } else {
            buildableLabel.setForeground(Color.GRAY);
        }
        buildingPanel.add(buildableLabel);
        return buildingPanel;
    }

    private JButton createColonyButton(int index) {
        JButton button = getLinkButton(colonies.get(index).getName(), null, String.valueOf(index));
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
        int action = Integer.valueOf(command).intValue();
        if (action == OK) {
            super.actionPerformed(event);
        } else {
            getCanvas().showColonyPanel(colonies.get(action));
        }
    }

}
