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
     * The current focus Tile as determined by the {@link #mapFocusPoint}.
     */
    private Tile focus = null;

    /**
     * The current speed when scrolling. This speed can increase when
     * scrolling.
     */
    private int scrollSpeed = 1;

    
    /*
     * These variables depend on the map scale and also the map pixel size,
     * which changes in updateSizedVariables().
     */
    
    /**
     * The current focus of the visible map given in pixels.
     * 
     * The maximum width of the map in pixels is:
     * <code>map.getWidth() * tileBounds.getWidth()</code>. 
     */
    private Point mapFocusPoint = null;

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
     *
     * @param tileBounds The new scaled tile sizes.
     */
    void updateSizeVariables(TileBounds tileBounds) {
        final TileBounds oldTileBounds = this.tileBounds;
        this.tileBounds = tileBounds;
        
        if (oldTileBounds != null && mapFocusPoint != null) {
            this.mapFocusPoint = new Point(
                (mapFocusPoint.x * tileBounds.getWidth()) / oldTileBounds.getWidth(),
                (mapFocusPoint.y * tileBounds.getHalfHeight()) / oldTileBounds.getHalfHeight()
            );
        }

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
        if (t == null) {
            scrollSpeed = 1;
            return false;
        }

        final int fx = t.getX(), fy = t.getY();
        if ((t = t.getNeighbourOrNull(direction)) == null) {
            scrollSpeed = 1;
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

        int scrollX = mapFocusPoint.x;
        int scrollY = mapFocusPoint.y;
        scrollSpeed += 1;
        if (tx > fx) {
            scrollX += 1 + scrollSpeed;
            if (ty > fy) {
                scrollY += 1 + scrollSpeed;
            } else if (ty < fy) {
                scrollY -= 1 + scrollSpeed;
            }
        } else if (tx < fx) {
            scrollX -= 1 + scrollSpeed;
            if (ty > fy) {
                scrollY += 1 + scrollSpeed;
            } else if (ty < fy) {
                scrollY -= 1 + scrollSpeed;
            }
        } else if (ty > fy) {
            scrollY += 1 + scrollSpeed;
        } else if (ty < fy) {
            scrollY -= 1 + scrollSpeed;
        } else {
            scrollSpeed = 1;
            return false;
        }
        
        setFocusMapPoint(new Point(scrollX, scrollY));
        
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
        return (tile.getX() - 1 > topLeftVisibleTile.getX())
                && (tile.getX() + 2 < bottomRightVisibleTile.getX())
                && (tile.getY() - 2 > topLeftVisibleTile.getY())
                && (tile.getY() + 3 < bottomRightVisibleTile.getY());
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
        
        setFocusMapPoint(new Point(
                tileBounds.getHalfWidth() + focus.getX() * tileBounds.getWidth() + (focus.getY() & 1) * tileBounds.getHalfWidth(),
                tileBounds.getHalfHeight() + focus.getY() * tileBounds.getHalfHeight()
                ));
        
        return true;
    }

    /**
     * The current focus of the visible map given in pixels.
     * 
     * The maximum width of the map in pixels is:
     * <code>map.getWidth() * tileBounds.getWidth()</code>.
     * 
     * @param mapFocusPoint The new focus point.
     * @return {@code true} if the focus was updated.
     */
    public boolean setFocusMapPoint(Point mapFocusPoint) {
        if (this.focus == null) {
            return false;
        }
        
        final Map map = this.focus.getMap();
        final int mapFocusPointX = inRange(tileBounds.getHalfWidth(), mapFocusPoint.x, map.getWidth() * tileBounds.getWidth());
        final int mapFocusPointY = inRange(tileBounds.getHalfHeight(), mapFocusPoint.y, map.getHeight() * tileBounds.getHalfHeight() - tileBounds.getHalfHeight());
        this.mapFocusPoint = new Point(mapFocusPointX, mapFocusPointY);
        
        final int tileX = (mapFocusPoint.x - tileBounds.getHalfWidth()) / tileBounds.getWidth();
        final int tileY = (mapFocusPoint.y - tileBounds.getHalfHeight()) / tileBounds.getHalfHeight();
        
        this.focus = map.getTile(
            inRange(0, tileX, map.getWidth() - 1),
            inRange(0, tileY, map.getHeight() - 1)
        );

        positionMap();
        
        return true;
    }
    
    /**
     * Gets the current focus of the visible map given in pixels.
     * 
     * @return The current focus map point that is between {@code 0}
     *   and <code>map.getWidth() * tileBounds.getWidth()</code>.
     */
    public Point getFocusMapPoint() {
        return mapFocusPoint;
    }
    
    private static int inRange(int min, int value, int max) {
        return Math.min(max, Math.max(min, value));
    }

    /**
     * Position the map so that the focus is displayed at the center.
     */
    void positionMap() {
        if (this.focus == null) {
            return;
        }
        
        final int focusTileX = (mapFocusPoint.x - tileBounds.getHalfWidth()) / tileBounds.getWidth();
        final int focusTileY = (mapFocusPoint.y - tileBounds.getHalfHeight()) / tileBounds.getHalfHeight();

        final int extraSpaceFocusTileX = (mapFocusPoint.x - tileBounds.getHalfWidth()) % tileBounds.getWidth();
        final int extraSpaceFocusTileY = (mapFocusPoint.y - tileBounds.getHalfHeight()) % tileBounds.getHalfHeight();
        
        final int topFullTiles = (size.height / 2 - extraSpaceFocusTileY) / tileBounds.getHalfHeight();
        final int extraSpaceY = (size.height / 2 - extraSpaceFocusTileY) % tileBounds.getHalfHeight();
        final int topTiles = topFullTiles + (extraSpaceY > 0 ? 1 : 0);
        
        final int topLeftTileY = focusTileY - topTiles;
        
        final int leftFullTiles = (size.width / 2 - extraSpaceFocusTileX) / tileBounds.getWidth();
        final int extraSpaceX = (size.width / 2 - extraSpaceFocusTileX) % tileBounds.getWidth();
        final int leftTiles = leftFullTiles + (extraSpaceX > 0 ? 1 : 0);
         
        this.topLeftVisibleTile = new Map.Position(focusTileX - leftTiles, topLeftTileY);
        
        this.topLeftVisibleTilePoint = new Point(
                ((extraSpaceX == 0) ? 0 : -(tileBounds.getWidth() - extraSpaceX)) - tileBounds.getHalfWidth(),
                ((extraSpaceY == 0) ? 0 : -(tileBounds.getHalfHeight() - extraSpaceY)) - tileBounds.getHalfHeight()
                );
        
        final int visibleTilesX = (size.width - topLeftVisibleTilePoint.x) / tileBounds.getWidth()
                + ((((size.width - topLeftVisibleTilePoint.x) % tileBounds.getWidth()) > 0) ? 1 : 0);
        
        final int visibleTilesY = (size.height - topLeftVisibleTilePoint.y) / tileBounds.getHalfHeight()
                + ((((size.height - topLeftVisibleTilePoint.y) % tileBounds.getHalfHeight()) > 0) ? 1 : 0);
        
        this.bottomRightVisibleTile = new Map.Position(topLeftVisibleTile.x + visibleTilesX - 1, topLeftVisibleTile.y + visibleTilesY - 1);
    }

    /**
     * Convert tile coordinates to pixel coordinates.
     *
     * Beware:  Only safe to call if the tile is known non-null and
     * currently visible.
     *
     * @param tile The {@code Tile} to convert coordinates.
     * @return The new {@code Point}.
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
    
    Map.Position getBottomRightVisibleTile() {
        return bottomRightVisibleTile;
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
     * @param map The {@code Map} to convert within.
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
    
    public void resetScrollSpeed() {
        scrollSpeed = 1;
    }
}
