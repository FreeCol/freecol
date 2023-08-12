package net.sf.freecol.client.gui.images;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.resources.ImageCache;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.util.ImageUtils;

/**
 * Creates a transition between base tiles. For example, this transition will make the prairie graphics blend into the grassland graphics.
 */
public final class BaseTileTransitionImageCreator {
    
    private final ImageLibrary lib;
    private final ImageCache imageCache;
    
    public BaseTileTransitionImageCreator(ImageLibrary lib, ImageCache imageCache) {
        this.lib = lib;
        this.imageCache = imageCache;
    }
    

    /**
     * Returns a transparent image for making a transition between
     * the given tiles.
     *  
     * @param tile The tile that should get a transition.
     * @param direction The direction to get the bordering tile from.
     * @param useNiceCorners Determines if the corners of the base transitions
     *      should be rendered nicely (takes more time).
     * @param useVariations Uses variations of the transition.
     * @return The image, or {@code null} if there is no transition that
     *      should be drawn.
     */
    public BufferedImage getBaseTileTransitionImage(Tile tile, Direction direction, boolean useNiceCorners, boolean useVariations) {
        /*
         * 1. Transitions between tiles in NE and SW directions are made using only the
         * graphics for the other tile (using a mask in order to make it fade out).
         *  
         * 2. When making transitions in NW and SE directions, we need to make the transition
         * using not only the graphics for the other tile -- but also with the transitions
         * mentioned in point 1 included. This is a costly operation, however, so it can be
         * activated using the "useNiceCorners" parameter.
         */
        
        final Tile borderingTile = tile.getNeighbourOrNull(direction);
        
        /*
         * If true, we need to render the transition as mentioned in point 2 of the previous
         * comment.
         */
        final boolean renderSpecialTransition = useNiceCorners && (direction == Direction.NW || direction == Direction.SE);
        
        if (borderingTile == null
                || !borderingTile.isExplored()
                || !tile.isExplored()) {
            // No transition needed in these cases.
            return null;
        }
        
        if (tile.getType() == borderingTile.getType()
                && !renderSpecialTransition) {
            // No transition needed in these cases.
            return null;
        }
        
        final ImageResource terrainImageResource = getTerrainOrBeachImageResource(tile, borderingTile);
        if (terrainImageResource == null) {
            // Handles missing resource gracefully.
            return null;
        }
        
        final ImageResource tileImageResource = ImageCache.getImageResource(ImageLibrary.getTerrainImageKey(tile.getType()));
        if (tileImageResource == null) {
            // Handles missing resource gracefully.
            return null;
        }
        if (terrainImageResource.getCachingKey().equals(tileImageResource.getCachingKey())
                && !renderSpecialTransition) {
            /*
             * No reason to make transitions between tiles with the same graphics.. 
             */
            return null;
        }
        
        final boolean beachTransitionNe;
        final boolean beachTransitionSw;
        if (renderSpecialTransition) {
            beachTransitionNe = shouldIncludeSpecialBeachTransitionInDirection(tile, borderingTile, Direction.NE);
            beachTransitionSw = shouldIncludeSpecialBeachTransitionInDirection(tile, borderingTile, Direction.SW);
        } else {
            beachTransitionNe = false;
            beachTransitionSw = false;
        }
        
        final ImageResource terrainMaskResource = lib.getTerrainMaskResource(direction);
        final int maskVariationNumber;
        if (useVariations && terrainMaskResource.getNumberOfVariations() > 1) {
            // Choose the same variation for both sides of the transition.
            final Tile transitionSeedTile = (direction == Direction.NW || direction == Direction.NE) ? tile : borderingTile;
            final int seed = ImageLibrary.variationSeedUsing(transitionSeedTile.getX(), transitionSeedTile.getY());
            
            // This never picks the last variation as that's just a simple, almost completely straight line, transition.
            maskVariationNumber = new Random(seed).nextInt(terrainMaskResource.getNumberOfVariations() - 1);
        } else {
            maskVariationNumber = terrainMaskResource.getNumberOfVariations() - 1;
        }
        
        /*
         * The transitionKey is used for caching, and needs to be unique for every transition.
         */
        final String transitionKey;
        if (renderSpecialTransition) {
            final Tile neBorderingTile = borderingTile.getNeighbourOrNull(Direction.NE);
            final Tile swBorderingTile = borderingTile.getNeighbourOrNull(Direction.SW);

            transitionKey = terrainImageResource.getCachingKey()
                    + ","
                    + (neBorderingTile != null && neBorderingTile.getType() != null ? neBorderingTile.getType().getId() : "null")
                    + ","
                    + (swBorderingTile != null && swBorderingTile.getType() != null ? swBorderingTile.getType().getId() : "null")
                    + ","
                    + maskVariationNumber
                    + ","
                    + beachTransitionNe
                    + beachTransitionSw
                    + "$baseTransition$gen";
        } else {
            transitionKey = terrainImageResource.getCachingKey() + "," + maskVariationNumber + "$baseTransition$gen";
        }
        
        /*
         * Using direction.ordinal() as the cached variation in order to get different
         * image cache hash values.
         */
        final BufferedImage transitionImage = imageCache.getCachedImageOrGenerate(transitionKey, lib.getTileSize(), false, direction.ordinal(), () -> {
            
            /*
             * Uses variation 0 to get the same variation every time as this reduces the amount
             * of memory used.
             */
            final BufferedImage terrainImage = imageCache.getCachedImage(terrainImageResource, lib.getTileSize(), false, 0);
            
            final BufferedImage transitionTileImage;
            if (!renderSpecialTransition) {
                transitionTileImage = terrainImage;
            } else {
                /*
                 * When doing the transitions for NW and SE, the base terrain should
                 * be the blended version of the neighbouring tiles. That is, the
                 * result after applying the transitions for NE and SW.
                 */
                
                transitionTileImage = ImageUtils.createBufferedImage(terrainImage.getWidth(), terrainImage.getHeight());
                final Graphics2D tranitionG2d = transitionTileImage.createGraphics();
                
                if (tile.isLand() || borderingTile.isLand()) {
                    tranitionG2d.drawImage(terrainImage, 0, 0, null);
                } else {
                    /*
                     * This handles the case where neither the tile nor the borderingTile are land tiles,
                     * but they are adjacent to land tiles.
                     * 
                     *  In this case, we need to include the beach in the transition. This only happens for
                     *  corners, but we are never-the-less using a mask for the entire edge. The reason being
                     *  that the terrain mask applied last to the transitionTileImage (before returning it)
                     *  reduces the edges to only the relevant two corners.
                     */
                    if (beachTransitionNe) {
                        tranitionG2d.drawImage(getSpecialBeachBaseTileTransitionImage(Direction.NE), 0, 0, null);
                    }
                    if (beachTransitionSw) {
                        tranitionG2d.drawImage(getSpecialBeachBaseTileTransitionImage(Direction.SW), 0, 0, null);
                    }
                }
                
                /*+
                 * These two includes the neighboring tiles to the bordering tiles, so that
                 * we get the correct blended transition for the cornes.
                 * 
                 * This is only needed for corners, but we are never-the-less using a mask for
                 * the entire edge. The reason being that the terrain mask applied last to the
                 * transitionTileImage (before returning it) reduces the edges to only the
                 * relevant two corners.
                 */
                final BufferedImage transitionImage2 = getBaseTileTransitionImage(borderingTile, Direction.NE, false, false);
                if (transitionImage2 != null) {
                    tranitionG2d.drawImage(transitionImage2, 0, 0, null);
                }
                final BufferedImage transitionImage3 = getBaseTileTransitionImage(borderingTile, Direction.SW, false, false);
                if (transitionImage3 != null) {
                    tranitionG2d.drawImage(transitionImage3, 0, 0, null);
                }
                
                tranitionG2d.dispose();
            }
            
            final BufferedImage terrainMaskImage = imageCache.getCachedImage(terrainMaskResource, lib.getTileSize(), false, maskVariationNumber);
            return ImageUtils.imageWithAlphaFromMask(transitionTileImage, terrainMaskImage);
        });

        return transitionImage;
    }

