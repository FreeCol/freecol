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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JComponent;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.SettlementType;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementStyle;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;

import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Holds various images that can be called upon by others in order to display
 * certain things.
 */
public final class ImageLibrary {

    private static final Logger logger = Logger.getLogger(ImageLibrary.class.getName());

    /**
     * Canonical sizes of images GUI elements and map are expecting
     * and current image files have.
     * ICON_SIZE, TILE_SIZE, TILE_OVERLAY_SIZE and TILE_FOREST_SIZE constants
     * are used in this way already, allowing them to tolerate changing sizes
     * in the files.
     * Most other images are currently still shown in a size of image size
     * times scaling factor times requested size.
     */
    public static final Dimension ICON_SIZE = new Dimension(32, 32),
                                  BUILDING_SIZE = new Dimension(128, 96),
                                  TILE_SIZE = new Dimension(128, 64),
                                  TILE_OVERLAY_SIZE = new Dimension(128, 96),
                                  TILE_FOREST_SIZE = new Dimension(128, 84);

    public static final String DELETE = "image.miscicon.delete",
                               PLOWED = "image.tile.model.improvement.plow",
                               UNIT_SELECT = "image.tile.unitSelect",
                               TILE_TAKEN = "image.tile.tileTaken",
                               TILE_OWNED_BY_INDIANS = "image.tileitem.nativeLand",
                               LOST_CITY_RUMOUR = "image.tileitem.lostCityRumour",
                               DARKNESS = "image.halo.dark",
                               ICON_LOCK = "image.icon.lock",
                               ICON_COIN = "image.icon.coin",
                               BELLS = "image.icon.model.goods.bells";

    public static enum PathType {
        NAVAL,
        WAGON,
        HORSE,
        FOOT;

        /**
         * Get a key for this path type.
         *
         * @return A key.
         */
        private String getKey() {
            return getEnumKey(this);
        }

        public String getImageKey() {
            return "image.tileitem.path." + getKey();
        }

        public String getNextTurnImageKey() {
            return "image.tileitem.path." + getKey() + ".nextTurn";
        }

        /**
         * Get the broad class of image to show along unit paths.
         *
         * @param u A <code>Unit</code> to classify.
         * @return A suitable <code>PathType</code>.
         */
        public static PathType getPathType(Unit u) {
            return (u == null) ? PathType.FOOT
                : (u.isNaval()) ? PathType.NAVAL
                : (u.isMounted()) ? PathType.HORSE
                : (u.isPerson()) ? PathType.FOOT
                : PathType.WAGON;
        }
    };


    /**
     * The scale factor used when creating this
     * <code>ImageLibrary</code>.  The value <code>1</code> is used if
     * this object is not a result of a scaling operation.
     */
    private final float scaleFactor;

    final Dimension tileSize, tileOverlaySize, tileForestSize;

    private final HashMap<String,BufferedImage> stringImageCache;

    /**
     * The constructor to use when needing an unscaled <code>ImageLibrary</code>.
     */
    public ImageLibrary() {
        this(1f);
    }

    /**
     * The constructor to use when needing a scaled <code>ImageLibrary</code>.
     * 
     * Please avoid using too many different scaling factors, as this will
     * lead to wasted memory for caching images in ResourceManager!
     * Currently, 0.25, 0.5, ..., 2.0 are used for the map. Colony tiles and
     * the GUI use 1.0 here (maybe also 1.25, 1.5, 1.75 and 2.0 in future),
     * but this gets multiplied for tiny 0.25(rarely, candidate for removal),
     * smaller 0.5, small 0.75 and normal 1.0 image retrieval methods.
     * 
     * @param scaleFactor The factor used when scaling. 2 is twice
     *      the size of the original images and 0.5 is half.
     */
    public ImageLibrary(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        tileSize = scaleDimension(TILE_SIZE, scaleFactor);
        tileOverlaySize = scaleDimension(TILE_OVERLAY_SIZE, scaleFactor);
        tileForestSize = scaleDimension(TILE_FOREST_SIZE, scaleFactor);
        stringImageCache = new HashMap<>();
    }


    /**
     * Returns the scaling factor used when creating this <code>ImageLibrary</code>.
     * It is 1 if the constructor without scaling factor was used to create
     * this object.
     * @return The scaling factor of this ImageLibrary.
     */
    public float getScaleFactor() {
        return scaleFactor;
    }

    public Dimension scaleDimension(Dimension size) {
        return scaleDimension(size, scaleFactor);
    }

    public static Dimension scaleDimension(Dimension size, float scaleFactor) {
        return new Dimension(Math.round(size.width*scaleFactor),
                             Math.round(size.height*scaleFactor));
    }

