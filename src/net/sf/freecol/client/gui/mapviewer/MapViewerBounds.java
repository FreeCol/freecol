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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;

/**
 * Calculates what part of the {@link Map} is visible on screen. This includes handling
 * the size, scaling and focus of the map.
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
public final class MapViewerBounds {

    /**
     * The size of the displayed {@link MapViewer}.
     */
    private Dimension size = null;

    /**
     * The current focus Tile. This is the tile we try to display
     * as close to the center of the screen as possible.
     */
    private Tile focus = null;

    /*
     * These variables depend on the map scale and also the map pixel size,
     * which changes in updateSizedVariables().
     */

    /**
     * Horizonal and vertical pixel distance to the center tile.
     */
    private int hSpace, vSpace;

    /**
     * Number of rows above/below the center tile.
     */
    private int centerRows;

    /**
     * Almost the number of columns to the left/right of the center tile.
     * There are special cases handled in getLeft/RightColumns().
     */
    private int centerColumns;

    /**
     * Special y-odd/even offsets to centerColumns. FIXME: explain!
     */
    private int columnEvenYOffset, columnOddYOffset;

    /**
     * The topmost and leftmost tile that is displayed in the MapViewer.
     */
    private Map.Position topLeftVisibleTile = new Map.Position(-1, -1);

    /**
     * The position of the top-left corner of {@link topLeftVisibleTile} when
     * rendering.
     */
    private Point topLeftVisibleTilePoint = new Point(0, 0);

    /**
     * The bottommost and rightmost tile that is displayed in the MapViewer.
     */
    private Map.Position bottomRightVisibleTile = new Map.Position(-1, -1);

    // Whether the map is currently aligned with the edge.
    private boolean alignedTop = false, alignedBottom = false,
            alignedLeft = false, alignedRight = false;

    /**
     * Scaled sizes of a tile. This is used when calculating other variables.
     */
    private TileBounds tileBounds;


    MapViewerBounds() {
        this.size = new Dimension(0, 0); // Start empty, update is coming
        this.tileBounds = new TileBounds(new Dimension(0, 0), 1f);
    }

    /**
     * Update the variables that depend on the screen size (and scale).
     */
    void updateSizeVariables(TileBounds tileBounds) {
        this.tileBounds = tileBounds;

        // If we draw a tile in the center of the frame, then there is
        // vSpace pixels above and below it, and hSpace pixels to the
        // left and right.
        this.vSpace = (this.size.height - tileBounds.getHeight()) / 2;
        this.hSpace = (this.size.width - tileBounds.getWidth()) / 2;

        // Calculate the number of map rows to the center tile.
        // Note use of halfHeight, because (x,y) -> (x,y+1) corresponds to:
        //   P(px,py) -> P(px+tw/2,py+th/2) (y even)
        //            -> P(px-tw/2,py+th/2) (y odd)
        // thus there is always a step of th/2.
        // We add 1 to include the center tile
        this.centerRows = (this.vSpace / tileBounds.getHalfHeight()) + 1;
        // If there is extra vertical space then another row can be
        // partially drawn
        int vExtra = this.vSpace % tileBounds.getHeight();
        if (vExtra != 0) this.centerRows++;

        // Calculate the number of map columns to the center tile.
        // This time we divide by the full width of the tile because
        // (x,y) -> (x+1,y) corresponds to:
        //   P(px,py) -> P(px+tw,ty)  ; #4
        // Again add 1 to include the center tile
        this.centerColumns = this.hSpace / tileBounds.getWidth() + 1;

        // If there extra horizontal space?
        int hExtra = this.hSpace % tileBounds.getWidth();
        // Special y-coordinate-dependent offsets to centerColumns.
        // If the row to display borders the left of the window
        // (i.e. y is even by Property#3) then if there is more than half
        // a tile width left over we display another column before
        // the center tile
        this.columnEvenYOffset = (hExtra > tileBounds.getHalfWidth()) ? 1 : 0;
        // If the row to display is inset (y odd) and there is no
        // extra space we display one fewer column before the center tile
        this.columnOddYOffset = (hExtra == 0) ? -1 : 0;

        positionMap();
    }


    /**
     * Sets the focus of the map but offset to the left or right so
     * that the focus position can still be visible when a popup is
     * raised.  If successful, the supplied position will either be at
     * the center of the left or right half of the map.
     * 
     * WARNING: This method changes the focus. You need to call
     *          {@code repaint()}
     *
     * @param tile The {@code Tile} to display.
     * @return Positive if the focus is on the right hand side, negative
     *     if on the left, zero on failure.
     */
    public int setOffsetFocus(Tile tile) {
        if (tile == null) return 0;
        setFocus(tile);
        positionMap();

        int ret = 0, moveX = -1;
        final Map map = tile.getMap();
        final int tx = tile.getX(), ty = tile.getY(),
                width = bottomRightVisibleTile.getX() - topLeftVisibleTile.getX();
        if (topLeftVisibleTile.getX() <= 0) { // At left edge already
            if (tx <= width / 4) {
                ret = -1;
            } else if (tx >= 3 * width / 4) {
                ret = 1;
            } else {
                moveX = tx + width / 4;
                ret = -1;
            }
        } else if (bottomRightVisibleTile.getX() >= width - 1) { // At right edge
            if (tx >= bottomRightVisibleTile.getX() - width / 4) {
                ret = 1;
            } else if (tx <= bottomRightVisibleTile.getX() - 3 * width / 4) {
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
            setFocus(other);
        } else {
            setFocus(tile);
        }
        return ret;
    }

    /**
     * Scroll the map in the given direction.
     * 
     * WARNING: This method changes the focus. You need to call
     *          {@code repaint()}
     *
     * @param direction The {@code Direction} to scroll in.
     * @return True if scrolling occurred.
     */
    public boolean scrollMap(Direction direction) {
        Tile t = getFocus();
        if (t == null) return false;
        final Map map = t.getMap();
        final int fx = t.getX(), fy = t.getY();
        if ((t = t.getNeighbourOrNull(direction)) == null) {
            return false;
        }

        if (Direction.longSides.contains(direction)) {
            /*
             * We need to scroll an additional tile when moving the
             * map diagonally in order to get the correct direction.
             * 
             * Please note that the tiles are diamond shapes, so that
             * diagonally on the screen is longSide for a tile.
             */
            final Tile extraScrollTile = t.getNeighbourOrNull(direction);
            if (extraScrollTile != null) {
                t = extraScrollTile;
            }
        }

        final int tx = t.getX(), ty = t.getY();

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
        setFocus(map.getTile(x, y));
        return true;
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
        return (tile.getX() - 1 > topLeftVisibleTile.getX() || alignedLeft)
                && (tile.getX() + 2 < bottomRightVisibleTile.getX() || alignedRight)
                && (tile.getY() - 2 > topLeftVisibleTile.getY() || alignedTop)
                && (tile.getY() + 3 < bottomRightVisibleTile.getY() || alignedBottom);
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

        final Point p = tileToPoint(tile);
        if (!rhs) {
            return p;
        }
        return new Point(p.x + tileBounds.getWidth(), p.y);
    }

    /**
     * Sets the focus tile.
     * 
     * WARNING: This method changes the focus. You need to call
     *          {@code repaint()}
     *
     * @param focus The new focus {@code Tile}.
     * @return {@code true} if the focus has changed, and
     *      {@code false} otherwise.
     */
    public boolean setFocus(Tile focus) {
        if (focus == null || this.focus == focus) {
            return false;
        }

        this.focus = focus;
        positionMap();
        return true;
    }

    /**
     * Position the map so that the focus is displayed at the center.
     */
    void positionMap() {
        if (this.focus == null) {
            return;
        }
        
        final int x = this.focus.getX(), y = this.focus.getY();
        final Map map = this.focus.getMap();
        final int mapWidth = map.getWidth(), mapHeight = map.getHeight();
        int leftColumns = getLeftColumns(y), rightColumns = getRightColumns(y);

        int leftColumn, rightColumn, topRow, bottomRow, topRowY, leftColumnX;

        /*
          PART 1
          ======
          Calculate: bottomRow, topRow, topRowY
          This will tell us which rows need to be drawn on the screen (from
          bottomRow until and including topRow).
         */
        alignedTop = false;
        alignedBottom = false;
        if ((this.size.height / tileBounds.getHalfHeight()) - 1 >= mapHeight) {
            bottomRow = mapHeight - 1;
            alignedTop = true; // We are at the top of the map
            topRow = 0;
            topRowY = 0;
        } else if (isMapNearTop(y)) {
            alignedTop = true; // We are at the top of the map
            bottomRow = (this.size.height / tileBounds.getHalfHeight()) - 1;
            if ((this.size.height % tileBounds.getHalfHeight()) != 0) {
                bottomRow++;
            }
            topRow = 0;

            topRowY = 0;
        } else if (isMapNearBottom(y, mapHeight)) {
            alignedBottom = true; // We are at the bottom of the map
            bottomRow = mapHeight - 1;

            topRow = this.size.height / tileBounds.getHalfHeight();
            if ((this.size.height % tileBounds.getHalfHeight()) > 0) {
                topRow++;
            }
            topRow = mapHeight - topRow;

            topRowY = (size.height - tileBounds.getHeight())
                    - (bottomRow - topRow) * tileBounds.getHalfHeight();
        } else { // We are not at the top of the map and not at the bottom
            bottomRow = y + this.centerRows - 1;
            topRow = y - this.centerRows;

            topRowY = this.vSpace - this.centerRows * tileBounds.getHalfHeight();
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
        if (this.size.width / (tileBounds.getWidth() - 1) >= mapWidth) {
            alignedLeft = true; // We are at the left side of the map
            leftColumn = 0;
            rightColumn = mapWidth - 1;
            leftColumnX = 0;
        } else if (isMapNearLeft(x, y)) {
            alignedLeft = true; // We are at the left side of the map
            leftColumn = 0;
            rightColumn = this.size.width / tileBounds.getWidth() - 1;
            if ((this.size.width % tileBounds.getWidth()) > 0) {
                rightColumn++;
            }

            leftColumnX = 0;
        } else if (isMapNearRight(x, y, mapWidth)) {
            alignedRight = true; // We are at the right side of the map
            rightColumn = mapWidth - 1;
            leftColumn = this.size.width / tileBounds.getWidth();
            if ((this.size.width % tileBounds.getWidth()) > 0) {
                leftColumn++;
            }
            leftColumnX = this.size.width - tileBounds.getWidth() - tileBounds.getHalfWidth()
                    - leftColumn * tileBounds.getWidth();
            leftColumn = rightColumn - leftColumn;
        } else { // We are not at the left of the map and not at the right
            leftColumn = x - leftColumns;
            rightColumn = x + rightColumns;

            leftColumnX = (this.size.width - tileBounds.getWidth()) / 2
                    - leftColumns * tileBounds.getWidth();
        }

        this.topLeftVisibleTile = new Map.Position(leftColumn, topRow);
        this.bottomRightVisibleTile = new Map.Position(rightColumn, bottomRow);
        this.topLeftVisibleTilePoint = new Point(leftColumnX, topRowY);
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
    Point tileToPoint(Tile tile) {
        final int tileX = tile.getX(), tileY = tile.getY();
        final int x = topLeftVisibleTilePoint.x
                + tileBounds.getWidth() * (tileX - topLeftVisibleTile.getX())
                + (tileY & 1) * tileBounds.getHalfWidth();
        final int y = topLeftVisibleTilePoint.y
                + tileBounds.getHalfHeight() * (tileY - topLeftVisibleTile.getY());

        return new Point(x, y);
    }
    
    /**
     * Calculate the bounds of the rectangle containing a Tile on the
     * screen.
     *
     * If the Tile is not on-screen an empty rectangle is returned.
     * The bounds includes a one-tile padding area above the Tile and
     * half a tile padding to each side. There is currently no padding
     * below the tile. Feel free to add more padding if we start using
     * larger images.
     *
     * @param tile The {@code Tile} on the screen.
     * @return The bounds {@code Rectangle}.
     */
    public Rectangle calculateDrawnTileBounds(Tile tile) {
        if (!isTileVisible(tile)) {
            return new Rectangle(0, 0, 0, 0);
        }
            
        final Point p = tileToPoint(tile);
        return new Rectangle(p.x - tileBounds.getHalfWidth(),
                p.y - tileBounds.getHeight(),
                tileBounds.getWidth() * 2,
                tileBounds.getHeight() * 2);
    }

    Map.Position getTopLeftVisibleTile() {
        return topLeftVisibleTile;
    }

    Point getTopLeftVisibleTilePoint() {
        return topLeftVisibleTilePoint;
    }

    Dimension getSize() {
        return size;
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
     * Converts the given screen coordinates to Map coordinates.
     * It checks to see to which Tile the given pixel 'belongs'.
     *
     * @param x The x-coordinate in pixels.
     * @param y The y-coordinate in pixels.
     * @return The {@code Tile} that is located at the given position
     *     on the screen.
     */
    Tile convertToMapTile(Map map, int x, int y) {
        if (map == null || this.focus == null) return null;

        // Set (leftOffset, topOffset) to the center of the focus tile
        final Point p = tileToPoint(this.focus);
        int leftOffset = p.x + tileBounds.getHalfWidth();
        int topOffset = p.y + tileBounds.getHalfHeight();

        // Next, we can calculate the center pixel of the tile-sized
        // rectangle that was clicked. First, we calculate the
        // difference in units of rows and columns.
        final int fx = this.focus.getX(), fy = this.focus.getY();
        int dcol = (x - leftOffset + (x > leftOffset ? tileBounds.getHalfWidth() : -tileBounds.getHalfWidth()))
                / tileBounds.getWidth();
        int drow = (y - topOffset + (y > topOffset ? tileBounds.getHalfHeight() : -tileBounds.getHalfHeight()))
                / tileBounds.getHeight();
        int px = leftOffset + dcol * tileBounds.getWidth();
        int py = topOffset + drow * tileBounds.getHeight();
        // Since rows are shifted, we need to correct.
        int newCol = fx + dcol;
        int newRow = fy + drow * 2;
        // Now, we check whether the central diamond of the calculated
        // rectangle was clicked, and adjust rows and columns
        // accordingly. See Direction.
        Direction direction = null;
        if (x > px) { // right half of the rectangle
            if (y > py) { // bottom right
                if ((y - py) > tileBounds.getHalfHeight() - (x - px)/2) {
                    direction = Direction.SE;
                }
            } else { // top right
                if ((y - py) < (x - px)/2 - tileBounds.getHalfHeight()) {
                    direction = Direction.NE;
                }
            }
        } else { // left half of the rectangle
            if (y > py) { // bottom left
                if ((y - py) > (x - px)/2 + tileBounds.getHalfHeight()) {
                    direction = Direction.SW;
                }
            } else { // top left
                if ((y - py) < (px - x)/2 - tileBounds.getHalfHeight()) {
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

    void changeSize(Dimension size, TileBounds tileBounds) {
        this.size = size;
        updateSizeVariables(tileBounds);
    }

    /**
     * Strict check for tile visibility (unlike onScreen).
     *
     * @param tile The {@code Tile} to check.
     * @return True if the tile is visible.
     */
    boolean isTileVisible(Tile tile) {
        if (tile == null) return false;
        return tile.getX() >= topLeftVisibleTile.getX() && tile.getX() <= bottomRightVisibleTile.getX()
                && tile.getY() >= topLeftVisibleTile.getY() && tile.getY() <= bottomRightVisibleTile.getY();
    }
    
    TileBounds getTileBounds() {
        return tileBounds;
    }
}
