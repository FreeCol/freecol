/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui.mapviewer;

import static net.sf.freecol.common.util.StringUtils.getBreakingPoint;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileItem;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.ImageUtils;


/**
 * TileViewer is a private helper class of MapViewer and SwingGUI.
 * 
 * This class is responsible for drawing map tiles
 * for MapViewer and some GUI-panels.
 *
 * It needs to be a FreeColClientHolder so it can check the client options.
 *
 * create*() routines return a newly created BufferedImage, drawn with
 * a Graphics2D object that is created and disposed within.
 * display*() routines use a Graphics2D object passed as a parameter to
 * draw to, which is neither created or disposed by the routine, just used.
 */
public final class TileViewer extends FreeColClientHolder {

    private static final Logger logger = Logger.getLogger(TileViewer.class.getName());

    /** Standard rescaling used in displayTile. */
    private static final RescaleOp standardRescale
        = new RescaleOp(new float[] { 0.8f, 0.8f, 0.8f, 1f },
                        new float[] { 0, 0, 0, 0 },
                        null);

    /** The offset to paint the occupation indicator (in pixels). */
    public static final int STATE_OFFSET_X = 25, STATE_OFFSET_Y = 10;

    /**
     * The image library derived from the parent map viewer.
     * Beware that while it is final, its scale can change and this
     * should be handled by calling updateScaledVariables().
     */
    private final ImageLibrary lib;

    /** Scaled road painter. */
    private RoadPainter rp;

    /** Helper variables for displaying tiles. */
    private int tileHeight, tileWidth, halfHeight;

    /** Font for tile text. */
    private Font tinyFont;

    /** Fonts for the colony population chip. */
    private Font emphFont, normFont;


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param lib The (appropriately scaled) {@code ImageLibrary} to use.
     */
    public TileViewer(FreeColClient freeColClient, ImageLibrary lib) {
        super(freeColClient);

        this.lib = lib;
        updateScaledVariables();
    }


    // Public API
    
    /**
     * Update all the variables dependent on the library scale factor.
     */
    public void updateScaledVariables() {
        // ATTENTION: we assume that all base tiles have the same size
        Dimension tileSize = this.lib.getTileSize();
        this.rp = new RoadPainter(tileSize);
        this.tileHeight = tileSize.height;
        this.tileWidth = tileSize.width;
        this.halfHeight = tileHeight/2;
        // Allow fonts to disappear if too small
        this.tinyFont = this.lib.getScaledFont("normal-plain-tiny", null);
        this.emphFont = this.lib.getScaledFont("simple-bold+italic-smaller", null);
        this.normFont = this.lib.getScaledFont("simple-bold-tiny", null);
    }
        
    /**
     * Create a {@code BufferedImage} and draw a {@code Tile} on it.
     * Draws the terrain and improvements.
     *
     * Public for {@link GUI#createTileImageWithBeachBorderAndItems}.
     *
     * @param tile The Tile to draw.
     * @return The image.
     */
    public BufferedImage createTileImageWithBeachBorderAndItems(Tile tile) {
        if (!tile.isExplored()) {
            return this.lib.getScaledTerrainImage(null, tile.getX(), tile.getY());
        }
        final TileType tileType = tile.getType();
        BufferedImage overlayImage = this.lib.getScaledOverlayImage(tile);
        final int compoundHeight
            = (overlayImage != null) ? overlayImage.getHeight()
            : (tileType.isForested()) ? this.lib.getForestedTileSize().height
            : this.tileHeight;
        BufferedImage image = ImageUtils.createBufferedImage(this.tileWidth, compoundHeight);
        Graphics2D g2d = image.createGraphics();
        g2d.translate(0, compoundHeight - this.tileHeight);
        displayAnimatedBaseTiles(g2d, tile, false);
        displayTileWithBeach(g2d, tile);
        displayTileItems(g2d, tile, null, overlayImage);
        g2d.dispose();
        return image;
    }

