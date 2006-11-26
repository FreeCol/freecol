package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Shape;

import javax.swing.border.AbstractBorder;

/**
 * A border created from a set of images. 
 */
public class FreeColImageBorder extends AbstractBorder {

    private Image topImage;
    private Image leftImage;
    private Image bottomImage;    
    private Image rightImage;
    
    private Image topLeftCornerImage;
    private Image topRightCornerImage;
    private Image bottomLeftCornerImage;
    private Image bottomRightCornerImage;
    
    private Insets insets;


    /**
     * Creates a border with the given set of images.
     */
    public FreeColImageBorder(Image topImage, Image leftImage, Image bottomImage, Image rightImage, 
                Image topLeftCornerImage, Image topRightCornerImage, Image bottomLeftCornerImage, Image bottomRightCornerImage) {       
        this.topImage = topImage;
        this.leftImage = leftImage;
        this.bottomImage = bottomImage;
        this.rightImage = rightImage;
        
        this.topLeftCornerImage = topLeftCornerImage;
        this.topRightCornerImage = topRightCornerImage;
        this.bottomLeftCornerImage = bottomLeftCornerImage;
        this.bottomRightCornerImage = bottomRightCornerImage;
        
        insets = getBorderInsets(null);
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
            //insets.set(top, left, bottom, right);
            insets.top = top;
            insets.left = left;
            insets.bottom = bottom;
            insets.right = right;
            
            return insets;
        }
    }
    
    private int getHeight(Image im) {
        return (im != null) ? im.getHeight(null) : 0;
    }
    
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
        final Shape originalClip = g.getClip();
        if (topLeftCornerImage != null) {
            g.drawImage(topLeftCornerImage, x + Math.max(insets.left - topLeftCornerImage.getWidth(null), 0), y, null);
        }
        if (topImage != null) {
            final int rcw = (topRightCornerImage != null) ? topRightCornerImage.getWidth(null) : 0;
            g.setClip(x + insets.left, y, width - rcw, insets.top);
            for (int i=0; i < width - rcw; i += topImage.getWidth(null)) {
                g.drawImage(topImage, x + insets.left + i, y + insets.top - topImage.getHeight(null), null);
            }
            g.setClip(originalClip);
        }
        if (topRightCornerImage != null) {
            g.drawImage(topRightCornerImage, x + width - Math.max(insets.right, topRightCornerImage.getWidth(null)), y, null);
        }
        g.setClip(x, y + insets.top, width, height - insets.bottom);
        if (leftImage != null) {
            for (int i=0; i<height - insets.bottom; i+=leftImage.getHeight(null)) {
                g.drawImage(leftImage, x + insets.left - leftImage.getWidth(null), y + insets.top + i, null);
            }
        }
        if (rightImage != null) {
            for (int i=0; i<height - insets.bottom; i+=rightImage.getHeight(null)) {
                g.drawImage(rightImage, x + width - insets.right, y + insets.top + i, null);
            }
        }
        g.setClip(originalClip);
                
        if (bottomLeftCornerImage != null) {
            g.drawImage(bottomLeftCornerImage, x + Math.max(insets.left - bottomLeftCornerImage.getWidth(null), 0), y + height - insets.bottom, null);
        }
        if (bottomImage != null) {
            int xx = ((bottomLeftCornerImage != null) ? bottomLeftCornerImage.getWidth(null) : 0);
            final int rcw = (bottomRightCornerImage != null) ? bottomRightCornerImage.getWidth(null) : 0;
            g.setClip(x + xx, y + height - insets.bottom, width - rcw - xx, insets.bottom);
            for (int i=0; i < width - rcw - xx; i += bottomImage.getWidth(null)) {
                g.drawImage(bottomImage, x + xx + i, y + height - insets.bottom, null);
            }
            g.setClip(originalClip);
        }
        if (bottomRightCornerImage != null) {
            g.drawImage(bottomRightCornerImage, x + width - Math.max(insets.right, bottomRightCornerImage.getWidth(null)), y + height - Math.max(insets.bottom, bottomRightCornerImage.getHeight(null)), null);
        }
    }
}
