/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import static net.sf.freecol.common.util.ImageUtils.fillTexture;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.border.AbstractBorder;

import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * A border created from a set of images. 
 */
public class FreeColImageBorder extends AbstractBorder implements FreeColBorder {

    private static final List<FreeColImageBorder> borders = new ArrayList<>();
    
    public static final FreeColImageBorder panelWithoutShadowBorder = new FreeColImageBorder("image.border.panel.noshadow");
    public static final FreeColImageBorder panelBorder = new FreeColImageBorder("image.border.panel");    
    public static final FreeColImageBorder buttonBorder = new FreeColImageBorder("image.border.button");
    public static final FreeColImageBorder simpleButtonBorder = new FreeColImageBorder("image.border.button.simple");
    public static final FreeColImageBorder menuBarBorder = new FreeColImageBorder("image.border.menu");
    public static final FreeColImageBorder woodenPanelBorder = new FreeColImageBorder("image.border.wooden");
    
    public static final FreeColImageBorder colonyWarehouseBorder = new FreeColImageBorder("image.border.colonyWarehouse");
    public static final FreeColImageBorder colonyPanelBorder = new FreeColImageBorder("image.border.colony.panel");
    public static final FreeColImageBorder innerColonyPanelBorder = new FreeColImageBorder("image.border.colony.panel.inner");
    public static final FreeColImageBorder outerColonyPanelBorder = new FreeColImageBorder("image.border.colony.panel.outer");

    private static float scaleFactor = 1;
    
    /**
     * The key used for getting the image resources.
     */
    private final String baseKey;
    
    /**
     * If true, no scaling to the border is applied.
     */
    private final boolean noScaling;
    
    private boolean initialized = false;

    /**
     * NW-corner
     */
    private BufferedImage topLeftCornerImage;
    
    /**
     * NNW
     */
    private BufferedImage topStartImage;
    
    /**
     * N-border
     */
    private BufferedImage topImage;
    
    /**
     * NNE
     */
    private BufferedImage topEndImage;
    
    /**
     * NE-corner
     */
    private BufferedImage topRightCornerImage;
    
    /**
     * ENE
     */
    private BufferedImage rightStartImage;
    
    /**
     * E-border
     */
    private BufferedImage rightImage;
    
    /**
     * ESE
     */
    private BufferedImage rightEndImage;
    
    /**
     * SE-corner
     */
    private BufferedImage bottomRightCornerImage;
    
    /**
     * SSE
     */
    private BufferedImage bottomEndImage;
    
    /**
     * S-border
     */
    private BufferedImage bottomImage;
    
    /**
     * SSW
     */
    private BufferedImage bottomStartImage;
    
    /**
     * SW-corner
     */
    private BufferedImage bottomLeftCornerImage;
    
    /**
     * WSW
     */
    private BufferedImage leftEndImage;
    
    /**
     * W-border
     */
    private BufferedImage leftImage;
    
    /**
     * WNW
     */
    private BufferedImage leftStartImage;


    private FreeColImageBorder(String baseKey) {
        this(baseKey, false);
    }
    

    private FreeColImageBorder(String baseKey, boolean noScaling) {
        this.baseKey = baseKey;
        this.noScaling = noScaling;
        
        loadImages();
        borders.add(this);
    }

    
    private void ensureInitialized() {
        if (!initialized) {
            loadImages();
        }
    }

    private void loadImages() {
        loadImages(getImage(baseKey + ".nw"),
                getImage(baseKey + ".nnw"),
                getImage(baseKey + ".n"),
                getImage(baseKey + ".nne"),
                getImage(baseKey + ".ne"),
                getImage(baseKey + ".ene"),
                getImage(baseKey + ".e"),
                getImage(baseKey + ".ese"),
                getImage(baseKey + ".se"),
                getImage(baseKey + ".sse"),
                getImage(baseKey + ".s"),
                getImage(baseKey + ".ssw"),
                getImage(baseKey + ".sw"),
                getImage(baseKey + ".wsw"),
                getImage(baseKey + ".w"),
                getImage(baseKey + ".wnw"));
        initialized = true;
    }

