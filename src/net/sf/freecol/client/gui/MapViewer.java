/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.animation.Animation;
import net.sf.freecol.client.gui.GUI.ViewMode;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.StringUtils.*;
import static net.sf.freecol.common.util.Utils.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * MapViewer is a private helper class of Canvas and SwingGUI.
 * 
 * This class is responsible for drawing the map/background on the
 * {@code Canvas}.
 *
 * The map projection has some interesting properties.  Here is the top
 * left corner of the map (truncated, aspect ratio very wrong) containing
 * x,y "map" coordinates.
 *
 *  /\  /\  /\    
 * /0 \/1 \/2 \   
 * \ 0/\ 0/\ 0/  
 *  \/0 \/1 \/    
 *  /\ 1/\ 1/\  
 * /0 \/1 \/2 \ 
 * \ 2/\ 2/\ 2/ 
 *  \/0 \/1 \/   
 *  /\ 3/\ 3/   
 * /0 \/1 \/     
 * \ 4/\ 4/
 *  \/  \/ 
 *
 * We distinguish ordinary "map" coordinates from the pixel
 * coordinates in the frame, which we will denote P(x,y).
 *
 * Pixel coordinates are measureed from the top left corner of the
 * frame to minimal extent of the rectangle containing the tile
 * (note: not actually inside the tile graphic).
 *
 * Let a general tile with map coordinates (x,y) have pixel coordinates
 * P(px,py)
 * Let tiles have width tw and height th.
 *
 * - Property #1:  Tile (x,0) borders the top of the window at P(?,0)
 * - Property #2:  Tile (x,1) is inset by one half tile height at P(?,th/2)
 * - Property #3:  Tile (0,y) borders the left of the window if y is even,
 *   and is thus at P(0,?), otherwise it is inset one half tile width
 *   at P(tw/2,?)
 * - Property #4:  Within a horizontal row, the tile directly to the 
 *   right of T0 is tile (x+1,y) at P(px+tw,py)
 * - Property #5:  Within a vertical column, the tile directly below
 *   tile T0 is tile (x,y+2) at P(px,py+th)
 *
 */
public final class MapViewer extends FreeColClientHolder {

    private static final Logger logger = Logger.getLogger(MapViewer.class.getName());

    private static enum BorderType { COUNTRY, REGION }

    /** How the map can be scaled. */
    private static final float MAP_SCALE_MIN = 0.25f;
    private static final float MAP_SCALE_MAX = 2.0f;
    private static final float MAP_SCALE_STEP = 0.25f;

    /** The height offset to paint a Unit at (in pixels). */
    private static final int UNIT_OFFSET = 20,
        OTHER_UNITS_OFFSET_X = -5, // Relative to the state indicator.
        OTHER_UNITS_OFFSET_Y = 1,
        OTHER_UNITS_WIDTH = 3,
        MAX_OTHER_UNITS = 10;

    private static class TextSpecification {

        public final String text;
        public final Font font;

        public TextSpecification(String newText, Font newFont) {
            this.text = newText;
            this.font = newFont;
        }
    }

    /** Scaled image library to use only for map operations. */
    private final ImageLibrary lib;

    /** The internal scaled tile viewer to use. */
    private final TileViewer tv;

    /** The map size. */
    private Dimension size = null;

    /** The current focus Tile. */
    private Tile focus = null;

    /** The units that are being animated and an associated reference count. */
    private final java.util.Map<Unit, Integer> unitsOutForAnimation
        = new HashMap<>();
    /** The labels being used in animation for a unit. */
    private final java.util.Map<Unit, JLabel> unitsOutForAnimationLabels
        = new HashMap<>();

    /** The cursor for the selected tile. */
    private TerrainCursor cursor;

    /** A path for a current goto order. */
    private PathNode gotoPath = null;

    /** A path for the active unit. */
    private PathNode unitPath = null;

    /** The view mode in use. */
    private ViewMode viewMode = ViewMode.MOVE_UNITS;
    /** The selected tile, for ViewMode.TERRAIN. */
    private Tile selectedTile;
    /** The active unit, for ViewMode.MOVE_UNITS. */
    private Unit activeUnit;

    /** The chat message area. */
    private final ChatDisplay chatDisplay;

    /*
     * These variables are directly dependent on the map scale, which
     * changes in changeScale().
     * Their consistency is maintained by calling updateScaledVariables().
     */

    /** Tile width and height, and half values thereof. */
    private int tileHeight, tileWidth, halfHeight, halfWidth;

    /** Fog of war area. */
    private final GeneralPath fog = new GeneralPath();

    /** Fonts (scaled). */
    private Font fontNormal, fontItalic, fontProduction, fontTiny;
        
    /** Points to use to draw the borders. */
    private final EnumMap<Direction, Point2D.Float> borderPoints
        = new EnumMap<>(Direction.class);
    /** Support points for quadTo calls when drawing borders. */
    private final EnumMap<Direction, Point2D.Float> controlPoints
        = new EnumMap<>(Direction.class);

    /** Stroke to draw the borders with. */
    private Stroke borderStroke = new BasicStroke(4);

    /** Stroke to draw the grid with. */
    private Stroke gridStroke = new BasicStroke(1);

    /*
     * These variables depend on the map scale and also the map pixel size,
     * which changes in changeSize().
     * Their consistency is maintained by calling updateSizedVariables().
     */

    /** Horizonal and vertical pixel distance to the center tile. */
    private int hSpace, vSpace;

    /** Number of rows above/below the center tile. */
    private int centerRows;
    /**
     * Almost the number of columns to the left/right of the center tile.
     * There are special cases handled in getLeft/RightColumns().
     */
    private int centerColumns;
    /** Special y-odd/even offsets to centerColumns. FIXME: explain! */
    private int columnEvenYOffset, columnOddYOffset;
    
    // The y-coordinate of the Tiles that will be drawn at the bottom
    private int bottomRow = -1;

    // The y-coordinate of the Tiles that will be drawn at the top
    private int topRow;

    // The y-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the top
    private int topRowY;

    // The x-coordinate of the Tiles that will be drawn at the left side
    private int leftColumn;

    // The x-coordinate of the Tiles that will be drawn at the right side
    private int rightColumn;

    // The x-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the left (can be less than 0)
    private int leftColumnX;

    // Whether the map is currently aligned with the edge.
    private boolean alignedTop = false, alignedBottom = false,
        alignedLeft = false, alignedRight = false;


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param lib An {@code ImageLibrary} to use for drawing to the map
     *     (and this is subject to the map scaling).
     * @param al An {@code ActionListener} for the cursor.
     */
    public MapViewer(FreeColClient freeColClient, ImageLibrary lib,
                     ActionListener al) {
        super(freeColClient);
        
        this.lib = lib;
        this.tv = new TileViewer(freeColClient, lib);
        this.size = new Dimension(0, 0); // Start empty, update is coming
        this.cursor = new TerrainCursor();
        this.cursor.addActionListener(al);
        this.chatDisplay = new ChatDisplay(freeColClient);

        updateScaledVariables();
    }


    // Internals
    
    // Trivial

    /**
     * Sets the focus tile.
     *
     * @param focus The new focus {@code Tile}.
     */
    private void setFocus(Tile focus) {
        this.focus = focus;
    }


    // Critical internals for consistency maintenance
    
