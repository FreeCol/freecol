package net.sf.freecol.client.gui.images;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.resources.ImageCache;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.util.ImageUtils;

/**
 * Class for generating the combined animated images for rivers.
 */
public final class RiverAnimationImageCreator {

    private final ImageLibrary lib;
    private final ImageCache imageCache;
    
    public RiverAnimationImageCreator(ImageLibrary lib, ImageCache imageCache) {
        this.lib = lib;
        this.imageCache = imageCache;
    }
    
    /**
     * Gets the combined animated image for river
     * 
     * @param tile The tile. 
     * @param ticks The number of ticks to get the correct animation frame.
     * @return A cached, generated image.
     */
    public BufferedImage getAnimatedScaledRiverTerrainImage(Tile tile, long ticks) {
        final ImageResource riverPebblesImageResource = getRiverPebblesImageResource();
        if (riverPebblesImageResource == null) {
            return null;
        }
        
        // Automatically determine:
        //final List<Direction> riverTransitions = determineRiverCombinations(tile);
        
        // Using hardcoded style:
        final List<Direction> riverTransitions = determineRiverTransitionsUsingStyle(tile);
        final String riverVariationKey = directionsToString(riverTransitions);
        
        final ImageResource riverWaterImageResource = ImageCache.getImageResource("image.tile.river.water");
        final int riverVariationNumber = riverWaterImageResource.getVariationNumberForTick(ticks);
        
        final int magnitude = tile.getRiver().getMagnitude();
        final String minorString = (magnitude <= 1) ? ".minor" : "";
        final List<Direction> minorToMajorTransitions = determineMinorToMajorRiverTransitions(tile, riverTransitions);
        
        final String generatedKey = riverWaterImageResource.getCachingKey() + ".river." + minorString + directionsToString(minorToMajorTransitions) + "." + riverVariationKey + "$gen";
        final BufferedImage result = imageCache.getCachedImageOrGenerate(generatedKey, lib.getTileSize(), false, riverVariationNumber, () -> {
            final BufferedImage riverPebblesImage = riverPebblesImageResource.getImage(lib.getTileSize(), false);
            final BufferedImage riverWaterImage = riverWaterImageResource.getVariation(riverVariationNumber).getImage(lib.getTileSize(), false);
            final String riverMaskKey = "image.mask.river" + minorString + (riverVariationKey.isEmpty() ? "" : "." + riverVariationKey);
            final BufferedImage baseMaskImage = this.imageCache.getSizedImage(riverMaskKey, lib.getTileSize(), false);
            final BufferedImage maskImage = createRiverMaskImageWithTransitions(baseMaskImage, minorToMajorTransitions);
            final BufferedImage baseTileMask = lib.getTerrainMask(null);
            
            return generateImage(riverPebblesImage, riverWaterImage, riverVariationNumber, maskImage, baseTileMask);
        });
        
        return result;
    }
    
    private static ImageResource getRiverPebblesImageResource() {
        final String key = "image.tile.river.pebbles";
        return ImageCache.getImageResource(key);
    }

    private BufferedImage createRiverMaskImageWithTransitions(BufferedImage baseMaskImage, List<Direction> minorToMajorTransitions) {
        if (minorToMajorTransitions.isEmpty()) {
            return baseMaskImage;
        }
        final BufferedImage resultImage = ImageUtils.createBufferedImage(baseMaskImage.getWidth(), baseMaskImage.getHeight());
        final Graphics2D g2d = resultImage.createGraphics();
        g2d.drawImage(baseMaskImage, 0, 0, null);
        for (Direction d : minorToMajorTransitions) {
            final String riverMaskKey = "image.mask.river.to_major." + d.toString().toLowerCase();
            final BufferedImage maskImage = this.imageCache.getSizedImage(riverMaskKey, lib.getTileSize(), false);
            g2d.drawImage(maskImage, 0, 0, null);
        }
        g2d.dispose();
        return resultImage;
    }
    
    private List<Direction> determineMinorToMajorRiverTransitions(Tile tile, List<Direction> riverTransitions) {
        if (tile.getRiver().getMagnitude() > 1) {
            return List.of();
        }
        final List<Direction> directionsWithRiverTransitions = riverTransitions.stream().filter(d -> {
            final Tile neighbour = tile.getNeighbourOrNull(d);
            return neighbour != null && (neighbour.hasRiver() && neighbour.getRiver().getMagnitude() > 1 || tile.isLand() && !neighbour.isLand());
        }).collect(Collectors.toList());
        
        return directionsWithRiverTransitions;
    }
    
    @SuppressWarnings("unused")
    private List<Direction> determineRiverCombinations(Tile tile) {
        final List<Direction> directionsWithRiver = Direction.longSides.stream().filter(d -> {
            final Tile neighbour = tile.getNeighbourOrNull(d);
            return neighbour != null && (neighbour.hasRiver() || tile.isLand() && !neighbour.isLand());
        }).collect(Collectors.toList());

        return directionsWithRiver;
    }
    
    private List<Direction> determineRiverTransitionsUsingStyle(Tile tile) {
        final TileImprovement river = tile.getRiver();
        if (river == null) {
            return List.of();
        }
        return river.getConnections().entrySet().stream().filter(e -> e.getValue() > 0).map(e -> e.getKey()).collect(Collectors.toList());
    }

    private String directionsToString(List<Direction> directions) {
        return directions.stream()
                .map(d -> d.toString().toLowerCase())
                .sorted()
                .reduce((a, b) -> a + "_" + b).orElse("");
    }
    
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
    private static BufferedImage generateImage(BufferedImage pebblesImage,
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
        
        
        final BufferedImage riverAndPebblesImage = ImageUtils.createBufferedImage(width, height);
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
        final BufferedImage expandedRiverMaskImage = ImageUtils.createBufferedImage(largerWidth, largerHeight);
        final Graphics2D expandedRiverMaskImageG2d = expandedRiverMaskImage.createGraphics();
        expandedRiverMaskImageG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        expandedRiverMaskImageG2d.drawImage(riverMaskImage, 0, 0, largerWidth, largerHeight, null);

        final int x = (largerWidth - origWidth) / 2;
        final int y = (largerHeight - origHeight) / 2;
        expandedRiverMaskImageG2d.drawImage(riverMaskImage, x, y, origWidth, origHeight, null);
        expandedRiverMaskImageG2d.dispose();
        return expandedRiverMaskImage.getSubimage(x, y, origWidth, origHeight);
    }
}
