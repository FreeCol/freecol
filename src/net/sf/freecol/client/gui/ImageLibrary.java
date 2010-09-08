/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Holds various images that can be called upon by others in order to display
 * certain things.
 */
public final class ImageLibrary {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ImageLibrary.class.getName());    
    
    public static final String UNIT_SELECT = "unitSelect.image",
                               DELETE = "delete.image",
                               PLOWED = "model.improvement.plow.image",
                               TILE_TAKEN = "tileTaken.image",
                               TILE_OWNED_BY_INDIANS = "nativeLand.image",
                               LOST_CITY_RUMOUR = "lostCityRumour.image",
                               DARKNESS = "halo.dark.image";

    /**
     * The scaling factor used when creating this
     * <code>ImageLibrary</code>. The value
     * <code>1</code> is used if this object is not
     * a result of a scaling operation.
     */
    private final float scalingFactor;


    /**
     * The constructor to use.
     * 
     */
    public ImageLibrary() {
        this(1);
    }

    public ImageLibrary(float scalingFactor) {
        this.scalingFactor = scalingFactor;
    }


    /**
     * Returns the scaling factor used when creating this ImageLibrary.
     * @return 1 unless {@link #getScaledImageLibrary} was used to create
     *      this object.
     */
    public float getScalingFactor() {
        return scalingFactor;
    }

    /**
     * Gets a scaled version of this <code>ImageLibrary</code>.
     * @param scalingFactor The factor used when scaling. 2 is twice
     *      the size of the original images and 0.5 is half.
     * @return A new <code>ImageLibrary</code>.
     */
    public ImageLibrary getScaledImageLibrary(float scalingFactor) throws FreeColException {
        return new ImageLibrary(scalingFactor);
    }

    /**
     * Returns the portrait of this Founding Father.
     *
     * @param father a <code>FoundingFather</code> value
     * @return an <code>Image</code> value
     */
    public Image getFoundingFatherImage(FoundingFather father) {
        return ResourceManager.getImage(father.getId() + ".image");
    }

    /**
     * Returns the monarch-image for the given tile.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public Image getMonarchImage(Nation nation) {
        return ResourceManager.getImage(nation.getId() + ".monarch.image");
    }

    /**
     * Returns the monarch-image icon for the given Nation.
     * 
     * @param nation The nation this monarch rules.
     * @return the monarch-image for the given nation.
     */
    public ImageIcon getMonarchImageIcon(Nation nation) {
        return ResourceManager.getImageIcon(nation.getId() + ".monarch.image");
    }

    /**
     * Returns the coat-of-arms image for the given Nation.
     * 
     * @param nation The nation.
     * @return the coat-of-arms of this nation
     */
    public ImageIcon getCoatOfArmsImageIcon(Nation nation) {
        return ResourceManager.getImageIcon(nation.getId() + ".coat-of-arms.image");
    }

    /**
     * Returns the coat-of-arms image for the given Nation.
     * 
     * @param nation The nation.
     * @return the coat-of-arms of this nation
     */
    public Image getCoatOfArmsImage(Nation nation) {
        return getCoatOfArmsImage(nation, scalingFactor);
    }

    public Image getCoatOfArmsImage(Nation nation, double scale) {
        return ResourceManager.getImage(nation.getId() + ".coat-of-arms.image", scale);
    }

    /**
     * Returns the bonus-image for the given tile.
     * 
     * @param tile
     * @return the bonus-image for the given tile.
     */
    public Image getBonusImage(Tile tile) {
        if (tile.hasResource()) {
            return getBonusImage(tile.getTileItemContainer().getResource().getType());
        } else {
            return null;
        }
    }

    public Image getBonusImage(ResourceType type) {
        return getBonusImage(type, scalingFactor);
    }

    public Image getBonusImage(ResourceType type, double scale) {
        return ResourceManager.getImage(type.getId() + ".image", scale);
    }

    /**
     * Returns the bonus-ImageIcon at the given index.
     * 
     * @param type The type of the bonus-ImageIcon to return.
     */
    public ImageIcon getBonusImageIcon(ResourceType type) {
        return new ImageIcon(getBonusImage(type));
    }

    public ImageIcon getScaledBonusImageIcon(ResourceType type, float scale) {
        return new ImageIcon(getBonusImage(type, scale));
    }


    /**
     * Converts an image to grayscale
     * 
     * @param image Source image to convert
     * @return The image in grayscale
     */
    /*
    private ImageIcon convertToGrayscale(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        ColorConvertOp filter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        BufferedImage srcImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        srcImage.createGraphics().drawImage(image, 0, 0, null);
        return new ImageIcon(filter.filter(srcImage, null));
    }
    */

    /**
     * Returns the scaled terrain-image for a terrain type (and position 0, 0).
     * 
     * @param type The type of the terrain-image to return.
     * @param scale The scale of the terrain image to return.
     * @return The terrain-image
     */
    public Image getCompoundTerrainImage(TileType type, double scale) {
        // Currently used for hills and mountains
        Image terrainImage = getTerrainImage(type, 0, 0, scale);
        Image overlayImage = getOverlayImage(type, 0, 0, scale);
        Image forestImage = type.isForested() ? getForestImage(type, scale) : null;
        if (overlayImage == null && forestImage == null) {
            return terrainImage;
        } else {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
            int width = terrainImage.getWidth(null);
            int height = terrainImage.getHeight(null);
            if (overlayImage != null) {
                height = Math.max(height, overlayImage.getHeight(null));
            }
            if (forestImage != null) {
                height = Math.max(height, forestImage.getHeight(null));
            }
            BufferedImage compositeImage = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0, height - terrainImage.getHeight(null), null);
            if (overlayImage != null) {
                g.drawImage(overlayImage, 0, height - overlayImage.getHeight(null), null);
            }
            if (forestImage != null) {
                g.drawImage(forestImage, 0, height - forestImage.getHeight(null), null);
            }
            g.dispose();
            return compositeImage;
        }
    }

    /**
     * Returns the overlay-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getOverlayImage(TileType type, int x, int y) {
        return getOverlayImage(type, x, y, scalingFactor);
    }

    public Image getOverlayImage(TileType type, int x, int y, double scale) {
        String key = type.getId() + ".overlay" + ((x + y) % 2) + ".image";
        if (ResourceManager.hasResource(key)) {
            return ResourceManager.getImage(key, scale);
        } else {
            return null;
        }
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
    public Image getTerrainImage(TileType type, int x, int y) {
        return getTerrainImage(type, x, y, scalingFactor);
    }

    public Image getTerrainImage(TileType type, int x, int y, double scale) {
        String key = (type == null) ? "model.tile.unexplored" : type.getId();
        // the pattern is mostly visible on ocean tiles this is an
        // attempt to break it up so it doesn't create big stripes or
        // chess-board effect
        int index = (( y % 8 <= 2) || ((x+y) % 2 == 0 )) ? 0 : 1;
        return ResourceManager.getImage(key + ".center" + index + ".image", scale);
    }

    /**
     * Returns the border terrain-image for the given type.
     * 
     * @param type The type of the terrain-image to return.
     * @param direction a <code>Direction</code> value
     * @param x The x-coordinate of the location of the tile that is being
     *            drawn.
     * @param y The x-coordinate of the location of the tile that is being
     *            drawn.
     * @return The terrain-image at the given index.
     */
    public Image getBorderImage(TileType type, Direction direction, int x, int y) {
        String key = (type == null) ? "model.tile.unexplored" : type.getId();
        // the pattern is mostly visible on ocean tiles this is an
        // attempt to break it up so it doesn't create big stripes or
        // chess-board effect
        String index = (( y % 8 <= 2) || ((x+y) % 2 == 0 )) ? "_even" : "_odd";
        return ResourceManager.getImage(key + ".border_" + direction + index + ".image", scalingFactor);
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
    public Image getRiverMouthImage(Direction direction, int magnitude, int x, int y) {
        String key = "delta_" + direction + (magnitude == 1 ? "_small" : "_large");
        return ResourceManager.getImage(key, scalingFactor);
    }

    /**
     * Returns the river image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getRiverImage(int index) {
        return getRiverImage(index, scalingFactor);
    }

    public Image getRiverImage(int index, double scale) {
        return ResourceManager.getImage("river" + index, scale);
    }

    /**
     * Returns the beach edge image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getBeachEdgeImage(int index) {
        return ResourceManager.getImage("model.tile.beach.edge" + index, scalingFactor);
    }

    /**
     * Returns the beach corner image at the given index.
     * 
     * @param index The index of the image to return.
     * @return The image at the given index.
     */
    public Image getBeachCornerImage(int index) {
        return ResourceManager.getImage("model.tile.beach.corner" + index, scalingFactor);
    }

    /**
     * Returns the forest image for a terrain type.
     * 
     * @param type The type of the terrain-image to return.
     * @return The image at the given index.
     */
    public Image getForestImage(TileType type) {
        return getForestImage(type, scalingFactor);
    }

    public Image getForestImage(TileType type, double scale) {
        return ResourceManager.getImage(type.getId() + ".forest", scale);
    }

    /**
     * Returns the image with the given id.
     * 
     * @param id The id of the image to return.
     * @return The image.
     */
    public Image getMiscImage(String id) {
        return getMiscImage(id, scalingFactor);
    }

    public Image getMiscImage(String id, double scale) {
        return ResourceManager.getImage(id, scale);
    }

    /**
     * Returns the image with the given id.
     * 
     * @param id The id of the image to return.
     * @return The image.
     */
    public ImageIcon getMiscImageIcon(String id) {
        return new ImageIcon(getMiscImage(id));
    }

    /**
     * Returns the goods-image at the given index.
     * 
     * @param goodsType The type of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public Image getGoodsImage(GoodsType goodsType) {
        return getGoodsImage(goodsType, scalingFactor);
    }

    public Image getGoodsImage(GoodsType goodsType, double scale) {
        return ResourceManager.getImage(goodsType.getId() + ".image", scale);
    }

    /**
     * Returns the goods-image for a goods type.
     * 
     * @param goodsType The type of the goods-image to return.
     * @return The goods-image at the given index.
     */
    public ImageIcon getGoodsImageIcon(GoodsType goodsType) {
        return ResourceManager.getImageIcon(goodsType.getId() + ".image");
    }

    /**
     * Returns the scaled goods-ImageIcon for a goods type.
     * 
     * @param type The type of the goods-ImageIcon to return.
     * @param scale The scale of the goods-ImageIcon to return.
     * @return The goods-ImageIcon at the given index.
     */
    public ImageIcon getScaledGoodsImageIcon(GoodsType type, double scale) {
        return new ImageIcon(getGoodsImage(type, scale));
    }

    /**
     * Returns the color of the given player.
     * 
     * @param player a <code>Player</code> value
     * @return The color of the given player.
     */
    public Color getColor(Player player) {
        return ResourceManager.getColor(player.getNationID() + ".color");
    }

    /**
     * Returns the color chip with the given color.
     * 
     * @param ownable an <code>Ownable</code> value
     * @param scale a <code>double</code> value
     * @return The color chip with the given color.
     */
    public Image getColorChip(Ownable ownable, double scale) {
        return ResourceManager.getChip(ownable.getOwner().getNationID() + ".chip", scale);
    }

    /**
     * Returns the mission chip with the given color.
     * 
     * @param ownable an <code>Ownable</code> value
     * @param expertMission Indicates whether or not the missionary is an
     *            expert.
     * @param scale a <code>double</code> value
     * @return The color chip with the given color.
     */
    public Image getMissionChip(Ownable ownable, boolean expertMission, double scale) {
        if (expertMission) {
            return ResourceManager.getChip(ownable.getOwner().getNationID()
                                           + ".mission.expert.chip", scale);
        } else {
            return ResourceManager.getChip(ownable.getOwner().getNationID()
                                           + ".mission.chip", scale);
        }
    }

    /**
     * Returns the alarm chip with the given color.
     * 
     * @param alarm The alarm level.
     * @param visited a <code>boolean</code> value
     * @param scale a <code>double</code> value
     * @return The alarm chip.
     */
    public Image getAlarmChip(Tension.Level alarm, final boolean visited, double scale) {
        if (visited) {
            return ResourceManager.getChip("alarmChip.visited."
                                           + alarm.toString().toLowerCase(), scale);
        } else {
            return ResourceManager.getChip("alarmChip." + alarm.toString().toLowerCase(), scale);
        }
    }

    /**
     * Returns the width of the terrain-image for a terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The width of the terrain-image at the given index.
     */
    public int getTerrainImageWidth(TileType type) {
        return getTerrainImage(type, 0, 0).getWidth(null);
    }

    /**
     * Returns the height of the terrain-image for a terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getTerrainImageHeight(TileType type) {
        return getTerrainImage(type, 0, 0).getHeight(null);
    }

    /**
     * Returns the height of the terrain-image including overlays and
     * forests for the given terrain type.
     * 
     * @param type The type of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getCompoundTerrainImageHeight(TileType type) {
        int height = getTerrainImageHeight(type);
        if (type != null) {
            Image overlayImage = getOverlayImage(type, 0, 0);
            if (overlayImage != null) {
                height = Math.max(height, overlayImage.getHeight(null));
            }
            if (type.isForested()) {
                height = Math.max(height, getForestImage(type).getHeight(null));
            }
        }
        return height;
    }

    /**
     * Returns the graphics that will represent the given settlement.
     * 
     * @param settlementType The type of settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public Image getSettlementImage(SettlementType settlementType) {
        return getSettlementImage(settlementType, scalingFactor);
    }

    public Image getSettlementImage(SettlementType settlementType, double scale) {
        return ResourceManager.getImage(settlementType.toString() + ".image", scale);
    }

    /**
     * Returns the graphics that will represent the given settlement.
     * 
     * @param settlement The settlement whose graphics type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public Image getSettlementImage(Settlement settlement) {
        return getSettlementImage(settlement, scalingFactor);
    }

    /**
     * Returns the graphics that will represent the given settlement.
     * 
     * @param settlement The settlement whose graphics type is needed.
     * @param scale a <code>double</code> value
     * @return The graphics that will represent the given settlement.
     */
    public Image getSettlementImage(Settlement settlement, double scale) {

        if (settlement instanceof Colony) {
            Colony colony = (Colony) settlement;

            // TODO: Put it in specification
            if (colony.isUndead()) {
                return getSettlementImage(SettlementType.UNDEAD, scale);
            } else {
                int stockadeLevel = 0;
                if (colony.getStockade() != null) {
                    stockadeLevel = colony.getStockade().getLevel();
                }
                int unitCount = colony.getUnitCount();
                switch(stockadeLevel) {
                case 0:
                    if (unitCount <= 3) {
                        return getSettlementImage(SettlementType.SMALL_COLONY, scale);
                    } else if (unitCount <= 7) {
                        return getSettlementImage(SettlementType.MEDIUM_COLONY, scale);
                    } else {
                        return getSettlementImage(SettlementType.LARGE_COLONY, scale);
                    }
                case 1:
                    if (unitCount > 7) {
                        return getSettlementImage(SettlementType.LARGE_STOCKADE, scale);
                    } else if (unitCount > 3) {
                        return getSettlementImage(SettlementType.MEDIUM_STOCKADE, scale);
                    } else {
                        return getSettlementImage(SettlementType.SMALL_STOCKADE, scale);
                    }
                case 2:
                    if (unitCount > 7) {
                        return getSettlementImage(SettlementType.LARGE_FORT, scale);
                    } else {
                        return getSettlementImage(SettlementType.MEDIUM_FORT, scale);
                    }
                case 3:
                    return getSettlementImage(SettlementType.LARGE_FORTRESS, scale);
                default:
                    return getSettlementImage(SettlementType.SMALL_COLONY, scale);
                }
            }

        } else { // IndianSettlement
            String key = settlement.getOwner().getNationID()
                + (settlement.isCapital() ? ".capital" : ".settlement")
                + ((((IndianSettlement) settlement).getMissionary() == null) ? "" : ".mission")
                + ".image";
            return ResourceManager.getImage(key, scale);
        }
    }

    /**
     * Returns the ImageIcon that will represent the given unit.
     * 
     * @param unit The unit whose graphics type is needed.
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(Unit unit) {
        return getUnitImageIcon(unit.getType(), unit.getRole(), scalingFactor);
    }

    public ImageIcon getUnitImageIcon(Unit unit, double scale) {
        return getUnitImageIcon(unit.getType(), unit.getRole(), scale);
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given type.
     *
     * @param unitType an <code>UnitType</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType) {
        return getUnitImageIcon(unitType, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, double scale) {
        Image im = ResourceManager.getImage(unitType.getId() + ".image", scale);
        return (im == null) ? null : new ImageIcon(im);
    }
    
    /**
     * Returns the ImageIcon that will represent a unit of the given
     * type and role.
     *
     * @param unitType an <code>UnitType</code> value
     * @param role a <code>Role</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, Role role) {
        return getUnitImageIcon(unitType, role, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, Role role, double scale) {
        final String roleStr = (role != Role.DEFAULT) ? "." + role.getId() : "";
        final Image im = ResourceManager.getImage(unitType.getId() + roleStr + ".image", scale);
        return (im != null) ? new ImageIcon(im) : null;
    }

    /**
     * Returns the ImageIcon that will represent the given unit.
     *
     * @param unit an <code>Unit</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(Unit unit, boolean grayscale) {
        return getUnitImageIcon(unit.getType(), unit.getRole(), grayscale);
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given type.
     *
     * @param unitType an <code>UnitType</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, boolean grayscale) {
        return getUnitImageIcon(unitType, Role.DEFAULT, grayscale);
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given
     * type and role.
     *
     * @param unitType an <code>UnitType</code> value
     * @param role a <code>Role</code> value
     * @param grayscale a <code>boolean</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, Role role, boolean grayscale) {
        return getUnitImageIcon(unitType, role, grayscale, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, Role role, boolean grayscale, double scale) {
        if (grayscale) {
            String key = unitType.getId() + (role == Role.DEFAULT ? "" : "." + role.getId()) + ".image";
            final Image im = ResourceManager.getGrayscaleImage(key, scale);
            return (im != null) ? new ImageIcon(im) : null;
        } else {
            return getUnitImageIcon(unitType, role, scale);
        }
    }


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
        Image image = ResourceManager.getImage(resource);
        int dx, dy, xmin, ymin;

        if (insets == null) {
            xmin = 0;
            ymin = 0;
        } else {
            xmin = insets.left;
            ymin = insets.top;
            width -= insets.left + insets.right;
            height -= insets.top + insets.bottom;
        }
        if (image != null && (dx = image.getWidth(null)) > 0
            && (dy = image.getHeight(null)) > 0) {
            int xmax, ymax;
            xmax = xmin + width;
            ymax = ymin + height;
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

    private String getPathType(Unit unit) {
        if (unit.isNaval()) {
            return "naval";
        } else if (unit.isMounted()) {
            return "horse";
        } else if (unit.getType().hasSkill() || unit.isUndead()) {
            return "foot";
        } else {
            return "wagon";
        }
    }

    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    public Image getPathImage(Unit u) {
        if (u == null) {
            return null;
        } else {
            return ResourceManager.getImage("path." + getPathType(u) + ".image");
        }
    }
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     *
    private Image getPathIllegalImage(Unit u) {
        if (u == null || u.isNaval()) {
            return (Image) UIManager.get("path.naval.illegal.image");
        } else if (u.isMounted()) {
            return (Image) UIManager.get("path.horse.illegal.image");
        } else if (u.getType() == Unit.WAGON_TRAIN || u.getType() == Unit.TREASURE_TRAIN || u.getType() == Unit.ARTILLERY || u.getType() == Unit.DAMAGED_ARTILLERY) {
            return (Image) UIManager.get("path.wagon.illegal.image");
        } else {
            return (Image) UIManager.get("path.foot.illegal.image");
        }
    }
    */
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    public Image getPathNextTurnImage(Unit u) {
        if (u == null) {
            return null;
        } else {
            return ResourceManager.getImage("path." + getPathType(u) + ".nextTurn.image");
        }
    }


}