    /**
     * Gets a suitable foreground color given a background color.
     *
     * Our eyes have different sensitivity towards red, green and
     * blue.  We want a foreground color with the inverse brightness.
     *
     * @param background The background <code>Color</code> to complement.
     * @return A suitable foreground <code>Color</code>.
     */
    public static Color getForegroundColor(Color background) {
        return (background == null
            || (background.getRed() * 0.3 + background.getGreen() * 0.59
                + background.getBlue() * 0.11 >= 126))
            ? Color.BLACK
            : Color.WHITE;
    }

    /**
     * The string border colors should be black unless the color of
     * the string is really dark.
     *
     * @param color The <code>Color</code> to complement.
     * @return A suitable border <code>Color</code>.
     */
    public static Color getStringBorderColor(Color color) {
        return (color.getRed() * 0.3
            + color.getGreen() * 0.59
            + color.getBlue() * 0.11 < 10) ? Color.WHITE
            : Color.BLACK;
    }


    // Simple Image retrieval methods

    /**
     * Returns true if the tile with the given coordinates is to be
     * considered "even".  This is useful to select different images
     * for the same tile type in order to prevent big stripes or a
     * checker-board effect.
     *
     * @param x an <code>int</code> value
     * @param y an <code>int</code> value
     * @return a <code>boolean</code> value
     */
    private static boolean isEven(int x, int y) {
        return ((y % 8 <= 2) || ((x + y) % 2 == 0 ));
    }

    /**
     * Returns the beach corner image at the given index.
     *
     * @param index The index of the image to return.
     * @param x an <code>int</code> value
     * @param y an <code>int</code> value
     * @return The image at the given index.
     */
    public BufferedImage getBeachCornerImage(int index, int x, int y) {
        return ResourceManager.getImage("image.tile.model.tile.beach.corner" + index
                                        + ".r" + ((isEven(x, y)) ? "0" : "1"),
                                        tileSize);
    }

    /**
     * Returns the beach edge image at the given index.
     *
     * @param index The index of the image to return.
     * @param x an <code>int</code> value
     * @param y an <code>int</code> value
     * @return The image at the given index.
     */
    public BufferedImage getBeachEdgeImage(int index, int x, int y) {
        return ResourceManager.getImage("image.tile.model.tile.beach.edge" + index
                                        + ".r"+ ((isEven(x, y)) ? "0" : "1"),
                                        tileSize);
    }

    /**
     * Returns the border terrain-image for the given type.
     *
     * @param type The type of the terrain-image to return.
     * @param direction a <code>Direction</code> value
     * @param x The x-coordinate of the location of the tile that is being
     *     drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *     drawn.
     * @return The terrain-image at the given index.
     */
    public BufferedImage getBorderImage(TileType type, Direction direction,
                                int x, int y) {
        String key = (type == null) ? "model.tile.unexplored" : type.getId();
        return ResourceManager.getImage("image.tile." + key + ".border." + direction
                                        + ".r" + ((isEven(x, y)) ?  "0" : "1"),
                                        tileSize);
    }

    /**
     * Returns the forest image for a terrain type.
     *
     * @param type The type of the terrain-image to return.
     * @return The image at the given index.
     */
    public BufferedImage getForestImage(TileType type) {
        return getForestImage(type, tileForestSize);
    }

    public static BufferedImage getForestImage(TileType type, Dimension size) {
        return ResourceManager.getImage("image.tileforest." + type.getId(),
                                        size);
    }

    public BufferedImage getForestImage(TileType type,
                                        TileImprovementStyle riverStyle) {
        return getForestImage(type, riverStyle, tileForestSize);
    }

    public static BufferedImage getForestImage(TileType type,
                                               TileImprovementStyle riverStyle,
                                               Dimension size) {
        if (riverStyle != null) {
            String key = "image.tileforest." + type.getId() + ".s" + riverStyle.getMask();
            // @compat 0.10.6
            // Workaround for BR#3599586.  America_large used to contain
            // tiles with an isolated river (old river style="0"!).
            // There will never be an image for these, so just drop the
            // river style.  The map is now fixed, this is just for the
            // the saved games.
            // Consider keeping the fallback, as its safer to have one.
            if (ResourceManager.hasImageResource(key))
            // end @compat
                return ResourceManager.getImage(key, size);
        }
        return ResourceManager.getImage("image.tileforest." + type.getId(), size);
    }

    /**
     * Returns the portrait of this Founding Father.
     *
     * @param father a <code>FoundingFather</code> value
     * @param grey if the image should be in greyscale
     * @return an <code>BufferedImage</code> value
     */
    public static BufferedImage getFoundingFatherImage(FoundingFather father, boolean grey) {
        String resource = "image.flavor." + father.getId();
        return grey
            ? ResourceManager.getGrayscaleImage(resource, 1f)
            : ResourceManager.getImage(resource);
    }

    public BufferedImage getSmallBuildableImage(BuildableType buildable, Player player) {
        // FIXME: distinguish national unit types
        float scale = scaleFactor * 0.75f;
        BufferedImage image = (buildable instanceof BuildingType)
            ? ImageLibrary.getBuildingImage(
                (BuildingType) buildable, player, scale)
            : ImageLibrary.getUnitImage((UnitType)buildable, scale);
        return image;
    }

