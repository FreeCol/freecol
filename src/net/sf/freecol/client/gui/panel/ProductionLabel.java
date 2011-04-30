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
import java.awt.Graphics;
import java.awt.Image;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The ProductionLabel represents Goods that are produced in a
 * WorkLocation or Settlement. It is similar to the GoodsLabel.
 */
public final class ProductionLabel extends JComponent {

    private static Logger logger = Logger.getLogger(ProductionLabel.class.getName());

    private final Canvas parent;

    /**
     * The maximum number of goodsIcons to display.
     */
    private int maxIcons = 7;

    /**
     * Whether to display positive integers with a "+" sign.
     */
    private boolean drawPlus = false;

    /**
     * Whether the ProductionLabel should be centered.
     */
    private boolean centered = true;

    /**
     * The compressed width of the ProductionLabel.
     */
    private int compressedWidth = -1;

    /**
     * The type of goods being produced.
     */
    private GoodsType goodsType;

    /**
     * The goodsIcon for this type of production.
     */
    private ImageIcon goodsIcon;

    /**
     * The amount of goods being produced.
     */
    private int production;

    /**
     * The amount of goods that could be produced.
     */
    private int maximumProduction = -1;

    /**
     * The smallest number to display above the goodsIcons.
     */
    private int displayNumber;

    /**
     * The smallest number to display above the goodsIcons.
     * used to Show stored items in ReportColonyPanel
     */
    private int stockNumber = -1;

    /**
     * Describe toolTipPrefix here.
     */
    private String toolTipPrefix = null;

    private Image stringImage = null;


    /**
     * Creates a new <code>ProductionLabel</code> instance.
     *
     * @param goods a <code>AbstractGoods</code> value
     * @param parent a <code>Canvas</code> value
     */
    public ProductionLabel(AbstractGoods goods, Canvas parent) {
        this(goods.getType(), goods.getAmount(), -1, parent);
    }

    /**
     * Creates a new <code>ProductionLabel</code> instance.
     *
     * @param goods a <code>AbstractGoods</code> value
     * @param maximum a <code>AbstractGoods</code> value
     * @param parent a <code>Canvas</code> value
     */
    public ProductionLabel(AbstractGoods goods, AbstractGoods maximum, Canvas parent) {
        this(goods.getType(), goods.getAmount(), maximum.getAmount(), parent);
    }

    /**
     * Creates a new <code>ProductionLabel</code> instance.
     *
     * @param goodsType an <code>int</code> value
     * @param amount an <code>int</code> value
     * @param parent a <code>Canvas</code> value
     */
    public ProductionLabel(GoodsType goodsType, int amount, Canvas parent) {
        this(goodsType, amount, -1, parent);
    }

    /**
     * Creates a new <code>ProductionLabel</code> instance.
     *
     * @param goodsType an <code>int</code> value
     * @param amount an <code>int</code> value
     * @param maximumProduction an <code>int</code> value
     * @param parent a <code>Canvas</code> value
     */
    public ProductionLabel(GoodsType goodsType, int amount, int maximumProduction, Canvas parent) {
        super();
        this.parent = parent;
        this.production = amount;
        this.goodsType = goodsType;
        this.maximumProduction = maximumProduction;
        ClientOptions options = parent.getClient().getClientOptions();
        maxIcons = options.getInteger(ClientOptions.MAX_NUMBER_OF_GOODS_IMAGES);
        displayNumber = options.getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT);


