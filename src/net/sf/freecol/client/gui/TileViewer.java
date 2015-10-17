/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.client.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.GameOptions;
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
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.Utils;

import static net.sf.freecol.common.util.StringUtils.*;


/**
 * TileViewer is a private helper class of MapViewer and SwingGUI.
 * 
 * This class is responsible for drawing map tiles
 * for MapViewer and some GUI-panels.
 */
public final class TileViewer {

    private static final Logger logger = Logger.getLogger(TileViewer.class.getName());


    private static class SortableImage implements Comparable<SortableImage> {

        public final BufferedImage image;
        public final int index;

        public SortableImage(BufferedImage image, int index) {
            this.image = image;
            this.index = index;
        }

        // Implement Comparable<SortableImage>

        @Override
        public int compareTo(SortableImage other) {
            return other.index - this.index;
        }

        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object other) {
            if (other instanceof SortableImage) {
                return this.compareTo((SortableImage)other) == 0;
            }
            return super.equals(other);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 37 * hash + Utils.hashCode(image);
            return 37 * hash + index;
        }
    }

    private final FreeColClient freeColClient;

    private ImageLibrary lib;

    private RoadPainter rp;

    // Helper variables for displaying.
    private int tileHeight, tileWidth, halfHeight, halfWidth;

    // The height offset to paint at (in pixels).
    static final int STATE_OFFSET_X = 25,
                     STATE_OFFSET_Y = 10;

    private final GeneralPath fog = new GeneralPath();


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    TileViewer(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;

        setImageLibraryAndUpdateData(new ImageLibrary());
    }


    /**
     * Gets the contained <code>ImageLibrary</code>.
     * 
     * @return The image library;
     */
    ImageLibrary getImageLibrary() {
        return lib;
    }

    /**
     * Returns the scaled terrain-image for a terrain type (and position 0, 0).
     *
     * @param type The type of the terrain-image to return.
     * @param size The maximum size of the terrain image to return.
     * @return The terrain-image
     */
    static BufferedImage createTileImageWithOverlayAndForest(
            TileType type, Dimension size) {
        Dimension size2 = new Dimension(
            (size.width > 0) ? size.width
                             : (2*ImageLibrary.TILE_SIZE.width*size.height +
                                   (ImageLibrary.TILE_OVERLAY_SIZE.height+1)) /
                               (2*ImageLibrary.TILE_OVERLAY_SIZE.height),
            -1);
        BufferedImage terrainImage = ImageLibrary.getTerrainImage(
            type, 0, 0, size2);
        BufferedImage overlayImage = ImageLibrary.getOverlayImage(
            type, type.getId(), size2);
        BufferedImage forestImage = type.isForested()
            ? ImageLibrary.getForestImage(type, size2)
            : null;
        if (overlayImage == null && forestImage == null) {
            return terrainImage;
        } else {
            int width = terrainImage.getWidth();
            int height = terrainImage.getHeight();
            if (overlayImage != null) {
                height = Math.max(height, overlayImage.getHeight());
            }
            if (forestImage != null) {
                height = Math.max(height, forestImage.getHeight());
            }
            BufferedImage compositeImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, height - terrainImage.getHeight(), null);
            if (overlayImage != null) {
                g.drawImage(overlayImage, 0, height - overlayImage.getHeight(), null);
            }
            if (forestImage != null) {
                g.drawImage(forestImage, 0, height - forestImage.getHeight(), null);
            }
            g.dispose();
            return compositeImage;
        }
    }

    /**
     * Create a <code>BufferedImage</code> and draw a <code>Tile</code> on it.
     * Draws the terrain and improvements.
     *
     * @param tile The Tile to draw.
     * @return The image.
     */
    BufferedImage createTileImageWithBeachBorderAndItems(Tile tile) {
        if (!tile.isExplored())
            return lib.getTerrainImage(null, tile.getX(), tile.getY());
        final TileType tileType = tile.getType();
        Dimension terrainTileSize = lib.tileSize;
        BufferedImage overlayImage = lib.getOverlayImage(tile);
        final int compoundHeight = (overlayImage != null)
            ? overlayImage.getHeight()
            : tileType.isForested()
                ? lib.tileForestSize.height
                : terrainTileSize.height;
        BufferedImage image = new BufferedImage(
            terrainTileSize.width, compoundHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.translate(0, compoundHeight - terrainTileSize.height);
        displayTileWithBeachAndBorder(g, tile);
        displayTileItems(g, tile, overlayImage);
        g.dispose();
        return image;
    }

    /**
     * Create a <code>BufferedImage</code> and draw a <code>Tile</code> on it.
     *
     * @param tile The <code>Tile</code> to draw.
     * @return The image.
     */
    BufferedImage createTileImage(Tile tile) {
        final TileType tileType = tile.getType();
        Dimension terrainTileSize = lib.tileSize;
        BufferedImage overlayImage = lib.getOverlayImage(tile);
        final int compoundHeight = (overlayImage != null)
            ? overlayImage.getHeight()
            : tileType.isForested()
                ? lib.tileForestSize.height
                : terrainTileSize.height;
        BufferedImage image = new BufferedImage(
            terrainTileSize.width, compoundHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.translate(0, compoundHeight - terrainTileSize.height);
        displayTile(g, tile, overlayImage);
        g.dispose();
        return image;
    }

    /**
     * Create a <code>BufferedImage</code> and draw a <code>Tile</code> on it.
     * The visualization of the <code>Tile</code> also includes information
     * from the corresponding <code>ColonyTile</code> of the given
     * <code>Colony</code>.
     *
     * @param tile The <code>Tile</code> to draw.
     * @param colony The <code>Colony</code> to create the visualization
     *      of the <code>Tile</code> for. This object is also used to
     *      get the <code>ColonyTile</code> for the given <code>Tile</code>.
     * @return The image.
     */
    BufferedImage createColonyTileImage(Tile tile, Colony colony) {
        final TileType tileType = tile.getType();
        Dimension terrainTileSize = lib.tileSize;
        BufferedImage overlayImage = lib.getOverlayImage(tile);
        final int compoundHeight = (overlayImage != null)
            ? overlayImage.getHeight()
            : tileType.isForested()
                ? lib.tileForestSize.height
                : terrainTileSize.height;
        BufferedImage image = new BufferedImage(
            terrainTileSize.width, compoundHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.translate(0, compoundHeight - terrainTileSize.height);
        displayColonyTile(g, tile, colony, overlayImage);
        g.dispose();
        return image;
    }

    /**
     * Displays the 3x3 tiles for the TilesPanel in ColonyPanel.
     * 
     * @param g The <code>Graphics2D</code> object on which to draw
     *      the <code>Tile</code>.
     * @param tiles The array containing the <code>Tile</code> objects to draw.
     * @param colony The <code>Colony</code> to create the visualization
     *      of the <code>Tile</code> objects for.
     */
    void displayColonyTiles(Graphics2D g, Tile[][] tiles, Colony colony) {
        Set<String> overlayCache = ImageLibrary.createOverlayCache();
        Dimension tileSize = lib.tileSize;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (tiles[x][y] != null) {
                    int xx = (((2 - x) + y) * tileSize.width) / 2;
                    int yy = ((x + y) * tileSize.height) / 2;
                    g.translate(xx, yy);
                    BufferedImage overlayImage = lib.getOverlayImage(
                        tiles[x][y], overlayCache);
                    displayColonyTile(g, tiles[x][y], colony, overlayImage);
                    g.translate(-xx, -yy);
                }
            }
        }
    }

    /**
     * Displays the given colony tile.
     * The visualization of the <code>Tile</code> also includes information
     * from the corresponding <code>ColonyTile</code> from the given
     * <code>Colony</code>.
     *
     * @param g The <code>Graphics2D</code> on which to draw.
     * @param tile The <code>Tile</code> to draw.
     * @param colony The <code>Colony</code> to create the visualization
     *      of the <code>Tile</code> for. This object is also used to
     *      get the <code>ColonyTile</code> for the given <code>Tile</code>.
     * @param overlayImage The BufferedImage of the tile overlay.
     */
    private void displayColonyTile(Graphics2D g, Tile tile, Colony colony,
                                  BufferedImage overlayImage) {
        displayTile(g, tile, overlayImage);

        ColonyTile colonyTile = colony.getColonyTile(tile);
        switch (colonyTile.getNoWorkReason()) {
        case NONE: case COLONY_CENTER: case CLAIM_REQUIRED:
            break;
        default:
            g.drawImage(lib.getMiscImage(ImageLibrary.TILE_TAKEN),
                        0, 0, null);
        }
        int price = colony.getOwner().getLandPrice(tile);
        if (price > 0 && !tile.hasSettlement()) {
            BufferedImage image = lib.getMiscImage(
                ImageLibrary.TILE_OWNED_BY_INDIANS);
            displayCenteredImage(g, image);
        }

        Unit unit = colonyTile.getOccupyingUnit();
        if (unit != null) {
            BufferedImage image = lib.getSmallerUnitImage(unit);
            g.drawImage(image,
                        tileWidth/4 - image.getWidth() / 2,
                        halfHeight - image.getHeight() / 2, null);
            // Draw an occupation and nation indicator.
            Player owner = freeColClient.getMyPlayer();
            String text = Messages.message(unit.getOccupationLabel(owner, false));
            g.drawImage(lib.getOccupationIndicatorChip(g, unit, text),
                        (int)(STATE_OFFSET_X * lib.getScaleFactor()),
                        0, null);
        }
    }

    /**
     * Displays the given <code>Tile</code>.
     *
     * @param g The Graphics2D on which to draw the <code>Tile</code>.
     * @param tile The <code>Tile</code> to draw.
     * @param overlayImage The BufferedImage for the tile overlay.
     */
    private void displayTile(Graphics2D g, Tile tile, BufferedImage overlayImage) {
        displayTileWithBeachAndBorder(g, tile);
        if (tile.isExplored()) {
            displayTileItems(g, tile, overlayImage);
            displaySettlementWithChipsOrPopulationNumber(g, tile, false);
            displayFogOfWar(g, tile);
            displayOptionalTileText(g, tile);
        }
    }

    /**
     * Sets the ImageLibrary and calculates various items that depend
     * on tile size.
     *
     * @param lib an <code>ImageLibrary</code> value
     */
    void setImageLibraryAndUpdateData(ImageLibrary lib) {
        this.lib = lib;
        // ATTENTION: we assume that all base tiles have the same size
        Dimension tileSize = lib.tileSize;
        rp = new RoadPainter(tileSize);
        tileHeight = tileSize.height;
        tileWidth = tileSize.width;
        halfHeight = tileHeight/2;
        halfWidth = tileWidth/2;

        fog.reset();
        fog.moveTo(halfWidth, 0);
        fog.lineTo(tileWidth, halfHeight);
        fog.lineTo(halfWidth, tileHeight);
        fog.lineTo(0, halfHeight);
        fog.closePath();
    }

    /**
     * Centers the given Image on the tile.
     *
     * @param g a <code>Graphics2D</code>
     * @param image the BufferedImage
     */
    void displayCenteredImage(Graphics2D g, BufferedImage image) {
        g.drawImage(image,
                    (tileWidth - image.getWidth())/2,
                    (tileHeight - image.getHeight())/2,
                    null);
    }

    /**
     * Centers the given Image on the tile, ensuring it is not drawing
     * over tiles south of it.
     *
     * @param g a <code>Graphics2D</code>
     * @param image the BufferedImage
     */
    void displayLargeCenteredImage(Graphics2D g, BufferedImage image) {
        int y = tileHeight - image.getHeight();
        if(y > 0)
            y /= 2;
        g.drawImage(image, (tileWidth - image.getWidth())/2, y, null);
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Only base terrain will be drawn.
     *
     * @param g The Graphics2D object on which to draw the Tile.
     * @param tile The Tile to draw.
     */
    void displayTileWithBeachAndBorder(Graphics2D g, Tile tile) {
        if (tile != null) {
            TileType tileType = tile.getType();
            int x = tile.getX();
            int y = tile.getY();
            // ATTENTION: we assume that all base tiles have the same size
            g.drawImage(lib.getTerrainImage(tileType, x, y),
                        0, 0, null);
            if (tile.isExplored()) {
                if (!tile.isLand() && tile.getStyle() > 0) {
                    int edgeStyle = tile.getStyle() >> 4;
                    if (edgeStyle > 0) {
                        g.drawImage(lib.getBeachEdgeImage(edgeStyle, x, y),
                                    0, 0, null);
                    }
                    int cornerStyle = tile.getStyle() & 15;
                    if (cornerStyle > 0) {
                        g.drawImage(lib.getBeachCornerImage(cornerStyle, x, y),
                                    0, 0, null);
                    }
                }

                List<SortableImage> imageBorders = new ArrayList<>(8);
                SortableImage si;
                for (Direction direction : Direction.values()) {
                    Tile borderingTile = tile.getNeighbourOrNull(direction);
                    if (borderingTile != null && borderingTile.isExplored()) {
                        TileType borderingTileType = borderingTile.getType();
                        if (borderingTileType != tileType) {
                            if (!tile.isLand() && borderingTile.isLand()) {
                                // If there is a Coast image (eg. beach) defined, use it, otherwise skip
                                // Draw the grass from the neighboring tile, spilling over on the side of this tile
                                si = new SortableImage(
                                    lib.getBorderImage(borderingTileType, direction, x, y),
                                    borderingTileType.getIndex());
                                imageBorders.add(si);
                                TileImprovement river = borderingTile.getRiver();
                                if (river != null && river.isConnectedTo(direction.getReverseDirection())) {
                                    si = new SortableImage(
                                        lib.getRiverMouthImage(direction,
                                            borderingTile.getRiver().getMagnitude(), x, y),
                                        -1);
                                    imageBorders.add(si);
                                }
                            } else if (!tile.isLand() || borderingTile.isLand()) {
                                if (borderingTileType.getIndex() < tileType.getIndex() &&
                                        !lib.getTerrainImage(tileType, 0, 0).equals(lib.getTerrainImage(borderingTileType, 0, 0))) {
                                    // Draw land terrain with bordering land type, or ocean/high seas limit,
                                    // if the tiles do not share same graphics (ocean & great river)
                                    si = new SortableImage(
                                            lib.getBorderImage(borderingTileType, direction, x, y),
                                            borderingTileType.getIndex());
                                    imageBorders.add(si);
                                }
                            }
                        }
                    }
                }
                Collections.sort(imageBorders);
                for (SortableImage sorted : imageBorders) {
                    g.drawImage(sorted.image, 0, 0, null);
                }
            }
        }
    }

    void displayUnknownTileBorder(Graphics2D g, Tile tile) {
        for (Direction direction : Direction.values()) {
            Tile borderingTile = tile.getNeighbourOrNull(direction);
            if (borderingTile != null && !borderingTile.isExplored()) {
                g.drawImage(lib.getBorderImage(
                        null, direction, tile.getX(), tile.getY()),
                    0, 0, null);
            }
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates.  Fog of war will be
     * drawn.
     *
     * @param g The <code>Graphics2D</code> object on which to draw
     *     the <code>Tile</code>.
     * @param tile The <code>Tile</code> to draw.
     */
    void displayFogOfWar(Graphics2D g, Tile tile) {
        if (freeColClient.getGame() != null
            && freeColClient.getGame().getSpecification()
                .getBoolean(GameOptions.FOG_OF_WAR)
            && freeColClient.getMyPlayer() != null
            && !freeColClient.getMyPlayer().canSee(tile)) {
            g.setColor(Color.BLACK);
            Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                      0.2f));
            g.fill(fog);
            g.setComposite(oldComposite);
        }
    }

    /**
     * Displays the Tile text for a Tile.
     * Shows tile names, coordinates and colony values.
     *
     * @param g The Graphics2D object on which the text gets drawn.
     * @param tile The Tile to draw the text on.
     */
    void displayOptionalTileText(Graphics2D g, Tile tile) {
        String text = null;
        int op = freeColClient.getClientOptions()
            .getInteger(ClientOptions.DISPLAY_TILE_TEXT);
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

        g.setColor(Color.BLACK);
        g.setFont(FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, lib.getScaleFactor()));
        if (text != null) {
            int b = getBreakingPoint(text);
            if (b == -1) {
                g.drawString(text,
                    (tileWidth - g.getFontMetrics().stringWidth(text)) / 2,
                    (tileHeight - g.getFontMetrics().getAscent()) / 2);
            } else {
                g.drawString(text.substring(0, b),
                    (tileWidth - g.getFontMetrics().stringWidth(text.substring(0, b)))/2,
                    halfHeight - (g.getFontMetrics().getAscent()*2)/3);
                g.drawString(text.substring(b+1),
                    (tileWidth - g.getFontMetrics().stringWidth(text.substring(b+1)))/2,
                    halfHeight + (g.getFontMetrics().getAscent()*2)/3);
            }
        }

        if (FreeColDebugger.debugDisplayCoordinates()) {
            String posString = tile.getX() + ", " + tile.getY();
            if (tile.getHighSeasCount() >= 0) {
                posString += "/" + Integer.toString(tile.getHighSeasCount());
            }
            g.drawString(posString,
                (tileWidth - g.getFontMetrics().stringWidth(posString)) / 2,
                (tileHeight - g.getFontMetrics().getAscent()) / 2);
        }
        String value = DebugUtils.getColonyValue(tile);
        if (value != null) {
            g.drawString(value,
                (tileWidth - g.getFontMetrics().stringWidth(value)) / 2,
                (tileHeight - g.getFontMetrics().getAscent()) / 2);
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Settlements and Lost
     * City Rumours will be shown.
     *
     * @param g The Graphics2D object on which to draw the Tile.
     * @param tile The Tile to draw.
     * @param withNumber Whether to display the number of units present.
     */
    void displaySettlementWithChipsOrPopulationNumber(
            Graphics2D g, Tile tile, boolean withNumber) {
        final Player player = freeColClient.getMyPlayer();
        final Settlement settlement = tile.getSettlement();

        if (settlement != null) {
            if (settlement instanceof Colony) {
                Colony colony = (Colony)settlement;

                // Draw image of colony in center of the tile.
                BufferedImage colonyImage = lib.getSettlementImage(settlement);
                displayLargeCenteredImage(g, colonyImage);

                if (withNumber) {
                    String populationString = Integer.toString(
                        colony.getDisplayUnitCount());
                    int bonus = colony.getProductionBonus();
                    Color theColor = ResourceManager.getColor(
                        "color.map.productionBonus." + bonus);
                    // if government admits even more units, use
                    // italic and bigger number icon
                    Font font = (colony.getPreferredSizeChange() > 0)
                        ? FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
                            FontLibrary.FontSize.SMALLER, Font.BOLD | Font.ITALIC,
                            lib.getScaleFactor())
                        : FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
                            FontLibrary.FontSize.TINY, Font.BOLD,
                            lib.getScaleFactor());
                    BufferedImage stringImage = lib.getStringImage(g,
                        populationString, theColor, font);
                    displayCenteredImage(g, stringImage);
                }

            } else if (settlement instanceof IndianSettlement) {
                IndianSettlement is = (IndianSettlement)settlement;
                BufferedImage settlementImage = lib.getSettlementImage(settlement);

                // Draw image of indian settlement in center of the tile.
                displayCenteredImage(g, settlementImage);

                BufferedImage chip;
                float xOffset = STATE_OFFSET_X * lib.getScaleFactor();
                float yOffset = STATE_OFFSET_Y * lib.getScaleFactor();
                final int colonyLabels = freeColClient.getClientOptions()
                    .getInteger(ClientOptions.COLONY_LABELS);
                if (colonyLabels != ClientOptions.COLONY_LABELS_MODERN) {
                    // Draw the settlement chip
                    chip = lib.getIndianSettlementChip(g, is);
                    g.drawImage(chip, (int)xOffset, (int)yOffset, null);
                    xOffset += chip.getWidth() + 2;

                    // Draw the mission chip if needed.
                    Unit missionary = is.getMissionary();
                    if (missionary != null) {
                        boolean expert
                            = missionary.hasAbility(Ability.EXPERT_MISSIONARY);
                        g.drawImage(lib.getMissionChip(g, missionary.getOwner(),
                                                       expert),
                                    (int)xOffset, (int)yOffset, null);
                        xOffset += chip.getWidth() + 2;
                    }
                }

                // Draw the alarm chip if needed.
                if ((chip = lib.getAlarmChip(g, is, player)) != null) {
                    g.drawImage(chip, (int)xOffset, (int)yOffset, null);
                }
            } else {
                logger.warning("Bogus settlement: " + settlement);
            }
        }
    }

    /**
     * Displays the given tile's items onto the given Graphics2D object.
     * Additions and improvements to Tile will be drawn.
     * Only works for explored tiles.
     *
     * @param g The Graphics2D object on which to draw the Tile.
     * @param tile The Tile to draw.
     * @param overlayImage The BufferedImage for the tile overlay.
     */
    void displayTileItems(Graphics2D g, Tile tile, BufferedImage overlayImage) {
        // ATTENTION: we assume that only overlays and forests
        // might be taller than a tile.

        // layer additions and improvements according to zIndex
        List<TileItem> tileItems = (tile.getTileItemContainer() != null)
            ? tile.getTileItemContainer().getTileItems()
            : new ArrayList<TileItem>();
        int startIndex = 0;
        for (int index = startIndex; index < tileItems.size(); index++) {
            if (tileItems.get(index).getZIndex() < Tile.OVERLAY_ZINDEX) {
                displayTileItem(g, tile, tileItems.get(index));
                startIndex = index + 1;
            } else {
                startIndex = index;
                break;
            }
        }
        // Tile Overlays (eg. hills and mountains)
        if (overlayImage != null) {
            g.drawImage(overlayImage,
                0, (tileHeight - overlayImage.getHeight()), null);
        }
        for (int index = startIndex; index < tileItems.size(); index++) {
            if (tileItems.get(index).getZIndex() < Tile.FOREST_ZINDEX) {
                displayTileItem(g, tile, tileItems.get(index));
                startIndex = index + 1;
            } else {
                startIndex = index;
                break;
            }
        }
        // Forest
        if (tile.isForested()) {
            BufferedImage forestImage = lib.getForestImage(
                tile.getType(), tile.getRiverStyle());
            g.drawImage(forestImage,
                0, (tileHeight - forestImage.getHeight()), null);
        }

        // draw all remaining items
        for (TileItem ti : tileItems.subList(startIndex, tileItems.size())) {
            displayTileItem(g, tile, ti);
        }
    }

    /**
     * Draws the given TileItem on the given Tile.
     */
    private void displayTileItem(Graphics2D g, Tile tile, TileItem item) {
        if (item instanceof TileImprovement) {
            displayTileImprovement(g, tile, (TileImprovement)item);
        } else if (item instanceof LostCityRumour) {
            displayLostCityRumour(g);
        } else {
            displayResourceTileItem(g, (Resource) item);
        }
    }

    private void displayResourceTileItem(Graphics2D g, Resource item) {
        BufferedImage bonusImage = lib.getMiscImage(
            "image.tileitem." + item.getType().getId());
        displayCenteredImage(g, bonusImage);
    }

    private void displayLostCityRumour(Graphics2D g) {
        displayCenteredImage(g,
            lib.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR));
    }

    private void displayTileImprovement(Graphics2D g,
                                        Tile tile, TileImprovement  ti) {
        if (ti.isComplete()) {
            if (ti.isRoad()) {
                rp.displayRoad(g, tile);
            } else if (ti.isRiver()
                    && ti.getMagnitude() < TileImprovement.FJORD_RIVER) {
                // @compat 0.10.5
                // America_large had some bogus rivers in 0.10.5
                if (ti.getStyle() != null)
                // end @compat 0.10.5
                    g.drawImage(lib.getRiverImage(ti.getStyle()), 0, 0, null);
            } else {
                String key = "image.tile." + ti.getType().getId();
                if (ResourceManager.hasImageResource(key)) {
                    // Has its own Overlay Image in Misc, use it
                    BufferedImage overlay = ResourceManager.getImage(key,
                        lib.tileSize);
                    g.drawImage(overlay, 0, 0, null);
                }
            }
        }
    }

}