    /**
     * Loads the images for the border.
     *
     * @param topLeftCornerImage NW-corner
     * @param topImage N-border
     * @param topRightCornerImage NE-corner
     * @param rightImage E-border
     * @param bottomRightCornerImage SE-corner
     * @param bottomImage S-border
     * @param bottomLeftCornerImage SW-corner
     * @param leftImage W-border
     */
    private void loadImages(BufferedImage topLeftCornerImage,
            BufferedImage topStartImage,
            BufferedImage topImage,
            BufferedImage topEndImage,
            BufferedImage topRightCornerImage,
            BufferedImage rightStartImage,
            BufferedImage rightImage,
            BufferedImage rightEndImage,
            BufferedImage bottomRightCornerImage,
            BufferedImage bottomEndImage,
            BufferedImage bottomImage,
            BufferedImage bottomStartImage,
            BufferedImage bottomLeftCornerImage,
            BufferedImage leftEndImage,
            BufferedImage leftImage,
            BufferedImage leftStartImage) {
        this.topLeftCornerImage = topLeftCornerImage;
        this.topStartImage = topStartImage;
        this.topImage = topImage;
        this.topEndImage = topEndImage;
        this.topRightCornerImage = topRightCornerImage;
        this.rightStartImage = rightStartImage;
        this.rightImage = rightImage;
        this.rightEndImage = rightEndImage;
        this.bottomRightCornerImage = bottomRightCornerImage;
        this.bottomEndImage = bottomEndImage;
        this.bottomImage = bottomImage;
        this.bottomStartImage = bottomStartImage;
        this.bottomLeftCornerImage = bottomLeftCornerImage;
        this.leftEndImage = leftEndImage;
        this.leftImage = leftImage;
        this.leftStartImage = leftStartImage;
    }
    
    public static void setScaleFactor(float scaleFactor) {
        FreeColImageBorder.scaleFactor = scaleFactor;
        reloadAllImages();
    }
    
    private static void reloadAllImages() {
        borders.stream().forEach(FreeColImageBorder::loadImages);
    }


    private BufferedImage getImage(String key) {
        final ImageResource ir = ResourceManager.getImageResource(key, false);
        if (ir == null) {
            return null;
        }
        final BufferedImage image = ir.getImage();
        if (noScaling) {
            return image;
        }
        final Dimension scaledDimensions = new Dimension(
            (int) Math.round(image.getWidth() * scaleFactor),
            (int) Math.round(image.getHeight() * scaleFactor)
        );
        return ir.getImage(scaledDimensions, false);
    }

    /**
     * Gets the insets of this border around the given component.
     *
     * @param c The {@code Component} having the border.
     * @return The {@code Insets}.
     */    
    @Override
    public Insets getBorderInsets(Component c) {
        ensureInitialized();
        return getBorderInsets(c, null);
    }
    
    /**
     * Returns spaces that are open on this border.
     * 
     * @param c The component having the border.
     * @return A list of areas that can be considered out-of-bounds for this border.
     */
    @Override
    public List<Rectangle> getOpenSpace(Component c) {
        ensureInitialized();
        
        final List<Rectangle> openSpace = new ArrayList<>();
        
        /*
         * For now, only the top part of the border is handled. Feel free to extend this method.
         */
        
        if (topStartImage != null && topStartImage.getHeight() < max(getHeight(topImage), getHeight(topEndImage))) {
            final int openSpaceHeight = max(getHeight(topImage), getHeight(topEndImage)) - topStartImage.getHeight();
            openSpace.add(new Rectangle(0, 0, topStartImage.getWidth(), openSpaceHeight));
        }
        if (topImage != null && topImage.getHeight() < max(getHeight(topStartImage), getHeight(topEndImage))) {
            final int openSpaceHeight = max(getHeight(topStartImage), getHeight(topEndImage)) - topImage.getHeight();
            final int x = getWidth(topStartImage);
            final int width = c.getWidth() - getWidth(topStartImage) - getWidth(topEndImage);
            openSpace.add(new Rectangle(x, 0, width, openSpaceHeight));
        }
        if (topEndImage != null && topEndImage.getHeight() < max(getHeight(topStartImage), getHeight(topImage))) {
            final int openSpaceHeight = max(getHeight(topStartImage), getHeight(topImage)) - topEndImage.getHeight();
            openSpace.add(new Rectangle(c.getWidth() - topEndImage.getWidth(), 0, topEndImage.getWidth(), openSpaceHeight));
        }
        
        return openSpace;
    }
    
    /**
     * Determines the inset of the top left corner.
     */
    public int getTopLeftCornerInsetY() {
        final int top = max(
                getHeight(topImage),
                getHeight(topStartImage),
                getHeight(topEndImage),
                getHeight(topLeftCornerImage),
                getHeight(topRightCornerImage)
            );
        return top - getHeight(topLeftCornerImage);
    }

    /**
     * Gets the insets of this border around the given component.
     *
     * @param c The {@code Component} having the border.
     * @param insets An instance of {@code Insets} to be updated.
     * @return The given instance of {@code Insets} if not
     *      {@code null}, or a new instance otherwise.
     */
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        ensureInitialized();
        int top = max(
            getHeight(topImage),
            getHeight(topStartImage),
            getHeight(topEndImage),
            getHeight(topLeftCornerImage),
            getHeight(topRightCornerImage)
        );
        int left = max(
            getWidth(leftImage),
            getWidth(leftStartImage),
            getWidth(leftEndImage),
            getWidth(topLeftCornerImage),
            getWidth(bottomLeftCornerImage)
        );
        int bottom = max(
            getHeight(bottomImage),
            getHeight(bottomStartImage),
            getHeight(bottomEndImage),
            getHeight(bottomLeftCornerImage),
            getHeight(bottomRightCornerImage)
        );
        int right = max(
            getWidth(rightImage),
            getWidth(rightStartImage),
            getWidth(rightEndImage),
            getWidth(topRightCornerImage),
            getWidth(bottomRightCornerImage)
        );

