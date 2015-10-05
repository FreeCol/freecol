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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;


/**
 * The ProductionLabel represents Goods that are produced in a
 * WorkLocation or Settlement. It is similar to the GoodsLabel.
 */
public final class ProductionLabel extends AbstractGoodsLabel {

    private static final Logger logger = Logger.getLogger(ProductionLabel.class.getName());

    /** The maximum number of goodsIcons to display. */
    private int maxIcons = 7;

    /** Whether to display positive integers with a "+" sign. */
    private boolean drawPlus = false;

    /** Whether the ProductionLabel should be centered. */
    private boolean centered = true;

    /** The compressed width of the ProductionLabel. */
    private int compressedWidth = -1;

    /** The goodsIcon for this type of production. */
    private ImageIcon goodsIcon;

    /** The amount of goods that could be produced. */
    private int maximumProduction = -1;

    /** The smallest number to display above the goodsIcons. */
    private int displayNumber;

    /**
     * The smallest number to display above the goodsIcons.
     * used to show stored items in ReportColonyPanel
     */
    private int stockNumber = -1;

    /** The image to display. */
    private Image stringImage = null;


    /**
     * Creates a new production label.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param ag The <code>AbstractGoods</code> to create a label for.
     */
    public ProductionLabel(FreeColClient freeColClient, AbstractGoods ag) {
        this(freeColClient, ag, -1);
    }

    /**
     * Creates a new production label.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param ag The <code>AbstractGoods</code> to create a label for.
     * @param maximumProduction The maximum production.
     */
    public ProductionLabel(FreeColClient freeColClient, AbstractGoods ag,
                           int maximumProduction) {
        this(freeColClient, ag, maximumProduction, -1);
    }

    /**
     * Creates a new production label.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param ag The <code>AbstractGoods</code> to create a label for.
     * @param maximumProduction The maximum production.
     * @param stockNumber The lower bound on number of items to display.
     */
    public ProductionLabel(FreeColClient freeColClient, AbstractGoods ag,
                           int maximumProduction, int stockNumber) {
        this(freeColClient,freeColClient.getGUI().getImageLibrary(), ag,
            maximumProduction, stockNumber);
    }

    /**
     * Creates a new production label.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param lib The <code>ImageLibrary</code> to use for the label.
     * @param ag The <code>AbstractGoods</code> to create a label for.
     */
    public ProductionLabel(FreeColClient freeColClient, ImageLibrary lib,
                           AbstractGoods ag) {
        this(freeColClient, lib, ag, -1, -1);
    }

    /**
     * Creates a new production label.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param lib The <code>ImageLibrary</code> to use for the label.
     * @param ag The <code>AbstractGoods</code> to create a label for.
     * @param maximumProduction The maximum production.
     * @param stockNumber The lower bound on number of items to display.
     */
    public ProductionLabel(FreeColClient freeColClient, ImageLibrary lib,
                           AbstractGoods ag,
                           int maximumProduction, int stockNumber) {
        super(lib, ag);

        if (getType() == null) {
            logger.warning("Bad production label (no type)\n"
                + FreeColDebugger.stackTraceToString());
        } else if (getAmount() == 0 && stockNumber < 0) {
            logger.warning("Bad production label: " + ag
                + " stock=" + stockNumber
                + "\n" + FreeColDebugger.stackTraceToString());
        }

        this.maximumProduction = maximumProduction;
        this.stockNumber = stockNumber;
        final ClientOptions options = freeColClient.getClientOptions();
        // Horses stack poorly, only show one icon
        // TODO: make this highly specific hack more generic
        final GoodsType horses = freeColClient.getGame().getSpecification()
            .getGoodsType("model.goods.horses");
        this.maxIcons = (ag.getType() == horses) ? 1
            : options.getInteger(ClientOptions.MAX_NUMBER_OF_GOODS_IMAGES);
        this.displayNumber = options
            .getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT);
        this.goodsIcon = (ag.getType() == null) ? null
            : new ImageIcon(lib.getIconImage(ag.getType()));
        this.compressedWidth = (this.goodsIcon == null) ? 0
            : this.goodsIcon.getIconWidth() * 2;

