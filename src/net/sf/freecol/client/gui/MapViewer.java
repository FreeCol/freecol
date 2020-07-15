/**
 *  Copyright (C) 2002-2020   The FreeCol Team
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * MapViewer is a private helper class of Canvas and SwingGUI.
 * 
 * This class is responsible for drawing the map/background on the
 * {@code Canvas}.
 *
 * In addition, the graphical state of the map (focus, active unit..)
 * is currently handled by this class.
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

    /** The internal tile viewer to use. */
    private TileViewer tv;

    /** Scaled ImageLibrary only used for map painting. */
    private ImageLibrary lib;

    /** The map size. */
    private Dimension size = null;

    /** The current focus Tile. */
    private Tile focus = null;

    /** The view mode in use. */
    private ViewMode viewMode = ViewMode.MOVE_UNITS;
    /** The selected tile. */
    private Tile selectedTile;
    /** The active unit. */
    private Unit activeUnit;

    /** The cursor for the selected tile. */
    private TerrainCursor cursor;

    /** A path for the active unit. */
    private PathNode unitPath;

    /** A path for a current goto order. */
    private PathNode gotoPath = null;

    /** The units that are being animated and an associated reference count. */
    private final java.util.Map<Unit, Integer> unitsOutForAnimation
        = new HashMap<>();
    /** The labels being used in animation for a unit. */
    private final java.util.Map<Unit, JLabel> unitsOutForAnimationLabels
        = new HashMap<>();

    /** Fog of war area. */
    private final GeneralPath fog = new GeneralPath();

    /** Fonts (scaled). */
    private Font fontNormal, fontItalic, fontProduction, fontTiny;
        
    /** The chat message area. */
    private final ChatDisplay chatDisplay;

    // Helper variables for displaying the map.
    private int tileHeight, tileWidth, halfHeight, halfWidth,
        topSpace, topRows, /*bottomSpace,*/ bottomRows, leftSpace, rightSpace;

    // The y-coordinate of the Tiles that will be drawn at the bottom
    private int bottomRow = -1;

    // The y-coordinate of the Tiles that will be drawn at the top
    private int topRow;

    // The y-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the bottom
    private int bottomRowY;

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


    // borders
    private final EnumMap<Direction, Point2D.Float> borderPoints =
        new EnumMap<>(Direction.class);

    private final EnumMap<Direction, Point2D.Float> controlPoints =
        new EnumMap<>(Direction.class);

    private Stroke borderStroke = new BasicStroke(4);

    private Stroke gridStroke = new BasicStroke(1);


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param al An {@code ActionListener} for the cursor.
     */
    public MapViewer(FreeColClient freeColClient, ActionListener al) {
        super(freeColClient);
        
        changeImageLibrary(new ImageLibrary());
        this.cursor = new TerrainCursor();
        this.cursor.addActionListener(al);
        this.chatDisplay = new ChatDisplay();
    }

   
    // Critical internals
    
    /**
     * Change the internal image libraries.
     *
     * Update several internal variables that depend directly on the
     * tile size in the image library.
     *
     * @param lib The new {@code ImageLibrary} to use.
     */
    private void changeImageLibrary(ImageLibrary lib) {
        this.lib = lib;
        this.tv = new TileViewer(getFreeColClient(), this.lib);

        // ATTENTION: we assume that all base tiles have the same size
        final Dimension tileSize = lib.tileSize;
        this.tileHeight = tileSize.height;
        this.tileWidth = tileSize.width;
        this.halfHeight = this.tileHeight/2;
        this.halfWidth = this.tileWidth/2;

        this.fog.reset();
        this.fog.moveTo(this.halfWidth, 0);
        this.fog.lineTo(this.tileWidth, this.halfHeight);
        this.fog.lineTo(this.halfWidth, this.tileHeight);
        this.fog.lineTo(0, this.halfHeight);
        this.fog.closePath();

        this.fontNormal = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.SMALLER, Font.BOLD,
            getScale());
        this.fontItalic = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.SMALLER, Font.BOLD|Font.ITALIC,
            getScale());
        this.fontProduction = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, Font.BOLD,
            getScale());
        this.fontTiny = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, Font.PLAIN,
            getScale());
            
        final int dx = this.tileWidth/16;
        final int dy = this.tileHeight/16;
        final int ddx = dx + dx/2;
        final int ddy = dy + dy/2;

        // small corners
        controlPoints.put(Direction.N,
                          new Point2D.Float(this.halfWidth, dy));
        controlPoints.put(Direction.E,
                          new Point2D.Float(this.tileWidth - dx, this.halfHeight));
        controlPoints.put(Direction.S,
                          new Point2D.Float(this.halfWidth, this.tileHeight - dy));
        controlPoints.put(Direction.W,
                          new Point2D.Float(dx, this.halfHeight));
        // big corners
        controlPoints.put(Direction.SE,
                          new Point2D.Float(this.halfWidth, this.tileHeight));
        controlPoints.put(Direction.NE,
                          new Point2D.Float(this.tileWidth, this.halfHeight));
        controlPoints.put(Direction.SW,
                          new Point2D.Float(0, this.halfHeight));
        controlPoints.put(Direction.NW,
                          new Point2D.Float(this.halfWidth, 0));
        // small corners
        borderPoints.put(Direction.NW,
                         new Point2D.Float(dx + ddx, this.halfHeight - ddy));
        borderPoints.put(Direction.N,
                         new Point2D.Float(this.halfWidth - ddx, dy + ddy));
        borderPoints.put(Direction.NE,
                         new Point2D.Float(this.halfWidth + ddx, dy + ddy));
        borderPoints.put(Direction.E,
                         new Point2D.Float(this.tileWidth - dx - ddx, this.halfHeight - ddy));
        borderPoints.put(Direction.SE,
                         new Point2D.Float(this.tileWidth - dx - ddx, this.halfHeight + ddy));
        borderPoints.put(Direction.S,
                         new Point2D.Float(this.halfWidth + ddx, this.tileHeight - dy - ddy));
        borderPoints.put(Direction.SW,
                         new Point2D.Float(this.halfWidth - ddx, this.tileHeight - dy - ddy));
        borderPoints.put(Direction.W,
                         new Point2D.Float(dx + ddx, this.halfHeight + ddy));

        borderStroke = new BasicStroke(dy);
        gridStroke = new BasicStroke(getScale());
    }

    // Accessors

    /**
     * Gets the contained {@code ImageLibrary}.
     *
     * @return The image library;
     */
    public ImageLibrary getImageLibrary() {
        return this.lib;
    }

    /**
     * Get the scale factor for the image library.
     *
     * @return The scale factor.
     */
    private float getScale() {
        return this.lib.getScaleFactor();
    }

    /**
     * Get the displayed map width.
     *
     * @return The width.
     */
    private int getScreenWidth() {
        return this.size.width;
    }
    
    /**
     * Get the displayed map height.
     *
     * @return The width.
     */
    private int getScreenHeight() {
        return this.size.height;
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
     * Sets the focus tile.
     *
     * @param focus The new focus {@code Tile}.
     */
    private void setFocus(Tile focus) {
        this.focus = focus;
    }

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
     * Used to determine where to display the cursor.
     *
     * @return The {@code Tile} found or null.
     */
    public Tile getActiveTile() {
        Tile ret = null;
        switch (getViewMode()) {
        case MOVE_UNITS:
            final Unit unit = getActiveUnit();
            if (unit != null) ret = unit.getTile();
            break;
        case TERRAIN:
            ret = getSelectedTile();
            break;
        default:
            break;
        }
        return (isTileVisible(ret)) ? ret : null;
    }

    /**
     * Set the current active unit path.
     *
     * @param path The new {@code PathNode}.
     */
    public void setUnitPath(PathNode path) {
        this.unitPath = path;
    }

    /**
     * Get the current goto path.
     *
     * @return The goto {@code PathNode}.
     */
    public PathNode getGotoPath() {
        return this.gotoPath;
    }


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


   // Higher level public routines

    /**
     * Change the displayed map size.
     *
     * @param size The new map size.
     */
    public void changeSize(Dimension size) {
        this.size = size;
        updateMapDisplayVariables();
        forceReposition();
    }

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
     * Force the next screen repaint to reposition the tiles on the window.
     */
    public void forceReposition() {
        bottomRow = -1;
    }
    
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
     * Tells that a chat message was received.
     *
     * @param message The chat message.
     */
    public void displayChat(GUIMessage message) {
        this.chatDisplay.addMessage(message);
    }


    // Cleanup underway below

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
        final Game game = getGame();
        if (game == null || game.getMap() == null
            || this.focus == null) return null;

        final int fx = this.focus.getX(), fy = this.focus.getY();
        int leftOffset;
        if (fx < getLeftColumns()) {
            // we are at the left side of the map
            if ((fy & 1) == 0) {
                leftOffset = tileWidth * fx + halfWidth;
            } else {
                leftOffset = tileWidth * (fx + 1);
            }
        } else {
            final int mapWidth = game.getMap().getWidth();
            if (fx >= mapWidth - getRightColumns()) {
                // we are at the right side of the map
                if ((fy & 1) == 0) {
                    leftOffset = getScreenWidth()
                        - tileWidth * (mapWidth - fx);
                } else {
                    leftOffset = getScreenWidth() - halfWidth
                        - tileWidth * (mapWidth - fx - 1);
                }
            } else {
                if ((fy & 1) == 0) {
                    leftOffset = (getScreenWidth() / 2);
                } else {
                    leftOffset = (getScreenWidth() / 2) + halfWidth;
                }
            }
        }

        int topOffset;
        if (fy < topRows) {
            // we are at the top of the map
            topOffset = (fy + 1) * halfHeight;
        } else {
            final int mapHeight = game.getMap().getHeight();
            if (fy >= mapHeight - bottomRows) {
                // we are at the bottom of the map
                topOffset = getScreenHeight() - halfHeight * (mapHeight - fy);
            } else {
                topOffset = getScreenHeight() / 2;
            }
        }

        // At this point (leftOffset, topOffset) is the center pixel
        // of the Tile that was on focus (= the Tile that should have
        // been drawn at the center of the screen if possible).

        // Next, we can calculate the center pixel of the tile-sized
        // rectangle that was clicked. First, we calculate the
        // difference in units of rows and columns.
        int dcol = (x - leftOffset + (x > leftOffset ? halfWidth : -halfWidth))
            / tileWidth;
        int drow = (y - topOffset + (y > topOffset ? halfHeight : -halfHeight))
            / tileHeight;
        int px = leftOffset + dcol * tileWidth;
        int py = topOffset + drow * tileHeight;
        // Since rows are shifted, we need to correct.
        int newCol = fx + dcol;
        int newRow = fy + drow * 2;
        // Now, we check whether the central diamond of the calculated
        // rectangle was clicked, and adjust rows and columns
        // accordingly. See Direction.
        Direction direction = null;
        if (x > px) {
            // right half of the rectangle
            if (y > py) {
                // bottom right
                if ((y - py) > halfHeight - (x - px)/2) {
                    direction = Direction.SE;
                }
            } else {
                // top right
                if ((y - py) < (x - px)/2 - halfHeight) {
                    direction = Direction.NE;
                }

            }
        } else {
            // left half of the rectangle
            if (y > py) {
                // bottom left
                if ((y - py) > (x - px)/2 + halfHeight) {
                    direction = Direction.SW;
                }
            } else {
                // top left
                if ((y - py) < (px - x)/2 - halfHeight) {
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
        logger.finest("Direction is " + direction
                      + ", new focus is " + col + ", " + row);
        return game.getMap().getTile(col, row);

    }

    /**
     * Calculate the bounds of the rectangle containing a Tile on the
     * screen, and return it.  If the Tile is not on-screen a maximal
     * rectangle is returned.  The bounds includes a one-tile padding
     * area above the Tile, to include the space needed by any units
     * in the Tile.
     *
     * @param tile The {@code Tile} on the screen.
     * @return The bounds {@code Rectangle}.
     */
    public Rectangle calculateTileBounds(Tile tile) {
        Rectangle result
            = new Rectangle(0, 0, getScreenWidth(), getScreenHeight());
        if (isTileVisible(tile)) {
            result.x = ((tile.getX() - leftColumn) * tileWidth) + leftColumnX;
            result.y = ((tile.getY() - topRow) * halfHeight) + topRowY - tileHeight;
            if ((tile.getY() & 1) != 0) {
                result.x += halfWidth;
            }
            result.width = tileWidth;
            result.height = tileHeight * 2;
        }
        return result;
    }

    /**
     * Gets the position of the given {@code Tile}
     * on the drawn map.
     *
     * @param t The {@code Tile} to check.
     * @param rhs If true, find the right edge of the tile, otherwise
     *     the left edge.
     * @return The position of the given {@code Tile}, or
     *     {@code null} if the {@code Tile} is not drawn on
     *     the mapboard.
     */
    public Point calculateTilePosition(Tile t, boolean rhs) {
        if (!isTileVisible(t)) return null;

        int x = ((t.getX() - leftColumn) * tileWidth) + leftColumnX;
        int y = ((t.getY() - topRow) * halfHeight) + topRowY;
        if ((t.getY() & 1) != 0) x += halfWidth;
        if (rhs) x += this.tileWidth;
        return new Point(x, y);
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
        int labelX = tileP.x + tileWidth / 2 - unitLabel.getWidth() / 2;
        int labelY = tileP.y + tileHeight / 2 - unitLabel.getHeight() / 2
            - (int) (UNIT_OFFSET * getScale());
        return new Point(labelX, labelY);
    }

    /**
     * Checks if a tile is displayed on the screen but not too close
     * to the edges.  The intent appears to be to have a two tile thick
     * boundary.
     *
     * @param tile The {@code Tile} to check.
     * @return True if the tile is roughly on screen.
     */
    public boolean onScreen(Tile tile) {
        repositionMapIfNeeded();
        return (tile.getY() - 2 > topRow || alignedTop)
            && (tile.getY() + 3 < bottomRow || alignedBottom)
            && (tile.getX() - 1 > leftColumn || alignedLeft)
            && (tile.getX() + 2 < rightColumn || alignedRight);
    }

    /**
     * Strict check for tile visibility (unlike onScreen).
     *
     * @param tile The {@code Tile} to check.
     * @return True if the tile is visible.
     */
    private boolean isTileVisible(Tile tile) {
        repositionMapIfNeeded();
        return tile != null
            && tile.getY() >= topRow     && tile.getY() <= bottomRow
            && tile.getX() >= leftColumn && tile.getX() <= rightColumn;
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
     * @see #getFocus
     */
    int setOffsetFocus(Tile tile) {
        if (tile == null) return 0;
        int where;
        final Map map = getGame().getMap();
        final int tx = tile.getX(), ty = tile.getY(),
            width = rightColumn - leftColumn;
        int moveX = -1;
        getGUI().setFocus(tile);
        positionMap(tile);
        if (leftColumn <= 0) { // At left edge already
            if (tx <= width / 4) {
                where = -1;
            } else if (tx >= 3 * width / 4) {
                where = 1;
            } else {
                moveX = tx + width / 4;
                where = -1;
            }
        } else if (rightColumn >= width - 1) { // At right edge
            if (tx >= rightColumn - width / 4) {
                where = 1;
            } else if (tx <= rightColumn - 3 * width / 4) {
                where = -1;
            } else {
                moveX = tx - width / 4;
                where = 1;
            }
        } else { // Move focus left 1/4 screen
            moveX = tx - width / 4;
            where = 1;
        }
        if (moveX >= 0) {
            Tile other = map.getTile(moveX, ty);
            getGUI().setFocus(other);
            positionMap(other);
        }
        return where;
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
        final Game game = getGame();
        int x = pos.getX(), y = pos.getY();
        int leftColumns = getLeftColumns(), rightColumns = getRightColumns();

        /*
          PART 1
          ======
          Calculate: bottomRow, topRow, bottomRowY, topRowY
          This will tell us which rows need to be drawn on the screen (from
          bottomRow until and including topRow).
          bottomRowY will tell us at which height the bottom row needs to be
          drawn.
        */
        alignedTop = false;
        alignedBottom = false;
        if (y < topRows) {
            alignedTop = true;
            // We are at the top of the map
            bottomRow = (getScreenHeight() / (halfHeight)) - 1;
            if ((getScreenHeight() % (halfHeight)) != 0) {
                bottomRow++;
            }
            topRow = 0;
            bottomRowY = bottomRow * (halfHeight);
            topRowY = 0;
        } else if (y >= (game.getMap().getHeight() - bottomRows)) {
            alignedBottom = true;
            // We are at the bottom of the map
            bottomRow = game.getMap().getHeight() - 1;

            topRow = getScreenHeight() / (halfHeight);
            if ((getScreenHeight() % (halfHeight)) > 0) {
                topRow++;
            }
            topRow = game.getMap().getHeight() - topRow;

            bottomRowY = getScreenHeight() - tileHeight;
            topRowY = bottomRowY - (bottomRow - topRow) * (halfHeight);
        } else {
            // We are not at the top of the map and not at the bottom
            bottomRow = y + bottomRows - 1;
            topRow = y - topRows;
            bottomRowY = topSpace + (halfHeight) * bottomRows;
            topRowY = topSpace - topRows * (halfHeight);
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
        if (x < leftColumns) {
            // We are at the left side of the map
            leftColumn = 0;

            rightColumn = getScreenWidth() / tileWidth - 1;
            if ((getScreenWidth() % tileWidth) > 0) {
                rightColumn++;
            }

            leftColumnX = 0;
            alignedLeft = true;
        } else if (x >= (game.getMap().getWidth() - rightColumns)) {
            // We are at the right side of the map
            rightColumn = game.getMap().getWidth() - 1;

            leftColumn = getScreenWidth() / tileWidth;
            if ((getScreenWidth() % tileWidth) > 0) {
                leftColumn++;
            }

            leftColumnX = getScreenWidth() - tileWidth - halfWidth -
                leftColumn * tileWidth;
            leftColumn = rightColumn - leftColumn;
            alignedRight = true;
        } else {
            // We are not at the left side of the map and not at the right side
            leftColumn = x - leftColumns;
            rightColumn = x + rightColumns;
            leftColumnX = (getScreenWidth() - tileWidth) / 2
                - leftColumns * tileWidth;
        }
    }

    /**
     * Scroll the map in the given direction.
     *
     * @param direction The {@code Direction} to scroll in.
     * @return True if scrolling occurred.
     */
    boolean scrollMap(Direction direction) {
        Tile t = getFocus();
        if (t == null) return false;
        int fx = t.getX(), fy = t.getY();
        if ((t = t.getNeighbourOrNull(direction)) == null) return false;
        int tx = t.getX(), ty = t.getY();
        int x, y;

        // When already close to an edge, resist moving the focus closer,
        // but if moving away immediately jump out of the `nearTo' area.
        if (isMapNearTop(ty) && isMapNearTop(fy)) {
            y = (ty <= fy) ? fy : topRows;
        } else if (isMapNearBottom(ty) && isMapNearBottom(fy)) {
            y = (ty >= fy) ? fy : getGame().getMap().getWidth()
                - bottomRows;
        } else {
            y = ty;
        }
        if (isMapNearLeft(tx, ty) && isMapNearLeft(fx, fy)) {
            x = (tx <= fx) ? fx : getLeftColumns(ty);
        } else if (isMapNearRight(tx, ty) && isMapNearRight(fx, fy)) {
            x = (tx >= fx) ? fx : getGame().getMap().getWidth()
                - getRightColumns(ty);
        } else {
            x = tx;
        }

        if (x == fx && y == fy) return false;
        getGUI().setFocus(getGame().getMap().getTile(x,y));
        return true;
    }

    /**
     * Is a y-coordinate near the bottom?
     *
     * @param y The y-coordinate.
     * @return True if near the bottom.
     */
    private boolean isMapNearBottom(int y) {
        return y >= getGame().getMap().getHeight() - bottomRows;
    }

    /**
     * Is an x,y coordinate near the left?
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if near the left.
     */
    private boolean isMapNearLeft(int x, int y) {
        return x < getLeftColumns(y);
    }

    /**
     * Is an x,y coordinate near the right?
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if near the right.
     */
    private boolean isMapNearRight(int x, int y) {
        return x >= getGame().getMap().getWidth() - getRightColumns(y);
    }

    /**
     * Returns the amount of columns that are to the left of the Tile
     * that is displayed in the center of the Map.
     *
     * @return The amount of columns that are to the left of the Tile
     *     that is displayed in the center of the Map.
     */
    private int getLeftColumns() {
        return getLeftColumns(getFocus().getY());
    }

    /**
     * Returns the amount of columns that are to the left of the Tile
     * with the given y-coordinate.
     *
     * @param y The y-coordinate of the Tile in question.
     * @return The amount of columns that are to the left of the Tile
     *     with the given y-coordinate.
     */
    private int getLeftColumns(int y) {
        int leftColumns = leftSpace / tileWidth + 1;

        if ((y & 1) == 0) {
            if ((leftSpace % tileWidth) > 32) {
                leftColumns++;
            }
        } else {
            if ((leftSpace % tileWidth) == 0) {
                leftColumns--;
            }
        }

        return leftColumns;
    }

    /**
     * Returns the amount of columns that are to the right of the Tile
     * that is displayed in the center of the Map.
     *
     * @return The amount of columns that are to the right of the Tile
     *     that is displayed in the center of the Map.
     */
    private int getRightColumns() {
        return getRightColumns(getFocus().getY());
    }

    /**
     * Returns the amount of columns that are to the right of the Tile
     * with the given y-coordinate.
     *
     * @param y The y-coordinate of the Tile in question.
     * @return The amount of columns that are to the right of the Tile
     *     with the given y-coordinate.
     */
    private int getRightColumns(int y) {
        int rightColumns = rightSpace / tileWidth + 1;

        if ((y & 1) == 0) {
            if ((rightSpace % tileWidth) == 0) {
                rightColumns--;
            }
        } else {
            if ((rightSpace % tileWidth) > 32) {
                rightColumns++;
            }
        }

        return rightColumns;
    }

    /**
     * Is a y-coordinate near the top?
     *
     * @param y The y-coordinate.
     * @return True if near the top.
     */
    private boolean isMapNearTop(int y) {
        return y < topRows;
    }

    /**
     * Gets the unit that should be displayed on the given tile.
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
     * Reset the scale of the map to the default.
     */
    void resetMapScale() {
        this.changeImageLibrary(new ImageLibrary());
        updateMapDisplayVariables();
    }

    boolean isAtMaxMapScale() {
        return getScale() >= MAP_SCALE_MAX;
    }

    boolean isAtMinMapScale() {
        return getScale() <= MAP_SCALE_MIN;
    }

    void increaseMapScale() {
        float newScale = getScale() + MAP_SCALE_STEP;
        if (newScale >= MAP_SCALE_MAX)
            newScale = MAP_SCALE_MAX;
        this.changeImageLibrary(new ImageLibrary(newScale));
        updateMapDisplayVariables();
    }

    void decreaseMapScale() {
        float newScale = getScale() - MAP_SCALE_STEP;
        if (newScale <= MAP_SCALE_MIN)
            newScale = MAP_SCALE_MIN;
        this.changeImageLibrary(new ImageLibrary(newScale));
        updateMapDisplayVariables();
    }

    /**
     * Update the *Space variables.
     */
    private void updateMapDisplayVariables() {
        // Calculate the number of rows that will be drawn above the
        // central tile
        topSpace = (getScreenHeight() - tileHeight) / 2;
        if ((topSpace % (halfHeight)) != 0) {
            topRows = topSpace / (halfHeight) + 2;
        } else {
            topRows = topSpace / (halfHeight) + 1;
        }
        bottomRows = topRows;
        leftSpace = (getScreenWidth() - tileWidth) / 2;
        rightSpace = leftSpace;
    }

    /**
     * Displays the Map.
     *
     * @param g2d The {@code Graphics2D} object on which to draw the Map.
     */
    @SuppressFBWarnings(value="NP_LOAD_OF_KNOWN_NULL_VALUE",
                        justification="lazy load of extra tiles")
    public void displayMap(Graphics2D g2d) {
        //final long now = now();
        final ClientOptions options = getClientOptions();
        final int colonyLabels
            = options.getInteger(ClientOptions.COLONY_LABELS);
        final Game game = getGame();
        final Map map = game.getMap();
        final Player player = getMyPlayer(); // Check, can be null in map editor

        // Remember transform
        AffineTransform originTransform = g2d.getTransform();
        Rectangle clipBounds = g2d.getClipBounds();

        // Position the map if it is not positioned yet
        repositionMapIfNeeded();

        // Determine which tiles need to be redrawn
        int firstRow = (clipBounds.y - topRowY) / (halfHeight) - 1;
        int clipTopY = topRowY + firstRow * (halfHeight);
        firstRow = topRow + firstRow;
        int firstColumn = (clipBounds.x - leftColumnX) / tileWidth - 1;
        int clipLeftX = leftColumnX + firstColumn * tileWidth;
        firstColumn = leftColumn + firstColumn;
        int lastRow = (clipBounds.y + clipBounds.height - topRowY)
            / (halfHeight);
        lastRow = topRow + lastRow;
        int lastColumn = (clipBounds.x + clipBounds.width - leftColumnX)
            / tileWidth;
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
            final int xt = (x-x0) * tileWidth + (((y&1)==1) ? halfWidth : 0);
            final int yt = (y-y0) * halfHeight;
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
            int nextX = halfWidth;
            int nextY = -halfHeight;
            for (int i = 0; i <= ((lastColumn - firstColumn) * 2 + 1); i++) {
                gridPath.lineTo(nextX, nextY);
                nextX += halfWidth;
                nextY = (nextY == 0 ? -halfHeight : 0);
            }

            // Display the grid
            g2d.setStroke(gridStroke);
            g2d.setColor(Color.BLACK);
            for (int row = firstRow; row <= lastRow; row++) {
                g2d.translate(0, halfHeight);
                AffineTransform rowTransform = g2d.getTransform();
                if ((row & 1) == 1) {
                    g2d.translate(halfWidth, 0);
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
                final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
                final int yt = (y-y0) * halfHeight;
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
                final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
                final int yt = (y-y0) * halfHeight;
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
                final int xt = (x-x0) * this.tileWidth + (y&1) * this.halfWidth;
                final int yt = (y-y0) * this.halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;
                g2d.fill(this.fog);
            }
            g2d.translate(-xt0, -yt0);
            g2d.setComposite(oldComposite);
        }

        // Display the Tile overlays
        Set<String> overlayCache = ImageLibrary.createOverlayCache();
        boolean withNumbers = colonyLabels == ClientOptions.COLONY_LABELS_CLASSIC;
        xt0 = yt0 = 0;
        for (Tile t : baseTiles) {
            if (!t.isExplored()) continue;
            final int x = t.getX();
            final int y = t.getY();
            final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
            final int yt = (y-y0) * halfHeight;
            g2d.translate(xt - xt0, yt - yt0);
            xt0 = xt; yt0 = yt;
            
            BufferedImage overlayImage
                = lib.getScaledOverlayImage(t, overlayCache);
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
                final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
                final int yt = (y-y0) * halfHeight;
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
                final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
                final int yt = (y-y0) * halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;

                displayTerritorialBorders(g2d, t, BorderType.COUNTRY, false);
            }
            g2d.translate(-xt0, -yt0);
        }

        // Display cursor for selected tile or active unit
        final Tile cursorTile = getActiveTile();
        if (cursorTile != null && this.cursor.isActive()) {
            final int x = cursorTile.getX();
            final int y = cursorTile.getY();
            if (x >= x0 && y >= y0 && x <= lastColumn && y <= lastRow) {
                final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
                final int yt = (y-y0) * halfHeight;
                g2d.translate(xt, yt);
                g2d.drawImage(lib.getScaledImage(ImageLibrary.UNIT_SELECT),
                              0, 0, null);
                g2d.translate(-xt, -yt);
            }
        }

        // Display units
        g2d.setColor(Color.BLACK);
        if (!game.isInRevengeMode()) {
            xt0 = yt0 = 0;
            for (Tile t : baseTiles) {
                // check for units
                Unit unit = findUnitInFront(t);
                if (unit == null || isOutForAnimation(unit)) continue;
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
                final int yt = (y-y0) * halfHeight;
                g2d.translate(xt - xt0, yt - yt0);
                xt0 = xt; yt0 = yt;

                displayUnit(g2d, unit);
            }
            g2d.translate(-xt0, -yt0);
        } else {
            /* Add extra rows and colums, as the dark halo is huge to enable
               a very slow fade into transparency, see BR#2580 */
            BufferedImage darkness = lib.getScaledImage(ImageLibrary.DARKNESS);
            xt0 = yt0 = 0;
            for (Tile t : map.subMap(x0-2, y0-4, lastColumn-firstColumn+1+4,
                                                 lastRow-firstRow+1+8)) {
                // check for units
                Unit unit = findUnitInFront(t);
                if (unit == null || isOutForAnimation(unit)) continue;
                final int x = t.getX();
                final int y = t.getY();
                final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
                final int yt = (y-y0) * halfHeight;
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
                final int xt = (x-x0) * tileWidth + (y&1) * halfWidth;
                final int yt = (y-y0) * halfHeight;
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
        this.chatDisplay.display(g2d, this.lib, this.size);

        // Timing log
        //final long gap = now() - now;
        //logger.finest("displayMap time = " + gap
        //    + " for " + firstColumn + " " + firstRow
        //    + " to " + lastColumn + " " + lastRow
        //    + " average "
        //    + ((double)gap) / ((lastRow-firstRow) * (lastColumn-firstColumn)));
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
        // int yOffset = lib.getSettlementImage(settlement).getHeight() + 1;
        int yOffset = tileHeight;
        switch (colonyLabels) {
        case ClientOptions.COLONY_LABELS_CLASSIC:
            BufferedImage img = lib.getStringImage(g2d, name, backgroundColor,
                                                   this.fontNormal);
            g2d.drawImage(img, rop, (tileWidth - img.getWidth())/2 + 1, yOffset);
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
                if (buildable != null) {
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
            
            int width = (int)((nameImage.getWidth() * getScale())
                + ((leftImage != null)
                    ? (leftImage.getWidth() * getScale()) + spacing
                    : 0)
                + ((rightImage != null)
                    ? (rightImage.getWidth() * getScale()) + spacing
                    : 0));
            int labelOffset = (tileWidth - width)/2;
            yOffset -= (nameImage.getHeight() * getScale())/2;
            if (leftImage != null) {
                g2d.drawImage(leftImage, rop, labelOffset, yOffset);
                labelOffset += (leftImage.getWidth() * getScale()) + spacing;
            }
            g2d.drawImage(nameImage, rop, labelOffset, yOffset);
            if (rightImage != null) {
                labelOffset += (nameImage.getWidth() * getScale()) + spacing;
                g2d.drawImage(rightImage, rop, labelOffset, yOffset);
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

            BufferedImage turns = (p.getTurns() <= 0) ? null
                : lib.getStringImage(g2d, Integer.toString(p.getTurns()),
                                     Color.WHITE, this.fontTiny);
            g2d.setColor((turns == null) ? Color.GREEN : Color.RED);

            if (debug) { // More detailed display
                if (this.activeUnit != null) {
                    image = ImageLibrary.getPathNextTurnImage(this.activeUnit);
                }
                turns = lib.getStringImage(g2d, Integer.toString(p.getTurns())
                    + "/" + Integer.toString(p.getMovesLeft()),
                    Color.WHITE, this.fontTiny);
            }

            g2d.translate(point.x, point.y);
            if (image == null) {
                g2d.fillOval(halfWidth, halfHeight, 10, 10);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(halfWidth, halfHeight, 10, 10);
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
        BufferedImage image = lib.getScaledUnitImage(unit, fade);
        Point p = calculateUnitImagePositionInTile(image);
        g2d.drawImage(image, p.x, p.y, null);

        // Draw an occupation and nation indicator.
        String text = Messages.message(unit.getOccupationLabel(player, false));
        g2d.drawImage(lib.getOccupationIndicatorChip(g2d, unit, text),
                    (int)(TileViewer.STATE_OFFSET_X * getScale()), 0,
                    null);

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
            int x1 = (int)((TileViewer.STATE_OFFSET_X + OTHER_UNITS_OFFSET_X)
                * getScale());
            int x2 = (int)((TileViewer.STATE_OFFSET_X + OTHER_UNITS_OFFSET_X
                    + OTHER_UNITS_WIDTH) * getScale());
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
        int unitX = (tileWidth - unitImage.getWidth()) / 2;
        int unitY = (tileHeight - unitImage.getHeight()) / 2 -
                    (int) (UNIT_OFFSET * getScale());

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
            g2d.setStroke(borderStroke);
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
            path.moveTo(borderPoints.get(Direction.longSides.get(0)).x,
                        borderPoints.get(Direction.longSides.get(0)).y);
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
                        path.lineTo(borderPoints.get(next).x,
                                    borderPoints.get(next).y);
                        path.quadTo(controlPoints.get(next).x,
                                    controlPoints.get(next).y,
                                    borderPoints.get(next2).x,
                                    borderPoints.get(next2).y);
                    } else {
                        int dx = 0, dy = 0;
                        switch(d) {
                        case NW: dx = halfWidth; dy = -halfHeight; break;
                        case NE: dx = halfWidth; dy = halfHeight; break;
                        case SE: dx = -halfWidth; dy = halfHeight; break;
                        case SW: dx = -halfWidth; dy = -halfHeight; break;
                        default: break;
                        }
                        if (tile1 != null
                            && ((type == BorderType.COUNTRY && owner.owns(tile1))
                                || (type == BorderType.REGION && tile1.getRegion() == region))) {
                            // short straight line
                            path.lineTo(borderPoints.get(next).x,
                                        borderPoints.get(next).y);
                            // big corner
                            Direction previous = d.getPreviousDirection();
                            Direction previous2 = previous.getPreviousDirection();
                            int ddx = 0, ddy = 0;
                            switch(d) {
                            case NW: ddy = -tileHeight; break;
                            case NE: ddx = tileWidth; break;
                            case SE: ddy = tileHeight; break;
                            case SW: ddx = -tileWidth; break;
                            default: break;
                            }
                            path.quadTo(controlPoints.get(previous).x + dx,
                                        controlPoints.get(previous).y + dy,
                                        borderPoints.get(previous2).x + ddx,
                                        borderPoints.get(previous2).y + ddy);
                        } else {
                            // straight line
                            path.lineTo(borderPoints.get(d).x + dx,
                                        borderPoints.get(d).y + dy);
                        }
                    }
                } else {
                    path.moveTo(borderPoints.get(next2).x,
                                borderPoints.get(next2).y);
                }
            }
            g2d.draw(path);
            g2d.setColor(oldColor);
            g2d.setStroke(oldStroke);
        }
    }

}
