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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.border.Border;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;

public class WorkProductionPanel extends FreeColPanel {

    private static final Border border = BorderFactory
        .createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK),
                              BorderFactory.createEmptyBorder(2, 2, 2, 2));

    public WorkProductionPanel(Canvas canvas, Unit unit) {
        super(canvas);

        setLayout(new MigLayout("wrap 3, insets 10 10 10 10", "[]30:push[right][]", ""));

        Colony colony = unit.getColony();
        UnitType unitType = unit.getType();

        JLabel headline = new JLabel();
        Set<Modifier> modifiers;
        Set<Modifier> basicModifiers;
        Set<Modifier> colonyModifiers = new LinkedHashSet<Modifier>();
        if (unit.getLocation() instanceof ColonyTile) {
            ColonyTile colonyTile = (ColonyTile) unit.getLocation();
            GoodsType goodsType = unit.getWorkType();
            basicModifiers = colonyTile.getProductionModifiers(goodsType, unitType);
            modifiers = sortModifiers(basicModifiers);
            basicModifiers.addAll(colony.getModifierSet(goodsType.getId()));
            if (colony.getProductionBonus() != 0) {
                modifiers.add(colony.getProductionModifier(goodsType));
            }

            add(localizedLabel(colonyTile.getLabel()), "span, align center, wrap 30");

            TileType tileType = colonyTile.getWorkTile().getType();
            int width = canvas.getClient().getImageLibrary().getTerrainImageWidth(tileType);
            int height = canvas.getClient().getImageLibrary().getTerrainImageHeight(tileType);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            canvas.getGUI().displayColonyTile((Graphics2D) image.getGraphics(), colonyTile.getWorkTile().getMap(),
                                              colonyTile.getWorkTile(), 0, 0, colony);
            add(new JLabel(new ImageIcon(image)));

        } else {
            Building building = (Building) unit.getLocation();
            GoodsType goodsType = building.getGoodsOutputType();
            basicModifiers = new LinkedHashSet<Modifier>();
            if (building.getType().getProductionModifier() != null) {
                basicModifiers.add(building.getType().getProductionModifier());
            }
            if (goodsType != null) {
                basicModifiers.addAll(unit.getType().getModifierSet(goodsType.getId()));
            }
            modifiers = sortModifiers(basicModifiers);
            for (Modifier modifier : colony.getModifierSet(goodsType.getId())) {
                if (modifier.getSource() != building.getType()) {
                    colonyModifiers.add(modifier);
                }
            }
            modifiers.addAll(sortModifiers(colonyModifiers));
            if (colony.getProductionBonus() != 0) {
                modifiers.add(colony.getProductionModifier(goodsType));
            }
            add(new JLabel(building.getName()), "span, align center, wrap 30");

            add(new JLabel(ResourceManager.getImageIcon(building.getType().getId() + ".image")));
        }

        add(new UnitLabel(unit, canvas, false, false), "wrap");

        int totalPercentage = 0;
        for (Modifier modifier : modifiers) {
            FreeColGameObjectType source = modifier.getSource();
            String sourceName;
            if (source == null) {
                sourceName = "???";
            } else {
                sourceName = source.getName();
                if (unitType != null && modifier.hasScope()) {
                    for (Scope scope : modifier.getScopes()) {
                        if (scope.appliesTo(unitType)) {
                            sourceName += " (" + unitType.getName() + ")";
                        }
                    }
                }
            }
            String bonus = getModifierFormat().format(modifier.getValue());
            boolean percentage = false;
            switch(modifier.getType()) {
            case ADDITIVE:
                if (modifier.getValue() > 0) {
                    bonus = "+" + bonus;
                }
                break;
            case PERCENTAGE:
                if (modifier.getValue() > 0) {
                    bonus = "+" + bonus;
                }
                percentage = true;
                break;
            case MULTIPLICATIVE:
                bonus = "\u00D7" + bonus;
                break;
            default:
            }
            if (modifier.getType() == Modifier.Type.PERCENTAGE) {
                totalPercentage += modifier.getValue();
            } else if (totalPercentage != 0) {
                String result = getModifierFormat().format(totalPercentage);
                if (totalPercentage > 0) {
                    result = "+" + result;
                }
                JLabel resultLabel = new JLabel(result);
                resultLabel.setBorder(border);
                add(resultLabel, "skip");
                add(new JLabel("%"));
                totalPercentage = 0;
            }
            add(new JLabel(sourceName), "newline");
            add(new JLabel(bonus));
            if (percentage) {
                add(new JLabel("%"));
            }
        }

        Font bigFont = getFont().deriveFont(Font.BOLD, 16);

        int result = (int) FeatureContainer.applyModifierSet(0, getGame().getTurn(), basicModifiers);
        result = (int) FeatureContainer.applyModifierSet(result, getGame().getTurn(), colonyModifiers);
        result += colony.getProductionBonus();

        JLabel finalLabel = new JLabel(Messages.message("model.source.finalResult.name"));
        finalLabel.setFont(bigFont);
        add(finalLabel, "newline");

        JLabel finalResult = new JLabel(getModifierFormat().format(result));
        finalResult.setFont(bigFont);
        finalResult.setBorder(border);
        add(finalResult, "wrap 30");

        add(okButton, "span, tag ok");

        setSize(getPreferredSize());

    }
}