        setFont(FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
            FontLibrary.FontSize.TINY, Font.BOLD, lib.getScaleFactor()));
        setForeground((getAmount() < 0) ? Color.RED : Color.WHITE);
        setToolTipText((getType() == null || getAmount() == 0) ? null
            : Messages.message(getAbstractGoods().getLabel()));

        final int amount = getAmount();
        boolean showMax = amount > 0 && maximumProduction > amount;
        if (amount < 0 || amount >= displayNumber || amount > maxIcons
            || stockNumber > 0 || showMax) {
            String number = "";
            if (stockNumber >= 0) { // Show stored items in ReportColonyPanel
                number = String.valueOf(stockNumber);
                drawPlus = true;
            }
            if (amount >= 0 && drawPlus) number += "+";
            number += String.valueOf(amount);
            if (showMax) number += "/" + String.valueOf(maximumProduction);
            
            BufferedImage dummy = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dummy.createGraphics();
            this.stringImage = lib.getStringImage(g, number,
                getForeground(), getFont());
            g.dispose();
        } else {
            this.stringImage = null;
        }
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        int stringWidth = (this.stringImage == null) ? 0
            : stringImage.getWidth(null);
        int drawImageCount = Math.min(Math.abs(getAmount()), this.maxIcons);
        if (drawImageCount == 0) drawImageCount = 1;
        int iconWidth = this.goodsIcon.getIconWidth();
        int pixelsPerIcon = iconWidth / 2;
        if (pixelsPerIcon - iconWidth < 0) {
            pixelsPerIcon = (compressedWidth - iconWidth) / drawImageCount;
        }
        int maxSpacing = iconWidth;

        // FIXME: Tune this: all icons are the same width, but many do
        // not take up the whole width, eg. bells
        boolean iconsTooFarApart = pixelsPerIcon > maxSpacing;
        if (iconsTooFarApart) pixelsPerIcon = maxSpacing;
        int coverage = pixelsPerIcon * (drawImageCount - 1) + iconWidth;
        int leftOffset = 0;
        int width = Math.max(getWidth(), Math.max(stringWidth, coverage));
        if (centered && coverage < width) leftOffset = (width - coverage)/2;
        int height = Math.max(getHeight(),
                              this.goodsIcon.getImage().getHeight(null));
        setSize(new Dimension(width, height));

        // Draw the icons onto the image:
        for (int i = 0; i < drawImageCount; i++) {
            this.goodsIcon.paintIcon(null, g, leftOffset + i*pixelsPerIcon, 0);
        }

        if (this.stringImage != null) {
            int textOffset = (width > stringWidth) ? (width - stringWidth)/2
                : 0;
            textOffset = (textOffset >= 0) ? textOffset : 0;
            g.drawImage(this.stringImage, textOffset,
                this.goodsIcon.getIconHeight()/2 - this.stringImage.getHeight(null)/2,
                null);
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        if (this.goodsIcon == null) return new Dimension(0, 0);
       
        int drawImageCount = Math.max(1, Math.min(Math.abs(getAmount()),
                                                  this.maxIcons));
        int iconWidth = this.goodsIcon.getIconWidth();
        int pixelsPerIcon = iconWidth / 2;
        if (pixelsPerIcon - iconWidth < 0) {
            pixelsPerIcon = (compressedWidth - iconWidth) / drawImageCount;
        }
        int maxSpacing = iconWidth;

        // FIXME: Tune this: all icons are the same width, but many do
        // not take up the whole width, eg. bells
        boolean iconsTooFarApart = pixelsPerIcon > maxSpacing;
        if (iconsTooFarApart) pixelsPerIcon = maxSpacing;
        int width = pixelsPerIcon * (drawImageCount - 1) + iconWidth;
        if (this.stringImage != null) {
            width = Math.max(this.stringImage.getWidth(null), width);
        }
        return new Dimension(width, goodsIcon.getImage().getHeight(null));
    }
}
