/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Panel for displaying {@code Tile}-information.
 */
public final class TileInfoPanel extends FreeColPanel {

    private static final int SLACK = 5; // Small gap
    private static final int PRODUCTION = 4;
    
    private Tile tile;

    // TODO: Find a way of removing the need for an extremely tiny font.
    //private final Font font = new JLabel().getFont().deriveFont(8f);

    /**
     * Create a {@code TileInfoPanel}.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public TileInfoPanel(FreeColClient freeColClient) {
        super(freeColClient, null,
              new MigLayout("fill, wrap " + (PRODUCTION+1) + ", gap 1 1",
                            "", ""));

        setSize(260, 130);
        setBorder(null);
        setOpaque(false);
        this.tile = null;
    }


    /**
     * Get the tile displayed.
     *
     * @return The <code>Tile</code>.
     */
    public Tile getTile() {
        return this.tile;
    }

    /**
     * Updates this {@code InfoPanel}.
     *
     * @param tile The displayed tile (or null if none)
     */
    public void update(Tile tile) {
        if (this.tile != tile) {
            this.tile = tile;
            update();
        }
    }

    /**
     * Update this panel unconditionally.
     */
    private void update() {
        removeAll();

        if (this.tile != null) {
            final ImageLibrary lib = getGUI().getTileImageLibrary();
            final Font font = FontLibrary
                .createFont(FontLibrary.FontType.NORMAL,
                    FontLibrary.FontSize.TINY, lib.getScaleFactor());
            BufferedImage image = getGUI()
                .createTileImageWithBeachBorderAndItems(this.tile);
            if (this.tile.isExplored()) {
                final int width = getWidth() - SLACK;
                String text = Messages.message(this.tile.getLabel());
                for (String s : splitText(text, " /",
                                          getFontMetrics(font), width)) {
                    JLabel label = new JLabel(s);
                    //itemLabel.setFont(font);
                    add(label, "span, align center");
                }
                add(new JLabel(new ImageIcon(image)), "spany");
                final Player owner = this.tile.getOwner();
                if (owner == null) {
                    add(new JLabel(), "span " + PRODUCTION);
                } else {
                    StringTemplate t = owner.getNationLabel();
                    add(Utility.localizedLabel(t), "span " + PRODUCTION);
                }

                JLabel defenceLabel = Utility.localizedLabel(StringTemplate
                    .template("infoPanel.defenseBonus")
                    .addAmount("%bonus%", this.tile.getDefenceBonusPercentage()));
                //defenceLabel.setFont(font);
                add(defenceLabel, "span " + PRODUCTION);

                JLabel moveLabel = Utility.localizedLabel(StringTemplate
                    .template("infoPanel.movementCost")
                    .addAmount("%cost%", this.tile.getType().getBasicMoveCost()/3));
                //moveLabel.setFont(font);
                add(moveLabel, "span " + PRODUCTION);

                List<AbstractGoods> produce
                    = sort(this.tile.getType().getPossibleProduction(true),
                           AbstractGoods.descendingAmountComparator);
                if (produce.isEmpty()) {
                    add(new JLabel(), "span " + PRODUCTION);
                } else {
                    for (AbstractGoods ag : produce) {
                        GoodsType type = ag.getType();
                        int n = this.tile.getPotentialProduction(type, null);
                        JLabel label = new JLabel(String.valueOf(n),
                            new ImageIcon(lib.getSmallGoodsTypeImage(type)),
                            JLabel.RIGHT);
                        label.setToolTipText(Messages.getName(type));
                        label.setFont(font);
                        add(label);
                    }
                }
            } else {
                add(Utility.localizedLabel("unexplored"),
                    "span, align center");
                add(new JLabel(new ImageIcon(image)), "spany");
            }
        }
        revalidate();
        repaint();
    }
}
