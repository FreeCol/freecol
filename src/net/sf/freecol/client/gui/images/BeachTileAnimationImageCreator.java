package net.sf.freecol.client.gui.images;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.sf.freecol.common.util.ImageUtils;

/**
 * Class for generating the combined animated image for ocean and beach
 * based on ocean image masks.
 */
public final class BeachTileAnimationImageCreator {

    /**
     * Generates the image.
     * 
     * @param beachImage The beach center tile image.
     * @param oceanImage The ocean image to use.
     * @param oceanImageVariationNumber The variation number of the
     *      ocean image given above.
     * @param oceanMaskImage A mask to be applied to the ocean image in
     *      order to make the beach visible below the ocean.
     * @param tileMask A center tile mask. This is used to ensure that
     *      no pixels are out of bounds of the base tile.
     * @return A base tile image fitting inside the standard "diamond".
     */
    public static BufferedImage generateImage(BufferedImage beachImage,
            BufferedImage oceanImage,
            int oceanImageVariationNumber,
            BufferedImage oceanMaskImage,
            BufferedImage tileMask) {
        final int width = oceanImage.getWidth();
        final int height = oceanImage.getHeight();
        
        if (beachImage.getWidth() != width || beachImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (oceanMaskImage.getWidth() != width || oceanMaskImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (tileMask.getWidth() != width || tileMask.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }

        final BufferedImage maskedInputImage = ImageUtils.imageWithAlphaFromMask(oceanImage, oceanMaskImage);
        
        final float extraOcean = 0.05f;
        
        final float alpha;
        if (oceanImageVariationNumber <= 10) {
            alpha = extraOcean * oceanImageVariationNumber;
        } else {
            alpha = extraOcean * 10 - extraOcean * (oceanImageVariationNumber - 10);
        }
        
        final BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = resultImage.createGraphics();
        g2d.drawImage(beachImage, 0, 0, null);
        g2d.drawImage(maskedInputImage, 0, 0, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(maskedInputImage, 0, 0, null);
        g2d.dispose();
    
        /*
         * The tileMask should not be necessary, but there are out-of-bounds pixels
         * from the beach displayed (on some of the zoom levels) if it's not applied.
         */
        return ImageUtils.imageWithAlphaFromMask(resultImage, tileMask);
    }
}
