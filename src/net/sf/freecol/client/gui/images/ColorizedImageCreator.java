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

package net.sf.freecol.client.gui.images;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import net.sf.freecol.common.resources.ImageCache;

/**
 * Colorizes an image.
 */
public final class ColorizedImageCreator {
    
    private final ImageCache imageCache;
    
    public ColorizedImageCreator(ImageCache imageCache) {
        this.imageCache = imageCache;
    }
    
    
    /**
     * Returns a colorized version of an image with the given key.
     * 
     * @param key The image key;
     * @param scalingFactor The scalingFactor for the image.
     * @param color The color used when colorizing.
     * @return A new image with the hue of the given color applied.
     */
    public BufferedImage getColorizedImage(String key, float scalingFactor, Color color) {
        final String compositeKey = "colorized##" + key + "##" + Integer.toHexString(color.getRGB());
        final BufferedImage image = this.imageCache.getScaledImage(key, scalingFactor, false);
        return imageCache.getCachedImageOrGenerate(compositeKey, new Dimension(image.getWidth(), image.getHeight()), false, 0, () -> {
            return colorize(image, color);
        });
    }

    
    private BufferedImage colorize(BufferedImage image, Color colorizeColor) {
        final float[] hsbColorize = Color.RGBtoHSB(colorizeColor.getRed(), colorizeColor.getGreen(), colorizeColor.getBlue(), null);
        
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] image1Pixels = image.getRGB(0, 0, width, height, null, 0, width);

        float[] hsb = new float[3];
        for (int i = 0; i < image1Pixels.length; i++) {
             final int argba = image1Pixels[i];
             final int a = (argba & 0XFF000000) >>> 24;
             final int r = (argba & 0XFF0000) >>> 16;
             final int g = (argba & 0XFF00) >>> 8;
             final int b = (argba & 0XFF);
             
             Color.RGBtoHSB(r, g, b, hsb);
             hsb[0] = hsbColorize[0];
             
             final int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
             final int argb = rgb & 0XFFFFFF | (a << 24);
             image1Pixels[i] = argb;
        }

        final BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        newImage.setRGB(0, 0, width, height, image1Pixels, 0, width);
        return newImage;
    }
}
