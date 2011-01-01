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
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;

/**
 * This panel represents a single building in a Colony.
 */
public class BuildingToolTip extends JToolTip {

    private static final Font arrowFont = ResourceManager.getFont("SimpleFont", Font.BOLD, 24f);

    /**
     * Creates this BuildingToolTip.
     * 
     * @param building The building to display information from.
     * @param parent a <code>Canvas</code> value
     */
    public BuildingToolTip(Building building, Canvas parent) {

        setLayout(new MigLayout("fill", "", ""));

        String buildingName = Messages.message(building.getNameKey());
        if (building.getMaxUnits() == 0) {
            buildingName = "(" + buildingName + ")";
        }

        boolean canTeach = building.getType().hasAbility("model.ability.teach");

        add(new JLabel(buildingName), "span, align center");

        if (building.getProductionNextTurn() == 0) {
            add(new JLabel(), "span");
        } else {
            ProductionLabel productionOutput = new ProductionLabel(building.getGoodsOutputType(),
                                                                   building.getProductionNextTurn(),
                                                                   building.getMaximumProduction(), parent);
            if (building.getGoodsInputNextTurn() == 0) {
                add(productionOutput, "span, align center");
            } else {
                ProductionLabel productionInput = new ProductionLabel(building.getGoodsInputType(),
                                                                      building.getGoodsInputNextTurn(),
                                                                      building.getMaximumGoodsInput(), parent);
                JLabel arrow = new JLabel("\u2192");
                arrow.setFont(arrowFont);
                add(productionInput, "span, split 3, align center");
                add(arrow);
                add(productionOutput);
            }
        }

        add(new JLabel(new ImageIcon(ResourceManager.getImage(building.getType().getId() + ".image"))));

        for (Unit unit : building.getUnitList()) {
            UnitLabel unitLabel = new UnitLabel(unit, parent, false);
            if (canTeach && unit.getStudent() != null) {
                JLabel progress = new JLabel(unit.getTurnsOfTraining() + "/" +
                                             unit.getNeededTurnsOfTraining());
                progress.setBackground(Color.WHITE);
                progress.setOpaque(true);
                UnitLabel studentLabel = new UnitLabel(unit.getStudent(), parent, true);
                studentLabel.setIgnoreLocation(true);
                add(unitLabel);
                add(progress, "split 2, flowy");
                add(studentLabel);
            } else  {
                add(unitLabel, "span 2");
            }
        }

    }

    public Dimension getPreferredSize() {
        return new Dimension(400, 200);
    }
}


