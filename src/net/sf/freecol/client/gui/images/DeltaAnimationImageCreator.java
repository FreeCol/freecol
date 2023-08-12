package net.sf.freecol.client.gui.images;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.resources.ImageCache;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.util.ImageUtils;

/**
 * Class for generating the combined animated images for river deltas.
 */
public final class DeltaAnimationImageCreator {

    private final ImageLibrary lib;
    private final ImageCache imageCache;
    
    public DeltaAnimationImageCreator(ImageLibrary lib, ImageCache imageCache) {
        this.lib = lib;
        this.imageCache = imageCache;
    }
    
    /**
     * Gets the combined animated image for the river delta.
     * 
     * @param direction The direction to where the river is coming from.
     * @param ticks The number of ticks to get the correct animation frame.
     * @return A cached, generated image.
     */
    public BufferedImage getAnimatedScaledRiverDeltaTerrainImage(Direction direction, long ticks) {
        final ImageResource riverPebblesImageResource = getRiverPebblesImageResource();
        if (riverPebblesImageResource == null) {
            return null;
        }
                
        final ImageResource waterImageResource = ImageCache.getImageResource("image.tile.river.water");
        final int waterVariationNumber = waterImageResource.getVariationNumberForTick(ticks);
        
        final String generatedKey = waterImageResource.getCachingKey() + ".riverDelta." + direction + "." + waterVariationNumber + "$gen";
        final BufferedImage result = imageCache.getCachedImageOrGenerate(generatedKey, lib.getTileSize(), false, waterVariationNumber, () -> {
            final BufferedImage riverPebblesImage = riverPebblesImageResource.getImage(lib.getTileSize(), false);
            final BufferedImage waterImage = waterImageResource.getVariation(waterVariationNumber).getImage(lib.getTileSize(), false);
            final String riverWaterMaskKey = "image.mask.river.delta." + direction.toString().toLowerCase();
            final BufferedImage riverWaterMaskImage = this.imageCache.getSizedImage(riverWaterMaskKey, lib.getTileSize(), false);
            final String riverPebblesMaskKey = "image.mask.river.delta.pebbles." + direction.toString().toLowerCase();
            final BufferedImage riverPebblesMaskImage = this.imageCache.getSizedImage(riverPebblesMaskKey, lib.getTileSize(), false);
            final BufferedImage baseTileMask = lib.getTerrainMask(null);
            
            return generateImage(riverPebblesImage, waterImage, waterVariationNumber, riverWaterMaskImage, riverPebblesMaskImage, baseTileMask);
        });
        
        return result;
    }
    
    private static ImageResource getRiverPebblesImageResource() {
        final String key = "image.tile.river.pebbles";
        return ImageCache.getImageResource(key);
    }
    
    /**
     * Generates the image.
     * 
     * @param pebblesImage The pebbles image. That is, the graphics the water is drawn upon.
     * @param waterImage The water to be drawn.
     * @param waterImageVariationNumber The variation number of the
     *      water image given above.
     * @param riverWaterMaskImage A mask to be applied to the water image in
     *      order to make the pebbles visible below the water.
     * @param riverPebblesMaskImage A mask to be applied to the pebbles image in
     *      order to make the resulting image properly fade out.
     * @param tileMask A center tile mask. This is used to ensure that
     *      no pixels are out of bounds of the base tile.
     * @return A base tile image fitting inside the standard "diamond".
     */
    private static BufferedImage generateImage(BufferedImage pebblesImage,
            BufferedImage waterImage,
            int waterImageVariationNumber,
            BufferedImage riverWaterMaskImage,
            BufferedImage riverPebblesMaskImage,
            BufferedImage tileMask) {
        final int width = waterImage.getWidth();
        final int height = waterImage.getHeight();
        
        if (pebblesImage.getWidth() != width || pebblesImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (riverWaterMaskImage.getWidth() != width || riverWaterMaskImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (riverPebblesMaskImage.getWidth() != width || riverPebblesMaskImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (tileMask.getWidth() != width || tileMask.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }

        final BufferedImage maskedPebblesImage = ImageUtils.imageWithAlphaFromMask(pebblesImage, riverPebblesMaskImage);
        final BufferedImage maskedWaterImage = ImageUtils.imageWithAlphaFromMask(waterImage, riverWaterMaskImage);
        
        final BufferedImage riverAndPebblesImage = ImageUtils.createBufferedImage(width, height);
        final Graphics2D g2d = riverAndPebblesImage.createGraphics();
        g2d.drawImage(maskedPebblesImage, 0, 0, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g2d.drawImage(maskedWaterImage, 0, 0, null);
        g2d.dispose();

        return ImageUtils.imageWithAlphaFromMask(riverAndPebblesImage, tileMask);
    }
}
