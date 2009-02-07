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
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.Autoscroll;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;

/**
 * This panel represents a single building in a Colony.
 */
public class BuildingPanel extends JPanel {

    private static final Font arrowFont = new Font("Dialog", Font.BOLD, 24);

    private final Canvas parent;

    private final Building building;
    private final boolean canTeach;
    private String buildingName;

    private ProductionLabel productionInput = null;
    private ProductionLabel productionOutput = null;;

    private List<UnitLabel> unitLabels = new ArrayList<UnitLabel>();

    /**
     * Creates this ASingleBuildingPanel.
     * 
     * @param building The building to display information from.
     */
    public BuildingPanel(Building building, Canvas parent) {

        this.building = building;
        this.parent = parent;

        setBackground(Color.WHITE);
        setLayout(new MigLayout("fill", "", "[][]push[]"));

        this.buildingName = building.getName();
        if (building.getMaxUnits() == 0) {
            buildingName = "(" + building.getName() + ")";
        }

        canTeach = building.getType().hasAbility("model.ability.teach");

        initialize();
    }

    public void initialize() {
   

        removeAll();
        unitLabels.clear();

        add(new JLabel(buildingName), "span, align center");

        if (building.getProductionNextTurn() == 0) {
            add(new JLabel(), "span");
        } else {
            productionOutput = new ProductionLabel(building.getGoodsOutputType(),
                                                   building.getProductionNextTurn(),
                                                   building.getMaximumProduction(), parent);
            if (building.getGoodsInputNextTurn() == 0) {
                add(productionOutput, "span, align center");
            } else {
                productionInput = new ProductionLabel(building.getGoodsInputType(),
                                                      building.getGoodsInputNextTurn(),
                                                      building.getMaximumGoodsInput(), parent);
                JLabel arrow = new JLabel("\u2192");
                arrow.setFont(arrowFont);
                add(productionInput, "span, split 3, align center");
                add(arrow);
                add(productionOutput);
            }
        }

        for (Unit unit : building.getUnitList()) {
            UnitLabel unitLabel = new UnitLabel(unit, parent, false);
            unitLabels.add(unitLabel);
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

        setSize(getPreferredSize());
    }

    /**
     * Paints this component.
     * 
     * @param g The graphics context in which to paint.
     */
    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        Image bgImage = ResourceManager.getImage(building.getType().getId() + ".image");
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, this);
        } else {
            Image tempImage = ResourceManager.getImage("BackgroundImage");

            if (tempImage != null) {
                for (int x = 0; x < width; x += tempImage.getWidth(null)) {
                    for (int y = 0; y < height; y += tempImage.getHeight(null)) {
                        g.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, width, height);
            }
        }
    }

    public Building getBuilding() {
        return building;
    }

    public void updateProductionLabel() {
        initialize();
    }

    public List<UnitLabel> getUnitLabels() {
        return unitLabels;
    }
}