    public static BufferedImage getBuildableImage(BuildableType buildable, Dimension size) {
        BufferedImage image = (buildable instanceof BuildingType)
            ? ImageLibrary.getBuildingImage((BuildingType) buildable, size)
            : ImageLibrary.getUnitImage((UnitType)buildable, size);
        return image;
    }

    public BufferedImage getSmallBuildingImage(Building building) {
        return getBuildingImage(building.getType(), building.getOwner(),
            scaleFactor * 0.75f);
    }

    public BufferedImage getBuildingImage(Building building) {
        return getBuildingImage(building.getType(), building.getOwner(),
            scaleFactor);
    }

    public static BufferedImage getBuildingImage(BuildingType buildingType,
                                                 Player player, float scale) {
        String key = "image.buildingicon." + buildingType.getId()
            + "." + player.getNationResourceKey();
        if (!ResourceManager.hasImageResource(key)) {
            key = "image.buildingicon." + buildingType.getId();
        }
        return ResourceManager.getImage(key, scale);
    }

    public static BufferedImage getBuildingImage(BuildingType buildingType,
                                                 float scale) {
        return ResourceManager.getImage("image.buildingicon."
            + buildingType.getId(), scale);
    }

    public static BufferedImage getBuildingImage(BuildingType buildingType,
                                                 Dimension size) {
        return ResourceManager.getImage("image.buildingicon."
            + buildingType.getId(), size);
    }

    public BufferedImage getSmallerIconImage(FreeColGameObjectType type) {
        return getMiscImage("image.icon." + type.getId(),
            scaleDimension(ICON_SIZE, scaleFactor * 0.5f));
    }

    public BufferedImage getSmallIconImage(FreeColGameObjectType type) {
        return getMiscImage("image.icon." + type.getId(),
            scaleDimension(ICON_SIZE, scaleFactor * 0.75f));
    }

    public BufferedImage getIconImage(FreeColGameObjectType type) {
        return getMiscImage("image.icon." + type.getId(),
            scaleDimension(ICON_SIZE, scaleFactor));
    }

    public BufferedImage getSmallerMiscIconImage(FreeColGameObjectType type) {
        return getMiscIconImage(type, scaleFactor * 0.5f);
    }

    public BufferedImage getSmallMiscIconImage(FreeColGameObjectType type) {
        return getMiscIconImage(type, scaleFactor * 0.75f);
    }

    public BufferedImage getMiscIconImage(FreeColGameObjectType type) {
        return getMiscIconImage(type, scaleFactor);
    }

    public static BufferedImage getMiscIconImage(FreeColGameObjectType type, float scale) {
        return ResourceManager.getImage("image.miscicon." + type.getId(), scale);
    }

    public static BufferedImage getMiscIconImage(FreeColGameObjectType type, Dimension size) {
        return ResourceManager.getImage("image.miscicon." + type.getId(), size);
    }

    /**
     * Returns the image with the given identifier.
     *
     * @param id The object identifier.
     * @return The image.
     */
    public BufferedImage getMiscImage(String id) {
        return getMiscImage(id, scaleFactor);
    }

    public static BufferedImage getMiscImage(String id, float scale) {
        return ResourceManager.getImage(id, scale);
    }

    public static BufferedImage getMiscImage(String id, Dimension size) {
        return ResourceManager.getImage(id, size);
    }

    /**
     * Returns the appropriate BufferedImage for a FreeColObject.
     * Please, use a more specific method!
     *
     * @param display The FreeColObject to display.
     * @param scale How much the image should be scaled.
     * @return The appropriate BufferedImage.
     */
    public BufferedImage getObjectImage(FreeColObject display, float scale) {
        try {
            final float combinedScale = scaleFactor * scale;
            final Dimension size = scaleDimension(ICON_SIZE, combinedScale);
            BufferedImage image;
            if (display instanceof Goods) {
                display = ((Goods)display).getType();
            } else if (display instanceof Player) {
                display = ((Player)display).getNation();
            }

            if (display instanceof Unit) {
                Unit unit = (Unit)display;
                image = getUnitImage(unit, size);
            } else if (display instanceof UnitType) {
                UnitType unitType = (UnitType)display;
                image = getUnitImage(unitType, size);
            } else if (display instanceof Settlement) {
                Settlement settlement = (Settlement)display;
                image = getSettlementImage(settlement, size);
            } else if (display instanceof LostCityRumour) {
                image = getMiscImage(ImageLibrary.LOST_CITY_RUMOUR, size);
            } else if (display instanceof GoodsType) {
                FreeColGameObjectType type = (FreeColGameObjectType)display;
                image = getIconImage(type);
            } else if (display instanceof Nation) {
                FreeColGameObjectType type = (FreeColGameObjectType)display;
                image = getMiscIconImage(type, size);
            } else if (display instanceof BuildingType) {
                BuildingType type = (BuildingType)display;
                image = getBuildingImage(type, size);
            } else {
                logger.warning("could not find image of unknown type for " + display);
                return null;
            }

            if (image == null) {
                logger.warning("could not find image for " + display);
                return null;
            }
            return image;
        } catch (Exception e) {
            logger.log(Level.WARNING, "could not find image", e);
            return null;
        }
    }

