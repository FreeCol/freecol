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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import javax.swing.border.AbstractBorder;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * A border created from a set of images. 
 */
public class FreeColImageBorder extends AbstractBorder {

    public static final FreeColImageBorder imageBorder = new FreeColImageBorder();


    // The buffered image objects
    private final BufferedImage topLeftCornerImage;
    private final BufferedImage topImage;
    private final BufferedImage topRightCornerImage;
    private final BufferedImage rightImage;
    private final BufferedImage bottomRightCornerImage;
    private final BufferedImage bottomImage;
    private final BufferedImage bottomLeftCornerImage;
    private final BufferedImage leftImage;


    /**
     * Creates the default border.
     */
    public FreeColImageBorder() {
        this(ResourceManager.getImage("image.menuborder.nw"),
             ResourceManager.getImage("image.menuborder.n"),
             ResourceManager.getImage("image.menuborder.ne"),
             ResourceManager.getImage("image.menuborder.e"),
             ResourceManager.getImage("image.menuborder.se"),
             ResourceManager.getImage("image.menuborder.s"),
             ResourceManager.getImage("image.menuborder.sw"),
             ResourceManager.getImage("image.menuborder.w"));
    }


    /**
     * Creates a border with the given set of images.<br />
     * Needs <code>BufferedImage</code> objects, because the images will
     * be used as Textures for the border.
     * @param topLeftCornerImage NW-corner
     * @param topImage N-border
     * @param topRightCornerImage NE-corner
     * @param rightImage E-border
     * @param bottomRightCornerImage SE-corner
     * @param bottomImage S-border
     * @param bottomLeftCornerImage SW-corner
     * @param leftImage W-border
     */
    public FreeColImageBorder(BufferedImage topLeftCornerImage,
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

    /**
     * Gets the insets of this border around the given component.
     *
     * @param c The <code>Component</code> having the border.
     * @return The <code>Insets</code>.
     */    
    @Override
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, null);
    }

    /**
     * Gets the insets of this border around the given component.
     *
     * @param c The <code>Component</code> having the border.
     * @param insets An instance of <code>Insets</code> to be updated.
     * @return The given instance of <code>Insets</code> if not
     *      <code>null</code>, or a new instance otherwise.
     */
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {        
        int top = Math.max(Math.max(getHeight(topImage), getHeight(topLeftCornerImage)), getHeight(topRightCornerImage));
        int left = Math.max(Math.max(getWidth(leftImage), getWidth(topLeftCornerImage)), getWidth(bottomLeftCornerImage));
        int bottom = Math.max(Math.max(getHeight(bottomImage), getHeight(bottomLeftCornerImage)), getHeight(bottomRightCornerImage));
        int right = Math.max(Math.max(getWidth(rightImage), getWidth(topRightCornerImage)), getWidth(bottomRightCornerImage));

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
     * @param c The <code>Component</code> to draw the border on.
     * @param g The <code>Graphics</code> used for painting the border.
     * @param x The x-component of the offset.
     * @param y The y-component of the offset.
     * @param width The width of the border.
     * @param height The height of the border.
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
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
        if(topImage != null) {
            ImageLibrary.fillTexture(g2, topImage,
                x + topLeftCornerWidth,
                y + insets.top - topHeight,
                width - topLeftCornerWidth - topRightCornerWidth,
                topHeight);
        }
        if(leftImage != null) {
            ImageLibrary.fillTexture(g2, leftImage,
                x + insets.left - leftWidth,
                y + topLeftCornerHeight,
                leftWidth,
                height - topLeftCornerHeight - bottomLeftCornerHeight);
        }
        if(bottomImage != null) {
            ImageLibrary.fillTexture(g2, bottomImage,
                x + bottomLeftCornerWidth,
                y + height - insets.bottom,
                width - bottomLeftCornerWidth - bottomRightCornerWidth,
                bottomHeight);
        }
        if(rightImage != null) {
            ImageLibrary.fillTexture(g2, rightImage,
                x + width - insets.right,
                y + topRightCornerHeight,
                rightWidth,
                height - topRightCornerHeight - bottomRightCornerHeight);
        }
        if(topLeftCornerImage != null) {
            ImageLibrary.fillTexture(g2, topLeftCornerImage,
                x + Math.max(insets.left, topLeftCornerWidth) - topLeftCornerWidth,
                y + Math.max(insets.top, topLeftCornerHeight) - topLeftCornerHeight,
                topLeftCornerWidth,
                topLeftCornerHeight);
        }
        if(topRightCornerImage != null) {
            ImageLibrary.fillTexture(g2, topRightCornerImage,
                x + width - Math.max(insets.right, topRightCornerWidth),
                y + Math.max(insets.top, topRightCornerHeight) - topRightCornerHeight,
                topRightCornerWidth,
                topRightCornerHeight);
        }
        if(bottomLeftCornerImage != null) {
            ImageLibrary.fillTexture(g2, bottomLeftCornerImage,
                x + Math.max(insets.left, bottomLeftCornerWidth) - bottomLeftCornerWidth,
                y + height - Math.max(insets.bottom, bottomLeftCornerHeight),
                bottomLeftCornerWidth,
                bottomLeftCornerHeight);
        }
        if(bottomRightCornerImage != null) {
            ImageLibrary.fillTexture(g2, bottomRightCornerImage,
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
