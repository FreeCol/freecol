/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

package net.sf.freecol.client.gui.panel.report;

import java.awt.GridLayout;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.label.ProductionLabel;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays the classic version of the colony report.
 */
public final class ReportClassicColonyPanel extends ReportPanel {

    /**
     * Compare buildings by their underlying type index (which reduces
     * to the order in the spec), then the FCO order.
     */
    private static final Comparator<Building> buildingTypeComparator
        = Comparator.<Building>comparingInt(b -> b.getType().getIndex())
            .thenComparing(FreeColObject.fcoComparator);

    private static final int COLONISTS_PER_ROW = 20;
    private static final int UNITS_PER_ROW = 14;
    private static final int GOODS_PER_ROW = 10;
    private static final int BUILDINGS_PER_ROW = 8;

    private final List<Colony> colonies = new ArrayList<>();


    /**
     * Creates a colony report.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportClassicColonyPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportColonyAction");
        
        this.colonies.addAll(freeColClient.getMyPlayer().getColonyList());
        update();
    }

    private void update() {
        final Specification spec = getSpecification();
        final ImageLibrary lib = getImageLibrary();
        
        reportPanel.removeAll();
        reportPanel.setLayout(new MigLayout("fill")); // Set the layout
        
        for (Colony colony : this.colonies) {
            // Name
            JButton button = Utility.getLinkButton(colony.getName(), null,
                colony.getId());
            button.addActionListener(this);
            reportPanel.add(button, "newline, split 2");
            reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");
            
            // Currently building
            BuildableType currentType = colony.getCurrentlyBuilding();
            JLabel buildableLabel = null;
            if (currentType != null) {
                buildableLabel = new JLabel(new ImageIcon(lib
                        .getSmallBuildableTypeImage(currentType, colony.getOwner())));
                Utility.localizeToolTip(buildableLabel,
                    currentType.getCurrentlyBuildingLabel());
                buildableLabel.setIcon(buildableLabel.getDisabledIcon());
            }
            
            // Units
            JPanel colonistsPanel
                = new JPanel(new GridLayout(0, COLONISTS_PER_ROW));
            colonistsPanel.setOpaque(false);
            for (Unit u : sort(colony.getUnits(), Unit.typeRoleComparator)) {
                colonistsPanel.add(new UnitLabel(getFreeColClient(), u,
                                                 true, true));
            }
            JPanel unitsPanel = new JPanel(new GridLayout(0, UNITS_PER_ROW));
            unitsPanel.setOpaque(false);
            for (Unit u : sort(colony.getTile().getUnits(),
                               Unit.typeRoleComparator)) {
                unitsPanel.add(new UnitLabel(getFreeColClient(), u,
                                             true, true));
            }
            if (buildableLabel != null
                && spec.getUnitTypeList().contains(currentType)) {
                unitsPanel.add(buildableLabel);
            }
            reportPanel.add(colonistsPanel, "newline, growx");
            reportPanel.add(unitsPanel, "newline, growx");
            
            // Production
            int count = 0;
            for (GoodsType gt : sort(spec.getGoodsTypeList(),
                                     GoodsType.goodsTypeComparator)) {
                int newValue = colony.getNetProductionOf(gt);
                int stockValue = colony.getGoodsCount(gt);
                if (newValue != 0 || stockValue > 0) {
                    int maxProduction
                        = sum(colony.getWorkLocationsForProducing(gt),
                              wl -> wl.getMaximumProductionOf(gt));
                    ProductionLabel productionLabel
                        = new ProductionLabel(getFreeColClient(),
                            new AbstractGoods(gt, newValue),
                            maxProduction, stockValue);
                    if (count % GOODS_PER_ROW == 0) {
                        reportPanel.add(productionLabel,
                            "newline, split " + GOODS_PER_ROW);
                    } else {
                        reportPanel.add(productionLabel);
                    }
                    count++;
                }
            }
            
            // Buildings
            JPanel buildingsPanel
                = new JPanel(new GridLayout(0, BUILDINGS_PER_ROW));
            buildingsPanel.setOpaque(false);
            for (Building building : sort(colony.getBuildings(),
                                          buildingTypeComparator)) {
                if (building.getType().isAutomaticBuild()) continue;
                JLabel buildingLabel = new JLabel(new ImageIcon(lib
                        .getSmallBuildingImage(building)));
                buildingLabel.setToolTipText(Messages.getName(building));
                buildingsPanel.add(buildingLabel);
            }
            if (buildableLabel != null
                && spec.getBuildingTypeList().contains(currentType)) {
                buildingsPanel.add(buildableLabel);
            }
            reportPanel.add(buildingsPanel, "newline, growx");
        }
    }
}
