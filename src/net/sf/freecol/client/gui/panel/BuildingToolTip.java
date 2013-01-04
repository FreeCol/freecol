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

import java.awt.Font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * A tooltip for a single building in a Colony.
 */
public class BuildingToolTip extends JToolTip {

    private static final JLabel arrow = new JLabel("\u2192");

    static {
        arrow.setFont(ResourceManager.getFont("SimpleFont", Font.BOLD, 24f));
    }


    /**
     * Creates this BuildingToolTip.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param building The building to display information from.
     * @param gui The <code>GUI</code> to display on.
     */
    public BuildingToolTip(FreeColClient freeColClient, Building building,
                           GUI gui) {
        final Game game = building.getGame();
        final int workplaces = building.getUnitCapacity();
        final GoodsType output = building.getGoodsOutputType();
        final Colony colony = building.getColony();

        String columns = "[align center]";
        for (int index = 0; index < workplaces; index++) {
            columns += "20[]5[]";
        }

        MigLayout layout = new MigLayout("fill, insets 20, wrap " + (2 * workplaces + 1),
                                         columns, "[][][align bottom]");
        setLayout(layout);

        JLabel buildingName
            = new JLabel(Messages.message(building.getNameKey()));
        buildingName.setFont(ResourceManager.getFont("SimpleFont", Font.BOLD, 16f));
        add(buildingName, "span");

        ProductionInfo info = building.getProductionInfo();
        if (info == null || info.getProduction().isEmpty()) {
            add(new JLabel(), "span");
        } else {
            AbstractGoods production = info.getProduction().get(0);
            AbstractGoods maximumProduction = info.getMaximumProduction().isEmpty()
                ? production : info.getMaximumProduction().get(0);
            ProductionLabel productionOutput = new ProductionLabel(freeColClient, gui, production, maximumProduction);
            if (info.getConsumption().isEmpty()) {
                add(productionOutput, "span");
            } else {
                AbstractGoods consumption = info.getConsumption().get(0);
                if (consumption.getAmount() > 0) {
                    AbstractGoods maximumConsumption = info.getMaximumConsumption().isEmpty()
                        ? consumption: info.getMaximumConsumption().get(0);
                    ProductionLabel productionInput = new ProductionLabel(freeColClient, gui, consumption, maximumConsumption);
                    add(productionInput, "span, split 3");
                    add(arrow);
                    add(productionOutput);
                } else {
                    add(new JLabel(gui.getImageLibrary().getGoodsImageIcon(consumption.getType())),
                        "span, split 3");
                    add(arrow);
                    add(new JLabel(gui.getImageLibrary().getGoodsImageIcon(production.getType())));
                }
            }
        }

        add(new JLabel(new ImageIcon(ResourceManager.getImage(building.getType().getId() + ".image"))));

        for (Unit unit : building.getUnitList()) {
            UnitLabel unitLabel = new UnitLabel(freeColClient, unit, gui, false);
            int production = building.getUnitProduction(unit);
            if (production > 0) {
                add(unitLabel);
                JLabel pLabel = new ProductionLabel(freeColClient, gui, output,
                                                    production);
                add(pLabel, "split 2");
                add(new JLabel());
            } else if (building.canTeach() && unit.getStudent() != null) {
                JLabel progress = new JLabel(unit.getTurnsOfTraining() + "/"
                                           + unit.getNeededTurnsOfTraining());
                UnitLabel sLabel = new UnitLabel(freeColClient, 
                                                 unit.getStudent(), gui, true);
                sLabel.setIgnoreLocation(true);
                add(unitLabel);
                add(progress, "split 2, flowy");
                add(sLabel);
            } else {
                add(unitLabel, "span 2");
            }
        }

        int diff = building.getUnitCapacity() - building.getUnitCount();
        for (int index = 0; index < diff; index++) {
            add(new JLabel(new ImageIcon(ResourceManager.getImage("placeholder.image"))), "span 2");
        }

        int breedingNumber = (output == null) ? GoodsType.INFINITY
            : output.getBreedingNumber();
        if (breedingNumber < GoodsType.INFINITY
            && breedingNumber > building.getColony().getGoodsCount(output)) {
            StringTemplate t = StringTemplate.template("buildingToolTip.breeding")
                .addAmount("%number%", breedingNumber)
                .add("%goods%", output.getNameKey());
            add(new JLabel(Messages.message(t)));
        }

        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            List<Modifier> modifiers = new ArrayList<Modifier>();
            modifiers.addAll(building.getProductionModifiers(output, null));
            Collections.sort(modifiers);
            for (Modifier m : modifiers) {
                JLabel[] mLabels = FreeColPanel.getModifierLabels(m, null,
                                                                  game.getTurn());
                for (int i = 0; i < mLabels.length; i++) {
                    if (mLabels[i] != null) {
                        if (i == 0) {
                            add(mLabels[i],"newline");
                        } else {
                            add(mLabels[i]);
                        }
                    }
                }
            }
        }

        setPreferredSize(layout.preferredLayoutSize(this));
    }
}
