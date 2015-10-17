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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.TileType;


/**
 * This panel displays details of terrain types in the Colopedia.
 */
public class TerrainDetailPanel
    extends ColopediaGameObjectTypePanel<TileType> {

    /**
     * Creates a new instance of this TerrainDetailPanel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colopediaPanel The parent <code>ColopediaPanel</code>.
     */
    public TerrainDetailPanel(FreeColClient freeColClient,
                              ColopediaPanel colopediaPanel) {
        super(freeColClient, colopediaPanel, PanelType.TERRAIN.getKey());
    }


    // Implement ColopediaDetailPanel

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSubTrees(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode node
            = new DefaultMutableTreeNode(new ColopediaTreeItem(this, getId(),
                                         getName(), null));
        for (TileType t : getSpecification().getTileTypeList()) {
            Image tile = SwingGUI.createTileImageWithOverlayAndForest(t,
                new Dimension(-1, ImageLibrary.ICON_SIZE.height));
            BufferedImage image = new BufferedImage(tile.getWidth(null),
                ImageLibrary.ICON_SIZE.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(tile, 0, (ImageLibrary.ICON_SIZE.height - tile.getHeight(null)) / 2, null);
            g.dispose();
            ImageIcon icon = new ImageIcon(image);
            node.add(buildItem(t, icon));
        }
        root.add(node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) return;

        TileType tileType = getSpecification().getTileType(id);
        panel.setLayout(new MigLayout("wrap 4, gap 20"));

        String movementCost = String.valueOf(tileType.getBasicMoveCost() / 3);
        String defenseBonus = Messages.message("none");
        Set<Modifier> defenceModifiers = tileType.getDefenceModifiers();
        if (!defenceModifiers.isEmpty()) {
            defenseBonus = ModifierFormat.getModifierAsString(defenceModifiers.iterator().next());
        }

        JLabel nameLabel = Utility.localizedHeaderLabel(tileType, FontLibrary.FontSize.SMALL);
        panel.add(nameLabel, "span, align center");

        panel.add(Utility.localizedLabel("colopedia.terrain.terrainImage"), "spany 3");
        Image terrainImage = SwingGUI.createTileImageWithOverlayAndForest(
            tileType, ImageLibrary.TILE_OVERLAY_SIZE);
        panel.add(new JLabel(new ImageIcon(terrainImage)), "spany 3");

        List<ResourceType> resourceList = tileType.getResourceTypes();
        if (!resourceList.isEmpty()) {
            panel.add(Utility.localizedLabel("colopedia.terrain.resource"));
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

        panel.add(Utility.localizedLabel("colopedia.terrain.movementCost"));
        panel.add(new JLabel(movementCost));

        panel.add(Utility.localizedLabel("colopedia.terrain.defenseBonus"));
        panel.add(new JLabel(defenseBonus));

        panel.add(Utility.localizedLabel("colopedia.terrain.unattendedProduction"));
        addProduction(panel, tileType.getPossibleProduction(true));

        panel.add(Utility.localizedLabel("colopedia.terrain.colonistProduction"));
        addProduction(panel, tileType.getPossibleProduction(false));

        panel.add(Utility.localizedLabel("colopedia.terrain.description"));
        panel.add(Utility.localizedTextArea(Messages.descriptionKey(tileType)),
                  "span, growx");
    }

    private void addProduction(JPanel panel, List<AbstractGoods> production) {
        if (production.isEmpty()) {
            panel.add(new JLabel(), "wrap");
        } else {
            // Drop the zero amount production (which need resources to work)
            Iterator<AbstractGoods> it = production.iterator();
            while (it.hasNext()) {
                AbstractGoods ag = it.next();
                if (ag.getAmount() <= 0) it.remove();
            }

            AbstractGoods ag = production.get(0);
            if (production.size() > 1) {
                panel.add(getGoodsButton(ag.getType(), ag.getAmount()),
                          "span, split " + production.size());
                for (int index = 1; index < production.size(); index++) {
                    ag = production.get(index);
                    panel.add(getGoodsButton(ag.getType(), ag.getAmount()));
                }
            } else {
                panel.add(getGoodsButton(ag.getType(), ag.getAmount()), "span");
            }
        }
    }
}
