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

import java.awt.Image;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.TileType;


/**
 * This panel displays details of terrain types in the Colopedia.
 */
public class TerrainDetailPanel extends ColopediaGameObjectTypePanel<TileType> {


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colopediaPanel The parent ColopediaPanel.
     */
    public TerrainDetailPanel(FreeColClient freeColClient,
                              ColopediaPanel colopediaPanel) {
        super(freeColClient, colopediaPanel,
              PanelType.TERRAIN.toString(), 0.25);
    }


    /**
     * Adds one or several subtrees for all the objects for which this
     * ColopediaDetailPanel could build a detail panel to the given
     * root node.
     *
     * @param root a <code>DefaultMutableTreeNode</code>
     */
    public void addSubTrees(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode node =
            new DefaultMutableTreeNode(new ColopediaTreeItem(this, getId(), getName(), null));
        for (TileType t : getSpecification().getTileTypeList()) {
            ImageIcon icon = new ImageIcon(getLibrary().getCompoundTerrainImage(t, getScale()));
            node.add(buildItem(t, icon));
        }
        root.add(node);
    }

    /**
     * Builds the details panel for the TileType with the given identifier.
     *
     * @param id The object identifier.
     * @param panel the detail panel to build
     */
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) {
            return;
        }

        TileType tileType = getSpecification().getTileType(id);
        panel.setLayout(new MigLayout("wrap 4, gap 20"));

        String movementCost = String.valueOf(tileType.getBasicMoveCost() / 3);
        String defenseBonus = Messages.message("none");
        Set<Modifier> defenceModifiers = tileType.getDefenceBonus();
        if (!defenceModifiers.isEmpty()) {
            defenseBonus = ModifierFormat.getModifierAsString(defenceModifiers.iterator().next());
        }

        JLabel nameLabel = localizedLabel(tileType.getNameKey());
        nameLabel.setFont(GUI.SMALL_HEADER_FONT);
        panel.add(nameLabel, "span, align center");

        panel.add(localizedLabel("colopedia.terrain.terrainImage"), "spany 3");
        Image terrainImage = getLibrary().getCompoundTerrainImage(tileType, 1);
        panel.add(new JLabel(new ImageIcon(terrainImage)), "spany 3");

        List<ResourceType> resourceList = tileType.getResourceTypes();
        if (resourceList.size() > 0) {
            panel.add(localizedLabel("colopedia.terrain.resource"));
            if (resourceList.size() > 1) {
                panel.add(getResourceButton(resourceList.get(0)), "split " + resourceList.size());
                for (int index = 1; index < resourceList.size(); index++) {
                    panel.add(getResourceButton(resourceList.get(index)));
                }
            } else {
                panel.add(getResourceButton(resourceList.get(0)));
            }
        } else {
            panel.add(new JLabel(), "wrap");
        }

        panel.add(localizedLabel("colopedia.terrain.movementCost"));
        panel.add(new JLabel(movementCost));

        panel.add(localizedLabel("colopedia.terrain.defenseBonus"));
        panel.add(new JLabel(defenseBonus));

        panel.add(localizedLabel("colopedia.terrain.production"));

        List<AbstractGoods> production = tileType.getProduction();
        if (production.size() > 0) {
            AbstractGoods goods = production.get(0);
            if (production.size() > 1) {
                panel.add(getGoodsButton(goods.getType(), goods.getAmount()),
                                "span, split " + production.size());
                for (int index = 1; index < production.size(); index++) {
                    goods = production.get(index);
                    panel.add(getGoodsButton(goods.getType(), goods.getAmount()));
                }
            } else {
                panel.add(getGoodsButton(goods.getType(), goods.getAmount()), "span");
            }
        } else {
            panel.add(new JLabel(), "wrap");
        }

        panel.add(localizedLabel("colopedia.terrain.description"));
        panel.add(GUI.getDefaultTextArea(Messages.message(tileType.getDescriptionKey()), 20),
                  "span, growx");
    }

}
