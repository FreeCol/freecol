package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Goods;

/**
 * This label holds production data in addition to the JLabel data.
 */
public final class ProductionLabel extends JLabel {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(ProductionLabel.class.getName());

    private final Canvas parent;

    /**
     * Describe maxIcons here.
     */
    private int maxIcons;

    /**
     * Describe drawPlus here.
     */
    private boolean drawPlus = true;

    /**
     * Describe centered here.
     */
    private boolean centered = true;

    /**
     * Describe compressedWidth here.
     */
    private int compressedWidth = -1;

    /**
     * Describe maxProduction here.
     */
    private int maxProduction = -1;

    /**
     * Describe goodsIcon here.
     */
    private ImageIcon goodsIcon;

    /**
     * Describe production here.
     */
    private int production;


    /**
     * Initializes this JLabel with the given unit data.
     * 
     * @param goods The Goods that this JLabel will visually represent.
     * @param parent The parent that knows more than we do.
     */
    public ProductionLabel(Goods goods, Canvas parent) {
        this(goods.getType(), goods.getAmount(), -1, parent);
    }

    public ProductionLabel(int goodsType, int amount, Canvas parent) {
        this(goodsType, amount, -1, parent);
    }

    public ProductionLabel(int goodsType, int amount, int maxProduction, Canvas parent) {
        super();
        this.parent = parent;
        goodsIcon = parent.getImageProvider().getGoodsImageIcon(goodsType);
        production = amount;
        this.maxProduction = maxProduction;
        maxIcons = parent.getClient().getClientOptions().getInteger(ClientOptions.MAX_NUMBER_OF_GOODS_IMAGES);
        if (amount < 0) {
            setForeground(Color.RED);
        } else {
            setForeground(Color.WHITE);
        }
        setPreferredSize(new Dimension(maxIcons * goodsIcon.getImage().getWidth(null),
                                       goodsIcon.getImage().getHeight(null)));

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
    }

    /**
     * Get the <code>MaxProduction</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getMaxProduction() {
        return maxProduction;
    }

    /**
     * Set the <code>MaxProduction</code> value.
     *
     * @param newMaxProduction The new MaxProduction value.
     */
    public void setMaxProduction(final int newMaxProduction) {
        this.maxProduction = newMaxProduction;
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
     * Paints this ProductionLabel.
     * 
     * @param g The graphics context in which to do the painting.
     */
    public void paintComponent(Graphics g) {

        super.paintComponent(g);

        int drawImageCount = Math.min(production, maxIcons);
        drawImageCount = Math.max(1, drawImageCount);

        int iconWidth = goodsIcon.getIconWidth();
        int pixelsPerIcon = compressedWidth / drawImageCount;
        if (pixelsPerIcon-iconWidth < 0) {
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
        int coverage = pixelsPerIcon * (drawImageCount-1) + iconWidth;
        int leftOffset = 0;
        boolean needToCenterImages = centered && coverage < getWidth();
        if (needToCenterImages) {
            leftOffset = (getWidth() - coverage)/2;
        }

        // Draw the icons onto the image:
        for (int i = 0; i < drawImageCount; i++) {
            goodsIcon.paintIcon(null, g, leftOffset + i*pixelsPerIcon, 0);
        }

        if (production > maxIcons) {
            String number = Integer.toString(production);
            if (production >= 0 && drawPlus) {
                number = "+"+ number;
            }
            if (maxProduction > production) {
                number = number + "/" + String.valueOf(maxProduction);
            }
            BufferedImage stringImage = parent.getGUI().createStringImage(this, number, getForeground(), getWidth(), 12);
            int textOffset = leftOffset + (coverage-stringImage.getWidth())/2;
            textOffset = (textOffset >= 0) ? textOffset : 0;
            g.drawImage(stringImage, textOffset,
                    goodsIcon.getIconHeight()/2 - stringImage.getHeight()/2, null);
        }
    }

}