    /**
     * Create a {@code BufferedImage} and draw a {@code Tile} on it.
     *
     * Public for {@link GUI#createTileImage}.
     *
     * @param tile The {@code Tile} to draw.
     * @param player The {@code Player} to draw for.
     * @return The image.
     */
    public BufferedImage createTileImage(Tile tile, Player player) {
        final TileType tileType = tile.getType();
        BufferedImage overlayImage = this.lib.getScaledOverlayImage(tile);
        final int compoundHeight = (overlayImage != null)
            ? overlayImage.getHeight()
            : (tileType.isForested())
            ? this.lib.getForestedTileSize().height
            : this.tileHeight;
        BufferedImage image = ImageUtils.createBufferedImage(this.tileWidth, compoundHeight);
        Graphics2D g2d = image.createGraphics();
        g2d.translate(0, compoundHeight - this.tileHeight);
        displayTile(g2d, tile, player, overlayImage);
        g2d.dispose();
        return image;
    }

    /**
     * Create a {@code BufferedImage} and draw a {@code Tile} on it.
     * The visualization of the {@code Tile} also includes information
     * from the corresponding {@code ColonyTile} of the given
     * {@code Colony}.
     *
     * Public for {@link GUI#createColonyTileImage}.
     *
     * @param tile The {@code Tile} to draw.
     * @param colony The {@code Colony} to create the visualization
     *      of the {@code Tile} for. This object is also used to
     *      get the {@code ColonyTile} for the given {@code Tile}.
     * @return The image.
     */
    public BufferedImage createColonyTileImage(Tile tile, Colony colony) {
        final TileType tileType = tile.getType();
        BufferedImage overlayImage = this.lib.getScaledOverlayImage(tile);
        final int compoundHeight = (overlayImage != null)
            ? overlayImage.getHeight()
            : ((tileType.isForested())
                ? this.lib.getForestedTileSize().height
                : this.tileHeight);
        BufferedImage image = ImageUtils.createBufferedImage(this.tileWidth, compoundHeight);
        Graphics2D g2d = image.createGraphics();
        g2d.translate(0, compoundHeight - this.tileHeight);
        displayColonyTile(g2d, tile, colony, overlayImage);
        g2d.dispose();
        return image;
    }

