/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TileImprovementType;
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
        super(freeColClient, colopediaPanel, PanelType.GOODS.getKey());
    }


    // Implement ColopediaDetailPanel

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSubTrees(DefaultMutableTreeNode root) {
        super.addSubTrees(root, getSpecification().getGoodsTypeList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) return;

        GoodsType type = getSpecification().getGoodsType(id);
        panel.setLayout(new MigLayout("wrap 4", "[]20[]"));

        JLabel name = Utility.localizedHeaderLabel(type, FontLibrary.FontSize.SMALL);
        panel.add(name, "span, align center, wrap 40");

        if (type.isFarmed()) {
            List<TileImprovementType> improvements = new ArrayList<>();
            List<Modifier> modifiers = new ArrayList<>();
            for (TileImprovementType improvementType :
                     getSpecification().getTileImprovementTypeList()) {
                Modifier productionModifier = improvementType.getProductionModifier(type);
                if (productionModifier != null) {
                    improvements.add(improvementType);
                    modifiers.add(productionModifier);
                }
            }

            panel.add(Utility.localizedLabel("colopedia.goods.improvedBy"),
                      "newline 20, top");
            if (improvements.isEmpty()) {
                panel.add(Utility.localizedLabel("none"), "span");
            } else {
                for (int index = 0; index < improvements.size(); index++) {
                    String constraints = (index == 0) ? "span" : "skip, span";
                    panel.add(Utility.localizedLabel(StringTemplate
                            .template("colopedia.goods.improvement")
                            .addName("%name%", improvements.get(index))
                            .addName("%amount%", ModifierFormat.getModifierAsString(modifiers.get(index)))),
                        constraints);
                }
            }
        } else {
            panel.add(Utility.localizedLabel("colopedia.goods.madeFrom"), "newline 20");
            if (type.isRefined()) {
                panel.add(getGoodsButton(type.getInputType()), "span");
            } else {
                panel.add(Utility.localizedLabel("nothing"), "span");
            }
        }

        panel.add(Utility.localizedLabel("colopedia.goods.makes"), "newline 20");
        if (type.isRawMaterial()) {
            panel.add(getGoodsButton(type.getOutputType()), "span");
        } else if (type.getStoredAs() != type) {
            panel.add(getGoodsButton(type.getStoredAs()), "span");
        } else {
            panel.add(Utility.localizedLabel("nothing"), "span");
        }

        if (type.isBuildingMaterial()) {
            List<BuildingType> buildingTypes = new ArrayList<>();
            boolean allTypes = filterBuildables(getSpecification().getBuildingTypeList(), buildingTypes, type);
            if (!buildingTypes.isEmpty()) {
                panel.add(Utility.localizedLabel("colopedia.goods.buildings"),
                          "newline 20");
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
            List<Role> roles = new ArrayList<>();
            allTypes = filterBuildables(getSpecification().getRoles(), roles, type);
            if (!roles.isEmpty()) {
                panel.add(Utility.localizedLabel("colopedia.goods.equipment"),
                          "newline 20");
                int count = 0;
                for (Role role : roles) {
                    JLabel label = Utility.localizedLabel(Messages.getName(role));
                    if (count > 0 && count % 3 == 0) {
                        panel.add(label, "skip");
                    } else {
                        panel.add(label);
                    }
                    count++;
                }
            }
            List<UnitType> unitTypes = new ArrayList<>();
            allTypes = filterBuildables(getSpecification().getUnitTypeList(), unitTypes, type);
            if (!unitTypes.isEmpty()) {
                panel.add(Utility.localizedLabel("colopedia.goods.units"),
                          "newline 20");
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
            panel.add(Utility.localizedLabel("colopedia.goods.breedingNumber"),
                      "newline 20");
            panel.add(new JLabel(Integer.toString(type.getBreedingNumber())));
        }

        panel.add(Utility.localizedLabel("colopedia.goods.description"),
                  "newline 20");
        panel.add(Utility.localizedTextArea(Messages.descriptionKey(type), 30),
                  "span, growx");
    }


    private <T extends BuildableType> boolean filterBuildables(List<T> input,
        List<T> output, GoodsType type) {
        boolean result = true;
        for (T bt : input) {
            if (bt.needsGoodsToBuild()) {
                if (AbstractGoods.containsType(type, bt.getRequiredGoods())) {
                    output.add(bt);
                } else {
                    result = false;
                }
            }
        }
        return result;
    }
}
