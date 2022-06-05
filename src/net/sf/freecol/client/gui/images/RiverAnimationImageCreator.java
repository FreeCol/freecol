package net.sf.freecol.client.gui.images;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import net.sf.freecol.common.util.ImageUtils;

/**
 * Class for generating the combined animated images for rivers.
 */
public final class RiverAnimationImageCreator {

    /**
     * Generates the image.
     * 
     * @param pebblesImage The pebbles image. That is, the graphics the water is drawn upon.
     * @param waterImage The water to be drawn.
     * @param riverImageVariationNumber The variation number of the
     *      water image given above.
     * @param riverMaskImage A mask to be applied to the water image in
     *      order to make the pebbles visible below the water. In addition, an
     *      enlarged (and less transparent) version of this mask is used for
     *      masking the pebbles.
     * @param tileMask A center tile mask. This is used to ensure that
     *      no pixels are out of bounds of the base tile.
     * @return A base tile image fitting inside the standard "diamond".
     */
    public static BufferedImage generateImage(BufferedImage pebblesImage,
            BufferedImage waterImage,
            int riverImageVariationNumber,
            BufferedImage riverMaskImage,
            BufferedImage tileMask) {
        final int width = waterImage.getWidth();
        final int height = waterImage.getHeight();
        
        if (pebblesImage.getWidth() != width || pebblesImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (riverMaskImage.getWidth() != width || riverMaskImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (tileMask.getWidth() != width || tileMask.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }

        final BufferedImage maskedInputImage = ImageUtils.imageWithAlphaFromMask(waterImage, riverMaskImage);
        
        
        final BufferedImage riverAndPebblesImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = riverAndPebblesImage.createGraphics();
        g2d.drawImage(pebblesImage, 0, 0, null); // TODO: Perhaps without pebbles for minor river?
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g2d.drawImage(maskedInputImage, 0, 0, null);
        g2d.dispose();

        final BufferedImage largerRiverMaskImage = toLargerMaskWithLessTransparency(riverMaskImage, width, height);
        final BufferedImage resultImage = ImageUtils.imageWithAlphaFromMask(riverAndPebblesImage, largerRiverMaskImage);       

        return ImageUtils.imageWithAlphaFromMask(resultImage, tileMask);
    }
    
    private static BufferedImage toLargerMaskWithLessTransparency(BufferedImage riverMaskImage, int origWidth, int origHeight) {
        final int largerWidth = (int) (origWidth * 1.25f);
        final int largerHeight = (int) (origHeight * 1.25f);
        final BufferedImage expandedRiverMaskImage = new BufferedImage(largerWidth, largerHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D expandedRiverMaskImageG2d = expandedRiverMaskImage.createGraphics();
        expandedRiverMaskImageG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        expandedRiverMaskImageG2d.drawImage(riverMaskImage, 0, 0, largerWidth, largerHeight, null);
        expandedRiverMaskImageG2d.drawImage(riverMaskImage, 0, 0, largerWidth, largerHeight, null);
        expandedRiverMaskImageG2d.dispose();
        return expandedRiverMaskImage.getSubimage((largerWidth - origWidth) / 2, (largerHeight - origHeight) / 2, origWidth, origHeight);
    }
}
