package net.sf.freecol.client.gui.images;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.resources.ImageCache;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.util.ImageUtils;

/**
 * Class for generating the combined animated image for ocean and beach
 * based on ocean image masks.
 */
public final class BeachTileAnimationImageCreator {

    private final ImageLibrary lib;
    private final ImageCache imageCache;
    
    public BeachTileAnimationImageCreator(ImageLibrary lib, ImageCache imageCache) {
        this.lib = lib;
        this.imageCache = imageCache;
    }
    
    
    /**
     * Gets the combined animated image for ocean and beach.
     * 
     * @param type The tile type.
     * @param directionsWithLand All directions where there are neighbouring land tiles. 
     * @param ticks The number of ticks to get the correct animation frame.
     * @return A cached, genereated image.
     */
    public BufferedImage getAnimatedScaledWaterAndBeachTerrainImage(TileType type, List<Direction> directionsWithLand, long ticks) {
        final ImageResource beachCenterImageResource = ImageLibrary.getBeachCenterImageResource();
        if (beachCenterImageResource == null) {
            return null;
        }
        
        final String beachVariationKey = determineDirectionCombinationKey(directionsWithLand);
        
        final ImageResource oceanImageResource = ImageCache.getImageResource(ImageLibrary.getTerrainImageKey(type));
        final int oceanVariationNumber = oceanImageResource.getVariationNumberForTick(ticks);
        
        final String generatedKey = oceanImageResource.getCachingKey() + ".beach." + beachVariationKey + "$gen";
        final BufferedImage result = imageCache.getCachedImageOrGenerate(generatedKey, lib.getTileSize(), false, oceanVariationNumber, () -> {
            final BufferedImage oceanImage = lib.getAnimatedScaledTerrainImage(type, ticks);
            final String beachMaskKey = "image.mask.beach." + beachVariationKey;
            final BufferedImage beachMaskImage = this.imageCache.getSizedImage(beachMaskKey, lib.getTileSize(), false);
            final BufferedImage beachImage = beachCenterImageResource.getImage(lib.getTileSize(), false);
            final BufferedImage tileMask = lib.getTerrainMask(null);
            
            return generateImage(beachImage, oceanImage, oceanVariationNumber, beachMaskImage, tileMask);
        });
        
        return result;
    }
    
    /**
     * Generates the image.
     * 
     * @param beachImage The beach center tile image.
     * @param oceanImage The ocean image to use.
     * @param oceanImageVariationNumber The variation number of the
     *      ocean image given above.
     * @param beachMaskImage A mask to be applied to the ocean image in
     *      order to make the beach visible below the ocean.
     * @param tileMask A center tile mask. This is used to ensure that
     *      no pixels are out of bounds of the base tile.
     * @return A base tile image fitting inside the standard "diamond".
     */
    private static BufferedImage generateImage(BufferedImage beachImage,
            BufferedImage oceanImage,
            int oceanImageVariationNumber,
            BufferedImage beachMaskImage,
            BufferedImage tileMask) {
        final int width = oceanImage.getWidth();
        final int height = oceanImage.getHeight();
        
        if (beachImage.getWidth() != width || beachImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (beachMaskImage.getWidth() != width || beachMaskImage.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }
        if (tileMask.getWidth() != width || tileMask.getHeight() != height) {
            throw new IllegalArgumentException("All images should be of the same size.");
        }

        final BufferedImage maskedInputImage = ImageUtils.imageWithAlphaFromMask(oceanImage, beachMaskImage);
        
        final float extraOcean = 0.05f;
        
        final float alpha;
        if (oceanImageVariationNumber <= 10) {
            alpha = extraOcean * oceanImageVariationNumber;
        } else {
            alpha = extraOcean * 10 - extraOcean * (oceanImageVariationNumber - 10);
        }
        
        final BufferedImage resultImage = ImageUtils.createBufferedImage(width, height);
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
    
    /**
     * Returns a string representing the {@code directions} given.
     * @param directions The directions that should be included
     * @return The directions in lowercase, ordered alphabetically and
     *      combined with "_". Corners that are a part of a longSide is
     *      not included in the returned {@code String}.
     */
    private String determineDirectionCombinationKey(List<Direction> directions) {
        final Optional<String> neighboursPre = directions.stream()
                .filter(d -> Direction.longSides.contains(d))
                .map(d -> d.toString().toLowerCase())
                .sorted()
                .reduce((a, b) -> a + "_" + b);
        
        /*
         * We need to remove some of the combinations where the corners are
         * included in the edges. This is done in order to reduce the number
         * of possible images -- although it would look nicer to have special
         * graphics for these cases as well.
         */
        final String beachVariationKey = directions.stream()
                .filter(d -> Direction.longSides.contains(d)
                        || neighboursPre.isEmpty()
                        || !neighboursPre.get().contains(d.toString().toLowerCase()))
                .map(d -> d.toString().toLowerCase())
                .sorted()
                .reduce((a, b) -> a + "_" + b).get();
        
        return beachVariationKey;
    }
}
