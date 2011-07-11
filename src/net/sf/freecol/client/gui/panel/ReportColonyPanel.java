/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.GridLayout;

import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Colony Report.
 */
public final class ReportColonyPanel extends ReportPanel {

    private static final int COLONISTS_PER_ROW = 16;
    private static final int UNITS_PER_ROW = 12;
    private static final int GOODS_PER_ROW = 10;
    private static final int BUILDINGS_PER_ROW = 8;

    private List<Colony> colonies;

    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public ReportColonyPanel(Canvas parent) {

        super(parent, Messages.message("reportColonyAction.name"));
        Player player = getMyPlayer();
        colonies = getFreeColClient().getClientOptions()
            .getSortedColonies(player);

        // Display Panel
        reportPanel.setLayout(new MigLayout("fill"));

        for (Colony colony : colonies) {

            // Name
            JButton button = getLinkButton(colony.getName(), null, colony.getId());
            button.addActionListener(this);
            reportPanel.add(button, "newline 20, split 2");
            reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

            // Units
            List<Unit> unitList = colony.getUnitList();
            Collections.sort(unitList, getUnitTypeComparator());
            for (int index = 0; index < unitList.size(); index++) {
                UnitLabel unitLabel = new UnitLabel(unitList.get(index), getCanvas(), true, true);
                if (index % COLONISTS_PER_ROW == 0) {
                    reportPanel.add(unitLabel, "newline, split " + COLONISTS_PER_ROW);
                } else {
                    reportPanel.add(unitLabel);
                }
            }
            unitList = colony.getTile().getUnitList();
            Collections.sort(unitList, getUnitTypeComparator());
            for (int index = 0; index < unitList.size(); index++) {
                UnitLabel unitLabel = new UnitLabel(unitList.get(index), getCanvas(), true, true);
                if (index % UNITS_PER_ROW == 0) {
                    reportPanel.add(unitLabel, "newline, split " + UNITS_PER_ROW);
                } else {
                    reportPanel.add(unitLabel);
                }
            }

            // Production
            GoodsType horses = getSpecification().getGoodsType("model.goods.horses");
            int count = 0;
            for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
                int newValue = colony.getNetProductionOf(goodsType);
                int stockValue = colony.getGoodsCount(goodsType);
                if (newValue != 0 || stockValue > 0) {
                    Building building = colony.getBuildingForProducing(goodsType);
                    ProductionLabel productionLabel = new ProductionLabel(goodsType, newValue, getCanvas());
                    if (building != null) {
                        productionLabel.setMaximumProduction(building.getMaximumProduction());
                    }
                    if (goodsType == horses) {
                        // horse images don't stack well
                        productionLabel.setMaxGoodsIcons(1);
                    }
                    // Show stored items in ReportColonyPanel
                    productionLabel.setStockNumber(stockValue);
                    if (count % GOODS_PER_ROW == 0) {
                        reportPanel.add(productionLabel, "newline, split " + GOODS_PER_ROW);
                    } else {
                        reportPanel.add(productionLabel);
                    }
                    count++;
                }
            }

            // Buildings
            JPanel buildingsPanel = new JPanel(new GridLayout(0, BUILDINGS_PER_ROW));
            List<Building> buildingList = colony.getBuildings();
            Collections.sort(buildingList);
            for (Building building : buildingList) {
                if(building.getType().isAutomaticBuild()) {
                    continue;
                }
                
                JLabel buildingLabel =
                    new JLabel(new ImageIcon(ResourceManager.getImage(building.getType().getId()
                                                                      + ".image", 0.66)));
                buildingLabel.setToolTipText(Messages.message(building.getNameKey()));
                buildingsPanel.add(buildingLabel);
            }

            BuildableType currentType = colony.getCurrentlyBuilding();
            if (currentType != null) {
                JLabel buildableLabel =
                    new JLabel(new ImageIcon(ResourceManager.getImage(currentType.getId()
                                                                      + ".image", 0.66)));
                buildableLabel.setToolTipText(Messages.message(StringTemplate.template("colonyPanel.currentlyBuilding")
                                                               .add("%buildable%", currentType.getNameKey())));
                buildableLabel.setIcon(buildableLabel.getDisabledIcon());
                buildingsPanel.add(buildableLabel);
            }
            reportPanel.add(buildingsPanel, "newline, growx");
        }

    }
}