        if (topImage == null) {
            top = 0;
        }
        if (leftImage == null) {
            left = 0;
        }
        if (bottomImage == null) {
            bottom = 0;
        }
        if (rightImage == null) {
            right = 0;
        }

        if (insets == null) {
            return new Insets(top, left, bottom, right);
        } else {
            insets.top = top;
            insets.left = left;
            insets.bottom = bottom;
            insets.right = right;
            return insets;
        }
    }
    
    private int max(int... numbers) {
        int highest = Integer.MIN_VALUE;
        for (int i : numbers) {
            if (i > highest) {
                highest = i;
            }
        }
        return highest;
    }

    /**
     * Paints the border on the given component.
     *
     * @param c The {@code Component} to draw the border on.
     * @param g The {@code Graphics} used for painting the border.
     * @param x The x-component of the offset.
     * @param y The y-component of the offset.
     * @param width The width of the border.
     * @param height The height of the border.
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        ensureInitialized();
        
        final Insets insets = getBorderInsets(c);
        final Graphics2D g2 = (Graphics2D) g;

        // Get width and height of the images
        final int topHeight = getHeight(topImage);
        final int leftWidth = getWidth(leftImage);
        final int bottomHeight = getHeight(bottomImage);
        final int rightWidth = getWidth(rightImage);
        
        final int topStartWidth = getWidth(topStartImage);
        final int topStartHeight = getHeight(topStartImage);
        final int topEndWidth = getWidth(topEndImage);
        final int topEndHeight = getHeight(topEndImage);
        final int leftStartWidth = getWidth(leftStartImage);
        final int leftStartHeight = getHeight(leftStartImage);
        final int leftEndWidth = getWidth(leftEndImage);
        final int leftEndHeight = getHeight(leftEndImage);
        final int bottomStartWidth = getWidth(bottomStartImage);
        final int bottomStartHeight = getHeight(bottomStartImage);
        final int bottomEndWidth = getWidth(bottomEndImage);
        final int bottomEndHeight = getHeight(bottomEndImage);
        final int rightStartWidth = getWidth(rightStartImage);
        final int rightStartHeight = getHeight(rightStartImage);
        final int rightEndWidth = getWidth(rightEndImage);
        final int rightEndHeight = getHeight(rightEndImage);
        
        final int topLeftCornerWidth = getWidth(topLeftCornerImage);
        final int topLeftCornerHeight = getHeight(topLeftCornerImage);
        final int topRightCornerWidth = getWidth(topRightCornerImage);
        final int topRightCornerHeight = getHeight(topRightCornerImage);
        final int bottomLeftCornerWidth = getWidth(bottomLeftCornerImage);
        final int bottomLeftCornerHeight = getHeight(bottomLeftCornerImage);
        final int bottomRightCornerWidth = getWidth(bottomRightCornerImage);
        final int bottomRightCornerHeight = getHeight(bottomRightCornerImage);

        // Add the image border
        if (topStartImage != null) {
            final int w = Math.min(topStartWidth, width - topLeftCornerWidth - topRightCornerWidth);
            fillTexture(g2, topStartImage,
                        x + topLeftCornerWidth,
                        y + insets.top - topStartHeight,
                        w,
                        topStartHeight);
        }
        if (topImage != null) {
            fillTexture(g2, topImage,
                        x + topLeftCornerWidth + topStartWidth,
                        y + insets.top - topHeight,
                        width - topLeftCornerWidth - topRightCornerWidth - topStartWidth - topEndWidth,
                        topHeight);
        }
        if (topEndImage != null) {
            final int w = Math.min(topEndWidth, width - topLeftCornerWidth - topRightCornerWidth - topStartWidth);
            fillTexture(g2, topEndImage,
                        x + width - Math.max(insets.right, topRightCornerWidth) - w,
                        y + insets.top - topEndHeight,
                        w,
                        topEndHeight);
        }
        if (leftStartImage != null) {
            final int h = Math.min(leftStartHeight, height - Math.max(insets.top, topLeftCornerHeight) - bottomLeftCornerHeight);
            fillTexture(g2, leftStartImage,
                        x + insets.left - leftStartWidth,
                        y + Math.max(insets.top, topLeftCornerHeight),
                        leftStartWidth,
                        h);
        }
        if (leftImage != null) {
            fillTexture(g2, leftImage,
                        x + insets.left - leftWidth,
                        y + Math.max(insets.top, topLeftCornerHeight) + leftStartHeight,
                        leftWidth,
                        height - Math.max(insets.top, topLeftCornerHeight) - Math.max(insets.bottom, bottomLeftCornerHeight) - leftStartHeight - leftEndHeight);
        }
        if (leftEndImage != null) {
            final int h = Math.min(leftEndHeight, height - topLeftCornerHeight - bottomLeftCornerHeight - leftStartHeight);
            fillTexture(g2, leftEndImage,
                        x + insets.left - leftEndWidth,
                        y + height - Math.max(insets.bottom, bottomLeftCornerHeight) - h,
                        leftEndWidth,
                        h);
        }
        if (bottomStartImage != null) {
            final int w = Math.min(bottomStartWidth, width - bottomLeftCornerWidth - bottomRightCornerWidth);
            fillTexture(g2, bottomStartImage,
                        x + bottomLeftCornerWidth,
                        y + height - insets.bottom,
                        w,
                        bottomStartHeight);
        }
        if (bottomImage != null) {
            fillTexture(g2, bottomImage,
                        x + bottomLeftCornerWidth + bottomStartWidth,
                        y + height - insets.bottom,
                        width - bottomLeftCornerWidth - bottomRightCornerWidth - bottomStartWidth - bottomEndWidth,
                        bottomHeight);
        }
        if (bottomEndImage != null) {
            final int w = Math.min(bottomEndWidth, width - bottomLeftCornerWidth - bottomRightCornerWidth - bottomStartWidth);
            fillTexture(g2, bottomEndImage,
                        x + width - Math.max(insets.right, bottomRightCornerWidth) - w,
                        y + height - insets.bottom,
                        w,
                        bottomEndHeight);
        }
        if (rightStartImage != null) {
            final int h = Math.min(rightStartHeight, height - Math.max(insets.top, topRightCornerHeight) - bottomRightCornerHeight);
            fillTexture(g2, rightStartImage,
                        x + width - insets.right,
                        y + Math.max(insets.top, topRightCornerHeight),
                        rightStartWidth,
                        h);
        }
        if (rightImage != null) {
            fillTexture(g2, rightImage,
                        x + width - insets.right,
                        y + Math.max(insets.top, topRightCornerHeight) + rightStartHeight,
                        rightWidth,
                        height - Math.max(insets.top, topRightCornerHeight) - bottomRightCornerHeight - rightStartHeight - rightEndHeight);
        }
        if (rightEndImage != null) {
            final int h = Math.min(rightEndHeight, height - topRightCornerHeight - bottomRightCornerHeight - rightStartHeight);
            fillTexture(g2, rightEndImage,
                        x + width - insets.right,
                        y + height - Math.max(insets.bottom, bottomRightCornerHeight) - h,
                        rightEndWidth,
                        h);
        }
        if (topLeftCornerImage != null) {
            fillTexture(g2, topLeftCornerImage,
                        x + Math.max(insets.left, topLeftCornerWidth) - topLeftCornerWidth,
                        y + Math.max(insets.top, topLeftCornerHeight) - topLeftCornerHeight,
                        topLeftCornerWidth,
                        topLeftCornerHeight);
        }
        if (topRightCornerImage != null) {
            fillTexture(g2, topRightCornerImage,
                        x + width - Math.max(insets.right, topRightCornerWidth),
                        y + Math.max(insets.top, topRightCornerHeight) - topRightCornerHeight,
                        topRightCornerWidth,
                        topRightCornerHeight);
        }
        if (bottomLeftCornerImage != null) {
            fillTexture(g2, bottomLeftCornerImage,
                        x + Math.max(insets.left, bottomLeftCornerWidth) - bottomLeftCornerWidth,
                        y + height - Math.max(insets.bottom, bottomLeftCornerHeight),
                        bottomLeftCornerWidth,
                        bottomLeftCornerHeight);
        }
        if (bottomRightCornerImage != null) {
            fillTexture(g2, bottomRightCornerImage,
                        x + width - Math.max(insets.right, bottomRightCornerWidth),
                        y + height - Math.max(insets.bottom, bottomRightCornerHeight),
                        bottomRightCornerWidth,
                        bottomRightCornerHeight);
        }
    }

    /**
     * Get the height of an image. If image is null, return 0.
     * @param im The image.
     * @return The height of the image.
     */
    private static int getHeight(Image im) {
        return (im == null) ? 0 : im.getHeight(null);
    }

    /**
     * Get the width of an image. If image is null, return 0.
     * @param im The image.
     * @return The width of the image.
     */
    private static int getWidth(Image im) {
        return (im == null) ? 0 : im.getWidth(null);
    }        
}
