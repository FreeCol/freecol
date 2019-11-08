/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.client.gui.tooltip;

import java.awt.Font;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.*;
import net.sf.freecol.client.gui.label.ProductionLabel;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A tooltip for a single building in a Colony.
 */
public class BuildingToolTip extends JToolTip {

    /**
     * Creates this BuildingToolTip.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param building The building to display information from.
     */
    public BuildingToolTip(FreeColClient freeColClient, Building building) {
        final ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        final Game game = building.getGame();
        final int workplaces = building.getUnitCapacity();
        // FIXME: consider several outputs
        final AbstractGoods output = first(building.getOutputs());
        final GoodsType outputType = (output == null) ? null : output.getType();

        StringBuilder sb = new StringBuilder(64);
        sb.append("[align center]");
        for (int index = 0; index < workplaces; index++) sb.append("20[]5[]");
        String columns = sb.toString();

        MigLayout layout = new MigLayout("fill, insets 20, wrap "
            + (2 * workplaces + 1), columns, "[][][align bottom]");
        setLayout(layout);

        JLabel buildingName = new JLabel(Messages.getName(building));
        buildingName.setFont(FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
            FontLibrary.FontSize.SMALLER, Font.BOLD, lib.getScaleFactor()));
        add(buildingName, "span");

        ProductionInfo info = building.getProductionInfo();
        AbstractGoods production = (info == null) ? null
            : first(info.getProduction());
        AbstractGoods consumption = (info == null) ? null
            : first(info.getConsumption());
        if (production == null || production.getAmount() <= 0) {
            add(new JLabel(), "span");
        } else {
            JLabel arrow = new JLabel(ResourceManager.getString("arrow.E"));
            arrow.setFont(FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
                FontLibrary.FontSize.SMALL, Font.BOLD, lib.getScaleFactor()));
            AbstractGoods maxProduction = (info == null) ? null
                : first(info.getMaximumProduction());
            ProductionLabel productionOutput
                = new ProductionLabel(freeColClient, production,
                    ((maxProduction == null) ? production
                        : maxProduction).getAmount());
            if (consumption == null) {
                add(productionOutput, "span");
            } else if (consumption.getAmount() > 0) {
                AbstractGoods maxConsumption = (info == null) ? null
                    : first(info.getMaximumConsumption());
                ProductionLabel productionInput
                    = new ProductionLabel(freeColClient, consumption,
                        ((maxConsumption == null) ? consumption
                            : maxConsumption).getAmount());
                add(productionInput, "span, split 3");
                add(arrow);
                add(productionOutput);
            } else {
                add(new JLabel(new ImageIcon(lib
                            .getScaledGoodsTypeImage(consumption.getType()))),
                    "span, split 3");
                add(arrow);
                add(new JLabel(new ImageIcon(lib
                            .getScaledGoodsTypeImage(production.getType()))));
            }
        }

        add(new JLabel(new ImageIcon(lib.getScaledBuildingImage(building))));

        if (outputType != null) {
            for (Unit unit : building.getUnitList()) {
                UnitLabel unitLabel = new UnitLabel(freeColClient, unit);
                int amount = building.getUnitProduction(unit, outputType);
                if (amount > 0) {
                    add(unitLabel);
                    JLabel pLabel = new ProductionLabel(freeColClient,
                        new AbstractGoods(outputType, amount));
                    add(pLabel, "split 2");
                    add(new JLabel());
                }
            }
        } else if (building.canTeach()) {
            for (Unit unit : building.getUnitList()) {
                UnitLabel unitLabel = new UnitLabel(freeColClient, unit);
                if (unit.getStudent() != null) {
                    JLabel progress = new JLabel(unit.getTurnsOfTraining() + "/"
                        + unit.getNeededTurnsOfTraining());
                    UnitLabel sLabel = new UnitLabel(freeColClient,
                        unit.getStudent(), true, true);
                    add(unitLabel);
                    add(progress, "split 2, flowy");
                    add(sLabel);
                } else {
                    add(unitLabel, "span 2");
                }
            }
        }

        int diff = building.getUnitCapacity() - building.getUnitCount();
        for (int index = 0; index < diff; index++) {
            add(new JLabel(new ImageIcon(lib
                        .getScaledImage("image.unit.placeholder"))),
                "span 2");
        }

        int breed = (outputType == null || !outputType.isBreedable()) ? -1
            : outputType.getBreedingNumber();
        if (breed > building.getColony().getGoodsCount(outputType)) {
            add(Utility.localizedLabel(StringTemplate
                    .template("buildingToolTip.breeding")
                    .addAmount("%number%", breed)
                    .addNamed("%goods%", outputType)));
        }

        if (outputType != null
            && FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            List<Modifier> mods
                = sort(building.getProductionModifiers(outputType, null),
                       Modifier.ascendingModifierIndexComparator);
            for (Modifier m : mods) {
                JLabel[] mLabels = ModifierFormat.getModifierLabels(m, null,
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


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        setLayout(null);
    }
}
