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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;

import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Unit;

import net.miginfocom.swing.MigLayout;

public class BuildingProductionPanel extends FreeColPanel implements ActionListener {

    private final JButton okButton = new JButton(Messages.message("ok"));

    public BuildingProductionPanel(Canvas canvas, Unit unit) {

        super(canvas);

        Colony colony = unit.getColony();
        Building building = unit.getWorkLocation();
        GoodsType goodsType = building.getGoodsOutputType();
        Set<Modifier> basicModifiers = building.getProductivityModifiers(unit);
        basicModifiers.addAll(unit.getColony().getModifierSet(goodsType.getId()));
        Set<Modifier> modifiers = sortModifiers(basicModifiers);
        if (colony.getProductionBonus() != 0) {
            modifiers.add(colony.getProductionModifier(goodsType));
        }

        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        enterPressesWhenFocused(okButton);

        setLayout(new MigLayout("", "[]20[align right][]", ""));

        add(new JLabel(building.getName()), "span, align center");

        // TODO: make this a building image
        add(new UnitLabel(unit, canvas, false, true), "newline");
        add(new JLabel(canvas.getGUI().getImageLibrary().getGoodsImageIcon(goodsType)));

        for (Modifier modifier : modifiers) {
            FreeColGameObjectType source = modifier.getSource();
            String sourceName;
            if (source == null) {
                sourceName = "???";
            } else {
                sourceName = source.getName();
            }
            add(new JLabel(sourceName), "newline");
            boolean percent = false;
            String bonus = getModifierFormat().format(modifier.getValue());
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
                percent = true;
                break;
            case MULTIPLICATIVE:
                bonus = "\u00D7" + bonus;
                break;
            default:
            }
            add(new JLabel(bonus));
            if (percent) {
                add(new JLabel("%"));
            }
        }

        Font bigFont = getFont().deriveFont(Font.BOLD, 20f);

        int result = (int) FeatureContainer.applyModifierSet(0, building.getGame().getTurn(), basicModifiers) +
            colony.getProductionBonus();
        JLabel finalLabel = new JLabel(Messages.message("modifiers.finalResult.name"));
        finalLabel.setFont(bigFont);
        add(new JSeparator(JSeparator.HORIZONTAL), "newline, span, growx");
        add(finalLabel);
        JLabel finalResult = new JLabel(getModifierFormat().format(result));
        finalResult.setFont(bigFont);
        add(finalResult);

        add(okButton, "newline 20, span, tag ok");
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
        getCanvas().remove(this);
    }

}