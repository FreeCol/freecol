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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

public class ColonyTileProductionPanel extends FreeColPanel implements ActionListener {

    private final JButton okButton = new JButton(Messages.message("ok"));
    private final Canvas parent;

    public ColonyTileProductionPanel(Canvas canvas, ColonyTile colonyTile, GoodsType goodsType) {

        parent = canvas;

        Colony colony = colonyTile.getColony();
        UnitType unitType = null;
        if (colonyTile.getUnit() != null) {
            unitType = colonyTile.getUnit().getType();
        }
        Set<Modifier> basicModifiers = colonyTile.getProductionModifiers(goodsType, unitType);
        basicModifiers.addAll(colony.getModifierSet(goodsType.getId()));
        Set<Modifier> modifiers = sortModifiers(basicModifiers);
        if (colony.getProductionBonus() != 0) {
            modifiers.add(colony.getProductionModifier(goodsType));
        }

        setLayout(new MigLayout("wrap 3, insets 30 30 10 30", "[]30:push[right][]", ""));

        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        enterPressesWhenFocused(okButton);

        add(new JLabel(colonyTile.getLabel()), "span, align center, wrap 30");

        TileType tileType = colonyTile.getWorkTile().getType();
        int width = canvas.getClient().getImageLibrary().getTerrainImageWidth(tileType);
        int height = canvas.getClient().getImageLibrary().getTerrainImageHeight(tileType);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        canvas.getGUI().displayColonyTile((Graphics2D) image.getGraphics(), colonyTile.getWorkTile().getMap(),
                                          colonyTile.getWorkTile(), 0, 0, colony);
        add(new JLabel(new ImageIcon(image)));
        add(new UnitLabel(colonyTile.getUnit(), parent, false, false), "wrap");

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
            add(new JLabel(sourceName), "newline");
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
            add(new JLabel(bonus));
            if (percentage) {
                add(new JLabel("%"));
            }
        }

        Font bigFont = getFont().deriveFont(Font.BOLD, 16);

        int result = (int) FeatureContainer.applyModifierSet(0, colonyTile.getGame().getTurn(), basicModifiers)
            + colony.getProductionBonus();
        JLabel finalLabel = new JLabel(Messages.message("modifiers.finalResult.name"));
        finalLabel.setFont(bigFont);
        add(finalLabel, "newline");

        JLabel finalResult = new JLabel(getModifierFormat().format(result));
        finalResult.setFont(bigFont);
        finalResult.setBorder(BorderFactory
                              .createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK),
                                                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        add(finalResult, "wrap 30");

        add(okButton, "span, align center");

        setSize(getPreferredSize());

    }

    public void requestFocus() {
        okButton.requestFocus();
    }


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        parent.remove(this);
    }


}