    /**
     * Update the variables that depend on the image library scale.
     */
    private void updateScaledVariables() {
        // ATTENTION: we assume that all base tiles have the same size
        final Dimension tileSize = this.lib.getTileSize();
        this.tileHeight = tileSize.height;
        this.tileWidth = tileSize.width;
        this.halfHeight = tileSize.height / 2;
        this.halfWidth = tileSize.width / 2;

        this.fog.reset();
        this.fog.moveTo(this.halfWidth, 0);
        this.fog.lineTo(this.tileWidth, this.halfHeight);
        this.fog.lineTo(this.halfWidth, this.tileHeight);
        this.fog.lineTo(0, this.halfHeight);
        this.fog.closePath();

        // Update fonts, make sure font{Normal,Italic} are non-null but
        // allow the others to disappear if they get too small
        this.fontNormal = this.lib.getScaledFont("normal-bold-smaller", null);
        this.fontItalic = this.lib.getScaledFont("normal-bold+italic-smaller", null);
        this.fontProduction = this.lib.getScaledFont("normal-bold-tiny", null);
        this.fontTiny = this.lib.getScaledFont("normal-plain-tiny", null);
        if (this.fontNormal == null) {
            this.fontNormal = (this.lib.getScaleFactor() < 1f)
                ? FontLibrary.getUnscaledFont("normal-bold-tiny", null)
                : FontLibrary.getUnscaledFont("normal-bold-max", null);
        }
        if (this.fontItalic == null) this.fontItalic = this.fontNormal;
        
        final int dx = this.tileWidth / 16;
        final int dy = this.tileHeight / 16;
        final int ddx = dx + dx / 2;
        final int ddy = dy + dy / 2;

        // small corners
        this.controlPoints.put(Direction.N,
            new Point2D.Float(this.halfWidth, dy));
        this.controlPoints.put(Direction.E,
            new Point2D.Float(this.tileWidth - dx, this.halfHeight));
        this.controlPoints.put(Direction.S,
            new Point2D.Float(this.halfWidth, this.tileHeight - dy));
        this.controlPoints.put(Direction.W,
            new Point2D.Float(dx, this.halfHeight));
        // big corners
        this.controlPoints.put(Direction.SE,
            new Point2D.Float(this.halfWidth, this.tileHeight));
        this.controlPoints.put(Direction.NE,
            new Point2D.Float(this.tileWidth, this.halfHeight));
        this.controlPoints.put(Direction.SW,
            new Point2D.Float(0, this.halfHeight));
        this.controlPoints.put(Direction.NW,
            new Point2D.Float(this.halfWidth, 0));
        // small corners
        this.borderPoints.put(Direction.NW,
            new Point2D.Float(dx + ddx, this.halfHeight - ddy));
        this.borderPoints.put(Direction.N,
            new Point2D.Float(this.halfWidth - ddx, dy + ddy));
        this.borderPoints.put(Direction.NE,
            new Point2D.Float(this.halfWidth + ddx, dy + ddy));
        this.borderPoints.put(Direction.E,
            new Point2D.Float(this.tileWidth - dx - ddx, this.halfHeight - ddy));
        this.borderPoints.put(Direction.SE,
            new Point2D.Float(this.tileWidth - dx - ddx, this.halfHeight + ddy));
        this.borderPoints.put(Direction.S,
            new Point2D.Float(this.halfWidth + ddx, this.tileHeight - dy - ddy));
        this.borderPoints.put(Direction.SW,
            new Point2D.Float(this.halfWidth - ddx, this.tileHeight - dy - ddy));
        this.borderPoints.put(Direction.W,
            new Point2D.Float(dx + ddx, this.halfHeight + ddy));

        this.borderStroke = new BasicStroke(dy);
        this.gridStroke = new BasicStroke(this.lib.getScaleFactor());

        updateSizeVariables();
    }

    /**
     * Update the variables that depend on the screen size (and scale).
     */
    private void updateSizeVariables() {
        // If we draw a tile in the center of the frame, then there is
        // vSpace pixels above and below it, and hSpace pixels to the
        // left and right.
        this.vSpace = (this.size.height - this.tileHeight) / 2;
        this.hSpace = (this.size.width - this.tileWidth) / 2;

        // Calculate the number of map rows to the center tile.
        // Note use of halfHeight, because (x,y) -> (x,y+1) corresponds to:
        //   P(px,py) -> P(px+tw/2,py+th/2) (y even)
        //            -> P(px-tw/2,py+th/2) (y odd)
        // thus there is always a step of th/2.
        // We add 1 to include the center tile
        this.centerRows = (this.vSpace / this.halfHeight) + 1;
        // If there is extra vertical space then another row can be
        // partially drawn
        int vExtra = this.vSpace % this.tileHeight;
        if (vExtra != 0) this.centerRows++;

        // Calculate the number of map columns to the center tile.
        // This time we divide by the full width of the tile because
        // (x,y) -> (x+1,y) corresponds to:
        //   P(px,py) -> P(px+tw,ty)  ; #4
        // Again add 1 to include the center tile
        this.centerColumns = this.hSpace / this.tileWidth + 1;

        // If there extra horizontal space?
        int hExtra = this.hSpace % this.tileWidth;
        // Special y-coordinate-dependent offsets to centerColumns.
        // If the row to display borders the left of the window
        // (i.e. y is even by Property#3) then if there is more than half
        // a tile width left over we display another column before
        // the center tile
        this.columnEvenYOffset = (hExtra > this.halfWidth) ? 1 : 0;
        // If the row to display is inset (y odd) and there is no
        // extra space we display one fewer column before the center tile
        this.columnOddYOffset = (hExtra == 0) ? -1 : 0;
    }


    // Other internals