    /**
     * Displays the 3x3 tiles for the TilesPanel in ColonyPanel.
     * 
     * Public for {@link GUI#displayColonyTiles}.
     *
     * @param g2d The {@code Graphics2D} object on which to draw
     *      the {@code Tile}.
     * @param tiles The array containing the {@code Tile} objects to draw.
     * @param colony The {@code Colony} to create the visualization
     *      of the {@code Tile} objects for.
     */
    public void displayColonyTiles(Graphics2D g2d, Tile[][] tiles,
                                   Colony colony) {
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[x].length; y++) {
                if (tiles[x][y] != null) {
                    int xx = (((tiles.length-1 - x) + y) * this.tileWidth) / 2;
                    int yy = ((x + y) * this.tileHeight) / 2;
                    g2d.translate(xx, yy);
                    BufferedImage overlayImage
                        = this.lib.getScaledOverlayImage(tiles[x][y]);
                    displayColonyTile(g2d, tiles[x][y], colony, overlayImage);
                    g2d.translate(-xx, -yy);
                }
            }
        }
    }

    /**
     * Displays the given colony tile.
     *
     * The visualization of the {@code Tile} also includes information
     * from the corresponding {@code ColonyTile} from the given
     * {@code Colony}.
     *
     * @param g2d The {@code Graphics2D} on which to draw.
     * @param tile The {@code Tile} to draw.
     * @param colony The {@code Colony} to create the visualization
     *      of the {@code Tile} for. This object is also used to
     *      get the {@code ColonyTile} for the given {@code Tile}.
     * @param overlayImage The BufferedImage of the tile overlay.
     */
    private void displayColonyTile(Graphics2D g2d, Tile tile, Colony colony,
                                   BufferedImage overlayImage) {
        displayTile(g2d, tile, colony.getOwner(), overlayImage);

        ColonyTile colonyTile = colony.getColonyTile(tile);
        if (colonyTile == null) return;

        switch (colonyTile.getNoWorkReason()) {
        case NONE: case COLONY_CENTER: case CLAIM_REQUIRED:
            break;
        default:
            g2d.drawImage(this.lib.getScaledImage(ImageLibrary.TILE_TAKEN),
                          0, 0, null);
        }
        int price = colony.getOwner().getLandPrice(tile);
        if (price > 0 && !tile.hasSettlement()) {
            displayCenteredImage(g2d,
                this.lib.getScaledImage(ImageLibrary.TILE_OWNED_BY_INDIANS));
        }

        Unit unit = colonyTile.getOccupyingUnit();
        if (unit != null) {
            BufferedImage image = this.lib.getSmallerUnitImage(unit);
            g2d.drawImage(image,
                          this.tileWidth/4 - image.getWidth() / 2,
                          this.halfHeight - image.getHeight() / 2, null);
            // Draw an occupation and nation indicator.
            Player owner = getMyPlayer();
            String text = Messages.message(unit.getOccupationLabel(owner, false));
            g2d.drawImage(this.lib.getOccupationIndicatorChip(g2d, unit, text),
                          this.lib.scaleInt(STATE_OFFSET_X), 0, null);
        }
    }

    /**
     * Displays the given {@code Tile}.
     *
     * @param g2d The {@code Graphics2D} on which to draw the tile.
     * @param tile The {@code Tile} to draw.
     * @param player The {@code Player} to draw for.
     * @param overlayImage The BufferedImage for the tile overlay.
     */
    private void displayTile(Graphics2D g2d, Tile tile, Player player,
                             BufferedImage overlayImage) {
        displayAnimatedBaseTiles(g2d, tile, false);
        displayTileWithBeach(g2d, tile);
        if (!tile.isExplored()) return;
        if (!getClientOptions().isRiverAnimationEnabled()
                || !tile.hasRiver() && !hasRiverDelta(tile)) {
            drawBaseTileTransitions(g2d, tile);
        }
        
        RescaleOp rop = (player.canSee(tile)) ? null : standardRescale;
        displayTileItems(g2d, tile, rop, overlayImage);
        displaySettlementWithChipsOrPopulationNumber(g2d, tile, false, rop);
        displayOptionalTileText(g2d, tile);
    }

    /**
     * Centers the given Image on the tile.
     *
     * @param g2d The {@code Graphics2D} on which to draw.
     * @param image The {@code BufferedImage} to draw.
     */
    public void displayCenteredImage(Graphics2D g2d, BufferedImage image) {
        displayCenteredImage(g2d, image, null);
    }

    /**
     * Centers the given Image on the tile.
     *
     * @param g2d The {@code Graphics2D} on which to draw.
     * @param image The {@code BufferedImage} to draw.
     * @param rop An optional {@code RescaleOp} for fog of war.
     */
    public void displayCenteredImage(Graphics2D g2d, BufferedImage image,
                                     RescaleOp rop) {
        g2d.drawImage(image, rop,
                      (this.tileWidth - image.getWidth())/2,
                      (this.tileHeight - image.getHeight())/2);
    }

    /**
     * Centers the given Image on the tile, ensuring it is not drawing
     * over tiles south of it.
     *
     * @param g2d The {@code Graphics2D} on which to draw.
     * @param image The {@code BufferedImage} to draw.
     * @param rop An optional {@code RescaleOp} for fog of war.
     */
    private void displayLargeCenteredImage(Graphics2D g2d, BufferedImage image,
                                           RescaleOp rop) {
        int x = (this.tileWidth - image.getWidth())/2,
            y = this.tileHeight - image.getHeight();
        if (y > 0) y /= 2;
        g2d.drawImage(image, rop, x, y);
    }

    /**
     * Draw animated ocean.
     *
     * @param g2d The {@code Graphics2D} object on which to draw the tile.
     * @param tile The {@code Tile} to draw if it is ocean.
     * @param freezeAnimation The animation is paused if set to {@code true}. 
     */
    public void displayAnimatedBaseTiles(Graphics2D g2d, Tile tile, boolean freezeAnimation) {
        final TileType tileType = tile.getType();
        if (tileType == null) {
            return;
        }
        
        /* 
         * TODO: Add a single shared clock for MapViewer and TileViewer.
         *       For now, just support water with 125ms frames.
         */
        final long ticks;
        if (freezeAnimation || !getClientOptions().isTerrainAnimationsEnabled()) {
            ticks = 0;
        } else {
            ticks = System.currentTimeMillis() / 125;
        }
        
        if (tileType.isWater()) { // TODO: And animation enabled
            final List<Direction> directionsWithLand = allDirectionsWithLand(tile);
            if (directionsWithLand.isEmpty()) {
                g2d.drawImage(this.lib.getAnimatedScaledTerrainImage(tileType, ticks), 0, 0, null);
            } else {
                g2d.drawImage(this.lib.getAnimatedScaledWaterAndBeachTerrainImage(tileType, directionsWithLand, ticks), 0, 0, null);
            }
            if (getClientOptions().isRiverAnimationEnabled() && hasRiverDelta(tile)) {
                drawBaseTileTransitions(g2d, tile);
                drawRiverMouth(g2d, tile, ticks);
            }
        } else if (getClientOptions().isRiverAnimationEnabled() && tile.hasRiver()) {
            g2d.drawImage(this.lib.getScaledTerrainImage(tileType, tile.getX(), tile.getY()), 0, 0, null);
            drawBaseTileTransitions(g2d, tile);
            final BufferedImage img = this.lib.getAnimatedScaledRiverTerrainImage(tile, ticks);
            if (img != null) {
                g2d.drawImage(img, 0, 0, null);
            }
        }
    }

    private List<Direction> allDirectionsWithLand(Tile tile) {
        final List<Direction> directionsWithLand = Arrays.asList(Direction.values()).stream().filter(d -> {
            final Tile neighbour = tile.getNeighbourOrNull(d);
            return neighbour != null && neighbour.isLand();
        }).collect(Collectors.toList());
        return directionsWithLand;
    }
    
    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Only base terrain will be drawn.
     *
     * @param g2d The {@code Graphics2D} object on which to draw the tile.
     * @param tile The {@code Tile} to draw.
     */
    public void displayTileWithBeach(Graphics2D g2d, Tile tile) {
        final TileType tileType = tile.getType();
        final int x = tile.getX();
        final int y = tile.getY();

        final boolean outForBaseTileAnimation = (tileType != null) && (tileType.isWater() || getClientOptions().isRiverAnimationEnabled() && tile.hasRiver());
        if (!outForBaseTileAnimation) {
            // ATTENTION: we assume that all base tiles have the same size
            g2d.drawImage(this.lib.getScaledTerrainImage(tileType, x, y), 0, 0, null);
        }

        if (!tile.isExplored()) return;
        
        /* 
         * This is the old beach style. We might want to keep this as an option for
         * systems with low memory.
         *
        if (!tile.isLand() && tile.getStyle() > 0) {
            int edgeStyle = tile.getStyle() >> 4;
            if (edgeStyle > 0) {
                g2d.drawImage(this.lib.getBeachEdgeImage(edgeStyle, x, y),
                              0, 0, null);
            }
            int cornerStyle = tile.getStyle() & 15;
            if (cornerStyle > 0) {
                g2d.drawImage(this.lib.getBeachCornerImage(cornerStyle, x, y),
                              0, 0, null);
            }
        }
        */
    }
    
    void drawBaseTileTransitions(Graphics2D g2d, Tile tile) {
        if (getClientOptions().getRange(ClientOptions.GRAPHICS_QUALITY) == ClientOptions.GRAPHICS_QUALITY_LOWEST) {
            return;
        }
        
        /*
         * We are using masks for creating transitions between base tiles.
         */
        
        drawBaseTileTransitionAtDirection(g2d, tile, Direction.NE);
        drawBaseTileTransitionAtDirection(g2d, tile, Direction.SW);
        
        drawBaseTileTransitionAtDirection(g2d, tile, Direction.NW);
        drawBaseTileTransitionAtDirection(g2d, tile, Direction.SE);
        
        if (!getClientOptions().isRiverAnimationEnabled()) {
            drawRiverMouth(g2d, tile, 0);
        }
    }
    
    private void drawBaseTileTransitionAtDirection(Graphics2D g2d, Tile tile, Direction direction) {
        final boolean useNiceCorners = getClientOptions().getRange(ClientOptions.GRAPHICS_QUALITY) >= ClientOptions.GRAPHICS_QUALITY_NORMAL;
        final boolean useVariations = getClientOptions().getRange(ClientOptions.GRAPHICS_QUALITY) >= ClientOptions.GRAPHICS_QUALITY_HIGH;
        final BufferedImage transitionImage = this.lib.getBaseTileTransitionImage(tile, direction, useNiceCorners, useVariations);
        if (transitionImage != null) {
            g2d.drawImage(transitionImage, 0, 0, null);
        }
    }
    
    boolean hasRiverDelta(Tile tile) {
        for (Direction direction : Direction.longSides) {
            Tile borderingTile = tile.getNeighbourOrNull(direction);
            if (borderingTile == null || tile.isLand() || !borderingTile.isLand() || !tile.isExplored()) {
                continue;
            }
            
            final TileImprovement river = borderingTile.getRiver();
            if (river == null) {
                continue;
            }
            
            final Direction reverseDirection = direction.getReverseDirection();
            final int magnitude = river.getRiverConnection(reverseDirection);
            if (magnitude <= 0) {
                continue;
            }
            return true;
        }
        return false;
    }
    
    private void drawRiverMouth(Graphics2D g2d, Tile tile, long ticks) {
        for (Direction direction : Direction.longSides) {
            Tile borderingTile = tile.getNeighbourOrNull(direction);
            if (borderingTile == null || tile.isLand() || !borderingTile.isLand() || !tile.isExplored()) {
                continue;
            }
            
            final TileImprovement river = borderingTile.getRiver();
            if (river == null) {
                continue;
            }
            
            final Direction reverseDirection = direction.getReverseDirection();
            final int magnitude = river.getRiverConnection(reverseDirection);
            if (magnitude <= 0) {
                continue;
            }

            /*
            final BufferedImage ri = this.lib.getRiverMouthImage(direction, magnitude, tile.getX(), tile.getY());
            g2d.drawImage(ri, 0, 0, null);
            */

            final BufferedImage riverDelta = lib.getAnimatedScaledRiverDeltaTerrainImage(direction, ticks);
            g2d.drawImage(riverDelta, 0, 0, null);
        }
    }

    public void displayUnknownTileBorder(Graphics2D g2d, Tile tile) {
        if (!tile.isExplored()) return;
        
        for (Direction direction : Direction.values()) {
            Tile borderingTile = tile.getNeighbourOrNull(direction);
            if (borderingTile != null && !borderingTile.isExplored()) {
                g2d.drawImage(this.lib.getBorderImage(null, direction,
                                                      tile.getX(), tile.getY()),
                              0, 0, null);
            }
        }
    }

    /**
     * Displays the Tile text for a Tile.
     * Shows tile names, coordinates and colony values.
     *
     * @param g2d The {@code Graphics2D} object on which the text gets drawn.
     * @param tile The {@code Tile} to draw the text on.
     */
    public void displayOptionalTileText(Graphics2D g2d, Tile tile) {
        // Do not bother when the text gets very small
        if (this.tinyFont == null) return;

        String text = null;
        int op = getClientOptions().getInteger(ClientOptions.DISPLAY_TILE_TEXT);
        switch (op) {
        case ClientOptions.DISPLAY_TILE_TEXT_NAMES:
            text = Messages.getName(tile);
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_OWNERS:
            if (tile.getOwner() != null) {
                text = Messages.message(tile.getOwner().getNationLabel());
            }
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_REGIONS:
            if (tile.getRegion() != null) {
                if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
                    && tile.getRegion().getName() == null) {
                    text = tile.getRegion().getSuffix();
                } else {
                    text = Messages.message(tile.getRegion().getLabel());
                }
            }
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_EMPTY:
            break;
        default:
            logger.warning("displayTileText option " + op + " out of range");
            break;
        }

        final FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(Color.BLACK);
        g2d.setFont(this.tinyFont);
        if (text != null) {
            int b = getBreakingPoint(text);
            if (b == -1) {
                g2d.drawString(text,
                    (this.tileWidth - fm.stringWidth(text)) / 2,
                    (this.tileHeight - fm.getAscent()) / 2);
            } else {
                g2d.drawString(text.substring(0, b),
                    (this.tileWidth - fm.stringWidth(text.substring(0, b)))/2,
                    this.halfHeight - (fm.getAscent()*2)/3);
                g2d.drawString(text.substring(b+1),
                    (this.tileWidth - fm.stringWidth(text.substring(b+1)))/2,
                    this.halfHeight + (fm.getAscent()*2)/3);
            }
        }

        if (FreeColDebugger.debugDisplayCoordinates()) {
            String posString = tile.getX() + ", " + tile.getY();
            if (tile.getHighSeasCount() >= 0) {
                posString += "/" + Integer.toString(tile.getHighSeasCount());
            }
            g2d.drawString(posString,
                (this.tileWidth - fm.stringWidth(posString)) / 2,
                (this.tileHeight - fm.getAscent()) / 2);
        }
        String value = DebugUtils.getColonyValue(tile);
        if (value != null) {
            g2d.drawString(value,
                (this.tileWidth - fm.stringWidth(value)) / 2,
                (this.tileHeight - fm.getAscent()) / 2);
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Settlements and Lost
     * City Rumours will be shown.
     *
     * @param g2d The Graphics2D object on which to draw the Tile.
     * @param tile The Tile to draw.
     * @param withNumber Whether to display the number of units present.
     * @param rop An optional RescaleOp for fog of war.
     */
    public void displaySettlementWithChipsOrPopulationNumber(Graphics2D g2d,
        Tile tile, boolean withNumber, RescaleOp rop) {
        final Player player = getMyPlayer();
        final Settlement settlement = tile.getSettlement();
        if (settlement == null) return;
        
        // Draw image of settlement in center of the tile
        BufferedImage sImage = this.lib.getScaledSettlementImage(settlement);
        displayLargeCenteredImage(g2d, sImage, rop);

        if (settlement instanceof Colony) {
            final Colony colony = (Colony)settlement;
            if (withNumber) {
                Font font = (colony.getPreferredSizeChange() > 0)
                    ? this.emphFont : this.normFont;
                if (font != null) {
                    String populationString
                        = Integer.toString(colony.getApparentUnitCount());
                    String colorString = "color.map.productionBonus."
                        + colony.getProductionBonus();
                    // If more units can be added, go larger and use italic
                    BufferedImage stringImage
                        = this.lib.getStringImage(g2d, populationString,
                            ImageLibrary.getColor(colorString), font);
                    displayCenteredImage(g2d, stringImage, rop);
                }
            }

        } else if (settlement instanceof IndianSettlement) {
            IndianSettlement is = (IndianSettlement)settlement;
            BufferedImage chip;
            float xOffset = this.lib.scaleInt(STATE_OFFSET_X);
            float yOffset = this.lib.scaleInt(STATE_OFFSET_Y);
            final int colonyLabels = getClientOptions()
                .getInteger(ClientOptions.DISPLAY_COLONY_LABELS);
            if (colonyLabels != ClientOptions.COLONY_LABELS_MODERN) {
                // Draw the settlement chip
                chip = this.lib.getIndianSettlementChip(g2d, is);
                int cWidth = chip.getWidth();
                g2d.drawImage(chip, rop, (int)xOffset, (int)yOffset);
                xOffset += cWidth + 2;

                // Draw the mission chip if needed.
                Unit missionary = is.getMissionary();
                if (missionary != null) {
                    boolean expert
                        = missionary.hasAbility(Ability.EXPERT_MISSIONARY);
                    g2d.drawImage(this.lib.getMissionChip(g2d,
                            missionary.getOwner(), expert),
                        rop, (int)xOffset, (int)yOffset);
                    xOffset += cWidth + 2;
                }
            }

            // Draw the alarm chip if needed.
            if ((chip = this.lib.getAlarmChip(g2d, is, player)) != null) {
                g2d.drawImage(chip, rop, (int)xOffset, (int)yOffset);
            }
        } else {
            logger.warning("Bogus settlement: " + settlement);
        }
    }

    /**
     * Displays the given tile's items onto the given Graphics2D object.
     * Additions and improvements to Tile will be drawn.
     * Only works for explored tiles.
     *
     * @param g2d The Graphics2D object on which to draw the Tile.
     * @param tile The Tile to draw.
     * @param rop An optional RescaleOp for fog of war.
     * @param overlayImage The BufferedImage for the tile overlay.
     */
    public void displayTileItems(Graphics2D g2d, Tile tile, RescaleOp rop,
                                 BufferedImage overlayImage) {
        // ATTENTION: we assume that only overlays and forests
        // might be taller than a tile.
        
         // layer additions and improvements according to zIndex
        List<TileItem> tileItems = new ArrayList<>(tile.getCompleteItems());
        Collections.sort(tileItems, (a, b) -> {
            return Integer.valueOf(a.getZIndex()).compareTo(b.getZIndex());
        });
        int startIndex = 0;
        for (int index = startIndex; index < tileItems.size(); index++) {
            if (tileItems.get(index).getZIndex() < Tile.OVERLAY_ZINDEX) {
                displayTileItem(g2d, tile, rop, tileItems.get(index));
                startIndex = index + 1;
            } else {
                startIndex = index;
                break;
            }
        }
        // Tile Overlays (eg. hills and mountains)
        if (overlayImage != null) {
            g2d.drawImage(overlayImage, rop,
                0, (this.tileHeight - overlayImage.getHeight()));
        }
        for (int index = startIndex; index < tileItems.size(); index++) {
            if (tileItems.get(index).getZIndex() < Tile.FOREST_ZINDEX) {
                displayTileItem(g2d, tile, rop, tileItems.get(index));
                startIndex = index + 1;
            } else {
                startIndex = index;
                break;
            }
        }
        // Forest
        if (tile.isForested()) {
            BufferedImage forestImage = this.lib.getScaledForestImage(tile.getType(),
                tile.getRiverStyle());
            g2d.drawImage(forestImage, rop, 0, (this.tileHeight - forestImage.getHeight()));
        } else if (getClientOptions().getRange(ClientOptions.GRAPHICS_QUALITY) >= ClientOptions.GRAPHICS_QUALITY_HIGH) {
            drawForestCornerImages(tile, g2d);
        }
        
        // draw all remaining items
        for (TileItem ti : tileItems.subList(startIndex, tileItems.size())) {
            displayTileItem(g2d, tile, rop, ti);
        }
    }


    private void drawForestCornerImages(Tile tile, Graphics2D g1) {
        drawForestCornerImageFor(tile, g1, Direction.NW, Direction.NE, Direction.N);
        drawForestCornerImageFor(tile, g1, Direction.SW, Direction.SE, Direction.S);
        drawForestCornerImageFor(tile, g1, Direction.NE, Direction.SE, Direction.E);
        drawForestCornerImageFor(tile, g1, Direction.NW, Direction.SW, Direction.W);
    }

    private void drawForestCornerImageFor(Tile tile, Graphics2D g1, Direction d1, Direction d2, Direction d3) {
        if (tile.hasRiver()) {
            return;
        }
        final Tile tile1 = tile.getNeighbourOrNull(d1);
        if (tile1 == null || !tile1.isForested()) {
            return;
        }
        final Tile tile2 = tile.getNeighbourOrNull(d2);
        if (hasSameForestedTileType(tile1, tile2)) {
            final BufferedImage forestCornerImage = lib.getForestCornerImage(tile1.getType(), d3);
            if (forestCornerImage != null) {
                g1.drawImage(forestCornerImage, 0, (this.tileHeight - forestCornerImage.getHeight()), null);
            }
        }
    }
    
    private boolean hasSameForestedTileType(Tile t1, Tile t2) {
        return t1 != null && t2 != null && t1.isForested() && t1.getType() == t2.getType();
    }

    /**
     * Draws the given TileItem on the given Tile.
     *
     * @param g2d The {@code Graphics} to draw to.
     * @param tile The {@code Tile} to draw from.
     * @param rop An optional RescaleOp for fog of war.
     * @param item The {@code TileItem} to draw.
     */
    private void displayTileItem(Graphics2D g2d, Tile tile, RescaleOp rop, TileItem item) {
        if (item instanceof TileImprovement) {
            TileImprovement ti = (TileImprovement)item;
            if (!ti.isComplete()) return;
            if (ti.isRoad()) {
                this.rp.displayRoad(g2d, tile, rop);
            } else if (ti.isRiver()) {
                if (getClientOptions().isRiverAnimationEnabled()) {
                    // Painted elsewhere.
                    return;
                }
                final BufferedImage img = this.lib.getAnimatedScaledRiverTerrainImage(tile, 0);
                if (img != null) {
                    g2d.drawImage(img, rop, 0, 0);
                }
            } else {
                BufferedImage img = this.lib.getTileImprovementImage(ti.getType().getId());
                if (img != null) g2d.drawImage(img, rop, 0, 0);
            }
        } else if (item instanceof LostCityRumour) {
            displayCenteredImage(g2d,
                this.lib.getScaledImage(ImageLibrary.LOST_CITY_RUMOUR),
                rop);
        } else if (item instanceof Resource) {
            displayCenteredImage(g2d,
                this.lib.getScaledResourceImage((Resource)item),
                rop);
        }
    }
}