    /**
     * Returns the monarch-image for the given tile.
     *
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public static BufferedImage getMonarchImage(Nation nation) {
        return ResourceManager.getImage("image.flavor.monarch." + nation.getId());
    }

    /**
     * Returns the overlay image for the given tile.
     *
     * @param tile The tile for which to return an image.
     * @return A pseudo-random terrain image.
     */
    public BufferedImage getOverlayImage(Tile tile) {
        return getOverlayImage(tile.getType(), tile.getId(), tileOverlaySize);
    }

    /**
     * Returns the overlay-image for the given type and scale.
     * Currently used for hills and mountains.
     *
     * @param type The type of the terrain-image to return.
     * @param id A string used to get a random image.
     * @param size The size of the image to return.
     * @return The terrain-image at the given index.
     */
    public static BufferedImage getOverlayImage(TileType type, String id,
                                                Dimension size) {
        String prefix = "image.tileoverlay." + type.getId();
        List<String> keys = ResourceManager.getImageKeys(prefix);
        return getRandomizedImage(keys, id, size);
    }

    public static Set<String> createOverlayCache() {
        return ResourceManager.getImageKeySet("image.tileoverlay.");
    }

    public BufferedImage getOverlayImage(Tile tile, Set<String> overlayCache) {
        return getOverlayImage(tile.getType(), tile.getId(), tileOverlaySize,
                               overlayCache);
    }

    public static BufferedImage getOverlayImage(TileType type, String id,
                                                Dimension size,
                                                Set<String> overlayCache) {
        final String prefix = "image.tileoverlay." + type.getId() + ".r";
        final List<String> keys = overlayCache.stream()
            .filter(k -> k.startsWith(prefix))
            .collect(Collectors.toList());
        return getRandomizedImage(keys, id, size);
    }

    private static BufferedImage getRandomizedImage(List<String> keys,
                                                    String id, Dimension size) {
        int count = keys.size();
        switch(count) {
            case 0:
                return null;
            case 1:
                return ResourceManager.getImage(keys.get(0), size);
            default:
                Collections.sort(keys);
                return ResourceManager.getImage(
                    keys.get(Math.abs(id.hashCode() % count)), size);
        }
    }

    /**
     * Gets an image to represent the path of given path type.
     *
     * @param pt The <code>PathType</code>
     * @return The <code>BufferedImage</code>.
     */
    public static BufferedImage getPathImage(PathType pt) {
        return (pt == null) ? null
            : ResourceManager.getImage(pt.getImageKey());
    }

    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     *
     * @param u The <code>Unit</code>
     * @return The <code>BufferedImage</code>.
     */
    public static BufferedImage getPathImage(Unit u) {
        return (u == null) ? null
            : getPathImage(PathType.getPathType(u));
    }

    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     *
     * @param pt The <code>PathType</code>
     * @return The <code>BufferedImage</code>.
     */
    public static BufferedImage getPathNextTurnImage(PathType pt) {
        return (pt == null) ? null
            : ResourceManager.getImage(pt.getNextTurnImageKey());
    }

    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     *
     * @param u The <code>Unit</code>
     * @return The <code>BufferedImage</code>.
     */
    public static BufferedImage getPathNextTurnImage(Unit u) {
        return (u == null) ? null
            : getPathNextTurnImage(PathType.getPathType(u));
    }

    /**
     * Returns the river image with the given style.
     *
     * @param style a <code>TileImprovementStyle</code> value
     * @return The image with the given style.
     */
    public BufferedImage getRiverImage(TileImprovementStyle style) {
        return getRiverImage(style.getString(), tileSize);
    }

    /**
     * Returns the river image with the given style.
     *
     * @param style the style code
     * @param size the image size
     * @return The image with the given style.
     */
    public static BufferedImage getRiverImage(String style, Dimension size) {
        return ResourceManager.getImage(
            "image.tile.model.improvement.river.s" + style, size);
    }

    /**
     * Returns the river mouth terrain-image for the direction and magnitude.
     *
     * @param direction a <code>Direction</code> value
     * @param magnitude an <code>int</code> value
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn (ignored).
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn (ignored).
     * @return The terrain-image at the given index.
     */
    public BufferedImage getRiverMouthImage(Direction direction, int magnitude,
                                    int x, int y) {
        String key = "image.tile.model.tile.delta." + direction
            + (magnitude == 1 ? ".small" : ".large");
        return ResourceManager.getImage(key, tileSize);
    }

