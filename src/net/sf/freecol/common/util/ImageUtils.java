/**
 *  Copyright (C) 2002-2020   The FreeCol Team
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

package net.sf.freecol.common.util;

import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.RenderingHints;
import java.awt.TexturePaint;


/**
 * Collection of static image manipulation utilities.
 * Only generic routines belong here, anything specific to FreeCol
 * belongs in ImageLibrary or ImageResource.
 */
public class ImageUtils {

    /**
     * Creates a buffered image from an input {@code Image}.
     * 
     * @param image The {@code Image}.
     * @return The new {@code BufferedImage}.
     */
    public static BufferedImage createBufferedImage(Image image) {
        if (image == null) return null;
        BufferedImage result
            = new BufferedImage(image.getWidth(null), image.getHeight(null),
                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return result;
    }

    /**
     * Create a buffered image that is roughly half the size in both
     * dimensions of an input {@code Image}.
     *
     * Note: perhaps this should be createQuarteredImage?
     *
     * @param image The {@code Image}.
     * @return The new halved {@code BufferedImage}.
     */ 
    public static BufferedImage createHalvedImage(Image image) {
        if (image == null) return null;
        final int width = image.getWidth(null), height = image.getHeight(null);
        int w = (width + 1) / 2, h = (height + 1) / 2;
        BufferedImage result = new BufferedImage(w, h,
                                                 BufferedImage.TYPE_INT_ARGB);
        // For halving bilinear should most correctly average 2x2 pixels.
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, w, h, null);
        g2d.dispose();
        return result;
    }

    /**
     * Create a buffered image that mirrors an input {@code Image}.
     *
     * @param image The {@code Image} object.
     * @return The new mirrored {@code BufferedImage} object.
     */ 
    public static BufferedImage createMirroredImage(Image image) {
        if (image == null) return null;
        final int width = image.getWidth(null);
        final int height = image.getHeight(null);
        BufferedImage result = new BufferedImage(width, height,
                                                 BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.drawImage(image, width, 0, -width, height, null);
        g2d.dispose();
        return result;
    }

    /**
     * Create a buffered image resized from a plain {@code Image}.
     *
     * Note that while the result will fit in width x height, it
     * tends to retain its original aspect ratio.
     *
     * @param image The {@code Image} object.
     * @param width The desired width.
     * @param height The desired height.
     * @return The new resized {@code BufferedImage} object.
     */ 
    public static BufferedImage createResizedImage(Image image,
                                                   int width, int height) {
        BufferedImage result = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
        return result;
    }

    /**
     * Create a faded version of an image.
     *
     * @param img The {@code Image} to fade.
     * @param fade The amount of fading.
     * @param target The offset.
     * @return The faded image.
     */
    public static BufferedImage fadeImage(Image img, float fade, float target) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.drawImage(img, 0, 0, null);

        float offset = target * (1.0f - fade);
        float[] scales = { fade, fade, fade, 1.0f };
        float[] offsets = { offset, offset, offset, 0.0f };
        RescaleOp rop = new RescaleOp(scales, offsets, null);
        g.drawImage(bi, rop, 0, 0);
        g.dispose();
        return bi;
    }

    /**
     * Fill a rectangle with a texture image.
     * 
     * @param g2d The {@code Graphics2D} used for painting the border.
     * @param img The {@code BufferedImage} to fill the texture.
     * @param x The x-component of the offset to start filling at.
     * @param y The y-component of the offset to start filling at.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     */
    public static void fillTexture(Graphics2D g2d, BufferedImage img,
                                   int x, int y, int width, int height) {
        Rectangle anchor = new Rectangle(x, y,
                                         img.getWidth(), img.getHeight());
        TexturePaint paint = new TexturePaint(img, anchor);
        g2d.setPaint(paint);
        g2d.fillRect(x, y, width, height);
    }

}
