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

package net.sf.freecol.client.gui.panel;

import static net.sf.freecol.common.util.ImageUtils.fillTexture;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.border.AbstractBorder;

import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * A border created from a set of images. 
 */
public class FreeColImageBorder extends AbstractBorder {

    private static final List<FreeColImageBorder> borders = new ArrayList<>();
    
    public static final FreeColImageBorder panelWithoutShadowBorder = new FreeColImageBorder("image.border.panel.noshadow");
    public static final FreeColImageBorder panelBorder = new FreeColImageBorder("image.border.panel");    
    public static final FreeColImageBorder buttonBorder = new FreeColImageBorder("image.border.button");
    public static final FreeColImageBorder simpleButtonBorder = new FreeColImageBorder("image.border.button.simple");
    public static final FreeColImageBorder menuBarBorder = new FreeColImageBorder("image.border.menu");

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
     * N-border
     */
    private BufferedImage topImage;
    
    /**
     * NE-corner
     */
    private BufferedImage topRightCornerImage;
    
    /**
     * E-border
     */
    private BufferedImage rightImage;
    
    /**
     * SE-corner
     */
    private BufferedImage bottomRightCornerImage;
    
    /**
     * S-border
     */
    private BufferedImage bottomImage;
    
    /**
     * SW-corner
     */
    private BufferedImage bottomLeftCornerImage;
    
    /**
     * W-border
     */
    private BufferedImage leftImage;


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
                getImage(baseKey + ".n"),
                getImage(baseKey + ".ne"),
                getImage(baseKey + ".e"),
                getImage(baseKey + ".se"),
                getImage(baseKey + ".s"),
                getImage(baseKey + ".sw"),
                getImage(baseKey + ".w"));
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
                               BufferedImage topImage,
                               BufferedImage topRightCornerImage,
                               BufferedImage rightImage,
                               BufferedImage bottomRightCornerImage,
                               BufferedImage bottomImage,
                               BufferedImage bottomLeftCornerImage,
                               BufferedImage leftImage) {
        this.topLeftCornerImage = topLeftCornerImage;
        this.topImage = topImage;
        this.topRightCornerImage = topRightCornerImage;
        this.rightImage = rightImage;
        this.bottomRightCornerImage = bottomRightCornerImage;
        this.bottomImage = bottomImage;
        this.bottomLeftCornerImage = bottomLeftCornerImage;
        this.leftImage = leftImage;      
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
        int top = Math.max(Math.max(getHeight(topImage),
                getHeight(topLeftCornerImage)),
            getHeight(topRightCornerImage));
        int left = Math.max(Math.max(getWidth(leftImage),
                getWidth(topLeftCornerImage)),
            getWidth(bottomLeftCornerImage));
        int bottom = Math.max(Math.max(getHeight(bottomImage),
                getHeight(bottomLeftCornerImage)),
            getHeight(bottomRightCornerImage));
        int right = Math.max(Math.max(getWidth(rightImage),
                getWidth(topRightCornerImage)),
            getWidth(bottomRightCornerImage));

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
        
        Insets insets = getBorderInsets(c);
        Graphics2D g2 = (Graphics2D) g;

        // Get width and height of the images
        int topHeight = getHeight(topImage);
        int leftWidth = getWidth(leftImage);
        int bottomHeight = getHeight(bottomImage);
        int rightWidth = getWidth(rightImage);  
        int topLeftCornerWidth = getWidth(topLeftCornerImage);
        int topLeftCornerHeight = getHeight(topLeftCornerImage);
        int topRightCornerWidth = getWidth(topRightCornerImage);
        int topRightCornerHeight = getHeight(topRightCornerImage);
        int bottomLeftCornerWidth = getWidth(bottomLeftCornerImage);
        int bottomLeftCornerHeight = getHeight(bottomLeftCornerImage);
        int bottomRightCornerWidth = getWidth(bottomRightCornerImage);
        int bottomRightCornerHeight = getHeight(bottomRightCornerImage);

        // Add the image border
        if (topImage != null) {
            fillTexture(g2, topImage,
                        x + topLeftCornerWidth,
                        y + insets.top - topHeight,
                        width - topLeftCornerWidth - topRightCornerWidth,
                        topHeight);
        }
        if(leftImage != null) {
            fillTexture(g2, leftImage,
                        x + insets.left - leftWidth,
                        y + topLeftCornerHeight,
                        leftWidth,
                        height - topLeftCornerHeight - bottomLeftCornerHeight);
        }
        if (bottomImage != null) {
            fillTexture(g2, bottomImage,
                        x + bottomLeftCornerWidth,
                        y + height - insets.bottom,
                        width - bottomLeftCornerWidth - bottomRightCornerWidth,
                        bottomHeight);
        }
        if (rightImage != null) {
            fillTexture(g2, rightImage,
                        x + width - insets.right,
                        y + topRightCornerHeight,
                        rightWidth,
                        height - topRightCornerHeight - bottomRightCornerHeight);
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
