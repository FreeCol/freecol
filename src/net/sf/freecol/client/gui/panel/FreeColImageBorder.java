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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import javax.swing.border.AbstractBorder;

import net.sf.freecol.common.resources.ResourceManager;

/**
 * A border created from a set of images. 
 */
public class FreeColImageBorder extends AbstractBorder {

    // The buffered image objects
    private BufferedImage topImage;
    private BufferedImage leftImage;
    private BufferedImage bottomImage;    
    private BufferedImage rightImage;
    private BufferedImage topLeftCornerImage;
    private BufferedImage topRightCornerImage;
    private BufferedImage bottomLeftCornerImage;
    private BufferedImage bottomRightCornerImage;
    
    public static final FreeColImageBorder imageBorder =
        new FreeColImageBorder(ResourceManager.getImage("menuborder.n.image"),
                               ResourceManager.getImage("menuborder.w.image"),
                               ResourceManager.getImage("menuborder.s.image"),
                               ResourceManager.getImage("menuborder.e.image"),
                               ResourceManager.getImage("menuborder.nw.image"),
                               ResourceManager.getImage("menuborder.ne.image"),
                               ResourceManager.getImage("menuborder.sw.image"),
                               ResourceManager.getImage("menuborder.se.image"));

    /**
     * Creates a border with the given set of images.<br />
     * Converts the <code>Image</code> objects to <code>BufferedImage</code>, because the images will
     * be used as Textures for the border.
     */
    public FreeColImageBorder(Image topImage, Image leftImage, Image bottomImage, Image rightImage, 
                              Image topLeftCornerImage, Image topRightCornerImage, Image bottomLeftCornerImage,
                              Image bottomRightCornerImage) {
        // Convert images to buffered images
        this.topImage = createBufferedImage(topImage);
        this.leftImage = createBufferedImage(leftImage);
        this.bottomImage = createBufferedImage(bottomImage);
        this.rightImage = createBufferedImage(rightImage);
        this.topLeftCornerImage = createBufferedImage(topLeftCornerImage);
        this.topRightCornerImage = createBufferedImage(topRightCornerImage);
        this.bottomLeftCornerImage = createBufferedImage(bottomLeftCornerImage);
        this.bottomRightCornerImage = createBufferedImage(bottomRightCornerImage);
    }
    
    /**
     * Creates a buffered image out of a given <code>Image</code> object.
     * 
     * @param img The <code>Image</code> object.
     * @return The created <code>BufferedImage</code> object.
     */
    private BufferedImage createBufferedImage(Image img) {
        if(img != null) {
            BufferedImage buff = new BufferedImage(getWidth(img), getHeight(img), BufferedImage.TYPE_INT_ARGB);
            Graphics gfx = buff.createGraphics();
            gfx.drawImage(img, 0, 0, null);
            gfx.dispose();
            return buff;
        } else {
            return null;
        }
    }
    
    /**
     * Gets the insets of this border around the given component.
     *
     * @param c The <code>Component</code> having the border.
     * @return The <code>Insets</code>.
     */    
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
    public Insets getBorderInsets(Component c, Insets insets) {        
        int top = Math.max(Math.max(getHeight(topImage), getHeight(topLeftCornerImage)), getHeight(topRightCornerImage));
        int left = Math.max(Math.max(getWidth(leftImage), getWidth(topLeftCornerImage)), getWidth(bottomLeftCornerImage));
        int bottom = Math.max(Math.max(getHeight(bottomImage), getHeight(bottomLeftCornerImage)), getHeight(bottomRightCornerImage));
        int right = Math.max(Math.max(getWidth(rightImage), getWidth(topRightCornerImage)), getWidth(bottomRightCornerImage));
    
        if (leftImage == null) {
            left = 0;
        }
        if (rightImage == null) {
            right = 0;
        }
        if (topImage == null) {
            top = 0;
        }
        if (bottomImage == null) {
            bottom = 0;
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
     * Get the height of an image. If image is null, return 0.
     * @param im The image.
     * @return The height of the image.
     */
    private int getHeight(Image im) {
        return (im != null) ? im.getHeight(null) : 0;
    }
    
    /**
     * Get the width of an image. If image is null, return 0.
     * @param im The image.
     * @return The width of the image.
     */
    private int getWidth(Image im) {
        return (im != null) ? im.getWidth(null) : 0;
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
            fillTexture(g2, topImage, x + topLeftCornerWidth, y + insets.top - topHeight, width - topLeftCornerWidth - topRightCornerWidth, topHeight);
        }
        if(leftImage != null) {
            fillTexture(g2, leftImage, x + insets.left - leftWidth, y + topLeftCornerHeight, leftWidth, height - topLeftCornerHeight - bottomLeftCornerHeight);
        }
        if(bottomImage != null) {
            fillTexture(g2, bottomImage, x + bottomLeftCornerWidth, y + height - insets.bottom, width - bottomLeftCornerWidth - bottomRightCornerWidth, bottomHeight);
        }
        if(rightImage != null) {
            fillTexture(g2, rightImage, x + width - insets.right, y + topRightCornerHeight, rightWidth, height - topRightCornerHeight - bottomRightCornerHeight);
        }
        if(topLeftCornerImage != null) {
            fillTexture(g2, topLeftCornerImage, x + Math.max(insets.left, topLeftCornerWidth) - topLeftCornerWidth, y + Math.max(insets.top, topLeftCornerHeight) - topLeftCornerHeight, topLeftCornerWidth, topLeftCornerHeight);
        }
        if(topRightCornerImage != null) {
            fillTexture(g2, topRightCornerImage, x + width - Math.max(insets.right, topRightCornerWidth), y + Math.max(insets.top, topRightCornerHeight) - topRightCornerHeight, topRightCornerWidth, topRightCornerHeight);
        }
        if(bottomLeftCornerImage != null) {
            fillTexture(g2, bottomLeftCornerImage, x + Math.max(insets.left, bottomLeftCornerWidth) - bottomLeftCornerWidth, y + height - Math.max(insets.bottom, bottomLeftCornerHeight), bottomLeftCornerWidth, bottomLeftCornerHeight);
        }
        if(bottomRightCornerImage != null) {
            fillTexture(g2, bottomRightCornerImage, x + width - Math.max(insets.right, bottomRightCornerWidth), y + height - Math.max(insets.bottom, bottomRightCornerHeight), bottomRightCornerWidth, bottomRightCornerHeight);
        }
    }
    
    /**
     * Fills a certain rectangle with the image texture.
     * 
     * @param g2 The <code>Graphics</code> used for painting the border.
     * @param img The <code>BufferedImage</code> to fill the texture.
     * @param x The x-component of the offset.
     * @param y The y-component of the offset.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     */
    public void fillTexture(Graphics2D g2, BufferedImage img, int x, int y, int width, int height) {
        Rectangle anchor = new Rectangle(x, y, getWidth(img), getHeight(img));
        TexturePaint paint = new TexturePaint(img, anchor);
        g2.setPaint(paint);
        g2.fillRect(x, y, width, height);
    }
}