    /**
     * Create a label to use for animating a unit.
     *
     * @param unit The {@code Unit} to animate.
     * @return A {@code JLabel} to use in animation.
     */
    private JLabel createUnitAnimationLabel(Unit unit) {
        final BufferedImage unitImg = this.lib.getScaledUnitImage(unit);
        final int width = this.halfWidth + unitImg.getWidth()/2;
        final int height = unitImg.getHeight();

        BufferedImage img = new BufferedImage(width, height,
                                              BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        final int unitX = (width - unitImg.getWidth()) / 2;
        g2d.drawImage(unitImg, unitX, 0, null);

        final Player player = getMyPlayer();
        String text = Messages.message(unit.getOccupationLabel(player, false));
        g2d.drawImage(this.lib.getOccupationIndicatorChip(g2d, unit, text),
                      0, 0, null);

        final JLabel label = new JLabel(new ImageIcon(img));
        label.setSize(width, height);
        g2d.dispose();
        return label;
    }

    // There is no getTop/BottomRows(), this.centerRows is sufficient
    
    /**
     * Get the number of columns that are to the left of the center tile
     * with the given y-coordinate.
     *
     * @param y The y-coordinate of the tile in question.
     * @return The number of columns.
     */
    private int getLeftColumns(int y) {
        return this.centerColumns
            + (((y & 1) == 0) ? this.columnEvenYOffset : this.columnOddYOffset);
    }

    /**
     * Get the number of columns that are to the right of the center tile
     * with the given y-coordinate.
     *
     * Note: same as getLeftColumns except the offset is subtracted.
     *
     * @param y The y-coordinate of the tile in question.
     * @return The number of columns.
     */
    private int getRightColumns(int y) {
        return this.centerColumns
            - (((y & 1) == 0) ? this.columnEvenYOffset : this.columnOddYOffset);
    }

    /**
     * Is a tile with given y-coordinate near the top?
     *
     * @param y The y-coordinate.
     * @return True if near the top.
     */
    private boolean isMapNearTop(int y) {
        return y < this.centerRows;
    }

    /**
     * Is a tile with given y-coordinate near the bottom?
     *
     * @param y The y-coordinate.
     * @param mapHeight The map tile height.
     * @return True if near the bottom.
     */
    private boolean isMapNearBottom(int y, int mapHeight) {
        return y >= mapHeight - this.centerRows;
    }

    /**
     * Is a tile with given x,y coordinates near the left?
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if near the left.
     */
    private boolean isMapNearLeft(int x, int y) {
        return x < getLeftColumns(y);
    }

    /**
     * Is a tile with given x,y coordinates near the right?
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param mapWidth The map tile width.
     * @return True if near the right.
     */
    private boolean isMapNearRight(int x, int y, int mapWidth) {
        return x >= mapWidth - getRightColumns(y);
    }

    /**
     * Convert tile coordinates to pixel coordinates.
     *
     * Beware:  Only safe to call if the tile is known non-null and
     * currently visible.
     *
     * @param tile The {@code Tile} to convert coordinates.
     * @param a An array to pass back the x,y pixel coordinates.
     */
    private void tileToPixelXY(Tile tile, int[] a) {
        final int x = tile.getX(), y = tile.getY();
        a[0] = leftColumnX + this.tileWidth * (x - leftColumn); // Property#4
        if ((y & 1) != 0) a[0] += this.halfWidth; // Property#2
        a[1] = topRowY + this.halfHeight * (y - topRow); // Property#5
    }


    // Public API

    // Animation support

    /**
     * Make an animation label for the unit, and reference count it.
     *
     * @param unit The {@code Unit} to animate.
     * @return A {@code JLabel} for the animation.
     */
    public JLabel enterUnitOutForAnimation(final Unit unit) {
        Integer i = this.unitsOutForAnimation.get(unit);
        if (i == null) {
            final JLabel unitLabel = createUnitAnimationLabel(unit);
            this.unitsOutForAnimationLabels.put(unit, unitLabel);
            i = 1;
        } else {
            i++;
        }
        this.unitsOutForAnimation.put(unit, i);
        return this.unitsOutForAnimationLabels.get(unit);
    }

    /**
     * Release an animation label for a unit, maintain the reference count.
     *
     * @param unit The {@code Unit} to animate.
     */
    public void releaseUnitOutForAnimation(final Unit unit) {
        Integer i = this.unitsOutForAnimation.get(unit);
        if (i == null) {
            throw new RuntimeException("Unit not out for animation: " + unit);
        }
        if (i == 1) {
            this.unitsOutForAnimation.remove(unit);
        } else {
            i--;
            this.unitsOutForAnimation.put(unit, i);
        }
    }

    /**
     * Is a given unit being animated?
     *
     * @param unit The {@code Unit} to check.
     * @return True if the unit is being animated.
     */
    public boolean isOutForAnimation(final Unit unit) {
        return this.unitsOutForAnimation.containsKey(unit);
    }

    /**
     * Get the position a unit label should be positioned in a tile.
     *
     * @param unitLabel The unit {@code JLabel}.
     * @param tile The {@code Tile} to position in.
     * @return The {@code Point} to position the label.
     */
    public Point getAnimationPosition(JLabel unitLabel, Tile tile) {
        return calculateUnitLabelPositionInTile(unitLabel,
            calculateTilePosition(tile, false));
    }


    // Cursor blinking

    /**
     * Start the cursor blink.
     */
    public void startCursorBlinking() {
        this.cursor.startBlinking();
    }

    /**
     * Stop the cursor blink.
     */
    public void stopCursorBlinking() {
        this.cursor.stopBlinking();
    }


    // Focus

    /**
     * Change the focus tile.
     *
     * @param tile The new focus {@code Tile}.
     */
    public void changeFocus(Tile tile) {
        setFocus(tile);
        forceReposition();
    }

    /**
     * Gets the focus tile, that is, the center tile of the displayed map.
     *
     * @return The center {@code Tile}.
     */
    public Tile getFocus() {
        return this.focus;
    }

    /**
     * Sets the focus of the map but offset to the left or right so
     * that the focus position can still be visible when a popup is
     * raised.  If successful, the supplied position will either be at
     * the center of the left or right half of the map.
     *
     * @param tile The {@code Tile} to display.
     * @return Positive if the focus is on the right hand side, negative
     *     if on the left, zero on failure.
     */
    public int setOffsetFocus(Tile tile) {
        if (tile == null) return 0;
        positionMap(tile);

        int ret = 0, moveX = -1;
        final Map map = getMap();
        final int tx = tile.getX(), ty = tile.getY(),
            width = rightColumn - leftColumn;
        if (leftColumn <= 0) { // At left edge already
            if (tx <= width / 4) {
                ret = -1;
            } else if (tx >= 3 * width / 4) {
                ret = 1;
            } else {
                moveX = tx + width / 4;
                ret = -1;
            }
        } else if (rightColumn >= width - 1) { // At right edge
            if (tx >= rightColumn - width / 4) {
                ret = 1;
            } else if (tx <= rightColumn - 3 * width / 4) {
                ret = -1;
            } else {
                moveX = tx - width / 4;
                ret = 1;
            }
        } else { // Move focus left 1/4 screen
            moveX = tx - width / 4;
            ret = 1;
        }
        if (moveX >= 0) {
            Tile other = map.getTile(moveX, ty);
            changeFocus(other);
        } else {
            changeFocus(tile);
        }
        return ret;
    }


    // Map wrangling, conversions between pixel coordinates, map coordinates,
    // and the tile/s thereat.
    
    /**
     * Converts the given screen coordinates to Map coordinates.
     * It checks to see to which Tile the given pixel 'belongs'.
     *
     * @param x The x-coordinate in pixels.
     * @param y The y-coordinate in pixels.
     * @return The {@code Tile} that is located at the given position
     *     on the screen.
     */
    public Tile convertToMapTile(int x, int y) {
        final Map map = getMap();
        if (map == null || this.focus == null) return null;

        // Set (leftOffset, topOffset) to the center of the focus tile
        int[] a = new int[2]; tileToPixelXY(this.focus, a);
        int leftOffset = a[0] + this.halfWidth;
        int topOffset = a[1] + this.halfHeight;

        // Next, we can calculate the center pixel of the tile-sized
        // rectangle that was clicked. First, we calculate the
        // difference in units of rows and columns.
        final int fx = this.focus.getX(), fy = this.focus.getY(),
            mapWidth = map.getWidth(), mapHeight = map.getHeight();
        int dcol = (x - leftOffset + (x > leftOffset ? this.halfWidth : -this.halfWidth))
            / this.tileWidth;
        int drow = (y - topOffset + (y > topOffset ? this.halfHeight : -this.halfHeight))
            / this.tileHeight;
        int px = leftOffset + dcol * this.tileWidth;
        int py = topOffset + drow * this.tileHeight;
        // Since rows are shifted, we need to correct.
        int newCol = fx + dcol;
        int newRow = fy + drow * 2;
        // Now, we check whether the central diamond of the calculated
        // rectangle was clicked, and adjust rows and columns
        // accordingly. See Direction.
        Direction direction = null;
        if (x > px) { // right half of the rectangle
            if (y > py) { // bottom right
                if ((y - py) > this.halfHeight - (x - px)/2) {
                    direction = Direction.SE;
                }
            } else { // top right
                if ((y - py) < (x - px)/2 - this.halfHeight) {
                    direction = Direction.NE;
                }
            }
        } else { // left half of the rectangle
            if (y > py) { // bottom left
                if ((y - py) > (x - px)/2 + this.halfHeight) {
                    direction = Direction.SW;
                }
            } else { // top left
                if ((y - py) < (px - x)/2 - this.halfHeight) {
                    direction = Direction.NW;
                }
            }
        }
        int col = newCol;
        int row = newRow;
        if (direction != null) {
            Map.Position step = direction.step(newCol, newRow);
            col = step.x;
            row = step.y;
        }
        return map.getTile(col, row);
    }

    /**
     * Calculate the bounds of the rectangle containing a Tile on the
     * screen.
     *
     * If the Tile is not on-screen a maximal rectangle is returned.
     * The bounds includes a one-tile padding area above the Tile, to
     * include the space needed by any units in the Tile.
     *
     * @param tile The {@code Tile} on the screen.
     * @return The bounds {@code Rectangle}.
     */
    public Rectangle calculateTileBounds(Tile tile) {
        if (!isTileVisible(tile)) {
            return new Rectangle(0, 0, this.size.width, this.size.height);
        }
            
        int[] a = new int[2]; tileToPixelXY(tile, a);
        return new Rectangle(a[0], a[1], this.tileWidth, this.tileHeight * 2);
    }

    /**
     * Gets the position of the given {@code Tile} on the visible map.
     *
     * @param tile The {@code Tile} to check.
     * @param rhs If true, find the right edge of the tile, otherwise
     *     the left edge.
     * @return The position of the given {@code Tile}, or {@code null}
     *     if the {@code Tile} is not visible.
     */
    public Point calculateTilePosition(Tile tile, boolean rhs) {
        if (!isTileVisible(tile)) return null;

        int[] a = new int[2]; tileToPixelXY(tile, a);
        if (rhs) a[0] += this.tileWidth;
        return new Point(a[0], a[1]);
    }

    /**
     * Gets the position where a unitLabel located at tile should be drawn.
     *
     * @param unitLabel The unit label.
     * @param tileP The position of the {@code Tile} on the screen.
     * @return The position where to put the label, null if tileP is null.
     */
    public Point calculateUnitLabelPositionInTile(JLabel unitLabel,
                                                  Point tileP) {
        if (tileP == null) return null;
        int labelX = tileP.x + this.tileWidth / 2 - unitLabel.getWidth() / 2;
        int labelY = tileP.y + this.tileHeight / 2 - unitLabel.getHeight() / 2
            - this.lib.scaleInt(UNIT_OFFSET);
        return new Point(labelX, labelY);
    }

    /**
     * Strict check for tile visibility (unlike onScreen).
     *
     * @param tile The {@code Tile} to check.
     * @return True if the tile is visible.
     */
    public boolean isTileVisible(Tile tile) {
        if (tile == null) return false;
        repositionMapIfNeeded();
        final int x = tile.getX(), y = tile.getY();
        return y >= topRow     && y <= bottomRow
            && x >= leftColumn && x <= rightColumn;
    }

    /**
     * Checks if a tile is displayed on the screen but not too close
     * to the edges.
     *
     * FIXME: The intent appears to be to have a two tile thick boundary?
     *
     * @param tile The {@code Tile} to check.
     * @return True if the tile is roughly on screen.
     */
    public boolean onScreen(Tile tile) {
        repositionMapIfNeeded();
        final int x = tile.getX(), y = tile.getY();
        return (y - 2 > topRow || alignedTop)
            && (y + 3 < bottomRow || alignedBottom)
            && (x - 1 > leftColumn || alignedLeft)
            && (x + 2 < rightColumn || alignedRight);
    }


    // Miscellaneous

    /**
     * Add a chat message.
     *
     * @param message The chat message.
     */
    public void displayChat(GUIMessage message) {
        this.chatDisplay.addMessage(message);
    }

    /**
     * Gets the unit that should be displayed on the given tile.
     *
     * Used mostly by displayMap, but public for SwingGUI.clickAt.
     *
     * @param unitTile The {@code Tile} to check.
     * @return The {@code Unit} to display or null if none found.
     */
    public Unit findUnitInFront(Tile unitTile) {
        Unit result;

        if (unitTile == null || unitTile.isEmpty()) {
            result = null;

        } else if (this.activeUnit != null
            && this.activeUnit.getTile() == unitTile
            && !isOutForAnimation(this.activeUnit)) {
            result = this.activeUnit;

        } else if (unitTile.hasSettlement()) {
            result = null;

        } else if (this.activeUnit != null
            && this.activeUnit.isOffensiveUnit()) {
            result = unitTile.getDefendingUnit(this.activeUnit);

        } else {
            // Find the unit with the most moves left, preferring active units.
            result = null;
            List<Unit> units = unitTile.getUnitList();
            int bestScore = -1;
            while (!units.isEmpty()) {
                Unit u = units.remove(0);
                if (isOutForAnimation(u)) continue;
                boolean active = u.getState() == Unit.UnitState.ACTIVE;
                int score = u.getMovesLeft() + ((active) ? 10000 : 0);
                if (bestScore < score) {
                    bestScore = score;
                    result = u;
                }
            }
        }
        return result;
    }

    /**
     * Scroll the map in the given direction.
     *
     * @param direction The {@code Direction} to scroll in.
     * @return True if scrolling occurred.
     */
    public boolean scrollMap(Direction direction) {
        Tile t = getFocus();
        if (t == null) return false;
        final int fx = t.getX(), fy = t.getY();
        if ((t = t.getNeighbourOrNull(direction)) == null) return false;
        final int tx = t.getX(), ty = t.getY();

        final Map map = getMap();
        final int mapHeight = map.getHeight(), mapWidth = map.getWidth();
        int x, y;
        // When already close to an edge, resist moving the focus closer,
        // but if moving away immediately jump out of the `nearTo' area.
        if (isMapNearTop(ty) && isMapNearTop(fy)) {
            y = (ty <= fy) ? fy : this.centerRows;
        } else if (isMapNearBottom(ty, mapHeight)
            && isMapNearBottom(fy, mapHeight)) {
            y = (ty >= fy) ? fy : mapHeight - this.centerRows;
        } else {
            y = ty;
        }
        if (isMapNearLeft(tx, ty) && isMapNearLeft(fx, fy)) {
            x = (tx <= fx) ? fx : getLeftColumns(ty);
        } else if (isMapNearRight(tx, ty, mapWidth)
            && isMapNearRight(fx, fy, mapWidth)) {
            x = (tx >= fx) ? fx : mapWidth - getRightColumns(ty);
        } else {
            x = tx;
        }

        if (x == fx && y == fy) return false;
        changeFocus(map.getTile(x, y));
        return true;
    }


    // Path wrangling, path variables are in MapViewer for displayMap
    
    /**
     * Change the goto path.
     * 
     * @param gotoPath The new goto {@code PathNode}.
     * @return True if the goto path was changed.
     */
    public boolean changeGotoPath(PathNode gotoPath) {
        if (this.gotoPath == gotoPath) return false;
        this.gotoPath = gotoPath;
        forceReposition();
        return true;
    }

    /**
     * Get the current goto path.
     *
     * @return The goto {@code PathNode}.
     */
    public PathNode getGotoPath() {
        return this.gotoPath;
    }

    /**
     * Set the current active unit path.
     *
     * @param path The new {@code PathNode}.
     */
    public void setUnitPath(PathNode path) {
        this.unitPath = path;
    }


    // Scale and size

    /**
     * Change the scale of the map.
     *
     * @param newScale The new map scale.
     */
    public void changeScale(float newScale) {
        this.lib.changeScaleFactor(newScale);
        this.tv.updateScaledVariables();
        updateScaledVariables();
    }

    /**
     * Change the displayed map size.
     *
     * @param size The new map size.
     */
    public void changeSize(Dimension size) {
        this.size = size;
        updateSizeVariables();
        forceReposition(); // TODO: needed?
    }


    // View Mode and associates

    /**
     * Get the view mode.
     *
     * @return The view mode.
     */
    public ViewMode getViewMode() {
        return this.viewMode;
    }

    /**
     * Set the view mode.
     *
     * @param vm The new {@code ViewMode}.
     */
    public void setViewMode(ViewMode vm) {
        this.viewMode = vm;
    }
    
    /**
     * Gets the active unit.
     *
     * @return The {@code Unit}.
     */
    public Unit getActiveUnit() {
        return this.activeUnit;
    }

    /**
     * Sets the active unit.
     *
     * @param activeUnit The new active {@code Unit}.
     */
    public void setActiveUnit(Unit activeUnit) {
        this.activeUnit = activeUnit;
    }

    /**
     * Gets the selected tile.
     *
     * @return The {@code Tile} selected.
     */
    public Tile getSelectedTile() {
        return this.selectedTile;
    }

    /**
     * Sets the selected tile.
     *
     * @param tile The new selected {@code Tile}.
     */
    public void setSelectedTile(Tile tile) {
        this.selectedTile = tile;
    }

    /**
     * Get either the tile with the active unit or the selected tile,
     * but only if it is visible.
     *
     * Used to determine where to display the cursor, for displayMap and
     * and the cursor action listener.
     *
     * @return The {@code Tile} found or null.
     */
    public Tile getCursorTile() {
        Tile ret = null;
        switch (this.viewMode) {
        case MOVE_UNITS:
            if (this.activeUnit != null) ret = this.activeUnit.getTile();
            break;
        case TERRAIN:
            ret = this.selectedTile;
            break;
        default:
            break;
        }
        return (isTileVisible(ret)) ? ret : null;
    }


    // TODO below

    /**
     * Force the next screen repaint to reposition the tiles on the window.
     */
    public void forceReposition() {
        bottomRow = -1;
    }
    
    private void repositionMapIfNeeded() {
        if (bottomRow < 0 && this.focus != null) positionMap(this.focus);
    }

    /**
     * Position the map so that the supplied tile is displayed at the center.
     *
     * @param pos The {@code Tile} to center at.
     */
    private void positionMap(Tile pos) {
        final int x = pos.getX(), y = pos.getY();
        final Map map = getMap();
        final int mapWidth = map.getWidth(), mapHeight = map.getHeight();
        int leftColumns = getLeftColumns(y), rightColumns = getRightColumns(y);

        /*
          PART 1
          ======
          Calculate: bottomRow, topRow, topRowY
          This will tell us which rows need to be drawn on the screen (from
          bottomRow until and including topRow).
        */
        alignedTop = false;
        alignedBottom = false;
        if (isMapNearTop(y)) {
            alignedTop = true; // We are at the top of the map
            bottomRow = (this.size.height / this.halfHeight) - 1;
            if ((this.size.height % this.halfHeight) != 0) {
                bottomRow++;
            }
            topRow = 0;

            topRowY = 0;
        } else if (isMapNearBottom(y, mapHeight)) {
            alignedBottom = true; // We are at the bottom of the map
            bottomRow = mapHeight - 1;

            topRow = this.size.height / this.halfHeight;
            if ((this.size.height % this.halfHeight) > 0) {
                topRow++;
            }
            topRow = mapHeight - topRow;
                
            topRowY = (size.height - this.tileHeight)
                - (bottomRow - topRow) * this.halfHeight;
        } else { // We are not at the top of the map and not at the bottom
            bottomRow = y + this.centerRows - 1;
            topRow = y - this.centerRows;

            topRowY = this.vSpace - this.centerRows * this.halfHeight;
        }

        /*
          PART 2
          ======
          Calculate: leftColumn, rightColumn, leftColumnX
          This will tell us which columns need to be drawn on the screen (from
          leftColumn until and including rightColumn).
          leftColumnX will tell us at which x-coordinate the left
          column needs to be drawn (this is for the Tiles where y&1 == 0;
          the others should be halfWidth more to the right).
        */
        alignedLeft = false;
        alignedRight = false;
        if (isMapNearLeft(x, y)) {
            alignedLeft = true; // We are at the left side of the map
            leftColumn = 0;
            rightColumn = this.size.width / this.tileWidth - 1;
            if ((this.size.width % this.tileWidth) > 0) {
                rightColumn++;
            }

            leftColumnX = 0;
        } else if (isMapNearRight(x, y, mapWidth)) {
            alignedRight = true; // We are at the right side of the map
            rightColumn = mapWidth - 1;
            leftColumn = this.size.width / this.tileWidth;
            if ((this.size.width % this.tileWidth) > 0) {
                leftColumn++;
            }
            leftColumnX = this.size.width - this.tileWidth - this.halfWidth
                - leftColumn * this.tileWidth;
            leftColumn = rightColumn - leftColumn;
        } else { // We are not at the left of the map and not at the right
            leftColumn = x - leftColumns;
            rightColumn = x + rightColumns;

            leftColumnX = (this.size.width - this.tileWidth) / 2
                - leftColumns * this.tileWidth;
        }
    }


    /**
     * Displays the Map.
     *
     * @param g2d The {@code Graphics2D} object on which to draw the Map.
     */
    @SuppressFBWarnings(value="NP_LOAD_OF_KNOWN_NULL_VALUE",
                        justification="lazy load of extra tiles")
    public void displayMap(Graphics2D g2d) {
        final long now = now();
        final ClientOptions options = getClientOptions();
        final int colonyLabels
            = options.getInteger(ClientOptions.COLONY_LABELS);
        final boolean revengeMode = getGame().isInRevengeMode();
        final Map map = getMap();
        final Player player = getMyPlayer(); // Check, can be null in map editor

        // Remember transform
        AffineTransform originTransform = g2d.getTransform();
        Rectangle clipBounds = g2d.getClipBounds();

        // Position the map if it is not positioned yet
        repositionMapIfNeeded();

        // Determine which tiles need to be redrawn
        int firstRow = (clipBounds.y - topRowY) / this.halfHeight - 1;
        int clipTopY = topRowY + firstRow * this.halfHeight;
        firstRow = topRow + firstRow;
        int firstColumn = (clipBounds.x - leftColumnX) / this.tileWidth - 1;
        int clipLeftX = leftColumnX + firstColumn * this.tileWidth;
        firstColumn = leftColumn + firstColumn;
        int lastRow = (clipBounds.y + clipBounds.height - topRowY)
            / this.halfHeight;
        lastRow = topRow + lastRow;
        int lastColumn = (clipBounds.x + clipBounds.width - leftColumnX)
            / this.tileWidth;
        lastColumn = leftColumn + lastColumn;

        // Clear background
        g2d.setColor(Color.black);
        g2d.fillRect(clipBounds.x, clipBounds.y,
                     clipBounds.width, clipBounds.height);

        // Set and remember transform for upper left corner
        g2d.translate(clipLeftX, clipTopY);
        AffineTransform baseTransform = g2d.getTransform();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        // Create the common tile lists
        final int x0 = firstColumn;
        final int y0 = firstRow;
        final List<Tile> baseTiles = map.subMap(x0, y0,
            lastColumn-firstColumn+1, lastRow-firstRow+1);
        List<Tile> extendedTiles = null;

        // Display the base Tiles
        int xt0 = 0, yt0 = 0;
        for (Tile t : baseTiles) {
            final int x = t.getX();
            final int y = t.getY();
            final int xt = (x-x0) * this.tileWidth
                + (((y&1)==1) ? this.halfWidth : 0);
            final int yt = (y-y0) * this.halfHeight;
            g2d.translate(xt - xt0, yt - yt0);
            xt0 = xt; yt0 = yt;

            this.tv.displayTileWithBeachAndBorder(g2d, t);
            this.tv.displayUnknownTileBorder(g2d, t);
        }
        g2d.translate(-xt0, -yt0);

        // Draw the grid, if needed
        if (options.getBoolean(ClientOptions.DISPLAY_GRID)) {
            // Generate a zigzag GeneralPath
            GeneralPath gridPath = new GeneralPath();
            gridPath.moveTo(0, 0);
            int nextX = this.halfWidth;
            int nextY = -this.halfHeight;
            for (int i = 0; i <= ((lastColumn - firstColumn) * 2 + 1); i++) {
                gridPath.lineTo(nextX, nextY);
                nextX += this.halfWidth;
                nextY = (nextY == 0) ? -this.halfHeight : 0;
            }

            // Display the grid
            g2d.setStroke(this.gridStroke);
            g2d.setColor(Color.BLACK);
            for (int row = firstRow; row <= lastRow; row++) {
                g2d.translate(0, this.halfHeight);
                AffineTransform rowTransform = g2d.getTransform();
                if ((row & 1) == 1) {
                    g2d.translate(this.halfWidth, 0);
                }
                g2d.draw(gridPath);
                g2d.setTransform(rowTransform);
            }
            g2d.setTransform(baseTransform);
        }

        // Paint full region borders
        if (options.getInteger(ClientOptions.DISPLAY_TILE_TEXT)
                == ClientOptions.DISPLAY_TILE_TEXT_REGIONS) {
            if (extendedTiles == null) {
                extendedTiles = map.subMap(x0, y0-1, lastColumn-firstColumn+1,
                                           lastRow-firstRow+1+1);
            }

            xt0 = yt0 = 0;
            for (Tile t : extendedTiles) {
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;
                displayTerritorialBorders(g2d, t, BorderType.REGION, true);
            }
            g2d.translate(-xt0, -yt0);
        }

        // Paint full country borders
        if (options.getBoolean(ClientOptions.DISPLAY_BORDERS)) {
            if (extendedTiles == null) {
                extendedTiles = map.subMap(x0, y0-1, lastColumn-firstColumn+1,
                                           lastRow-firstRow+1+1);
            }

            xt0 = yt0 = 0;
            for (Tile t : extendedTiles) {
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;
                displayTerritorialBorders(g2d, t, BorderType.COUNTRY, true);
            }
            g2d.translate(-xt0, -yt0);
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_OFF);

        // Apply fog of war to flat parts of all tiles
        RescaleOp fow = null;
        if (player != null
            && getSpecification().getBoolean(GameOptions.FOG_OF_WAR)) {
            // Knowing that we have FOW, prepare a rescaling for the
            // overlay step below.
            fow = new RescaleOp(new float[] { 0.8f, 0.8f, 0.8f, 1f },
                                new float[] { 0, 0, 0, 0 },
                                null);

            final Composite oldComposite = g2d.getComposite();
            g2d.setColor(Color.BLACK);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                        0.2f));
            xt0 = yt0 = 0;
            for (Tile t : baseTiles) {
                if (!t.isExplored() || player.canSee(t)) continue;
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;
                g2d.fill(this.fog);
            }
            g2d.translate(-xt0, -yt0);
            g2d.setComposite(oldComposite);
        }

