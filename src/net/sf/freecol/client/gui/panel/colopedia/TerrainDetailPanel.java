/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui.panel.colopedia;

import static net.sf.freecol.common.util.CollectionUtils.first;
import static net.sf.freecol.common.util.CollectionUtils.transform;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.ModifierFormat;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.util.ImageUtils;


/**
 * This panel displays details of terrain types in the Colopedia.
 */
public class TerrainDetailPanel
    extends ColopediaGameObjectTypePanel<TileType> {

    /**
     * Creates a new instance of this TerrainDetailPanel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colopediaPanel The parent {@code ColopediaPanel}.
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
            final Dimension size = getListItemIconSize();
            final BufferedImage tileImage = getImageLibrary().getTileImageWithOverlayAndForest(t, new Dimension(-1, size.height));
            final ImageIcon icon = new ImageIcon(ImageUtils.createCenteredImage(tileImage, size));
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
        Modifier defenceModifier = first(tileType.getDefenceModifiers());
        if (defenceModifier != null) {
            defenseBonus = ModifierFormat.getModifierAsString(defenceModifier);
        }

        JLabel nameLabel = Utility.localizedHeaderLabel(tileType,
            Utility.FONTSPEC_SUBTITLE);
        panel.add(nameLabel, "span, align center");

        panel.add(Utility.localizedLabel("colopedia.terrain.terrainImage"), "spany 3");
        Image terrainImage = getImageLibrary()
            .getTileImageWithOverlayAndForest(tileType,
                ImageLibrary.TILE_OVERLAY_SIZE);
        panel.add(new JLabel(new ImageIcon(terrainImage)), "spany 3");

        List<ResourceType> resourceList = tileType.getResourceTypeValues();
        ResourceType rt = first(resourceList);
        if (rt != null) {
            panel.add(Utility.localizedLabel("colopedia.terrain.resource"));
            if (resourceList.size() > 1) {
                panel.add(getResourceButton(rt), "split " + resourceList.size());
                for (int index = 1; index < resourceList.size(); index++) {
                    panel.add(getResourceButton(resourceList.get(index)));
                }
            } else {
                panel.add(getResourceButton(rt));
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

    private void addProduction(JPanel panel, Stream<AbstractGoods> production) {
        // Positive production only
        List<AbstractGoods> pro = transform(production, AbstractGoods::isPositive);
        String tag = null;
        switch (pro.size()) {
        case 0:
            panel.add(new JLabel(), "wrap");
            break;
        case 1:
            tag = "span";
            break;
        default:
            tag = "span, split " + pro.size();
            break;
        }
        for (AbstractGoods ag : pro) {
            panel.add(getGoodsButton(ag.getType(), ag.getAmount()), tag);
            tag = null;
        }
    }
}
