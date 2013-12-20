/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.SettlementType;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementStyle;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.Utils;


/**
 * Holds various images that can be called upon by others in order to display
 * certain things.
 */
public final class ImageLibrary {

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
     * <code>ImageLibrary</code>.  The value <code>1</code> is used if
     * this object is not a result of a scaling operation.
     */
    private final float scalingFactor;


    /**
     * The constructor to use.
     */
    public ImageLibrary() {
        this(1);
    }

    public ImageLibrary(float scalingFactor) {
        this.scalingFactor = scalingFactor;
    }


    /**
     * Create a "chip" with the given text and colors.
     *
     * @param text The text to display.
     * @param border The border <code>Color</code>.
     * @param background The background <code>Color</code>.
     * @param foreground The foreground <code>Color</code>.
     * @return A chip.
     */
    private Image createChip(String text, Color border,
                             Color background, Color foreground) {
        // Draw it and put it in the cache
        Font font = ResourceManager.getFont("SimpleFont", Font.BOLD,
            (float)Math.rint(12 * getScalingFactor()));
        // hopefully, this is big enough
        BufferedImage bi = new BufferedImage(100, 100,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        TextLayout label = new TextLayout(text, font,
                                          g2.getFontRenderContext());
        float padding = 6 * getScalingFactor();
        int width = (int)(label.getBounds().getWidth() + padding);
        int height = (int)(label.getAscent() + label.getDescent() + padding);
        g2.setColor(border);
        g2.fillRect(0, 0, width, height);
        g2.setColor(background);
        g2.fillRect(1, 1, width - 2, height - 2);
        g2.setColor(foreground);
        label.draw(g2, (float)(padding/2 - label.getBounds().getX()),
                   label.getAscent() + padding/2);
        g2.dispose();
        return bi.getSubimage(0, 0, width, height);
    }

    /**
     * Create a filled "chip" with the given text and colors.
     *
     * @param text The text to display.
     * @param border The border <code>Color</code>.
     * @param background The background <code>Color</code>.
     * @param amount How much to fill the chip with the fill color
     * @param fill The fill <code>Color</code>.
     * @param foreground The foreground <code>Color</code>.
     * @return A chip.
     */
    private Image createFilledChip(String text, Color border, Color background,
                                   double amount, Color fill,
                                   Color foreground) {
        // Draw it and put it in the cache
        Font font = ResourceManager.getFont("SimpleFont", Font.BOLD,
            (float)Math.rint(12 * getScalingFactor()));
        // hopefully, this is big enough
        BufferedImage bi = new BufferedImage(100, 100,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        TextLayout label = new TextLayout(text, font,
                                          g2.getFontRenderContext());
        float padding = 6 * getScalingFactor();
        int width = (int)(label.getBounds().getWidth() + padding);
        int height = (int)(label.getAscent() + label.getDescent() + padding);
        g2.setColor(border);
        g2.fillRect(0, 0, width, height);
        g2.setColor(background);
        g2.fillRect(1, 1, width - 2, height - 2);
        if (amount > 0.0 && amount <= 1.0) {
            g2.setColor(fill);
            g2.fillRect(1, 1, width - 2, (int)((height - 2) * amount));
        }
        g2.setColor(foreground);
        label.draw(g2, (float)(padding/2 - label.getBounds().getX()),
                   label.getAscent() + padding/2);
        g2.dispose();
        return bi.getSubimage(0, 0, width, height);
    }

    /**
     * Create a faded version of an image.
     *
     * @param img The <code>Image</code> to fade.
     * @param fade The amount of fading.
     * @param target The offset.
     * @return The faded image.
     */
    public BufferedImage fadeImage(Image img, float fade, float target) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.getGraphics();
        g.drawImage(img, 0, 0, null);

        float offset = target * (1.0f - fade);
        float[] scales = { fade, fade, fade, 1.0f };
        float[] offsets = { offset, offset, offset, 0.0f };
        RescaleOp rop = new RescaleOp(scales, offsets, null);

        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(bi, rop, 0, 0);
        return bi;
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
    public Color getForegroundColor(Color background) {
        return (background.getRed() * 0.3
                + background.getGreen() * 0.59
                + background.getBlue() * 0.11 < 126) ? Color.WHITE
            : Color.BLACK;
    }

    /**
     * Describe <code>getStringBorderColor</code> method here.
     *
     * The string border colors should be black unless the color of
     * the string is really dark.
     *
     * @param color The <code>Color</code> to complement.
     * @return A suitable border <code>Color</code>.
     */
    public Color getStringBorderColor(Color color) {
        return (color.getRed() * 0.3
            + color.getGreen() * 0.59
            + color.getBlue() * 0.11 < 10) ? Color.WHITE
            : Color.BLACK;
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


    /**
     * Gets a chip image for the alarm at an Indian settlement.
     * The background is either the native owner's, or that of the
     * most hated nation if any.
     *
     * @param is The <code>IndianSettlement</code> to check.
     * @param player The observing <code>Player</code>.
     * @return An alarm chip, or null if none suitable.
     */
    public Image getAlarmChip(IndianSettlement is, Player player) {
        Player enemy;
        if (player == null || !is.hasContacted(player)
            || (enemy = is.getMostHated()) == null) return null;
        Color ownerColor = is.getOwner().getNationColor();
        Color enemyColor = enemy.getNationColor();
        // Set amount to [0-4] corresponding to HAPPY, CONTENT,
        // DISPLEASED, ANGRY, HATEFUL but only if the player is the
        // most hated, because other nation alarm is not nor should be
        // serialized to the client.
        int amount = 4;
        if (player == enemy) {
            Tension alarm = is.getAlarm(enemy);
            amount = (alarm == null) ? 4 : alarm.getLevel().ordinal();
            if (amount == 0) amount = 1; // Show *something*!
        }
        Color foreground = getForegroundColor(enemyColor);
        String text = Messages.message((is.worthScouting(player))
                                       ? "indianSettlement.contacted"
                                       : "indianSettlement.scouted");
        String key = "dynamic.alarm." + text + "." + ownerColor.getRGB()
            + "." + amount + "." + enemyColor.getRGB();
        Image img = (Image)ResourceManager.getImage(key);
        if (img == null) {
            img = createFilledChip(text, Color.BLACK, ownerColor, amount/4.0,
                                   enemyColor, foreground);
            ResourceManager.addGameMapping(key, new ImageResource(img));
        }
        return img;
    }

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
    private boolean isEven(int x, int y) {
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
    public Image getBeachCornerImage(int index, int x, int y) {
        return ResourceManager.getImage("model.tile.beach.corner" + index
                                        + ((isEven(x, y)) ? "_even" : "_odd"),
                                        scalingFactor);
    }

    /**
     * Returns the beach edge image at the given index.
     *
     * @param index The index of the image to return.
     * @param x an <code>int</code> value
     * @param y an <code>int</code> value
     * @return The image at the given index.
     */
    public Image getBeachEdgeImage(int index, int x, int y) {
        return ResourceManager.getImage("model.tile.beach.edge" + index
                                        + ((isEven(x, y)) ? "_even" : "_odd"),
                                        scalingFactor);
    }

    /**
     * Gets the bonus image of the given type.
     *
     * @param type The <code>ResourceType</code> to look up.
     * @return The bonus image.
     */
    public Image getBonusImage(ResourceType type) {
        return getBonusImage(type, scalingFactor);
    }

    public Image getBonusImage(ResourceType type, double scale) {
        return ResourceManager.getImage(type.getId() + ".image", scale);
    }

    /**
     * Returns the bonus-image for the given tile.
     *
     * @param tile The <code>Tile</code> with the image on it.
     * @return The bonus-image for the given tile.
     */
    public Image getBonusImage(Tile tile) {
        return (tile.hasResource()) ? getBonusImage(tile.getTileItemContainer()
            .getResource().getType())
            : null;
    }

    /**
     * Returns the bonus-ImageIcon at the given index.
     *
     * @param type The type of the bonus-ImageIcon to return.
     * @return <code>ImageIcon</code>
     */
    public ImageIcon getBonusImageIcon(ResourceType type) {
        return new ImageIcon(getBonusImage(type));
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
    public Image getBorderImage(TileType type, Direction direction,
                                int x, int y) {
        String key = (type == null) ? "model.tile.unexplored" : type.getId();
        return ResourceManager.getImage(key + ".border_" + direction
                                        + ((isEven(x, y)) ?  "_even" : "_odd")
                                        + ".image", scalingFactor);
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
        return ResourceManager.getImage(nation.getId() + ".image", scale);
    }

    /**
     * Returns the coat-of-arms image for the given Nation.
     *
     * @param nation The nation.
     * @return the coat-of-arms of this nation
     */
    public ImageIcon getCoatOfArmsImageIcon(Nation nation) {
        return ResourceManager.getImageIcon(nation.getId() + ".image");
    }

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
        Image forestImage = type.isForested() ? getForestImage(type, scale)
            : null;
        if (overlayImage == null && forestImage == null) {
            return terrainImage;
        } else {
            GraphicsConfiguration gc
                = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            int width = terrainImage.getWidth(null);
            int height = terrainImage.getHeight(null);
            if (overlayImage != null) {
                height = Math.max(height, overlayImage.getHeight(null));
            }
            if (forestImage != null) {
                height = Math.max(height, forestImage.getHeight(null));
            }
            BufferedImage compositeImage
                = gc.createCompatibleImage(width, height,
                                           Transparency.TRANSLUCENT);
            Graphics2D g = compositeImage.createGraphics();
            g.drawImage(terrainImage, 0,
                        height - terrainImage.getHeight(null), null);
            if (overlayImage != null) {
                g.drawImage(overlayImage, 0,
                            height - overlayImage.getHeight(null), null);
            }
            if (forestImage != null) {
                g.drawImage(forestImage, 0,
                            height - forestImage.getHeight(null), null);
            }
            g.dispose();
            return compositeImage;
        }
    }

    /**
     * Returns the height of the terrain-image including overlays and
     * forests for the given terrain type.
     *
     * @param type The type of the terrain-image.
     * @return The height of the terrain-image at the given index.
     */
    public int getCompoundTerrainImageHeight(TileType type) {
        Image terrain = getTerrainImage(type, 0, 0);
        int height = terrain.getHeight(null);
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
     * Returns the forest image for a terrain type.
     *
     * @param type The type of the terrain-image to return.
     * @return The image at the given index.
     */
    public Image getForestImage(TileType type) {
        return getForestImage(type, scalingFactor);
    }

    public Image getForestImage(TileType type, TileImprovementStyle riverStyle) {
        return getForestImage(type, riverStyle, scalingFactor);
    }

    public Image getForestImage(TileType type, double scale) {
        return ResourceManager.getImage(type.getId() + ".forest", scale);
    }

    public Image getForestImage(TileType type, TileImprovementStyle riverStyle, double scale) {
        if (riverStyle == null) {
            return ResourceManager.getImage(type.getId() + ".forest", scale);
        } else {
            return ResourceManager.getImage(type.getId() + ".forest" + riverStyle.getMask(), scale);
        }
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

    public Image getBuildingImage(Building building) {
        return getBuildingImage(building.getType(), building.getOwner(), scalingFactor);
    }

    public Image getBuildingImage(Building building, double scale) {
        return getBuildingImage(building.getType(), building.getOwner(), scale);
    }

    public Image getBuildingImage(BuildingType buildingType, Player player, double scale) {
        String key = buildingType.getId() + "." + player.getNationNameKey() + ".image";
        if (!ResourceManager.hasResource(key)) {
            key = buildingType.getId() + ".image";
        }
        return ResourceManager.getImage(key, scale);
    }

    /**
     * Gets the owner chip for the settlement.
     *
     * @param is The <code>IndianSettlement</code> to check.
     * @param text The text for the chip.
     * @return A chip.
     */
    public Image getIndianSettlementChip(IndianSettlement is, String text) {
        Color background = is.getOwner().getNationColor();
        String key = "dynamic.indianSettlement." + text + "."
             + Integer.toHexString(background.getRGB());
        Image img = ResourceManager.getImage(key);
        if (img == null) {
            img = createChip(text, Color.BLACK, background,
                             getForegroundColor(background));
            ResourceManager.addGameMapping(key, new ImageResource(img));
        }
        return img;
    }

    public Image getImage(FreeColGameObjectType type) {
        return ResourceManager.getImage(type.getId() + ".image", scalingFactor);
    }

    public Image getImage(FreeColGameObjectType type, double scale) {
        return ResourceManager.getImage(type.getId() + ".image", scale);
    }

    /**
     * Returns the appropriate ImageIcon for Object.
     *
     * @param display The Object to display.
     * @return The appropriate ImageIcon.
     */
    public ImageIcon getImageIcon(Object display, boolean small) {
        Image image = null;
        if (display instanceof Goods) display = ((Goods)display).getType();

        if (display == null) {
            return new ImageIcon();
        } else if (display instanceof GoodsType) {
            GoodsType goodsType = (GoodsType)display;
            try {
                image = this.getGoodsImage(goodsType);
            } catch (Exception e) {
                logger.log(Level.WARNING, "could not find image for goods "
                    + goodsType, e);
            }
        } else if (display instanceof Unit) {
            Unit unit = (Unit)display;
            try {
                image = this.getUnitImageIcon(unit).getImage();
            } catch (Exception e) {
                logger.log(Level.WARNING, "could not find image for unit "
                    + unit, e);
            }
        } else if (display instanceof UnitType) {
            UnitType unitType = (UnitType)display;
            try {
                image = this.getUnitImageIcon(unitType).getImage();
            } catch (Exception e) {
                logger.log(Level.WARNING, "could not find image for unit "
                    + unitType, e);
            }
        } else if (display instanceof Settlement) {
            Settlement settlement = (Settlement)display;
            try {
                image = this.getSettlementImage(settlement);
            } catch (Exception e) {
                logger.log(Level.WARNING, "could not find image for settlement "
                    + settlement.getId(), e);
            }
        } else if (display instanceof LostCityRumour) {
            try {
                image = this.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR);
            } catch (Exception e) {
                logger.log(Level.WARNING, "could not find image for LCR", e);
            }
        } else if (display instanceof Player) {
            image = this.getCoatOfArmsImage(((Player)display).getNation());
        }
        if (image != null && small) {
            int width = (image.getWidth(null) / 3) * 2;
            int height = (image.getHeight(null) / 3) * 2;
            return new ImageIcon(image.getScaledInstance(width, height,
                                                         Image.SCALE_SMOOTH));
        } else {
            return (image == null) ? null : new ImageIcon(image);
        }
    }

    /**
     * Returns the image with the given identifier.
     *
     * @param id The object identifier.
     * @return The image.
     */
    public Image getMiscImage(String id) {
        return getMiscImage(id, scalingFactor);
    }

    public Image getMiscImage(String id, double scale) {
        return ResourceManager.getImage(id, scale);
    }

    /**
     * Returns the image with the given identifier.
     *
     * @param id The object identifier.
     * @return The image.
     */
    public ImageIcon getMiscImageIcon(String id) {
        return new ImageIcon(getMiscImage(id));
    }

    /**
     * Gets the mission chip for a native settlement.
     *
     * @param owner The player that owns the mission.
     * @param expert True if the unit is an expert.
     * @return A suitable chip, or null if no mission is present.
     */
    public Image getMissionChip(Player owner, boolean expert) {
        Color background = owner.getNationColor();
        String key = "dynamic.mission." + ((expert) ? "expert" : "normal")
            + "." + Integer.toHexString(background.getRGB());
        Image img = ResourceManager.getImage(key, 1.0);
        if (img == null) {
            Color foreground = ResourceManager.getColor("mission."
                + ((expert) ? "expert" : "normal") + ".foreground.color");
            if (foreground == null) {
                foreground = (expert) ? Color.BLACK : Color.GRAY;
            }
            img = createChip("\u271D", Color.BLACK, background, foreground);
            ResourceManager.addGameMapping(key, new ImageResource(img));
        }
        return img;
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
     * Gets a chip for an occupation indicator, i.e. a small image with a
     * single letter or symbol that indicates the Unit's state.
     *
     * @param unit The <code>Unit</code> with the occupation.
     * @param text The text for the chip.
     * @return A suitable chip.
     */
    public Image getOccupationIndicatorChip(Unit unit, String text) {
        Color backgroundColor = unit.getOwner().getNationColor();
        Color foregroundColor = (unit.getState() == Unit.UnitState.FORTIFIED)
            ? Color.GRAY : getForegroundColor(backgroundColor);
        String key = "dynamic.occupationIndicator." + text
            + "." + Integer.toHexString(backgroundColor.getRGB());
        Image img = ResourceManager.getImage(key, getScalingFactor());
        if (img == null) {
            img = createChip(text, Color.BLACK,
                             backgroundColor, foregroundColor);
            ResourceManager.addGameMapping(key, new ImageResource(img));
        }
        return img;
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
        return (u == null) ? null
            : ResourceManager.getImage("path." + getPathType(u) + ".image");
    }

    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     *
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    public Image getPathNextTurnImage(Unit u) {
        return (u == null) ? null
            : ResourceManager.getImage("path." + getPathType(u) + ".nextTurn.image");
    }

    /**
     * Returns the river image with the given style.
     *
     * @param style a <code>TileImprovementStyle</code> value
     * @return The image with the given style.
     */
    public Image getRiverImage(TileImprovementStyle style) {
        return getRiverImage(style, scalingFactor);
    }

    /**
     * Returns the river image with the given style.
     *
     * @param style a <code>TileImprovementStyle</code> value
     * @param scale a <code>double</code> value
     * @return The image with the given style.
     */
    public Image getRiverImage(TileImprovementStyle style, double scale) {
        return getRiverImage(style.getString(), scale);
    }

    /**
     * Returns the river image with the given style.
     *
     * @param style a <code>String</code> value
     * @param scale a <code>double</code> value
     * @return The image with the given style.
     */
    public Image getRiverImage(String style, double scale) {
        return ResourceManager.getImage("model.tile.river" + style, scale);
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
    public Image getRiverMouthImage(Direction direction, int magnitude,
                                    int x, int y) {
        String key = "model.tile.delta_" + direction
            + (magnitude == 1 ? "_small" : "_large");
        return ResourceManager.getImage(key, scalingFactor);
    }

    public ImageIcon getScaledBonusImageIcon(ResourceType type, float scale) {
        return new ImageIcon(getBonusImage(type, scale));
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
     * Gets a scaled version of this <code>ImageLibrary</code>.
     * @param scalingFactor The factor used when scaling. 2 is twice
     *      the size of the original images and 0.5 is half.
     * @return A new <code>ImageLibrary</code>.
     * @throws FreeColException
     */
    public ImageLibrary getScaledImageLibrary(float scalingFactor)
        throws FreeColException {
        return new ImageLibrary(scalingFactor);
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
        return ResourceManager.getImage(settlement.getImageKey(), scale);
    }

    /**
     * Returns the graphics that will represent the given settlement.
     *
     * @param settlementType The type of settlement whose graphics
     *     type is needed.
     * @return The graphics that will represent the given settlement.
     */
    public Image getSettlementImage(SettlementType settlementType) {
        return getSettlementImage(settlementType, scalingFactor);
    }

    public Image getSettlementImage(SettlementType settlementType,
                                    double scale) {
        return ResourceManager.getImage(settlementType.getId() + ".image",
                                        scale);
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
    public Image getStringImage(Graphics g, String text, Color color,
                                Font font) {
        if (color == null) {
            logger.warning("createStringImage called with color null");
            color = Color.WHITE;
        }

        // Lookup in the cache if the image has been generated already
        String key = "dynamic.stringImage." + text
            + "." + font.getFontName().replace(' ', '-')
            + "." + Integer.toString(font.getSize())
            + "." + Integer.toHexString(color.getRGB());
        Image img = ResourceManager.getImage(key);// ,getScalingFactor());
        if (img == null) {
            // create an image of the appropriate size
            FontMetrics fm = g.getFontMetrics(font);
            BufferedImage bi = new BufferedImage(fm.stringWidth(text) + 4,
                fm.getMaxAscent() + fm.getMaxDescent(),
                BufferedImage.TYPE_INT_ARGB);
            // draw the string with selected color
            Graphics2D big = bi.createGraphics();
            big.setColor(color);
            big.setFont(font);
            big.drawString(text, 2, fm.getMaxAscent());

            // draw the border around letters
            int textColor = color.getRGB();
            int borderColor = getStringBorderColor(color).getRGB();
            for (int biX = 0; biX < bi.getWidth(); biX++) {
                for (int biY = 0; biY < bi.getHeight(); biY++) {
                    int r = bi.getRGB(biX, biY);
                    if (r == textColor) continue;

                    for (int cX = -1; cX <= 1; cX++) {
                        for (int cY = -1; cY <= 1; cY++) {
                            if (biX+cX >= 0 && biY+cY >= 0
                                && biX+cX < bi.getWidth() && biY+cY < bi.getHeight()
                                && bi.getRGB(biX + cX, biY + cY) == textColor) {
                                bi.setRGB(biX, biY, borderColor);
                                continue;
                            }
                        }
                    }
                }
            }
            ResourceManager.addGameMapping(key, new ImageResource(bi));
            img = ResourceManager.getImage(key);//, getScalingFactor());
        }
        return img;
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
        return ResourceManager.getImage(key + ".center"
            + (isEven(x, y) ? "0" : "1") + ".image", scale);
    }

    /**
     * Returns the ImageIcon that will represent the given unit.
     *
     * @param unit The unit whose graphics type is needed.
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(Unit unit) {
        return getUnitImageIcon(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), false, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(Unit unit, boolean grayscale) {
        return getUnitImageIcon(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), grayscale, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(Unit unit, boolean grayscale,
                                      double scale) {
        return getUnitImageIcon(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), grayscale, scale);
    }

    public ImageIcon getUnitImageIcon(Unit unit, double scale) {
        return getUnitImageIcon(unit.getType(), unit.getRole().getId(),
            unit.hasNativeEthnicity(), false, scale);
    }

    /**
     * Returns the ImageIcon that will represent a unit of the given type.
     *
     * @param unitType an <code>UnitType</code> value
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType) {
        return getUnitImageIcon(unitType, "model.role.default", false,
                                false, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, boolean grayscale) {
        return getUnitImageIcon(unitType, "model.role.default", false,
                                grayscale, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, boolean grayscale,
                                      double scale) {
        return getUnitImageIcon(unitType, "model.role.default", false,
                                grayscale, scale);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, double scale) {
        return getUnitImageIcon(unitType, "model.role.default", false,
                                false, scale);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, String roleId) {
        return getUnitImageIcon(unitType, roleId, false,
                                false, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, String roleId,
                                      boolean grayscale) {
        return getUnitImageIcon(unitType, roleId, false,
                                grayscale, scalingFactor);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, String roleId,
                                      boolean grayscale, double scale) {
        return getUnitImageIcon(unitType, roleId, false,
                                grayscale, scale);
    }

    public ImageIcon getUnitImageIcon(UnitType unitType, String roleId,
                                      double scale) {
        return getUnitImageIcon(unitType, roleId, false,
                                false, scale);
    }

    /**
     * Returns the ImageIcon that will represent a unit with the given
     * specifics.
     *
     * @param unitType the type of unit to be represented
     * @param role unit has equipment that affects its abilities/appearance
     * @param nativeEthnicity draws the unit with native skin tones
     * @param grayscale draws the icon in an inactive/disabled-looking state
     * @return an <code>ImageIcon</code> value
     */
    public ImageIcon getUnitImageIcon(UnitType unitType, String roleId,
                                      boolean nativeEthnicity,
                                      boolean grayscale, double scale) {
        // units that can only be native don't need the .native key part
        if (unitType.getId().equals("model.unit.indianConvert")
            || unitType.getId().equals("model.unit.brave")) {
            nativeEthnicity = false;
        } else for (Entry<String, Boolean> entry
                      : unitType.getRequiredAbilities().entrySet()) {
            if (entry.getKey().equals(Ability.NATIVE)
                && entry.getValue() == true) {
                nativeEthnicity = false;
            }
        }

        // try to get an image matching the key
        String roleQual = ("model.role.default".equals(roleId)) ? ""
            : "." + Utils.lastPart(roleId, ".");
        String key = unitType.getId() + roleQual
            + ((nativeEthnicity) ? ".native" : "")
            + ".image";
        if (!ResourceManager.hasResource(key) && nativeEthnicity) {
            key = unitType.getId() + roleQual + ".image";
        }
        Image image = (grayscale)
            ? ResourceManager.getGrayscaleImage(key, scale)
            : ResourceManager.getImage(key, scale);
        if (image == null) {
            // FIXME: these require the game specification, which
            // ImageLibrary doesn't yet have access to

            /*
            if (role != Role.DEFAULT && !unitType.getId().equals("model.unit.freeColonist")) {
                // try a free colonist with the same role
                unitType = getGame().getSpecification().getUnitType("model.unit.freeColonist");
                return getUnitImageIcon(unitType, role, false, grayscale, scale);
            } else {
                // give up, draw a standard unit icon
                unitType = getGame().getSpecification().getUnitType("model.unit.freeColonist");
                return getUnitImageIcon(unitType, Role.DEFAULT, false, grayscale, scale);
            }*/
            return null;
        }
        return new ImageIcon(image);
    }

    /*
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
}