        // Display the Tile overlays
        boolean withNumbers = colonyLabels == ClientOptions.COLONY_LABELS_CLASSIC;
        xt0 = yt0 = 0;
        for (Tile t : baseTiles) {
            if (!t.isExplored()) continue;
            final int x = t.getX();
            final int y = t.getY();
            final int xt = (x-x0) * this.tileWidth
                + (y&1) * this.halfWidth;
            final int yt = (y-y0) * this.halfHeight;
            g2d.translate(xt - xt0, yt - yt0);
            xt0 = xt; yt0 = yt;
            
            BufferedImage overlayImage = this.lib.getScaledOverlayImage(t);
            RescaleOp rop = (player == null || player.canSee(t)) ? null : fow;
            this.tv.displayTileItems(g2d, t, rop, overlayImage);
            this.tv.displaySettlementWithChipsOrPopulationNumber(g2d, t,
                withNumbers, rop);
            this.tv.displayOptionalTileText(g2d, t);
        }
        g2d.translate(-xt0, -yt0);
        
        // Paint transparent region borders
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        if (options.getInteger(ClientOptions.DISPLAY_TILE_TEXT)
            == ClientOptions.DISPLAY_TILE_TEXT_REGIONS) {
            if (extendedTiles == null) {
                extendedTiles = map.subMap(x0, y0-1, lastColumn-firstColumn+1,
                                           lastRow-firstRow+1+1);
            }

            xt0 = yt0 = 0;
            for (Tile t : extendedTiles) {
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;

                displayTerritorialBorders(g2d, t, BorderType.REGION, false);
            }
            g2d.translate(-xt0, -yt0);
        }

