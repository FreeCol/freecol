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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.StringTemplate;

/**
 * The ProductionLabel represents Goods that are produced in a
 * WorkLocation or Settlement. It is similar to the GoodsLabel.
 */
public final class ProductionMultiplesLabel extends JComponent {

    private static Logger logger = Logger.getLogger(ProductionMultiplesLabel.class.getName());

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
    private GoodsType goodsType[];

    /**
     * The goodsIcon for this type of production.
     * Indexes correlate with goodsType[].
     */
    private ImageIcon goodsIcon[];

    /**
     * The amount of goods being produced.
     * Indexes correlate with goodsType[].
     */
    private int production[];
    private int totalProduction;

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

    /**
     * Allow labels to include multiple goods.
     * This is especially useful for Food.
     * 
     * @param goodsType
     * @param amount
     * @param maximumProduction
     * @param parent
     */
    public ProductionMultiplesLabel(List<AbstractGoods> goods, Canvas parent) {
        super();
        this.parent = parent;
        //this.maximumProduction = maximumProduction;
        ClientOptions options = parent.getClient().getClientOptions();
        maxIcons = options.getInteger(ClientOptions.MAX_NUMBER_OF_GOODS_IMAGES);
        displayNumber = options.getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT);

        setFont(new Font("Dialog", Font.BOLD, 12));
        totalProduction = 0;
    	
        if (goods != null) {
            int size = goods.size();
            goodsType = new GoodsType[size];
            goodsIcon = new ImageIcon[size];
            production = new int[size];
            for (int ii=0; ii < size; ii++) {
                AbstractGoods current = goods.get(ii);
                goodsType[ii] = current.getType();
                goodsIcon[ii] = parent.getImageLibrary().getGoodsImageIcon(current.getType());
                production[ii] = current.getAmount();
                totalProduction += current.getAmount();
            }
            compressedWidth = getMaximumIconWidth()*2;
            updateToolTipText();
        }
        
        if (totalProduction < 0) {
            setForeground(Color.RED);
        } else {
            setForeground(Color.WHITE);
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
    	logger.warning("GETTING ProductionMultiplesLabel's getGoodsIcon...");
        return goodsIcon[0];
    }

    /**
     * Set the <code>GoodsIcon</code> value.
     *
     * @param newGoodsIcon The new GoodsIcon value.
     */
    public void setGoodsIcon(final ImageIcon newGoodsIcon) {
    	logger.warning("RESETTING ProductionMultiplesLabel's setGoodsIcon generally instead of specifically...");
        this.goodsIcon[0] = newGoodsIcon;
    }

    /**
     * Get the <code>Production</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getProduction() {
        return totalProduction;
    }

    /**
     * Set the <code>Production</code> value.
     *
     * @param newProduction The new Production value.
     */
    public void setProduction(final int newProduction) {
    	logger.warning("RESETTING ProductionMultiplesLabel's value generally instead of specifically to: "+ newProduction);
        this.totalProduction = newProduction;
        updateToolTipText();
    }

    private void updateToolTipText() {
        if (goodsType == null || goodsType.length == 0 || totalProduction == 0) {
            setToolTipText(null);
        } else {
            StringTemplate template = StringTemplate.label(", ");
            for (int index = 0; index < goodsType.length; index++) {
                template.addStringTemplate(StringTemplate.template("model.goods.goodsAmount")
                                           .add("%goods%", goodsType[index].getNameKey())
                                           .addAmount("%amount%", production[index]));
            }
            String text = Messages.message(template);
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

        if (goodsIcon == null || totalProduction == 0) {
            return new Dimension(0, 0);
        } else {
            return new Dimension(getPreferredWidth(), getMaximumIconHeight());
        }
    }


    // TODO: get rid of the ugly code duplication
    /**
     * Returns only the width component of the preferred size.
     *
     * @return an <code>int</code> value
     */
    public int getPreferredWidth() {

        if (goodsIcon == null || totalProduction == 0) {
            return 0;
        }

        int drawImageCount = Math.min(Math.abs(totalProduction), maxIcons);

        int iconWidth = getMaximumIconWidth();
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

        return pixelsPerIcon * (drawImageCount - 1) + iconWidth;

    }
    
    /**
     * Need to determine what the phatest icon is
     * @return the width
     */
    public int getMaximumIconWidth() {
    	int width = 0;
    	for( int ii=0; ii < goodsIcon.length; ii++ ) {
    		if( goodsIcon[ii].getIconWidth() > width ) {
    			width = goodsIcon[ii].getIconWidth();
    		}
    	}
    	return width;
    }

    /**
     * Need to determine the tallest icon
     * TODO: Why does this use the image instead of the Icon like width??
     * @return the height
     */
    public int getMaximumIconHeight() {
    	int height = 0;
    	for( int ii=0; ii < goodsIcon.length; ii++ ) {
    		if( goodsIcon[ii].getImage().getHeight(null) > height ) {
    			height = goodsIcon[ii].getImage().getHeight(null);
    		}
    	}
    	return height;
    }

    /**
     * Paints this ProductionLabel.
     * 
     * @param g The graphics context in which to do the painting.
     */
    public void paintComponent(Graphics g) {

        if (goodsIcon == null || (totalProduction == 0 && stockNumber<0) ) {
            return;
        }

        int drawImageCount = Math.min(Math.abs(totalProduction), maxIcons);
        if (drawImageCount==0) {
            drawImageCount=1;
        }

        int iconWidth = getMaximumIconWidth();
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

        boolean needToCenterImages = centered && coverage < getWidth();
        if (needToCenterImages) {
            leftOffset = (getWidth() - coverage)/2;
        }

        int width = Math.max(getWidth(), coverage);
        int height = Math.max(getHeight(), getMaximumIconHeight());
        setSize(new Dimension(width, height));

        // Go through all icons for this label
        int countImages = 0;
        int leftImageOffset = 0;
        for( int indexGoods = 0; indexGoods < goodsIcon.length; indexGoods++ ) {
            // Draw the icons onto the image:
            for (int i = 0; i < Math.abs(production[indexGoods]); i++) {
//                goodsIcon[indexGoods].paintIcon(null, g, leftOffset + i*pixelsPerIcon, 0);
                goodsIcon[indexGoods].paintIcon(null, g, leftOffset + leftImageOffset, 0);
            	leftImageOffset += pixelsPerIcon;
                if( ++countImages >= drawImageCount ) {
                	indexGoods = goodsIcon.length;
                	break;
                }
            }
        }
        

        if (totalProduction >= displayNumber || totalProduction < 0 || maxIcons < totalProduction || stockNumber>0) {
            String number = "";
            if (stockNumber >= 0 ) {
                number = Integer.toString(stockNumber);  // Show stored items in ReportColonyPanel
                drawPlus = true;
            }
            if (totalProduction >=0 && drawPlus ) {
                number = number + "+" + Integer.toString(totalProduction);
            } else {
                number = number + Integer.toString(totalProduction);
            }
            if (maximumProduction > totalProduction && totalProduction > 0) {
                number = number + "/" + String.valueOf(maximumProduction);
            }
            BufferedImage stringImage = parent.getGUI().createStringImage(this, number, getForeground(), width, 12);
            int textOffset = leftOffset + (coverage - stringImage.getWidth())/2;
            textOffset = (textOffset >= 0) ? textOffset : 0;
            g.drawImage(stringImage, textOffset,
//                    goodsIcon.getIconHeight()/2 - stringImage.getHeight()/2, null);
                    getMaximumIconHeight()/2 - stringImage.getHeight()/2, null);
        }
    }

}
