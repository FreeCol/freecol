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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JSeparator;
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

        List<Modifier> unitModifiers = new ArrayList<Modifier>();
        List<Modifier> modifiers = new ArrayList<Modifier>();
        if (unit.getLocation() instanceof ColonyTile) {
            ColonyTile colonyTile = (ColonyTile) unit.getLocation();
            GoodsType goodsType = unit.getWorkType();
            Set<Modifier> tileModifiers = colonyTile.getProductionModifiers(goodsType, unitType);
            if (FeatureContainer.applyModifierSet(0f, getGame().getTurn(), tileModifiers) > 0) {
                tileModifiers.addAll(unit.getModifierSet(goodsType.getId()));
                unitModifiers.addAll(tileModifiers);
                if (colony.getProductionBonus() != 0) {
                    modifiers.add(colony.getProductionModifier(goodsType));
                }
                modifiers.addAll(colony.getModifierSet(goodsType.getId()));
            }
            Collections.sort(unitModifiers);
            add(localizedLabel(colonyTile.getLabel()), "span, align center, wrap 30");

            TileType tileType = colonyTile.getWorkTile().getType();
            int width = canvas.getFreeColClient().getGUI().getImageLibrary()
                .getTerrainImageWidth(tileType);
            int height = canvas.getFreeColClient().getGUI().getImageLibrary()
                .getTerrainImageHeight(tileType);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            canvas.getMapViewer().displayColonyTile((Graphics2D) image.getGraphics(),
                                              colonyTile.getWorkTile(), colony);
            add(new JLabel(new ImageIcon(image)));

        } else if (unit.getLocation() instanceof Building) {
            Building building = (Building) unit.getLocation();
            GoodsType goodsType = building.getGoodsOutputType();
            if (building.getType().getProductionModifier() != null) {
                unitModifiers.add(building.getType().getProductionModifier());
            }
            if (colony.getProductionBonus() != 0) {
                unitModifiers.add(colony.getProductionModifier(goodsType));
            }

            if (goodsType != null) {
                unitModifiers.addAll(unit.getType().getModifierSet(goodsType.getId()));
            }
            modifiers.addAll(colony.getModifierSet(goodsType.getId()));

            add(localizedLabel(building.getNameKey()), "span, align center, wrap 30");

            add(new JLabel(ResourceManager.getImageIcon(building.getType().getId() + ".image")));
        }
        Collections.sort(modifiers);

        add(new UnitLabel(unit, canvas, false, false), "wrap");

        float result = 0;
        for (Modifier modifier : unitModifiers) {
            result = addModifier(modifier, unitType, result);
        }

        if (!modifiers.isEmpty()) {
            add(new JSeparator(JSeparator.HORIZONTAL), "span, growx");

            for (Modifier modifier : modifiers) {
                result = addModifier(modifier, unitType, result);
            }
        }

        Font bigFont = getFont().deriveFont(Font.BOLD, 16);

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


    private float addModifier(Modifier modifier, UnitType unitType, float result) {
        FreeColGameObjectType source = modifier.getSource();
        String sourceName;
        if (source == null) {
            sourceName = "???";
        } else {
            sourceName = Messages.message(source.getNameKey());
            if (unitType != null && modifier.hasScope()) {
                for (Scope scope : modifier.getScopes()) {
                    if (scope.appliesTo(unitType)) {
                        sourceName += " (" + Messages.message(unitType.getNameKey()) + ")";
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

        add(new JLabel(sourceName), "newline");
        add(new JLabel(bonus));
        add(new JLabel(percentage ? "%" : ""));

        return modifier.applyTo(result);
    }



}