        // Paint transparent country borders
        if (options.getBoolean(ClientOptions.DISPLAY_BORDERS)) {
            if (extendedTiles == null) {
                extendedTiles = map.subMap(x0, y0-1, lastColumn-firstColumn+1,
                                           lastRow-firstRow+1+1);
            }

            xt0 = yt0 = 0;
            for (Tile t : extendedTiles) {
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;

                displayTerritorialBorders(g2d, t, BorderType.COUNTRY, false);
            }
            g2d.translate(-xt0, -yt0);
        }

        // Display cursor for selected tile or active unit
        final Tile cursorTile = getCursorTile();
        if (cursorTile != null && this.cursor.isActive()) {
            final int x = cursorTile.getX();
            final int y = cursorTile.getY();
            if (x >= x0 && y >= y0 && x <= lastColumn && y <= lastRow) {
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt, yt);
                g2d.drawImage(this.lib.getScaledImage(ImageLibrary.UNIT_SELECT),
                              0, 0, null);
                g2d.translate(-xt, -yt);
            }
        }

        // Display units
        g2d.setColor(Color.BLACK);
        if (!revengeMode) {
            xt0 = yt0 = 0;
            for (Tile t : baseTiles) {
                // check for units
                Unit unit = findUnitInFront(t);
                if (unit == null || isOutForAnimation(unit)) continue;
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;

                displayUnit(g2d, unit);
            }
            g2d.translate(-xt0, -yt0);
        } else {
            /* Add extra rows and colums, as the dark halo is huge to enable
               a very slow fade into transparency, see BR#2580 */
            BufferedImage darkness
                = this.lib.getScaledImage(ImageLibrary.DARKNESS);
            xt0 = yt0 = 0;
            for (Tile t : map.subMap(x0-2, y0-4, lastColumn-firstColumn+1+4,
                                                 lastRow-firstRow+1+8)) {
                // check for units
                Unit unit = findUnitInFront(t);
                if (unit == null || isOutForAnimation(unit)) continue;
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;

                if (unit.isUndead()) {
                    this.tv.displayCenteredImage(g2d, darkness);
                }
                displayUnit(g2d, unit);
            }
            g2d.translate(-xt0, -yt0);
        }

        // Display the colony names, if needed
        if (colonyLabels != ClientOptions.COLONY_LABELS_NONE) {
            if (extendedTiles == null) {
                extendedTiles = map.subMap(x0, y0-1, lastColumn-firstColumn+1,
                                           lastRow-firstRow+1+1);
            }
            /* For settlement names and territorial borders 1 extra row needs
               to be drawn in north to prevent missing parts on partial redraws,
               as they can reach below their tiles, see BR#2580 */
            xt0 = yt0 = 0;
            for (Tile t : extendedTiles) {
                Settlement settlement = t.getSettlement();
                if (settlement == null) continue;
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * this.tileWidth
                    + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0); 
                xt0 = xt; yt0 = yt;
                RescaleOp rop = (player == null || player.canSee(t))
                    ? null : fow;

                displaySettlementLabels(g2d, settlement, player,
                                        colonyLabels, rop);
            }
            g2d.translate(-xt0, -yt0);
        }

        // Restore original transform to allow for more drawing
        g2d.setTransform(originTransform);

        // Display goto path
        if (this.unitPath != null) displayPath(g2d, this.unitPath);
        else if (this.gotoPath != null) displayPath(g2d, this.gotoPath);

        // Draw the chat
        this.chatDisplay.display(g2d, this.size);

        // Timing log
        if (logger.isLoggable(Level.FINEST)) {
            final long gap = now() - now;
            double avg = ((double)gap)
                / ((lastRow-firstRow) * (lastColumn-firstColumn));
            StringBuilder sb = new StringBuilder(128);
            sb.append("displayMap time = ").append(gap)
                .append(" for ").append(firstColumn)
                .append(" ").append(firstRow)
                .append(" to ").append(lastColumn)
                .append(" ").append(lastRow)
                .append(" average ").append(avg);
            logger.finest(sb.toString());
        }
    }

    private void displaySettlementLabels(Graphics2D g2d, Settlement settlement,
                                         Player player, int colonyLabels,
                                         RescaleOp rop) {
        if (settlement.isDisposed()) {
            logger.warning("Settlement display race detected: "
                           + settlement.getName());
            return;
        }
        String name = Messages.message(settlement.getLocationLabelFor(player));
        if (name == null) return;

        Color backgroundColor = settlement.getOwner().getNationColor();
        if (backgroundColor == null) backgroundColor = Color.WHITE;
        // int yOffset = this.lib.getSettlementImage(settlement).getHeight() + 1;
        int yOffset = this.tileHeight;
        switch (colonyLabels) {
        case ClientOptions.COLONY_LABELS_CLASSIC:
            BufferedImage img = this.lib.getStringImage(g2d, name, backgroundColor,
                                                        this.fontNormal);
            g2d.drawImage(img, rop, (this.tileWidth - img.getWidth())/2 + 1,
                          yOffset);
            break;

        case ClientOptions.COLONY_LABELS_MODERN:
        default:
            backgroundColor = new Color(backgroundColor.getRed(),
                                        backgroundColor.getGreen(),
                                        backgroundColor.getBlue(), 128);
            TextSpecification[] specs = new TextSpecification[1];
            if (settlement instanceof Colony
                && settlement.getOwner() == player) {
                Colony colony = (Colony) settlement;
                BuildableType buildable = colony.getCurrentlyBuilding();
                if (buildable != null && this.fontProduction != null) {
                    specs = new TextSpecification[2];
                    String t = Messages.getName(buildable) + " " +
                        Turn.getTurnsText(colony.getTurnsToComplete(buildable));
                    specs[1] = new TextSpecification(t, this.fontProduction);
                }
            }
            specs[0] = new TextSpecification(name, this.fontNormal);

            BufferedImage nameImage = createLabel(g2d, specs, backgroundColor);
            int spacing = 3;
            BufferedImage leftImage = null;
            BufferedImage rightImage = null;
            if (settlement instanceof Colony) {
                Colony colony = (Colony)settlement;
                String string = Integer.toString(colony.getApparentUnitCount());
                leftImage = createLabel(g2d, string,
                    ((colony.getPreferredSizeChange() > 0)
                        ? this.fontItalic : this.fontNormal),
                    backgroundColor);
                if (player.owns(settlement)) {
                    int bonusProduction = colony.getProductionBonus();
                    if (bonusProduction != 0) {
                        String bonus = (bonusProduction > 0)
                            ? "+" + bonusProduction
                            : Integer.toString(bonusProduction);
                        rightImage = createLabel(g2d, bonus, this.fontNormal,
                                                 backgroundColor);
                    }
                }
            } else if (settlement instanceof IndianSettlement) {
                IndianSettlement is = (IndianSettlement) settlement;
                if (is.getType().isCapital()) {
                    leftImage = createCapitalLabel(nameImage.getHeight(),
                        5, backgroundColor);
                }
                
                Unit missionary = is.getMissionary();
                if (missionary != null) {
                    boolean expert = missionary.hasAbility(Ability.EXPERT_MISSIONARY);
                    backgroundColor = missionary.getOwner().getNationColor();
                    backgroundColor = new Color(backgroundColor.getRed(),
                                                backgroundColor.getGreen(),
                                                backgroundColor.getBlue(), 128);
                    rightImage = createReligiousMissionLabel(nameImage.getHeight(), 5,
                                                             backgroundColor, expert);
                }
            }
            
            int width = this.lib.scaleInt(nameImage.getWidth()
                + ((leftImage == null) ? 0 : leftImage.getWidth() + spacing)
                + ((rightImage == null) ? 0 : rightImage.getWidth() + spacing));
            int xOffset = (this.tileWidth - width)/2;
            yOffset -= this.lib.scaleInt(nameImage.getHeight())/2;
            if (leftImage != null) {
                g2d.drawImage(leftImage, rop, xOffset, yOffset);
                xOffset += this.lib.scaleInt(leftImage.getWidth() + spacing);
            }
            g2d.drawImage(nameImage, rop, xOffset, yOffset);
            if (rightImage != null) {
                xOffset += this.lib.scaleInt(nameImage.getWidth() + spacing);
                g2d.drawImage(rightImage, rop, xOffset, yOffset);
            }
            break;
        }
    }

    /**
     * Draws the pentagram indicating a native capital.
     *
     * @param extent The nominal height of the image.
     * @param padding Padding to add around the image.
     * @param backgroundColor The image background color.
     * @return A suitable {@code BufferedImage}.
     */
    private static BufferedImage createCapitalLabel(int extent, int padding,
                                                    Color backgroundColor) {
        // create path
        double deg2rad = Math.PI/180.0;
        double angle = -90.0 * deg2rad;
        double offset = extent * 0.5;
        double size1 = (extent - padding - padding) * 0.5;

        GeneralPath path = new GeneralPath();
        path.moveTo(Math.cos(angle) * size1 + offset, Math.sin(angle) * size1 + offset);
        for (int i = 0; i < 4; i++) {
            angle += 144 * deg2rad;
            path.lineTo(Math.cos(angle) * size1 + offset, Math.sin(angle) * size1 + offset);
        }
        path.closePath();

        // draw everything
        BufferedImage bi = new BufferedImage(extent, extent, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(backgroundColor);
        g2d.fill(new RoundRectangle2D.Float(0, 0, extent, extent, padding, padding));
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(path);
        g2d.setColor(Color.WHITE);
        g2d.fill(path);
        g2d.dispose();
        return bi;
    }

    /**
     * Creates an BufferedImage that shows the given text centred on a
     * translucent rounded rectangle with the given color.
     *
     * @param g2d a {@code Graphics2D}
     * @param text a {@code String}
     * @param font a {@code Font}
     * @param backgroundColor a {@code Color}
     * @return an {@code BufferedImage}
     */
    private static BufferedImage createLabel(Graphics2D g2d, String text,
                                             Font font, Color backgroundColor) {
        TextSpecification[] specs = new TextSpecification[1];
        specs[0] = new TextSpecification(text, font);
        return createLabel(g2d, specs, backgroundColor);
    }

    /**
     * Creates an BufferedImage that shows the given text centred on a
     * translucent rounded rectangle with the given color.
     *
     * @param g2d a {@code Graphics2D}
     * @param textSpecs a {@code TextSpecification} array
     * @param backgroundColor a {@code Color}
     * @return a {@code BufferedImage}
     */
    private static BufferedImage createLabel(Graphics2D g2d,
                                             TextSpecification[] textSpecs,
                                             Color backgroundColor) {
        int hPadding = 15;
        int vPadding = 10;
        int linePadding = 5;
        int width = 0;
        int height = vPadding;
        int i;

        TextSpecification spec;
        TextLayout[] labels = new TextLayout[textSpecs.length];
        TextLayout label;

        for (i = 0; i < textSpecs.length; i++) {
            spec = textSpecs[i];
            label = new TextLayout(spec.text, spec.font,
                                   g2d.getFontRenderContext());
            labels[i] = label;
            Rectangle textRectangle = label.getPixelBounds(null, 0, 0);
            width = Math.max(width, textRectangle.width + hPadding);
            if (i > 0) height += linePadding;
            height += (int) (label.getAscent() + label.getDescent());
        }

        int radius = Math.min(hPadding, vPadding);

        BufferedImage bi = new BufferedImage(width, height,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                            RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setColor(backgroundColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, radius, radius));
        g2.setColor(ImageLibrary.makeForegroundColor(backgroundColor));
        float y = vPadding / 2.0f;
        for (i = 0; i < labels.length; i++) {
            Rectangle textRectangle = labels[i].getPixelBounds(null, 0, 0);
            float x = (width - textRectangle.width) / 2.0f;
            y += labels[i].getAscent();
            labels[i].draw(g2, x, y);
            y += labels[i].getDescent() + linePadding;
        }
        g2.dispose();
        return bi;
    }

    /**
     * Draws a cross indicating a religious mission is present in the
     * native village.
     *
     * @param extent The nominal height of the image.
     * @param padding Padding to add around the image.
     * @param backgroundColor The image background color.
     * @param expertMissionary True if the label should show expertise.
     * @return A suitable {@code BufferedImage}.
     */
    private static BufferedImage createReligiousMissionLabel(int extent,
            int padding, Color backgroundColor, boolean expertMissionary) {
        // create path
        double offset = extent * 0.5;
        double size1 = extent - padding - padding;
        double bar = size1 / 3.0;
        double inset = 0.0;
        double kludge = 0.0;

        GeneralPath circle = new GeneralPath();
        GeneralPath cross = new GeneralPath();
        if (expertMissionary) {
            // this is meant to represent the eucharist (the -1, +1 thing is a nasty kludge)
            circle.append(new Ellipse2D.Double(padding-1, padding-1, size1+1, size1+1), false);
            inset = 4.0;
            bar = (size1 - inset - inset) / 3.0;
            // more nasty -1, +1 kludges
            kludge = 1.0;
        }
        offset -= 1.0;
        cross.moveTo(offset, padding + inset - kludge);
        cross.lineTo(offset, extent - padding - inset);
        cross.moveTo(offset - bar, padding + bar + inset);
        cross.lineTo(offset + bar + 1, padding + bar + inset);

        // draw everything
        BufferedImage bi = new BufferedImage(extent, extent, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(backgroundColor);
        g2d.fill(new RoundRectangle2D.Float(0, 0, extent, extent, padding, padding));
        g2d.setColor(ImageLibrary.makeForegroundColor(backgroundColor));
        if (expertMissionary) {
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(circle);
            g2d.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        } else {
            g2d.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g2d.draw(cross);
        g2d.dispose();
        return bi;
    }


    /**
     * Display a path.
     *
     * @param g2d The {@code Graphics2D} to display on.
     * @param path The {@code PathNode} to display.
     */
    private void displayPath(Graphics2D g2d, PathNode path) {
        final boolean debug = FreeColDebugger
            .isInDebugMode(FreeColDebugger.DebugMode.PATHS);

        for (PathNode p = path; p != null; p = p.next) {
            Tile tile = p.getTile();
            if (tile == null) continue;
            Point point = calculateTilePosition(tile, false);
            if (point == null) continue;

            BufferedImage image = (p.isOnCarrier())
                ? ImageLibrary.getPathImage(ImageLibrary.PathType.NAVAL)
                : (this.activeUnit != null)
                ? ImageLibrary.getPathImage(this.activeUnit)
                : null;

            BufferedImage turns = null;
            if (this.fontTiny != null) {
                if (debug) { // More detailed display
                    if (this.activeUnit != null) {
                        image = ImageLibrary.getPathNextTurnImage(this.activeUnit);
                    }
                    turns = this.lib.getStringImage(g2d,
                        Integer.toString(p.getTurns())
                        + "/" + Integer.toString(p.getMovesLeft()),
                        Color.WHITE, this.fontTiny);
                } else {
                    turns = (p.getTurns() <= 0) ? null
                        : this.lib.getStringImage(g2d,
                            Integer.toString(p.getTurns()),
                            Color.WHITE, this.fontTiny);
                }
                g2d.setColor((turns == null) ? Color.GREEN : Color.RED);
            }

            g2d.translate(point.x, point.y);
            if (image == null) {
                g2d.fillOval(this.halfWidth, this.halfHeight, 10, 10);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(this.halfWidth, this.halfHeight, 10, 10);
            } else {
                this.tv.displayCenteredImage(g2d, image);
                if (turns != null) {
                    this.tv.displayCenteredImage(g2d, turns);
                }
            }
            g2d.translate(-point.x, -point.y);
        }
    }

    /**
     * Displays the given Unit onto the given Graphics2D object at the
     * location specified by the coordinates.
     *
     * @param g2d The Graphics2D object on which to draw the Unit.
     * @param unit The Unit to draw.
     */
    private void displayUnit(Graphics2D g2d, Unit unit) {
        final Player player = getMyPlayer();

        // Draw the unit.
        // If unit is sentry, draw in grayscale
        boolean fade = (unit.getState() == Unit.UnitState.SENTRY)
            || (unit.hasTile()
                && player != null && !player.canSee(unit.getTile()));
        BufferedImage image = this.lib.getScaledUnitImage(unit, fade);
        Point p = calculateUnitImagePositionInTile(image);
        g2d.drawImage(image, p.x, p.y, null);

        // Draw an occupation and nation indicator.
        String text = Messages.message(unit.getOccupationLabel(player, false));
        g2d.drawImage(this.lib.getOccupationIndicatorChip(g2d, unit, text),
                      this.lib.scaleInt(TileViewer.STATE_OFFSET_X), 0, null);

        // Draw one small line for each additional unit (like in civ3).
        int unitsOnTile = 0;
        if (unit.hasTile()) {
            // When a unit is moving from tile to tile, it is
            // removed from the source tile.  So the unit stack
            // indicator cannot be drawn during the movement see
            // UnitMoveAnimation.animate() for details
            unitsOnTile = unit.getTile().getTotalUnitCount();
        }
        if (unitsOnTile > 1) {
            g2d.setColor(Color.WHITE);
            int unitLinesY = OTHER_UNITS_OFFSET_Y;
            int x1 = this.lib.scaleInt(TileViewer.STATE_OFFSET_X
                + OTHER_UNITS_OFFSET_X);
            int x2 = this.lib.scaleInt(TileViewer.STATE_OFFSET_X
                + OTHER_UNITS_OFFSET_X + OTHER_UNITS_WIDTH);
            for (int i = 0; i < unitsOnTile && i < MAX_OTHER_UNITS; i++) {
                g2d.drawLine(x1, unitLinesY, x2, unitLinesY);
                unitLinesY += 2;
            }
        }

        // FOR DEBUGGING
        net.sf.freecol.server.ai.AIUnit au;
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
            && player != null && !player.owns(unit)
            && unit.getOwner().isAI()
            && getFreeColServer() != null
            && getFreeColServer().getAIMain() != null
            && (au = getFreeColServer().getAIMain().getAIUnit(unit)) != null) {
            if (FreeColDebugger.debugShowMission()) {
                g2d.setColor(Color.WHITE);
                g2d.drawString((!au.hasMission()) ? "No mission"
                    : lastPart(au.getMission().getClass().toString(), "."),
                    0, 0);
            }
            if (FreeColDebugger.debugShowMissionInfo() && au.hasMission()) {
                g2d.setColor(Color.WHITE);
                g2d.drawString(au.getMission().toString(), 0, 25);
            }
        }
    }

    /**
     * Gets the coordinates to draw a unit in a given tile.
     *
     * @param unitImage The unit's image
     * @return The coordinates where the unit should be drawn on screen
     */
    private Point calculateUnitImagePositionInTile(BufferedImage unitImage) {
        int unitX = (this.tileWidth - unitImage.getWidth()) / 2;
        int unitY = (this.tileHeight - unitImage.getHeight()) / 2
            - this.lib.scaleInt(UNIT_OFFSET);
        return new Point(unitX, unitY);
    }

    /**
     * Draws the borders of a territory on the given Tile. The
     * territory is either a country or a region.
     *
     * @param g2d a {@code Graphics2D}
     * @param tile a {@code Tile}
     * @param type a {@code BorderType}
     * @param opaque a {@code boolean}
     */
    private void displayTerritorialBorders(Graphics2D g2d, Tile tile,
                                           BorderType type, boolean opaque) {
        Player owner = tile.getOwner();
        Region region = tile.getRegion();
        if ((type == BorderType.COUNTRY && owner != null)
            || (type == BorderType.REGION && region != null)) {
            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(this.borderStroke);
            Color oldColor = g2d.getColor();
            Color c = null;
            if (type == BorderType.COUNTRY)
                c = owner.getNationColor();
            if (c == null)
                c = Color.WHITE;
            Color newColor = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                                 (opaque) ? 255 : 100);
            g2d.setColor(newColor);
            GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            path.moveTo(this.borderPoints.get(Direction.longSides.get(0)).x,
                        this.borderPoints.get(Direction.longSides.get(0)).y);
            for (Direction d : Direction.longSides) {
                Tile otherTile = tile.getNeighbourOrNull(d);
                Direction next = d.getNextDirection();
                Direction next2 = next.getNextDirection();
                if (otherTile == null
                    || (type == BorderType.COUNTRY && !owner.owns(otherTile))
                    || (type == BorderType.REGION && otherTile.getRegion() != region)) {
                    Tile tile1 = tile.getNeighbourOrNull(next);
                    Tile tile2 = tile.getNeighbourOrNull(next2);
                    if (tile2 == null
                        || (type == BorderType.COUNTRY && !owner.owns(tile2))
                        || (type == BorderType.REGION && tile2.getRegion() != region)) {
                        // small corner
                        path.lineTo(this.borderPoints.get(next).x,
                                    this.borderPoints.get(next).y);
                        path.quadTo(this.controlPoints.get(next).x,
                                    this.controlPoints.get(next).y,
                                    this.borderPoints.get(next2).x,
                                    this.borderPoints.get(next2).y);
                    } else {
                        int dx = 0, dy = 0;
                        switch(d) {
                        case NW: dx = this.halfWidth; dy = -this.halfHeight; break;
                        case NE: dx = this.halfWidth; dy = this.halfHeight; break;
                        case SE: dx = -this.halfWidth; dy = this.halfHeight; break;
                        case SW: dx = -this.halfWidth; dy = -this.halfHeight; break;
                        default: break;
                        }
                        if (tile1 != null
                            && ((type == BorderType.COUNTRY && owner.owns(tile1))
                                || (type == BorderType.REGION && tile1.getRegion() == region))) {
                            // short straight line
                            path.lineTo(this.borderPoints.get(next).x,
                                        this.borderPoints.get(next).y);
                            // big corner
                            Direction previous = d.getPreviousDirection();
                            Direction previous2 = previous.getPreviousDirection();
                            int ddx = 0, ddy = 0;
                            switch(d) {
                            case NW: ddy = -this.tileHeight; break;
                            case NE: ddx = this.tileWidth; break;
                            case SE: ddy = this.tileHeight; break;
                            case SW: ddx = -this.tileWidth; break;
                            default: break;
                            }
                            path.quadTo(this.controlPoints.get(previous).x + dx,
                                        this.controlPoints.get(previous).y + dy,
                                        this.borderPoints.get(previous2).x + ddx,
                                        this.borderPoints.get(previous2).y + ddy);
                        } else {
                            // straight line
                            path.lineTo(this.borderPoints.get(d).x + dx,
                                        this.borderPoints.get(d).y + dy);
                        }
                    }
                } else {
                    path.moveTo(this.borderPoints.get(next2).x,
                                this.borderPoints.get(next2).y);
                }
            }
            g2d.draw(path);
            g2d.setColor(oldColor);
            g2d.setStroke(oldStroke);
        }
    }

}