    public BufferedImage getSmallSettlementImage(Settlement settlement) {
        return getSettlementImage(settlement, scaleFactor * 0.75f);
    }

    /**
     * Returns the graphics that will represent the given settlement.
     *
     * @param settlement The settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public BufferedImage getSettlementImage(Settlement settlement) {
        return getSettlementImage(settlement, scaleFactor);
    }

    /**
     * Returns the graphics that will represent the given settlement.
     *
     * @param settlement The settlement whose graphics type is needed.
     * @param scale a <code>double</code> value
     * @return The graphics that will represent the given settlement.
     */
    public static BufferedImage getSettlementImage(Settlement settlement, float scale) {
        return ResourceManager.getImage(settlement.getImageKey(), scale);
    }

    public static BufferedImage getSettlementImage(Settlement settlement, Dimension size) {
        return ResourceManager.getImage(settlement.getImageKey(), size);
    }

    /**
     * Returns the graphics that will represent the given settlement.
     *
     * @param settlementType The type of settlement whose graphics
     *     type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public BufferedImage getSettlementImage(SettlementType settlementType) {
        return getSettlementImage(settlementType, scaleFactor);
    }

    public static BufferedImage getSettlementImage(SettlementType settlementType,
                                    float scale) {
        return ResourceManager.getImage("image.tileitem." + settlementType.getId(),
                                        scale);
    }

    /**
     * Returns the terrain-image for the given type.
     *
     * @param type The type of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public BufferedImage getTerrainImage(TileType type, int x, int y) {
        return getTerrainImage(type, x, y, tileSize);
    }

    public static BufferedImage getTerrainImage(TileType type, int x, int y,
                                                Dimension size) {
        String key = (type == null) ? "model.tile.unexplored" : type.getId();
        return ResourceManager.getImage("image.tile." + key + ".center.r"
            + (isEven(x, y) ? "0" : "1"), size);
    }

    public BufferedImage getSmallerUnitImage(Unit unit) {
        return getUnitImage(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), false, scaleFactor * 0.5f);
    }

    public BufferedImage getSmallUnitImage(Unit unit) {
        return getUnitImage(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), false, scaleFactor * 0.75f);
    }

    public BufferedImage getSmallUnitImage(Unit unit, boolean grayscale) {
        return getUnitImage(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), grayscale, scaleFactor * 0.75f);
    }

    public BufferedImage getUnitImage(Unit unit) {
        return getUnitImage(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), false, scaleFactor);
    }

    public BufferedImage getUnitImage(Unit unit, boolean grayscale) {
        return getUnitImage(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), grayscale, scaleFactor);
    }

    public static BufferedImage getUnitImage(Unit unit, float scale) {
        return getUnitImage(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), false, scale);
    }

    public BufferedImage getTinyUnitImage(UnitType unitType) {
        return getUnitImage(unitType, unitType.getDisplayRoleId(),
            false, false, scaleFactor * 0.25f);
    }

    public BufferedImage getTinyUnitImage(UnitType unitType, boolean grayscale) {
        return getUnitImage(unitType, unitType.getDisplayRoleId(),
            false, grayscale, scaleFactor * 0.25f);
    }

    public BufferedImage getSmallerUnitImage(UnitType unitType) {
        return getUnitImage(unitType, unitType.getDisplayRoleId(),
            false, false, scaleFactor * 0.5f);
    }

    public BufferedImage getSmallUnitImage(UnitType unitType) {
        return getUnitImage(unitType, unitType.getDisplayRoleId(),
            false, false, scaleFactor * 0.75f);
    }

    public BufferedImage getSmallUnitImage(UnitType unitType, boolean grayscale) {
        return getUnitImage(unitType, unitType.getDisplayRoleId(),
            false, grayscale, scaleFactor * 0.75f);
    }

    public BufferedImage getSmallUnitImage(UnitType unitType, String roleId,
                                   boolean grayscale) {
        return getUnitImage(unitType, roleId,
            false, grayscale, scaleFactor * 0.75f);
    }

    public BufferedImage getUnitImage(UnitType unitType) {
        return getUnitImage(unitType, unitType.getDisplayRoleId(),
            false, false, scaleFactor);
    }

    public static BufferedImage getUnitImage(UnitType unitType, float scale) {
        return getUnitImage(unitType, unitType.getDisplayRoleId(),
            false, false, scale);
    }

    /**
     * Gets the image that will represent a given unit.
     *
     * @param unitType The type of unit to be represented.
     * @param roleId The id of the unit role.
     * @param nativeEthnicity If true the unit is a former native.
     * @param grayscale If true draw in inactive/disabled-looking state.
     * @param scale How much the image is scaled.
     * @return A suitable <code>BufferedImage</code>.
     */
    public static BufferedImage getUnitImage(UnitType unitType, String roleId,
                                             boolean nativeEthnicity,
                                             boolean grayscale, float scale) {
        // units that can only be native don't need the .native key part
        if (unitType.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)) {
            nativeEthnicity = false;
        }