        setFont(ResourceManager.getFont("SimpleFont", Font.BOLD, 12f));
        if (amount < 0) {
            setForeground(Color.RED);
        } else {
            setForeground(Color.WHITE);
        }
        if (goodsType != null) {
            setGoodsIcon(parent.getImageLibrary().getGoodsImageIcon(goodsType));
            updateToolTipText();
        }
    }

    /**
     * Get the <code>ToolTipPrefix</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getToolTipPrefix() {
        return toolTipPrefix;
    }

    /**
     * Set the <code>ToolTipPrefix</code> value.
     *
     * @param newToolTipPrefix The new ToolTipPrefix value.
     */
    public void setToolTipPrefix(final String newToolTipPrefix) {
        this.toolTipPrefix = newToolTipPrefix;
        updateToolTipText();
    }

    /**
     * Returns the parent Canvas object.
     *
     * @return This ProductionLabel's Canvas.
     */
    public Canvas getCanvas() {
        return parent;
    }

    /**
     * Get the <code>DisplayNumber</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getDisplayNumber() {
        return displayNumber;
    }

    /**
     * Set the <code>DisplayNumber</code> value.
     *
     * @param newDisplayNumber The new DisplayNumber value.
     */
    public void setDisplayNumber(final int newDisplayNumber) {
        this.displayNumber = newDisplayNumber;
    }

    /**
     * Get the <code>GoodsIcon</code> value.
     *
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getGoodsIcon() {
        return goodsIcon;
    }

    /**
     * Set the <code>GoodsIcon</code> value.
     *
     * @param newGoodsIcon The new GoodsIcon value.
     */
    public void setGoodsIcon(final ImageIcon newGoodsIcon) {
        this.goodsIcon = newGoodsIcon;
        compressedWidth = goodsIcon.getIconWidth()*2;
    }

    /**
     * Get the <code>Production</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getProduction() {
        return production;
    }

    /**
     * Set the <code>Production</code> value.
     *
     * @param newProduction The new Production value.
     */
    public void setProduction(final int newProduction) {
        this.production = newProduction;
        updateToolTipText();
    }

    private void updateToolTipText() {
        if (goodsType == null || production == 0) {
            setToolTipText(null);
        } else {
            String text = Messages.message(StringTemplate.template("model.goods.goodsAmount")
                                           .add("%goods%", goodsType.getNameKey())
                                           .addAmount("%amount%", production));
            if (toolTipPrefix != null) {
                text = toolTipPrefix + " " + text;
            }
            setToolTipText(text);
        }
    }

    /**
     * Get the <code>MaximumProduction</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getMaximumProduction() {
        return maximumProduction;
    }

    /**
     * Set the <code>MaximumProduction</code> value.
     *
     * @param newMaximumProduction The new MaximumProduction value.
     */
    public void setMaximumProduction(final int newMaximumProduction) {
        this.maximumProduction = newMaximumProduction;
    }

    /**
     * Get the <code>MaxGoodsIcons</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getMaxGoodsIcons() {
        return maxIcons;
    }

    /**
     * Set the <code>MaxGoodsIcons</code> value.
     *
     * @param newMaxGoodsIcons The new MaxGoodsIcons value.
     */
    public void setMaxGoodsIcons(final int newMaxGoodsIcons) {
        this.maxIcons = newMaxGoodsIcons;
    }

    /**
     * Get the <code>stockNumber</code> value.
     * used to Show stored items in ReportColonyPanel
     *
     * @return an <code>int</code> value
     */
    public int getStockNumber() {
        return stockNumber;
    }

    /**
     * Set the <code>stockNumber</code> value.
     * used to Show stored items in ReportColonyPanel
     *
     * @param newStockNumber The new StockNumber value.
     */
    public void setStockNumber(final int newStockNumber) {
        this.stockNumber = newStockNumber;
    }

    /**
     * Get the <code>DrawPlus</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean drawPlus() {
        return drawPlus;
    }

    /**
     * Set the <code>DrawPlus</code> value.
     *
     * @param newDrawPlus The new DrawPlus value.
     */
    public void setDrawPlus(final boolean newDrawPlus) {
        this.drawPlus = newDrawPlus;
    }

    /**
     * Get the <code>Centered</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isCentered() {
        return centered;
    }

    /**
     * Set the <code>Centered</code> value.
     *
     * @param newCentered The new Centered value.
     */
    public void setCentered(final boolean newCentered) {
        this.centered = newCentered;
    }

    /**
     * Get the <code>CompressedWidth</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getCompressedWidth() {
        return compressedWidth;
    }

    /**
     * Set the <code>CompressedWidth</code> value.
     *
     * @param newCompressedWidth The new CompressedWidth value.
     */
    public void setCompressedWidth(final int newCompressedWidth) {
        this.compressedWidth = newCompressedWidth;
    }

    /**
     * Overrides the <code>getPreferredSize</code> method.
     *
     * @return a <code>Dimension</code> value
     */
    public Dimension getPreferredSize() {

        if (goodsIcon == null) {
            return new Dimension(0, 0);
        } else {
            return new Dimension(getPreferredWidth(), goodsIcon.getImage().getHeight(null));
        }
    }


    // TODO: get rid of the ugly code duplication
    /**
     * Returns only the width component of the preferred size.
     *
     * @return an <code>int</code> value
     */
    public int getPreferredWidth() {

        if (goodsIcon == null) {
            return 0;
        }

        int drawImageCount = Math.max(1, Math.min(Math.abs(production), maxIcons));

        int iconWidth = goodsIcon.getIconWidth();
        int pixelsPerIcon = iconWidth / 2;
        if (pixelsPerIcon - iconWidth < 0) {
            pixelsPerIcon = (compressedWidth - iconWidth) / drawImageCount;
        }
        int maxSpacing = iconWidth;

        /* TODO Tune this: all icons are the same width, but many
         * do not take up the whole width, eg. bells
         */
        boolean iconsTooFarApart = pixelsPerIcon > maxSpacing;
        if (iconsTooFarApart) {
            pixelsPerIcon = maxSpacing;
        }

        int width = pixelsPerIcon * (drawImageCount - 1) + iconWidth;
        if (getStringImage() == null) {
            return width;
        } else {
            return Math.max(getStringImage().getWidth(null), width);
        }

    }

    /**
     * Paints this ProductionLabel.
     *
     * @param g The graphics context in which to do the painting.
     */
    public void paintComponent(Graphics g) {

        if (goodsIcon == null || (production == 0 && stockNumber<0) ) {
            logger.fine("Empty production label: fix this!");
            return;
        }

        int stringWidth = getStringImage() == null ? 0 : getStringImage().getWidth(null);

        int drawImageCount = Math.min(Math.abs(production), maxIcons);
        if (drawImageCount==0) {
            drawImageCount=1;
        }

        int iconWidth = goodsIcon.getIconWidth();
        int pixelsPerIcon = iconWidth / 2;
        if (pixelsPerIcon - iconWidth < 0) {
            pixelsPerIcon = (compressedWidth - iconWidth) / drawImageCount;
        }
        int maxSpacing = iconWidth;

        /* TODO Tune this: all icons are the same width, but many
         * do not take up the whole width, eg. bells
         */
        boolean iconsTooFarApart = pixelsPerIcon > maxSpacing;
        if (iconsTooFarApart) {
            pixelsPerIcon = maxSpacing;
        }
        int coverage = pixelsPerIcon * (drawImageCount - 1) + iconWidth;
        int leftOffset = 0;

        int width = Math.max(getWidth(), Math.max(stringWidth, coverage));

        if (centered && coverage < width) {
            leftOffset = (width - coverage)/2;
        }

        int height = Math.max(getHeight(), goodsIcon.getImage().getHeight(null));
        setSize(new Dimension(width, height));


        // Draw the icons onto the image:
        for (int i = 0; i < drawImageCount; i++) {
            goodsIcon.paintIcon(null, g, leftOffset + i*pixelsPerIcon, 0);
        }

        if (stringImage != null) {
            int textOffset = width > stringWidth ? (width - stringWidth)/2 : 0;
            textOffset = (textOffset >= 0) ? textOffset : 0;
            g.drawImage(stringImage, textOffset,
                        goodsIcon.getIconHeight()/2 - stringImage.getHeight(null)/2, null);
        }
    }


    private Image getStringImage() {
        if (stringImage == null) {
            if (production >= displayNumber || production < 0 || maxIcons < production || stockNumber > 0
                || (maximumProduction > production && production > 0)) {
                String number = "";
                if (stockNumber >= 0 ) {
                    number = Integer.toString(stockNumber);  // Show stored items in ReportColonyPanel
                    drawPlus = true;
                }
                if (production >=0 && drawPlus ) {
                    number = number + "+" + Integer.toString(production);
                } else {
                    number = number + Integer.toString(production);
                }
                if (maximumProduction > production && production > 0) {
                    number = number + "/" + String.valueOf(maximumProduction);
                }
                Font font = ResourceManager.getFont("SimpleFont", Font.BOLD, 12f);
                stringImage = parent.getGUI().createStringImage(getCanvas().getGraphics(),
                                                                number, getForeground(), font);
            }
        }
        return stringImage;
    }


}
