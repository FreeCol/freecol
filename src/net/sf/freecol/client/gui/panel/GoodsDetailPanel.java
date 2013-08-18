/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays details of goods types in the Colopedia.
 */
public class GoodsDetailPanel extends ColopediaGameObjectTypePanel<GoodsType> {


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colopediaPanel The parent ColopediaPanel.
     */
    public GoodsDetailPanel(FreeColClient freeColClient,
                            ColopediaPanel colopediaPanel) {
        super(freeColClient, colopediaPanel, PanelType.GOODS.toString(), 0.75);
    }


    /**
     * Adds one or several subtrees for all the objects for which this
     * ColopediaDetailPanel could build a detail panel to the given
     * root node.
     *
     * @param root a <code>DefaultMutableTreeNode</code>
     */
    public void addSubTrees(DefaultMutableTreeNode root) {
        super.addSubTrees(root, getSpecification().getGoodsTypeList());
    }

    /**
     * Builds the details panel for the GoodsType with the given identifier.
     *
     * @param id The object identifier.
     * @param panel the detail panel to build
     */
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) {
            return;
        }

        GoodsType type = getSpecification().getGoodsType(id);
        panel.setLayout(new MigLayout("wrap 4", "[]20[]"));

        JLabel name = localizedLabel(type.getNameKey());
        name.setFont(smallHeaderFont);
        panel.add(name, "span, align center, wrap 40");

        if (type.isFarmed()) {
            List<TileImprovementType> improvements = new ArrayList<TileImprovementType>();
            List<Modifier> modifiers = new ArrayList<Modifier>();
            for (TileImprovementType improvementType :
                     getSpecification().getTileImprovementTypeList()) {
                Modifier productionModifier = improvementType.getProductionModifier(type);
                if (productionModifier != null) {
                    improvements.add(improvementType);
                    modifiers.add(productionModifier);
                }
            }

            panel.add(localizedLabel("colopedia.goods.improvedBy"), "newline 20, top");
            if (improvements.size() == 0) {
                panel.add(localizedLabel("none"), "span");
            } else {
                for (int index = 0; index < improvements.size(); index++) {
                    String constraints = (index == 0) ? "span" : "skip, span";
                    panel.add(localizedLabel(StringTemplate.template("colopedia.goods.improvement")
                                                   .addName("%name%", improvements.get(index))
                                                   .addName("%amount%", getModifierAsString(modifiers.get(index)))),
                                    constraints);
                }
            }
        } else {
            panel.add(localizedLabel("colopedia.goods.madeFrom"), "newline 20");
            if (type.isRefined()) {
                panel.add(getGoodsButton(type.getInputType()), "span");
            } else {
                panel.add(localizedLabel("nothing"), "span");
            }
        }

        panel.add(localizedLabel("colopedia.goods.makes"), "newline 20");
        if (type.isRawMaterial()) {
            panel.add(getGoodsButton(type.getOutputType()), "span");
        } else if (type.getStoredAs() != type) {
            panel.add(getGoodsButton(type.getStoredAs()), "span");
        } else {
            panel.add(localizedLabel("nothing"), "span");
        }

        if (type.isBuildingMaterial()) {
            List<BuildingType> buildingTypes = new ArrayList<BuildingType>();
            boolean allTypes = filterBuildables(getSpecification().getBuildingTypeList(), buildingTypes, type);
            if (buildingTypes.size() > 0) {
                panel.add(localizedLabel("colopedia.goods.buildings"), "newline 20");
                if (allTypes) {
                    JButton button = getButton(PanelType.BUILDINGS,
                                               Messages.message("colopedia.goods.allBuildings"),
                                               null);
                    panel.add(button, "span");
                } else {
                    int count = 0;
                    for (BuildingType building : buildingTypes) {
                        JButton label = getButton(building);
                        if (count > 0 && count % 3 == 0) {
                            panel.add(label, "skip");
                        } else {
                            panel.add(label);
                        }
                        count++;
                    }
                }
            }
            List<Role> roles = new ArrayList<Role>();
            allTypes = filterBuildables(getSpecification().getRoles(), roles, type);
            if (!roles.isEmpty()) {
                panel.add(localizedLabel("colopedia.goods.equipment"), "newline 20");
                int count = 0;
                for (Role role : roles) {
                    JLabel label = localizedLabel(role.getId() + ".name");
                    if (count > 0 && count % 3 == 0) {
                        panel.add(label, "skip");
                    } else {
                        panel.add(label);
                    }
                    count++;
                }
            }
            List<UnitType> unitTypes = new ArrayList<UnitType>();
            allTypes = filterBuildables(getSpecification().getUnitTypeList(), unitTypes, type);
            if (unitTypes.size() > 0) {
                panel.add(localizedLabel("colopedia.goods.units"), "newline 20");
                if (allTypes) {
                    JButton button = getButton(PanelType.UNITS,
                                               Messages.message("colopedia.goods.allUnits"),
                                               null);
                    panel.add(button, "span");
                } else {
                    int count = 0;
                    for (UnitType unit : unitTypes) {
                        JButton label = getButton(unit);
                        if (count > 0 && count % 3 == 0) {
                            panel.add(label, "skip");
                        } else {
                            panel.add(label);
                        }
                        count++;
                    }
                }
            }
        }

        if (type.getBreedingNumber() < FreeColObject.INFINITY) {
            panel.add(localizedLabel("colopedia.goods.breedingNumber"), "newline 20");
            panel.add(new JLabel(Integer.toString(type.getBreedingNumber())));
        }

        panel.add(localizedLabel("colopedia.goods.description"), "newline 20");
        panel.add(getDefaultTextArea(Messages.message(type.getDescriptionKey()), 30), "span, growx");
    }


    private <T extends BuildableType> boolean filterBuildables(List<T> input, List<T> output, GoodsType type) {
        boolean result = true;
        loop: for (T buildableType : input) {
            if (buildableType.needsGoodsToBuild()) {
                for (AbstractGoods goods : buildableType.getRequiredGoods()) {
                    if (type == goods.getType()) {
                        output.add(buildableType);
                        continue loop;
                    }
                }
                result = false;
            }
        }
        return result;
    }
}