    /**
     * Checks if the beach should be included when making the transition for a corner.
     * 
     * @param tile The tile that should get a transition.
     * @param borderingTile The main tile used for making the transition (the neighboring tiles
     *      are used as well when making corners).
     * @param direction The direction to the neighboring tile that should be checked in order to
     *      determine if a beach corner transition is needed.
     * @return {@code true} if the beach should be rendered at the corner, and {@code false} otherwise.
     */
    private boolean shouldIncludeSpecialBeachTransitionInDirection(Tile tile, Tile borderingTile, Direction direction) {
        if (tile.isLand() || borderingTile.isLand()) {
            return false;
        }
        final Tile tileNeighbour = tile.getNeighbourOrNull(direction);
        final Tile borderingTileNeighbour = borderingTile.getNeighbourOrNull(direction);
        return tileNeighbour != null && tileNeighbour.isLand()
                || borderingTileNeighbour != null && borderingTileNeighbour.isLand();
    }

    /**
     * Gets the beach overlay to use when making base tile transitions with beach.
     */
    private BufferedImage getSpecialBeachBaseTileTransitionImage(Direction direction) {
        final String transitionKey = "specialBeachTransition." + direction.toString() + "$gen";
        final BufferedImage resultImage = imageCache.getCachedImageOrGenerate(transitionKey, lib.getTileSize(), false, direction.ordinal(), () -> {
            final BufferedImage beachImage = imageCache.getCachedImage(ImageLibrary.getBeachCenterImageResource(), lib.getTileSize(), false, 0);
            return ImageUtils.imageWithAlphaFromMask(beachImage, getSpecialBeachBaseTileTransitionMask(direction));
        });
        
        return resultImage;
    }
    
    private BufferedImage getSpecialBeachBaseTileTransitionMask(Direction direction) {
        final String key = "image.mask.special.beach." + direction.toString().toLowerCase();
        return this.imageCache.getSizedImage(key, lib.getTileSize(), false);
    }

    /**
     * Gets the terrain image resource, or the beach, in order to create transitions.
     */
    private ImageResource getTerrainOrBeachImageResource(Tile tile, final Tile borderingTile) {
        if (borderingTile == null) {
            return null;
        }
        final ImageResource terrainImageResource;
        final boolean notABeachTransition = borderingTile.isLand() || !borderingTile.isLand() && !tile.isLand();;
        if (notABeachTransition) {
            terrainImageResource = ImageCache.getImageResource(ImageLibrary.getTerrainImageKey(borderingTile.getType()));
        } else {
            terrainImageResource = ImageLibrary.getBeachCenterImageResource();
        }
        return terrainImageResource;
    }

}