        // try to get an image matching the key
        String roleQual = (Role.isDefaultRoleId(roleId)) ? ""
            : "." + Role.getRoleSuffix(roleId);
        String key = "image.unit." + unitType.getId() + roleQual
            + ((nativeEthnicity) ? ".native" : "");
        if (!ResourceManager.hasImageResource(key) && nativeEthnicity) {
            key = "image.unit." + unitType.getId() + roleQual;
        }
        BufferedImage image = (grayscale)
            ? ResourceManager.getGrayscaleImage(key, scale)
            : ResourceManager.getImage(key, scale);
        return image;
    }

    public static BufferedImage getUnitImage(Unit unit, Dimension size) {
        return getUnitImage(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), size);
    }

    public static BufferedImage getUnitImage(UnitType unitType, Dimension size) {
        String roleId = unitType.getDisplayRoleId();
        String roleQual = (Role.isDefaultRoleId(roleId)) ? ""
            : "." + Role.getRoleSuffix(roleId);
        String key = "image.unit." + unitType.getId() + roleQual;
        return ResourceManager.getImage(key, size);
    }

    public static BufferedImage getUnitImage(UnitType unitType, String roleId,
                                             boolean nativeEthnicity,
                                             Dimension size) {
        // units that can only be native don't need the .native key part
        if (unitType.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)) {
            nativeEthnicity = false;
        }

        // try to get an image matching the key
        String roleQual = (Role.isDefaultRoleId(roleId)) ? ""
            : "." + Role.getRoleSuffix(roleId);
        String key = "image.unit." + unitType.getId() + roleQual
            + ((nativeEthnicity) ? ".native" : "");
        if (!ResourceManager.hasImageResource(key) && nativeEthnicity) {
            key = "image.unit." + unitType.getId() + roleQual;
        }
        BufferedImage image = ResourceManager.getImage(key, size);
        return image;
    }


    // Methods for dynamically drawing images

    /**
     * Draw a (usually small) background image into a (usually larger)
     * space specified by a component, tiling the image to fill up the
     * space.  If the image is not available, just fill with the background
     * colour.
     *
     * @param resource The name of the <code>ImageResource</code> to tile with.
     * @param g The <code>Graphics</code> to draw to.
     * @param c The <code>JComponent</code> that defines the space.
     * @param insets Optional <code>Insets</code> to apply.
     */
    public static void drawTiledImage(String resource, Graphics g,
                                      JComponent c, Insets insets) {
        int width = c.getWidth();
        int height = c.getHeight();
        int xmin, ymin;
        if (insets == null) {
            xmin = 0;
            ymin = 0;
        } else {
            xmin = insets.left;
            ymin = insets.top;
            width -= insets.left + insets.right;
            height -= insets.top + insets.bottom;
        }

        if (ResourceManager.hasImageResource(resource)) {
            BufferedImage image = ResourceManager.getImage(resource);
            int dx = image.getWidth();
            int dy = image.getHeight();
            int xmax = xmin + width;
            int ymax = ymin + height;
            for (int x = xmin; x < xmax; x += dx) {
                for (int y = ymin; y < ymax; y += dy) {
                    g.drawImage(image, x, y, null);
                }
            }
        } else {
            g.setColor(c.getBackground());
            g.fillRect(xmin, ymin, width, height);
        }
    }

    /**
     * Fills a certain rectangle with the image texture.
     * 
     * @param g2 The <code>Graphics</code> used for painting the border.
     * @param img The <code>BufferedImage</code> to fill the texture.
     * @param x The x-component of the offset.
     * @param y The y-component of the offset.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     */
    public static void fillTexture(Graphics2D g2, BufferedImage img,
                                   int x, int y, int width, int height) {
        Rectangle anchor = new Rectangle(
            x, y, img.getWidth(), img.getHeight());
        TexturePaint paint = new TexturePaint(img, anchor);
        g2.setPaint(paint);
        g2.fillRect(x, y, width, height);
    }


    /**
     * Creates a buffered image out of a given <code>Image</code> object.
     * 
     * @param image The <code>Image</code> object.
     * @return The created <code>BufferedImage</code> object.
     */
    public static BufferedImage createBufferedImage(Image image) {
        if(image == null)
            return null;
        BufferedImage result = new BufferedImage(image.getWidth(null),
            image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    public static BufferedImage createMirroredImage(Image image) {
        if(image == null)
            return null;
        final int width = image.getWidth(null);
        final int height = image.getHeight(null);
        BufferedImage result = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, width, 0, -width, height, null);
        g.dispose();
        return result;
    }

    public static BufferedImage createResizedImage(Image image,
                                                  int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    /**
     * Create a faded version of an image.
     *
     * @param img The <code>Image</code> to fade.
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
     * Create a "chip" with the given text and colors.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param text The text to display.
     * @param border The border <code>Color</code>.
     * @param background The background <code>Color</code>.
     * @param foreground The foreground <code>Color</code>.
     * @return A chip.
     */
    private BufferedImage createChip(Graphics2D g, String text, Color border,
                                     Color background, Color foreground) {
        Font font = FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
            FontLibrary.FontSize.TINY, Font.BOLD, scaleFactor);
        FontMetrics fm = g.getFontMetrics(font);
        int padding = (int)(6 * scaleFactor);
        BufferedImage bi = new BufferedImage(fm.stringWidth(text) + padding,
            fm.getMaxAscent() + fm.getMaxDescent() + padding,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setFont(font);
        int width = bi.getWidth();
        int height = bi.getHeight();
        g2.setColor(border);
        g2.fillRect(0, 0, width, height);
        g2.setColor(background);
        g2.fillRect(1, 1, width - 2, height - 2);
        g2.setColor(foreground);
        g2.drawString(text, padding/2, fm.getMaxAscent() + padding/2);
        g2.dispose();
        return bi;
    }

    /**
     * Create a filled "chip" with the given text and colors.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param text The text to display.
     * @param border The border <code>Color</code>.
     * @param background The background <code>Color</code>.
     * @param amount How much to fill the chip with the fill color
     * @param fill The fill <code>Color</code>.
     * @param foreground The foreground <code>Color</code>.
     * @return A chip.
     */
    private BufferedImage createFilledChip(Graphics2D g, String text,
                                           Color border, Color background,
                                           double amount, Color fill,
                                           Color foreground) {
        Font font = FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
            FontLibrary.FontSize.TINY, Font.BOLD, scaleFactor);
        FontMetrics fm = g.getFontMetrics(font);
        int padding = (int)(6 * scaleFactor);
        BufferedImage bi = new BufferedImage(fm.stringWidth(text) + padding,
            fm.getMaxAscent() + fm.getMaxDescent() + padding,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setFont(font);
        int width = bi.getWidth();
        int height = bi.getHeight();
        g2.setColor(border);
        g2.fillRect(0, 0, width, height);
        g2.setColor(background);
        g2.fillRect(1, 1, width - 2, height - 2);
        if (amount > 0.0 && amount <= 1.0) {
            g2.setColor(fill);
            g2.fillRect(1, 1, width - 2, (int)((height - 2) * amount));
        }
        g2.setColor(foreground);
        g2.drawString(text, padding/2, fm.getMaxAscent() + padding/2);
        g2.dispose();
        return bi;
    }


    // Methods for dynamically drawing images and adding them to ResourceManager

    /**
     * Gets a chip image for the alarm at an Indian settlement.
     * The background is either the native owner's, or that of the
     * most hated nation if any.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param is The <code>IndianSettlement</code> to check.
     * @param player The observing <code>Player</code>.
     * @return An alarm chip, or null if none suitable.
     */
    public BufferedImage getAlarmChip(Graphics2D g,
                                      IndianSettlement is, Player player) {
        if (player == null || !is.hasContacted(player)) return null;
        Color ownerColor = is.getOwner().getNationColor();
        Player enemy = is.getMostHated();
        Color enemyColor = (enemy == null) ? Nation.UNKNOWN_NATION_COLOR
            : enemy.getNationColor();
        // Set amount to [0-4] corresponding to HAPPY, CONTENT,
        // DISPLEASED, ANGRY, HATEFUL but only if the player is the
        // most hated, because other nation alarm is not nor should be
        // serialized to the client.
        int amount = 4;
        if (enemy == null) {
            amount = 0;
        } else if (player == enemy) {
            Tension alarm = is.getAlarm(enemy);
            amount = (alarm == null) ? 4 : alarm.getLevel().ordinal();
            if (amount == 0) amount = 1; // Show *something*!
        }
        Color foreground = getForegroundColor(enemyColor);
        String text = ResourceManager.getString((is.worthScouting(player))
                                                ? "indianAlarmChip.contacted"
                                                : "indianAlarmChip.scouted");
        return createFilledChip(g, text, Color.BLACK, ownerColor, amount/4.0,
                                   enemyColor, foreground);
    }

    /**
     * Gets the owner chip for the settlement.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param is The <code>IndianSettlement</code> to check.
     * @return A chip.
     */
    public BufferedImage getIndianSettlementChip(Graphics2D g,
                                                 IndianSettlement is) {
        String text = ResourceManager.getString("indianSettlementChip."
            + ((is.getType().isCapital()) ? "capital" : "normal"));
        Color background = is.getOwner().getNationColor();
        return createChip(g, text, Color.BLACK, background,
                          getForegroundColor(background));
    }

    /**
     * Gets the mission chip for a native settlement.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param owner The player that owns the mission.
     * @param expert True if the unit is an expert.
     * @return A suitable chip, or null if no mission is present.
     */
    public BufferedImage getMissionChip(Graphics2D g,
                                        Player owner, boolean expert) {
        Color background = owner.getNationColor();
        String key = "color.foreground.mission."
            + ((expert) ? "expert" : "normal");
        Color foreground;
        if (ResourceManager.hasColorResource(key)) {
            foreground = ResourceManager.getColor(key);
        } else {
            foreground = (expert) ? Color.BLACK : Color.GRAY;
        }
        return createChip(g, ResourceManager.getString("cross"),
                          Color.BLACK, background, foreground);
    }

    /**
     * Gets a chip for an occupation indicator, i.e. a small image with a
     * single letter or symbol that indicates the Unit's state.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param unit The <code>Unit</code> with the occupation.
     * @param text The text for the chip.
     * @return A suitable chip.
     */
    public BufferedImage getOccupationIndicatorChip(Graphics2D g,
                                                    Unit unit, String text) {
        Color backgroundColor = unit.getOwner().getNationColor();
        Color foregroundColor = (unit.getState() == Unit.UnitState.FORTIFIED)
            ? Color.GRAY : getForegroundColor(backgroundColor);
        return createChip(g, text, Color.BLACK, backgroundColor, foregroundColor);
    }

    /**
     * Gets an image with a string of a given color and with
     * a black border around the glyphs.
     *
     * @param g A <code>Graphics</code>-object for getting the font metrics.
     * @param text The <code>String</code> to make an image of.
     * @param color The <code>Color</code> to use for the text.
     * @param font The <code>Font</code> to display the text with.
     * @return The image that was created.
     */
    public BufferedImage getStringImage(Graphics g, String text, Color color,
                                Font font) {
        if (color == null) {
            logger.warning("createStringImage called with color null");
            color = Color.WHITE;
        }

        // Lookup in the cache if the image has been generated already
        String key = text
            + "." + font.getFontName().replace(' ', '-')
            + "." + Integer.toString(font.getSize())
            + "." + Integer.toHexString(color.getRGB());
        BufferedImage img = stringImageCache.get(key);
        if (img != null) {
            return img;
        }
        // create an image of the appropriate size
        FontMetrics fm = g.getFontMetrics(font);
        int width = fm.stringWidth(text) + 4;
        int height = fm.getMaxAscent() + fm.getMaxDescent();
        BufferedImage bi = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        // draw the string with selected color
        Graphics2D g2 = bi.createGraphics();
        g2.setColor(getStringBorderColor(color));
        g2.setFont(font);
        g2.drawString(text, 2, fm.getMaxAscent());

        // draw the border around letters
        int borderWidth = 1;
        int borderColor = getStringBorderColor(color).getRGB();
        int srcRGB, dstRGB, srcA;
        for (int biY = 0; biY < height; biY++) {
            for (int biX = borderWidth; biX < width - borderWidth; biX++) {
                int biXI = width - biX - 1;
                for (int d = 1; d <= borderWidth; d++) {
                    // left to right
                    srcRGB = bi.getRGB(biX, biY);
                    srcA = (srcRGB >> 24) & 0xFF;
                    dstRGB = bi.getRGB(biX - d, biY);
                    if (dstRGB != borderColor) {
                        if (srcA > 0) {
                            bi.setRGB(biX, biY, borderColor);
                            bi.setRGB(biX - d, biY, srcRGB);
                        }
                    }
                    // right to left
                    srcRGB = bi.getRGB(biXI, biY);
                    srcA = (srcRGB >> 24) & 0xFF;
                    dstRGB = bi.getRGB(biXI + d, biY);
                    if (dstRGB != borderColor) {
                        if (srcA > 0) {
                            bi.setRGB(biXI, biY, borderColor);
                            bi.setRGB(biXI + d, biY, srcRGB);
                        }
                    }
                }
            }
        }
        for (int biX = 0; biX < width; biX++) {
            for (int biY = borderWidth; biY < height - borderWidth; biY++) {
                int biYI = height - biY - 1;
                for (int d = 1; d <= borderWidth; d++) {
                    // top to bottom
                    srcRGB = bi.getRGB(biX, biY);
                    srcA = (srcRGB >> 24) & 0xFF;
                    dstRGB = bi.getRGB(biX, biY - d);
                    if (dstRGB != borderColor) {
                        if (srcA > 0) {
                            bi.setRGB(biX, biY, borderColor);
                            bi.setRGB(biX, biY - d, srcRGB);
                        }
                    }
                    // bottom to top
                    srcRGB = bi.getRGB(biX, biYI);
                    srcA = (srcRGB >> 24) & 0xFF;
                    dstRGB = bi.getRGB(biX, biYI + d);
                    if (dstRGB != borderColor) {
                        if (srcA > 0) {
                            bi.setRGB(biX, biYI, borderColor);
                            bi.setRGB(biX, biYI + d, srcRGB);
                        }
                    }
                }
            }
        }

        g2.setColor(color);
        g2.drawString(text, 2, fm.getMaxAscent());
        g2.dispose();

        stringImageCache.put(key, bi);
        return bi;
    }

}
