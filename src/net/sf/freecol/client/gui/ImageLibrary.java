/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import net.sf.freecol.common.io.sza.SimpleZippedAnimation;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColSpecObjectType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.SettlementType;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementStyle;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.Video;
import static net.sf.freecol.common.util.CollectionUtils.*;
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

    /**
     * Constants for the current "named" scales (tiny, smaller, small) plus
     * a trivial value unscaled.
     */
    public static final float TINY_SCALE = 0.25f,
                              SMALLER_SCALE = 0.5f,
                              SMALL_SCALE = 0.75f,
                              NORMAL_SCALE = 1f;

    // TODO: should these be hidden?
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
    private static String RIVER_STYLE_PREFIX
        = "image.tile.model.improvement.river.s";

    /** The action button key prefixes. */
    private static final List<String> buttonKeys
        = makeUnmodifiableList("image.miscicon.button.normal.",
                               "image.miscicon.button.highlighted.",
                               "image.miscicon.button.pressed.",
                               "image.miscicon.button.disabled.");

    /** Helper to distinguish different types of paths. */
    public enum PathType {
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
         * @param u A {@code Unit} to classify.
         * @return A suitable {@code PathType}.
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
     * {@code ImageLibrary}.  The value {@code 1} is used if
     * this object is not a result of a scaling operation.
     */
    private final float scaleFactor;

    /** Fixed tile dimensions. */
    final Dimension tileSize, tileOverlaySize, tileForestSize;

    /** Cache for the string images. */
    private final HashMap<String,BufferedImage> stringImageCache;


    /**
     * The constructor to use for an unscaled {@code ImageLibrary}.
     */
    public ImageLibrary() {
        this(NORMAL_SCALE);
    }

    /**
     * The constructor to use for a scaled {@code ImageLibrary}.
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
        this.tileSize = scaleDimension(TILE_SIZE, scaleFactor);
        this.tileOverlaySize = scaleDimension(TILE_OVERLAY_SIZE, scaleFactor);
        this.tileForestSize = scaleDimension(TILE_FOREST_SIZE, scaleFactor);
        this.stringImageCache = new HashMap<>();
    }


    /**
     * Get the scaling factor used when creating this {@code ImageLibrary}.
     *
     * It is 1f if the constructor without scaling factor was used to create
     * this object.
     *
     * @return The scaling factor of this ImageLibrary.
     */
    public float getScaleFactor() {
        return this.scaleFactor;
    }

    /**
     * Scale a dimension with the current internal scale.
     *
     * @param size The {@code Dimension} to scale.
     * @return The scaled {@code Dimension}.
     */
    public Dimension scale(Dimension size) {
        return scaleDimension(size, this.scaleFactor);
    }

    /**
     * Scale a dimension with the current internal scale and an extra override.
     *
     * @param size The {@code Dimension} to scale.
     * @param extraFactor An extra scaling.
     * @return The scaled {@code Dimension}.
     */
    public Dimension scale(Dimension size, float extraFactor) {
        return scaleDimension(size, this.scaleFactor * extraFactor);
    }
    
    /**
     * Absolute dimenion scaling helper routine.
     *
     * @param size The {@code Dimension} to scale.
     * @param scaleFactor The scale to use.
     * @return The scaled {@code Dimension}.
     */
    public static Dimension scaleDimension(Dimension size, float scaleFactor) {
        return new Dimension(Math.round(size.width * scaleFactor),
                             Math.round(size.height * scaleFactor));
    }

    /**
     * Should the tile with the given coordinates be considered "even"?
     *
     * This is useful to select different images for the same tile
     * type in order to prevent big stripes or a checker-board effect.
     *
     * @param x The tile x coordinate.
     * @param y The tile y coordinate.
     * @return True if the tile should be considered even.
     */
    private static boolean isSpecialEven(int x, int y) {
        return ((y % 8 <= 2) || ((x + y) % 2 == 0));
    }

    private static BufferedImage getRandomizedImage(List<String> keys,
                                                    String id, Dimension size) {
        final int count = keys.size();
        switch (count) {
        case 0:
            return null;
        case 1:
            return getSizedImageInternal(keys.get(0), size, false);
        default:
            keys.sort(Comparator.naturalOrder());
            String key = keys.get(Math.abs(id.hashCode() % count));
            return getSizedImageInternal(key, size, false);
        }
    }


    // Animation handling

    public static SimpleZippedAnimation getSZA(String key, float scale) {
        return (!ResourceManager.hasSZAResource(key)) ? null
            : ResourceManager.getSZA(key, scale);
    }


    // Color handling

    /**
     * Derive a suitable foreground color from a background color.
     *
     * Our eyes have different sensitivity towards red, green and
     * blue.  We want a foreground color with the inverse brightness.
     *
     * @param background The background {@code Color} to complement.
     * @return A suitable foreground {@code Color}.
     */
    public static Color makeForegroundColor(Color background) {
        return (background == null
            || (background.getRed() * 0.3 + background.getGreen() * 0.59
                + background.getBlue() * 0.11 >= 126))
            ? Color.BLACK
            : Color.WHITE;
    }

    /**
     * Derive a string border color from the string color.  Black
     * unless the color of the string is really dark.
     *
     * @param color The {@code Color} to complement.
     * @return A suitable border {@code Color}.
     */
    private static Color makeStringBorderColor(Color color) {
        return (color.getRed() * 0.3
            + color.getGreen() * 0.59
            + color.getBlue() * 0.11 < 10) ? Color.WHITE
            : Color.BLACK;
    }

    /**
     * Get a color.
     *
     * @param key The color name.
     * @return The {@code Color} found by the resource manager.
     */
    public static Color getColor(final String key) {
        return getColor(key, null);
    }

    /**
     * Get a color.
     *
     * @param key The color name.
     * @param replacement A replacement {@code Color} to use if the named color
     *     can not be found by the resource manager.
     * @return The {@code Color} found by the resource manager.
     */
    public static Color getColor(final String key, final Color replacement) {
        return ResourceManager.getColor(key, replacement);
    }

    /**
     * Get a foreground color for a given goods type and amount in a
     * given location.
     *
     * @param goodsType The {@code GoodsType} to use.
     * @param amount The amount of goods.
     * @param location The {@code Location} for the goods.
     * @return A suitable {@code Color}.
     */
    public static Color getGoodsColor(GoodsType goodsType, int amount,
                                      Location location) {
        final String key = (!goodsType.limitIgnored()
            && location instanceof Colony
            && ((Colony)location).getWarehouseCapacity() < amount)
            ? "color.foreground.GoodsLabel.capacityExceeded"
            : (location instanceof Colony && goodsType.isStorable()
                && ((Colony)location).getExportData(goodsType).getExported())
            ? "color.foreground.GoodsLabel.exported"
            : (amount == 0)
            ? "color.foreground.GoodsLabel.zeroAmount"
            : (amount < 0)
            ? "color.foreground.GoodsLabel.negativeAmount"
            : "color.foreground.GoodsLabel.positiveAmount";
        return getColor(key, Color.BLACK);
    }

    public static Color getMinimapBackgroundColor() {
        return getColor("color.background.MiniMap");
    }

    public static Color getMinimapBorderColor() {
        return getColor("color.border.MiniMap");
    }

    public static Color getMinimapEconomicColor(TileType type) {
        final String key = "color.economic.MiniMap." + type.getId();
        return getColor(key);
    }

    public static Color getMinimapPoliticsColor(TileType type) {
        final String key = "color.politics.MiniMap." + type.getId();
        return getColor(key);
    }

    /**
     * Get the road color.
     *
     * @return The road color.
     */
    public static Color getRoadColor() {
        return getColor("color.map.road");
    }


    // Image manipulation

    /**
     * Draw a (usually small) background image into a (usually larger)
     * space specified by a component, tiling the image to fill up the
     * space.  If the image is not available, just fill with the background
     * colour.
     *
     * @param key The name of the {@code ImageResource} to tile with.
     * @param g The {@code Graphics} to draw to.
     * @param c The {@code JComponent} that defines the space.
     * @param insets Optional {@code Insets} to apply.
     */
    public static void drawTiledImage(String key, Graphics g,
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

        if (ResourceManager.hasImageResource(key)) {
            BufferedImage image = getUnscaledImage(key);
            // FIXME: Test and profile if calling fillTexture is better.
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
     * @param g2 The {@code Graphics} used for painting the border.
     * @param img The {@code BufferedImage} to fill the texture.
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
     * Creates a buffered image out of a given {@code Image} object.
     * 
     * @param image The {@code Image} object.
     * @return The created {@code BufferedImage} object.
     */
    public static BufferedImage createBufferedImage(Image image) {
        if (image == null) return null;
        BufferedImage result = new BufferedImage(image.getWidth(null),
            image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    public static BufferedImage createMirroredImage(Image image) {
        if (image == null) return null;
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
     * @param img The {@code Image} to fade.
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


    // Fundamental image retrieval

    /**
     * Just get an image without any scaling.
     *
     * @param key The image key.
     * @return The image found.
     */
    public static BufferedImage getUnscaledImage(String key) {
        return ResourceManager.getImage(key);
    }

    /**
     * Get the image for the given identifier, scaling and grayscale choice.
     *
     * This routine *should* remain private, so as to discourage arbitrary
     * choices of scale factor.  Callers should use the
     * get{Scaled,Small,Smaller,Tiny}Foo routines unless there is a compelling
     * reason for a special size.
     *
     * @param key The image key.
     * @param scale The scale factor.
     * @param grayscale If true, return the grayscale version of the image.
     * @return The {@code BufferedImage} found by the {@code ResourceManager}.
     */
    private static BufferedImage getScaledImageInternal(String key, float scale,
                                                        boolean grayscale) {
        return ResourceManager.getImage(key, scale, grayscale);
    }

    /**
     * Get the image for the given identifier, size and grayscale choice.
     *
     * @param key The image key.
     * @param size The {@code Dimension} required.
     * @param grayscale If true, return the grayscale version of the image.
     * @return The {@code BufferedImage} found by the {@code ResourceManager}.
     */
    private static BufferedImage getSizedImageInternal(String key,
                                                       Dimension size,
                                                       boolean grayscale) {
        return ResourceManager.getImage(key, size, grayscale);
    }

    /**
     * Get the image for the given identifier, using the current scaling.
     *
     * @param key The image key.
     * @return The {@code BufferedImage} found by the {@code ResourceManager}.
     */
    public BufferedImage getScaledImage(String key) {
        return getScaledImageInternal(key, this.scaleFactor, false);
    }

    public BufferedImage getSmallImage(String key) {
        return getScaledImageInternal(key, this.scaleFactor * SMALL_SCALE, false);
    }

    public BufferedImage getSmallerImage(String key) {
        return getScaledImageInternal(key, this.scaleFactor * SMALLER_SCALE, false);
    }
    
    public BufferedImage getTinyImage(String key) {
        return getScaledImageInternal(key, this.scaleFactor * TINY_SCALE, false);
    }


    // Miscellaneous image handling

    /**
     * Get the button images for a given key.
     *
     * @param key The key to look up.
     * @return The list of button images found.
     */
    public static List<BufferedImage> getButtonImages(final String key) {
        List<BufferedImage> ret = new ArrayList<>();
        for (String base : buttonKeys) {
            String k = base + key;
            if (ResourceManager.hasImageResource(k)) {
                ret.add(getUnscaledImage(k));
            }
        }
        return ret;
    }
        
    public static BufferedImage getCanvasBackgroundImage() {
        final String key = "image.flavor.Canvas.map";
        return (!ResourceManager.hasImageResource(key)) ? null
            : getUnscaledImage(key);
    }

    public static BufferedImage getColopediaCellImage(boolean expanded) {
        final String key = "image.icon.Colopedia."
            + ((expanded) ? "open" : "closed") + "Section";
        return getUnscaledImage(key);
    }

    public static BufferedImage getColorCellRendererBackground() {
        return getUnscaledImage("image.background.ColorCellRenderer");
    }
    
    /**
     * Get the standard cursor.
     *
     * @return A suitable default {@code Cursor}.
     */
    public static Cursor getCursor() {
        String key = "image.icon.cursor.go";
        if (ResourceManager.hasImageResource(key)) {
            Image im = getUnscaledImage(key);
            return Toolkit.getDefaultToolkit().createCustomCursor(im,
                new Point(im.getWidth(null)/2, im.getHeight(null)/2), "go");
        }
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }
    
    /**
     * Returns the portrait of this Founding Father.
     *
     * @param father The {@code FoundingFather} to look for.
     * @param grayscale True if the image should be grayscale.
     * @return The {@code BufferedImage} found.
     */
    public static BufferedImage getFoundingFatherImage(FoundingFather father,
                                                       boolean grayscale) {
        final String key = "image.flavor." + father.getId();
        return getScaledImageInternal(key, NORMAL_SCALE, grayscale);
    }

    public static BufferedImage getInformationPanelSkin(Player player) {
        String key = (player == null) ? "image.skin.InformationPanel"
            : (player.isRebel()) ? "image.skin.InformationPanel.rebel"
            : "image.skin.InformationPanel." + player.getNationResourceKey();
        if (!ResourceManager.hasImageResource(key))
            key = "image.skin.InformationPanel";
        return getUnscaledImage(key);
    }

    public static BufferedImage getLCRImage(Dimension size) {
        return getSizedImageInternal(LOST_CITY_RUMOUR, size, false);
    }

    public static BufferedImage getLibertyImage() {
        return getSizedImageInternal(BELLS, ICON_SIZE, false);
    }

    public JLabel getLockLabel() {
        return new JLabel(new ImageIcon(getSmallerImage(ICON_LOCK)));
    }

    public static BufferedImage getMeetingImage(Player meet) {
        final String base = "image.flavor.event.meeting.";
        String key = base + meet.getNationResourceKey();
        if (!ResourceManager.hasImageResource(key)) key = base + "natives";
        return getUnscaledImage(key);
    }
        
    public static BufferedImage getMiniMapBackground() {
        return getUnscaledImage("image.background.MiniMap");
    }

    public static BufferedImage getMiniMapSkin() {
        final String key = "image.skin.MiniMap";
        return (!ResourceManager.hasImageResource(key)) ? null
            : getUnscaledImage(key);
    }

    /**
     * Get the appropriate BufferedImage for a FreeColObject.
     *
     * @param display The {@code FreeColObject} to display.
     * @param size The image size.
     * @return The appropriate {@code BufferedImage}.
     */
    private static BufferedImage getObjectImageInternal(FreeColObject display,
                                                        Dimension size) {
        final FreeColObject derived = display.getDisplayObject();
        // Not all types have a meaningful image.
        BufferedImage image = (derived instanceof BuildingType)
            ? getBuildingTypeImage((BuildingType)derived, size)
            : (derived instanceof GoodsType)
            ? getGoodsTypeImage((GoodsType)derived, size)
            : (derived instanceof LostCityRumour)
            ? getLCRImage(size)
            : (derived instanceof Nation)
            ? getNationImage((Nation)derived, size)
            : (derived instanceof ResourceType)
            ? getResourceTypeImage((ResourceType)derived, size, false)
            : (derived instanceof Settlement)
            ? getSettlementImage((Settlement)derived, size)
            : (derived instanceof TileType)
            ? getTerrainImage((TileType)derived, 0, 0, size)
            : (derived instanceof UnitType)
            ? getUnitTypeImage((UnitType)derived, size)
            : null;
        if (image == null) {
            logger.warning("Could not find image for " + display);
            return null;
        }
        return image;
    }

    /**
     * Get the appropriate BufferedImage for a FreeColObject.
     *
     * Please use a more specific method!
     *
     * @param display The {@code FreeColObject} to display.
     * @param scale How much the image should be scaled.
     * @return The appropriate {@code BufferedImage}.
     */
    public BufferedImage getObjectImage(FreeColObject display, float scale) {
        return getObjectImageInternal(display, scale(ICON_SIZE, scale));
    }

    /**
     * Get the appropriate BufferedImage for a FreeColObject.
     *
     * Please use a more specific method!
     *
     * @param display The {@code FreeColObject} to display.
     * @param size The image size.
     * @return The appropriate {@code BufferedImage}.
     */
    public BufferedImage getObjectImage(FreeColObject display, Dimension size) {
        return getObjectImageInternal(display, size);
    }

    /**
     * Get the generic placeholder image.
     *
     * @return The placeholder {@code BufferedImage}.
     */     
    public static BufferedImage getPlaceholderImage() {
        return getSizedImageInternal("image.unit.placeholder",
                                     ICON_SIZE, false);
    }
    
    public static BufferedImage getReplacementImage(Dimension size) {
        return getSizedImageInternal(ResourceManager.REPLACEMENT_IMAGE, size, false);
    }


    // BuildingType/Building/Buildable handling

    private static String getBuildingTypeKey(BuildingType buildingType) {
        return "image.buildingicon." + buildingType.getId();
    }
    
    public static BufferedImage getBuildableTypeImage(BuildableType buildable,
                                                      Dimension size) {
        return (buildable instanceof BuildingType)
            ? getBuildingTypeImage((BuildingType)buildable, size)
            : getUnitTypeImage((UnitType)buildable, size);
    }

    public BufferedImage getSmallBuildableTypeImage(BuildableType buildable,
                                                    Player player) {
        float scale = this.scaleFactor * SMALL_SCALE;
        return (buildable instanceof BuildingType)
            ? getScaledBuildingTypeImage((BuildingType)buildable, player, scale)
            : getUnitTypeImage((UnitType)buildable, scale);
    }

    public static BufferedImage getBuildingTypeImage(BuildingType buildingType,
                                                     Dimension size) {
        final String key = getBuildingTypeKey(buildingType);
        return getSizedImageInternal(key, size, false);
    }

    public BufferedImage getScaledBuildingTypeImage(BuildingType buildingType,
                                                    float scale) {
        final String key = getBuildingTypeKey(buildingType);
        return getScaledImageInternal(key, scale, false);
    }

    private BufferedImage getScaledBuildingTypeImage(BuildingType buildingType,
                                                     Player player,
                                                     float scale) {
        final String key = getBuildingTypeKey(buildingType);
        final String extraKey = key + "." + player.getNationResourceKey();
        final boolean hasExtra = ResourceManager.hasImageResource(extraKey);
        return getScaledImageInternal((hasExtra) ? extraKey : key, scale, false);
    }

    public BufferedImage getScaledBuildingImage(Building building) {
        return getScaledBuildingTypeImage(building.getType(),
                                          building.getOwner(),
                                          this.scaleFactor);
    }

    public BufferedImage getSmallBuildingImage(Building building) {
        return getScaledBuildingTypeImage(building.getType(),
                                          building.getOwner(),
                                          this.scaleFactor * SMALL_SCALE);
    }


    // Goods image handling

    public static BufferedImage getGoodsTypeImage(GoodsType gt,
                                                  Dimension size) {
        final String key = "image.icon." + gt.getId();
        return getSizedImageInternal(key, size, false);
    }
    
    public BufferedImage getScaledGoodsTypeImage(GoodsType gt) {
        return getGoodsTypeImage(gt, scale(ICON_SIZE));
    }

    public BufferedImage getSmallGoodsTypeImage(GoodsType gt) {
        return getGoodsTypeImage(gt, scale(ICON_SIZE, SMALL_SCALE));
    }

    public BufferedImage getSmallerGoodsTypeImage(GoodsType gt) {
        return getGoodsTypeImage(gt, scale(ICON_SIZE, SMALLER_SCALE));
    }
    

    // Nation image handling

    private static String getMercenaryLeaderKey(int n) {
        return "image.flavor.model.mercenaries." + n;
    }

    private static String getMonarchKey(String nationId) {
        return "image.flavor.monarch." + nationId;
    }

    /**
     * Returns the monarch-image for the given tile.
     *
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public static BufferedImage getMonarchImage(Nation nation) {
        final String key = getMonarchKey(nation.getId());
        return getUnscaledImage(key);
    }

    /**
     * Get the "monarch" image from a key.
     *
     * The key may be a nation identifier, or if an integer it is a
     * mercenary leader.
     *
     * @param monarchKey The key to examine.
     * @return A suitable {@code BufferedImage}.
     */
    public static BufferedImage getMonarchImage(String monarchKey) {
        String key;
        try {
            int n = Integer.parseInt(monarchKey);
            key = getMercenaryLeaderKey(n);
        } catch (Exception e) {
            key = getMonarchKey(monarchKey);
        }
        return getUnscaledImage(key);
    }
            
        
    private static String getNationKey(Nation nation) {
        return "image.miscicon." + nation.getId();
    }
    
    public static BufferedImage getNationImage(Nation nation, Dimension size) {
        
        return getSizedImageInternal(getNationKey(nation), size, false);
    }

    public static BufferedImage getNationImage(Nation nation, float scale) {
        return getScaledImageInternal(getNationKey(nation), scale, false);
    }
    
    public BufferedImage getScaledNationImage(Nation nation) {
        return getNationImage(nation, this.scaleFactor);
    }

    public BufferedImage getSmallNationImage(Nation nation) {
        return getNationImage(nation, this.scaleFactor * SMALL_SCALE);
    }

    public BufferedImage getSmallerNationImage(Nation nation) {
        return getNationImage(nation, this.scaleFactor * SMALLER_SCALE);
    }


    // Path type image handling

    /**
     * Gets an image to represent the path of given path type.
     *
     * @param pt The {@code PathType}
     * @return The {@code BufferedImage}.
     */
    public static BufferedImage getPathImage(PathType pt) {
        return (pt == null) ? null
            : getUnscaledImage(pt.getImageKey());
    }

    /**
     * Gets an image to represent the path of the given {@code Unit}.
     *
     * @param u The {@code Unit}
     * @return The {@code BufferedImage}.
     */
    public static BufferedImage getPathImage(Unit u) {
        return (u == null) ? null
            : getPathImage(PathType.getPathType(u));
    }

    /**
     * Gets an image to represent the path of the given {@code Unit}.
     *
     * @param pt The {@code PathType}
     * @return The {@code BufferedImage}.
     */
    private static BufferedImage getPathNextTurnImage(PathType pt) {
        return (pt == null) ? null
            : getUnscaledImage(pt.getNextTurnImageKey());
    }

    /**
     * Gets an image to represent the path of the given {@code Unit}.
     *
     * @param u The {@code Unit}
     * @return The {@code BufferedImage}.
     */
    public static BufferedImage getPathNextTurnImage(Unit u) {
        return (u == null) ? null
            : getPathNextTurnImage(PathType.getPathType(u));
    }

    
    // Terrain image handling

    /**
     * Returns the beach corner image at the given index.
     *
     * @param index The index of the image to return.
     * @param x an {@code int} value
     * @param y an {@code int} value
     * @return The image at the given index.
     */
    public BufferedImage getBeachCornerImage(int index, int x, int y) {
        final String key = "image.tile.model.tile.beach.corner" + index
            + ".r" + ((isSpecialEven(x, y)) ? "0" : "1");
        return getSizedImageInternal(key, this.tileSize, false);
    }

    /**
     * Returns the beach edge image at the given index.
     *
     * @param index The index of the image to return.
     * @param x an {@code int} value
     * @param y an {@code int} value
     * @return The image at the given index.
     */
    public BufferedImage getBeachEdgeImage(int index, int x, int y) {
        final String key = "image.tile.model.tile.beach.edge" + index
            + ".r"+ ((isSpecialEven(x, y)) ? "0" : "1");
        return getSizedImageInternal(key, this.tileSize, false);
    }

    /**
     * Returns the border terrain-image for the given type.
     *
     * @param type The type of the terrain-image to return.
     * @param direction a {@code Direction} value
     * @param x The x-coordinate of the tile that is being drawn.
     * @param y The x-coordinate of the tile that is being drawn.
     * @return The terrain-image at the given index.
     */
    public BufferedImage getBorderImage(TileType type, Direction direction,
                                        int x, int y) {
        final String key = "image.tile."
            + ((type==null) ? "model.tile.unexplored" : type.getId())
            + ".border." + direction
            + ".r" + ((isSpecialEven(x, y)) ?  "0" : "1");
        return getSizedImageInternal(key, this.tileSize, false);
    }


    /**
     * Get the forest image for a terrain type.
     *
     * @param type The type of the terrain-image to return.
     * @param riverStyle An optional river style to apply.
     * @param size The image size.
     * @return The image at the given index.
     */
    private static BufferedImage getForestImageInternal(TileType type,
                                                        TileImprovementStyle riverStyle,
                                                        Dimension size) {
        String key;
        if (riverStyle != null) {
            String mask = riverStyle.getMask();
            // Ensure unconnected river tiles are visible in map editor
            key = "image.tileforest." + type.getId() + ".s"
                + (("0000".equals(mask)) ? "0100" : mask);
            // Safety check providing fallback for incomplete mods
            if (ResourceManager.hasImageResource(key)) {
                return getSizedImageInternal(key, size, false);
            }
        }
        key = "image.tileforest." + type.getId();
        return getSizedImageInternal(key, size, false);
    }

    public static BufferedImage getForestImage(TileType type, Dimension size) {
        return getForestImageInternal(type, null, size);
    }

    public BufferedImage getScaledForestImage(TileType type) {
        return getForestImageInternal(type, null, this.tileForestSize);
    }

    public BufferedImage getScaledForestImage(TileType type,
                                              TileImprovementStyle riverStyle) {
        return getForestImageInternal(type, riverStyle, this.tileForestSize);
    }


    /**
     * Get the overlay-image for the given type and scale.
     * Currently used for hills and mountains.
     *
     * @param type The type of the terrain-image to return.
     * @param id A string used to get a random image.
     * @param size The size of the image to return.
     * @param overlayCache An optional overlay cache to draw from.
     * @return The terrain-image at the given index.
     */
    private static BufferedImage getOverlayImageInternal(TileType type,
                                                         String id,
                                                         Dimension size,
                                                         Set<String> overlayCache) {
        final String prefix = "image.tileoverlay." + type.getId() + ".r";
        List<String> keys = (overlayCache == null)
            ? ResourceManager.getImageKeys(prefix)
            : transform(overlayCache, k -> k.startsWith(prefix));
        return getRandomizedImage(keys, id, size);
    }

    public static Set<String> createOverlayCache() {
        return ResourceManager.getImageKeySet("image.tileoverlay.");
    }

    public static BufferedImage getOverlayImage(TileType type, Dimension size) {
        return getOverlayImageInternal(type, type.getId(), size, null);
    }

    public BufferedImage getScaledOverlayImage(Tile tile) {
        return getOverlayImageInternal(tile.getType(), tile.getId(),
                                       this.tileOverlaySize, null);
    }

    public BufferedImage getScaledOverlayImage(Tile tile,
                                               Set<String> overlayCache) {
        return getOverlayImageInternal(tile.getType(), tile.getId(),
                                       this.tileOverlaySize, overlayCache);
    }


    private static String getResourceTypeKey(ResourceType rt) {
        return "image.tileitem." + rt.getId();
    }
    
    public static BufferedImage getResourceTypeImage(ResourceType rt,
                                                     Dimension size,
                                                     boolean grayscale) {
        return getSizedImageInternal(getResourceTypeKey(rt), size, grayscale);
    }

    private static BufferedImage getResourceTypeImage(ResourceType rt,
                                                      float scale,
                                                      boolean grayscale) {
        return getScaledImageInternal(getResourceTypeKey(rt), scale, grayscale);
    }

    public BufferedImage getScaledResourceTypeImage(ResourceType rt) {
        return getResourceTypeImage(rt, this.scaleFactor, false);
    }

    public BufferedImage getSmallResourceTypeImage(ResourceType rt) {
        return getResourceTypeImage(rt,
                                    this.scaleFactor * SMALL_SCALE, false);
    }

    public BufferedImage getScaledResourceImage(Resource resource) {
        return getResourceTypeImage(resource.getType(),
                                    this.scaleFactor, false);
    }


    private static String getRiverStyleKey(String style) {
        return RIVER_STYLE_PREFIX + style;
    }

    /**
     * Returns the river image with the given style.
     *
     * @param style the style code
     * @param size the image size
     * @return The image with the given style.
     */
    private static BufferedImage getRiverImageInternal(String style,
                                                       Dimension size) {
        return getSizedImageInternal(getRiverStyleKey(style), size, false);
    }

    public static BufferedImage getRiverImage(String style, Dimension size) {
        return getRiverImageInternal(style, size);
    }
    
    /**
     * Returns the river image with the given style and scale.
     *
     * @param style The improvement style identifier.
     * @param scale A scale factor.
     * @return The image with the given style.
     */
    private BufferedImage getScaledRiverImage(String style, float scale) {
        return getRiverImageInternal(style,
            scaleDimension(this.tileSize, scale));
    }

    /**
     * Returns the river image with the given style.
     *
     * @param style a {@code TileImprovementStyle} value
     * @return The image with the given style.
     */
    public BufferedImage getScaledRiverImage(TileImprovementStyle style) {
        return getRiverImageInternal(style.getString(), this.tileSize);
    }

    public BufferedImage getScaledRiverImage(String style) {
        return getScaledRiverImage(style, this.scaleFactor);
    }

    public BufferedImage getSmallerRiverImage(String style) {
        return getScaledRiverImage(style, this.scaleFactor * SMALLER_SCALE);
    }
    
    
    /**
     * Returns the river mouth terrain-image for the direction and magnitude.
     *
     * @param direction a {@code Direction} value
     * @param magnitude an {@code int} value
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn (ignored).
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn (ignored).
     * @return The terrain-image at the given index.
     */
    public BufferedImage getRiverMouthImage(Direction direction, int magnitude,
                                            int x, int y) {
        final String key = "image.tile.model.tile.delta." + direction
            + ((magnitude == 1) ? ".small" : ".large");
        return getSizedImageInternal(key, this.tileSize, false);
    }

    /**
     * Get a list of river style image keys.
     *
     * @param all If true accept all non-0000 keys.
     * @return A list of river style image keys.
     */
    public static List<String> getRiverStyleKeys(final boolean all) {
        return ResourceManager.getImageKeys(RIVER_STYLE_PREFIX)
            .stream()
            .map(key -> key.substring(RIVER_STYLE_PREFIX.length()))
            .filter(style ->
                (all || !style.contains("1")) && !"0000".equals(style))
            .sorted().collect(Collectors.toList());
    }

        
    private static String getSettlementTypeKey(SettlementType settlementType) {
        return "image.tileitem." + settlementType.getId();
    }
    
    private static BufferedImage getSettlementTypeImage(SettlementType settlementType,
                                                        float scale) {
        final String key = getSettlementTypeKey(settlementType);
        return getScaledImageInternal(key, scale, false);
    }

    public static BufferedImage getSettlementTypeImage(SettlementType settlementType,
                                                       Dimension size) {
        final String key = getSettlementTypeKey(settlementType);
        return getSizedImageInternal(key, size, false);
    }
    
    /**
     * Returns the graphics that will represent the given settlement.
     *
     * @param settlementType The type of settlement whose graphics
     *     type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public BufferedImage getScaledSettlementTypeImage(SettlementType settlementType) {
        return getSettlementTypeImage(settlementType, this.scaleFactor);
    }

    public BufferedImage getSmallerSettlementTypeImage(SettlementType settlementType) {
        return getSettlementTypeImage(settlementType,
                                      this.scaleFactor * SMALLER_SCALE);
    }

    private static String getSettlementKey(Settlement settlement) {
        String key = getSettlementTypeKey(settlement.getType());
        if (settlement instanceof Colony) {
            Colony colony = (Colony)settlement;
            if (colony.isUndead()) {
                key += ".undead";
            } else {
                int count = colony.getApparentUnitCount();
                key += (count <= 3) ? ".small"
                    : (count <= 7) ? ".medium"
                    : ".large";
                String stockade = colony.getStockadeKey();
                if (stockade != null) key += "." + stockade;
            }
        } else if (settlement instanceof IndianSettlement) {
            IndianSettlement is = (IndianSettlement)settlement;
            if (is.hasMissionary()) key += ".mission";
        }
        return key;
    }

    /**
     * Returns the graphics that will represent the given settlement.
     *
     * @param settlement The settlement whose graphics type is needed.
     * @param scale a {@code double} value
     * @return The graphics that will represent the given settlement.
     */
    public static BufferedImage getSettlementImage(Settlement settlement,
                                                   float scale) {
        return getScaledImageInternal(getSettlementKey(settlement), scale, false);
    }

    public static BufferedImage getSettlementImage(Settlement settlement,
                                                   Dimension size) {
        return getSizedImageInternal(getSettlementKey(settlement), size, false);
    }

    /**
     * Returns the graphics that will represent the given settlement.
     *
     * @param settlement The settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public BufferedImage getScaledSettlementImage(Settlement settlement) {
        return getSettlementImage(settlement, this.scaleFactor);
    }

    public BufferedImage getSmallSettlementImage(Settlement settlement) {
        return getSettlementImage(settlement, this.scaleFactor * SMALL_SCALE);
    }

    public BufferedImage getSmallerSettlementImage(Settlement settlement) {
        return getSettlementImage(settlement, this.scaleFactor * SMALLER_SCALE);
    }
    

    public static String getTerrainImageKey(TileType type, int x, int y) {
        return "image.tile."
            + ((type == null) ? "model.tile.unexplored" : type.getId())
            + ".center.r" + (isSpecialEven(x, y) ? "0" : "1");
    }
        
    /**
     * Gets the terrain-image for the given type.
     *
     * @param type The type of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param size The image size.
     * @return The terrain-image at the given index.
     */
    private static BufferedImage getTerrainImageInternal(TileType type,
                                                         int x, int y,
                                                         Dimension size) {
        return getSizedImageInternal(getTerrainImageKey(type, x, y),
                                     size, false);
    }
    
    public static BufferedImage getTerrainImage(TileType type, int x, int y,
                                                Dimension size) {
        return getTerrainImageInternal(type, x, y, size);
    }

    public BufferedImage getScaledTerrainImage(TileType type, int x, int y) {
        return getTerrainImageInternal(type, x, y, this.tileSize);
    }


    /**
     * Get the tile improvement image with for a given identifier.
     *
     * @param id The tile improvement identifier.
     * @return The image found, or null if it does not exist.
     */
    public BufferedImage getTileImprovementImage(String id) {
        final String key = "image.tile." + id;
        return (!ResourceManager.hasImageResource(key)) ? null
            // Has its own Overlay Image in Misc, use it
            : getSizedImageInternal(key, this.tileSize, false);
    }


    // Unit image handling
    
    /**
     * Get the unit image key for the given parameters.
     *
     * @param unitType The type of unit to be represented.
     * @param roleId The id of the unit role.
     * @param nativeEthnicity If true the unit is a former native.
     * @return A suitable key.
     */
    private static String getUnitTypeImageKey(UnitType unitType, String roleId,
                                              boolean nativeEthnicity) {
        // Units that can only be native don't need the .native key part
        if (nativeEthnicity
            && unitType.hasAbility(Ability.BORN_IN_INDIAN_SETTLEMENT)) {
            nativeEthnicity = false;
        }
        // Try to get an image matching the key
        String roleQual = (Role.isDefaultRoleId(roleId)) ? ""
            : "." + Role.getRoleIdSuffix(roleId);
        String key = "image.unit." + unitType.getId() + roleQual
            + ((nativeEthnicity) ? ".native" : "");
        if (nativeEthnicity && !ResourceManager.hasImageResource(key)) {
            key = "image.unit." + unitType.getId() + roleQual;
        }
        return key;
    }

    /**
     * Fundamental unit image accessor.
     *
     * @param unitType The type of unit to be represented.
     * @param roleId The id of the unit role.
     * @param nativeEthnicity If true the unit is a former native.
     * @param grayscale If true draw in inactive/disabled-looking state.
     * @param scale How much the image is scaled.
     * @return A suitable {@code BufferedImage}.
     */
    private static BufferedImage getUnitTypeImage(UnitType unitType,
                                                  String roleId,
                                                  boolean nativeEthnicity,
                                                  boolean grayscale,
                                                  float scale) {
        final String key = getUnitTypeImageKey(unitType, roleId,
                                               nativeEthnicity);
        return getScaledImageInternal(key, scale, grayscale);
    }

    private static BufferedImage getUnitTypeImage(UnitType unitType,
                                                  float scale) {
        return getUnitTypeImage(unitType, unitType.getDisplayRoleId(), false,
                                false, scale);
    }

    private static BufferedImage getUnitTypeImage(UnitType unitType,
                                                  String roleId,
                                                  boolean nativeEthnicity,
                                                  Dimension size) {
        final String key = getUnitTypeImageKey(unitType, roleId,
                                               nativeEthnicity);
        return getSizedImageInternal(key, size, false);
    }

    private static BufferedImage getUnitTypeImage(UnitType unitType,
                                                  Dimension size) {
        return getUnitTypeImage(unitType, unitType.getDisplayRoleId(), false,
                                size);
    }

    public BufferedImage getScaledUnitTypeImage(UnitType unitType) {
        return getUnitTypeImage(unitType, unitType.getDisplayRoleId(), false,
                                false, this.scaleFactor);
    }

    public BufferedImage getSmallUnitTypeImage(UnitType unitType, String roleId,
                                               boolean grayscale) {
        return getUnitTypeImage(unitType, roleId, false,
                                grayscale, this.scaleFactor * SMALL_SCALE);
    }

    public BufferedImage getSmallUnitTypeImage(UnitType unitType,
                                               boolean grayscale) {
        return getSmallUnitTypeImage(unitType, unitType.getDisplayRoleId(),
                                     grayscale);
    }

    public BufferedImage getSmallUnitTypeImage(UnitType unitType) {
        return getSmallUnitTypeImage(unitType, unitType.getDisplayRoleId(),
                                     false);
    }

    public BufferedImage getSmallerUnitTypeImage(UnitType unitType) {
        return getUnitTypeImage(unitType, unitType.getDisplayRoleId(),
                                false, false, this.scaleFactor * SMALLER_SCALE);
    }

    public BufferedImage getTinyUnitTypeImage(UnitType unitType,
                                              boolean grayscale) {
        return getUnitTypeImage(unitType, unitType.getDisplayRoleId(), false,
                                grayscale, this.scaleFactor * TINY_SCALE);
    }

    public BufferedImage getTinyUnitTypeImage(UnitType unitType) {
        return getTinyUnitTypeImage(unitType, false);
    }

    private static BufferedImage getUnitImage(Unit unit, boolean grayscale,
                                              float scale) {
        return getUnitTypeImage(unit.getType(), unit.getRole().getId(),
                                unit.hasNativeEthnicity(), grayscale, scale);
    }

    public BufferedImage getScaledUnitImage(Unit unit, boolean grayscale) {
        return getUnitImage(unit, grayscale, this.scaleFactor);
    }

    public BufferedImage getScaledUnitImage(Unit unit) {
        return getScaledUnitImage(unit, false);
    }

    public BufferedImage getSmallUnitImage(Unit unit, boolean grayscale) {
        return getUnitImage(unit, grayscale,
                            this.scaleFactor * SMALL_SCALE);
    }

    public BufferedImage getSmallUnitImage(Unit unit) {
        return getSmallUnitImage(unit, false);
    }

    public BufferedImage getSmallerUnitImage(Unit unit) {
        return getUnitImage(unit, false,
                            this.scaleFactor * SMALLER_SCALE);
    }

    public BufferedImage getTinyUnitImage(Unit unit) {
        return getUnitImage(unit, false,
                            this.scaleFactor * TINY_SCALE);
    }
    

    // Accessors for small "chip" images.
    // TODO: cache these too?

    /**
     * Gets a chip image for the alarm at an Indian settlement.
     * The background is either the native owner's, or that of the
     * most-hatednation, if any.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param is The {@code IndianSettlement} to check.
     * @param player The observing {@code Player}.
     * @return An alarm chip, or null if none suitable.
     */
    public BufferedImage getAlarmChip(Graphics2D g, IndianSettlement is,
                                      Player player) {
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
        Color foreground = makeForegroundColor(enemyColor);
        final String text = ResourceManager.getString((is.worthScouting(player))
            ? "indianAlarmChip.contacted" : "indianAlarmChip.scouted");
        return createChip(g, text, Color.BLACK,
                          ownerColor, amount/4.0, enemyColor, foreground, true);
    }

    /**
     * Gets the owner chip for the settlement.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param is The {@code IndianSettlement} to check.
     * @return A chip.
     */
    public BufferedImage getIndianSettlementChip(Graphics2D g,
                                                 IndianSettlement is) {
        final String key = ResourceManager.getString("indianSettlementChip."
            + ((is.getType().isCapital()) ? "capital" : "normal"));
        Color background = is.getOwner().getNationColor();
        return createChip(g, key, Color.BLACK, background, 0, Color.BLACK,
                          makeForegroundColor(background), true);
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
        final Color background = owner.getNationColor();
        final String key = "color.foreground.mission."
            + ((expert) ? "expert" : "normal");
        final Color foreground = getColor(key,
            (expert) ? Color.BLACK : Color.GRAY);
        return createChip(g, ResourceManager.getString("cross"), Color.BLACK,
                          background, 0, Color.BLACK, foreground, true);
    }

    /**
     * Gets a chip for an occupation indicator, i.e. a small image with a
     * single letter or symbol that indicates the Unit's state.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param unit The {@code Unit} with the occupation.
     * @param text The text for the chip.
     * @return A suitable chip.
     */
    public BufferedImage getOccupationIndicatorChip(Graphics2D g,
                                                    Unit unit, String text) {
        final Color background = unit.getOwner().getNationColor();
        final Color foreground = (unit.getState() == Unit.UnitState.FORTIFIED)
            ? Color.GRAY : makeForegroundColor(background);
        return createChip(g, text, Color.BLACK,
                          background, 0, Color.BLACK, foreground, true);
    }

    /**
     * Create a "chip" with the given text and colors.
     *
     * @param g Graphics2D for getting the FontMetrics.
     * @param text The text to display.
     * @param border The border {@code Color}.
     * @param background The background {@code Color}.
     * @param amount How much to fill the chip with the fill color
     * @param fill The fill {@code Color}.
     * @param foreground The foreground {@code Color}.
     * @param filled Whether the chip is filled or not
     * @return A chip.
     */
    private BufferedImage createChip(Graphics2D g, String text,
                                     Color border, Color background,
                                     double amount, Color fill,
                                     Color foreground,
                                     boolean filled) {
        Font font = FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
            FontLibrary.FontSize.TINY, Font.BOLD, this.scaleFactor);
        FontMetrics fm = g.getFontMetrics(font);
        int padding = (int)(6 * this.scaleFactor);
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
        if (!filled) {
            if (amount > 0.0 && amount <= 1.0) {
                g2.setColor(fill);
                g2.fillRect(1, 1, width - 2, (int)((height - 2) * amount));
            }
        }
        g2.setColor(foreground);
        g2.drawString(text, padding/2, fm.getMaxAscent() + padding/2);
        g2.dispose();
        return bi;
    }


    // Get special images for strings with particular color and font
    
    public BufferedImage getStringImage(Graphics g, String text, String color,
                                        FontLibrary.FontType type,
                                        FontLibrary.FontSize size, int style) {
        return getStringImage(g, text, getColor(color), type, size, style);
    }
    
    public BufferedImage getStringImage(Graphics g, String text, Color color,
                                        FontLibrary.FontType type,
                                        FontLibrary.FontSize size, int style) {
        return getStringImage(g, text, color,
            FontLibrary.createFont(type, size, style, getScaleFactor()));
    }
    
    /**
     * Gets an image with a string of a given color and with
     * a black border around the glyphs.
     *
     * @param g A {@code Graphics}-object for getting the font metrics.
     * @param text The {@code String} to make an image of.
     * @param color The {@code Color} to use for the text.
     * @param font The {@code Font} to display the text with.
     * @return The {@code BufferedImage} found or created.
     */
    public BufferedImage getStringImage(Graphics g, String text, Color color,
                                        Font font) {
        if (color == null) {
            logger.warning("getStringImage called with color null");
            color = Color.WHITE;
        }

        // Check the cache
        final String key = text
            + "." + font.getFontName().replace(' ', '-')
            + "." + Integer.toString(font.getSize())
            + "." + Integer.toHexString(color.getRGB());
        BufferedImage img = this.stringImageCache.get(key);
        if (img != null) return img;

        img = createStringImage(text, color, font, g.getFontMetrics(font));
        this.stringImageCache.put(key, img);
        return img;
    }

    /**
     * Create a string image.
     *
     * @param text The {@code String} to make an image of.
     * @param color The {@code Color} to use for the text.
     * @param font The {@code Font} to display the text with.
     * @param fm The {@code FontMetrics} to use with the font.
     * @return The image that was created.
     */
    private BufferedImage createStringImage(String text, Color color,
                                            Font font, FontMetrics fm) {
        final int width = fm.stringWidth(text) + 4;
        final int height = fm.getMaxAscent() + fm.getMaxDescent();
        BufferedImage img = new BufferedImage(width, height,
                                              BufferedImage.TYPE_INT_ARGB);
        // draw the string with selected color
        Graphics2D g = img.createGraphics();
        g.setColor(makeStringBorderColor(color));
        g.setFont(font);
        g.drawString(text, 2, fm.getMaxAscent());

        // draw the border around letters
        int borderWidth = 1;
        int borderColor = makeStringBorderColor(color).getRGB();
        int srcRGB, dstRGB, srcA;
        for (int biY = 0; biY < height; biY++) {
            for (int biX = borderWidth; biX < width - borderWidth; biX++) {
                int biXI = width - biX - 1;
                for (int d = 1; d <= borderWidth; d++) {
                    // left to right
                    srcRGB = img.getRGB(biX, biY);
                    srcA = (srcRGB >> 24) & 0xFF;
                    dstRGB = img.getRGB(biX - d, biY);
                    if (dstRGB != borderColor) {
                        if (srcA > 0) {
                            img.setRGB(biX, biY, borderColor);
                            img.setRGB(biX - d, biY, srcRGB);
                        }
                    }
                    // right to left
                    srcRGB = img.getRGB(biXI, biY);
                    srcA = (srcRGB >> 24) & 0xFF;
                    dstRGB = img.getRGB(biXI + d, biY);
                    if (dstRGB != borderColor) {
                        if (srcA > 0) {
                            img.setRGB(biXI, biY, borderColor);
                            img.setRGB(biXI + d, biY, srcRGB);
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
                    srcRGB = img.getRGB(biX, biY);
                    srcA = (srcRGB >> 24) & 0xFF;
                    dstRGB = img.getRGB(biX, biY - d);
                    if (dstRGB != borderColor) {
                        if (srcA > 0) {
                            img.setRGB(biX, biY, borderColor);
                            img.setRGB(biX, biY - d, srcRGB);
                        }
                    }
                    // bottom to top
                    srcRGB = img.getRGB(biX, biYI);
                    srcA = (srcRGB >> 24) & 0xFF;
                    dstRGB = img.getRGB(biX, biYI + d);
                    if (dstRGB != borderColor) {
                        if (srcA > 0) {
                            img.setRGB(biX, biYI, borderColor);
                            img.setRGB(biX, biYI + d, srcRGB);
                        }
                    }
                }
            }
        }

        g.setColor(color);
        g.drawString(text, 2, fm.getMaxAscent());
        g.dispose();
        return img;
    }


    // Video

    public static Video getVideo(String key) {
        return ResourceManager.getVideo(key);
    }
}
