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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.pathfinding.CostDecider;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.common.model.pathfinding.GoalDecider;
import net.sf.freecol.common.model.pathfinding.GoalDeciders;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * A rectangular isometric map.  The map is represented as a
 * two-dimensional array of tiles.  Off-map destinations, such as
 * {@link Europe}, can be reached via the {@link HighSeas}.
 *
 * In theory, a {@link Game} might contain several Map instances
 * connected by the HighSeas.
 */
public class Map extends FreeColGameObject implements Location {

    private static final Logger logger = Logger.getLogger(Map.class.getName());

    public static final String TAG = "map";

    /**
     * Possible actions by the unit travelling along a path in consideration
     * of the next tile.
     */
    private static enum MoveStep { FAIL, BYLAND, BYWATER, EMBARK, DISEMBARK };

    /**
     * The number of tiles from the upper edge that are considered
     * polar by default.
     */
    public final static int POLAR_HEIGHT = 2;

    /**
     * The layers included in the map. The RIVERS layer includes all
     * natural tile improvements that are not resources.
     */
    public static enum Layer {
        NONE, LAND, TERRAIN, REGIONS, RIVERS, RESOURCES, RUMOURS, NATIVES, ALL;
    };

    /** A position on the Map. */
    public static final class Position {
        
        /** The coordinates of the position. */
        public final int x, y;


        /**
         * Creates a new {@code Position} object with the given
         * coordinates.
         *
         * @param posX The x-coordinate for this position.
         * @param posY The y-coordinate for this position.
         */
        public Position(int posX, int posY) {
            x = posX;
            y = posY;
        }

        /**
         * Creates a new {@code Position} object with the coordinates
         * of a supplied tile.
         *
         * @param tile The {@code Tile} to extract coordinates from.
         */
        public Position(Tile tile) {
            this(tile.getX(), tile.getY());
        }

        /**
         * Creates a new {@code Position} from an existing one with
         * an optional step in a given direction.
         *
         * @param start The starting {@code Position}.
         * @param direction An optional {@code Direction} to step.
         */
        public Position(Position start, Direction direction) {
            Position step = (direction == null) ? start
                : direction.step(start.x, start.y);
            this.x = step.x;
            this.y = step.y;
        }


        /**
         * Gets the x-coordinate of this Position.
         *
         * @return The x-coordinate of this Position.
         */
        public int getX() {
            return x;
        }

        /**
         * Gets the y-coordinate of this Position.
         *
         * @return The y-coordinate of this Position.
         */
        public int getY() {
            return y;
        }

        /**
         * Checks whether a position is valid within a given map size.
         *
         * @param width The width of the map.
         * @param height The height of the map.
         * @return True if the given position is within the bounds of the map.
         */
        public boolean isValid(int width, int height) {
            return Map.inBox(x, y, width, height);
        }

        /**
         * Gets the distance in tiles between two map positions.
         * With an isometric map this is a non-trivial task.
         * The formula below has been developed largely through trial and
         * error.  It should cover all cases, but I wouldn't bet my
         * life on it.
         *
         * @param ax The x-coordinate of the first position.
         * @param ay The y-coordinate of the first position.
         * @param bx The x-coordinate of the second position.
         * @param by The y-coordinate of the second position.
         * @return The distance in tiles between the positions.
         */
        public static int getXYDistance(int ax, int ay, int bx, int by) {
            int r = (bx - ax) - (ay - by) / 2;

            if (by > ay && ay % 2 == 0 && by % 2 != 0) {
                r++;
            } else if (by < ay && ay % 2 != 0 && by % 2 == 0) {
                r--;
            }
            return Math.max(Math.abs(ay - by + r), Math.abs(r));
        }

        /**
         * Gets the distance in tiles between two map positions.
         * With an isometric map this is a non-trivial task.
         * The formula below has been developed largely through trial and
         * error.  It should cover all cases, but I wouldn't bet my
         * life on it.
         *
         * @param position The other {@code Position} to compare.
         * @return The distance in tiles to the other position.
         */
        public int getDistance(Position position) {
            return getXYDistance(getX(), getY(),
                               position.getX(), position.getY());
        }

        /**
         * Get the direction from this position to an adjacent position.
         *
         * @param other The adjacent {@code Position}.
         * @return The {@code Direction}, or null if not adjacent.
         */
        public Direction getDirection(Position other) {
            return find(Direction.values(),
                        matchKeyEquals(other, d -> new Position(this, d)));
        }

        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Position) {
                Position other = (Position)o;
                return x == other.x && y == other.y;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return x | (y << 16);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }


    /** The width and height of the map. */
    private int width = -1, height = -1;

    /**
     * The tiles that this map contains, as a 2D array.  This starts
     * out unassigned, but is initialized in setTiles().  Then the
     * individual tiles are filled in with setTile(), however once a
     * Tile is present further calls to setTile will copyIn to it.
     */
    private Tile[][] tileArray;

    /**
     * The tiles that this map contains, as a list.
     * This is populated in setTile().
     */
    private List<Tile> tileList = new ArrayList<>();

    /** The highest map layer included. */
    private Layer layer;

    /**
     * The latitude of the northern edge of the map. A negative value
     * indicates northern latitude, a positive value southern
     * latitude. Thus, -30 equals 30°N, and 40 equals 40°S.
     */
    private int minimumLatitude = -90;

    /**
     * The latitude of the southern edge of the map. A negative value
     * indicates northern latitude, a positive value southern
     * latitude. Thus, -30 equals 30°N, and 40 equals 40°S.
     */
    private int maximumLatitude = 90;

    /** Variable used to convert rows to latitude. */
    private float latitudePerRow;

    /** The regions on the map. */
    private final List<Region> regions = new ArrayList<>();

    // Do not serialize below

    /** The search tracing status. */
    private boolean traceSearch = false;


    /**
     * Create a new {@code Map} from a collection of tiles.
     *
     * @param game The enclosing {@code Game}.
     * @param width The map width.
     * @param height The map height.
     */
    public Map(Game game, int width, int height) {
        super(game);

        setTiles(width, height);
        setLayer(Layer.RESOURCES);
        calculateLatitudePerRow();
        initializeTraceSearch();
    }

    /**
     * Create a new {@code Map} from an input stream.
     *
     * @param game The enclosing {@code Game}.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public Map(Game game, FreeColXMLReader xr) throws XMLStreamException {
        this(game, xr.getAttribute(WIDTH_TAG, -1),
                   xr.getAttribute(HEIGHT_TAG, -1));

        readFromXML(xr);
    }

    /**
     * Creates a new {@code Map} with the given object identifier.
     *
     * The object should be initialized later.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public Map(Game game, String id) {
        super(game, id);

        initializeTraceSearch();
    }


    /**
     * Enable full path logging by default if in PATHS debug mode.
     */
    private void initializeTraceSearch() {
        this.traceSearch = FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.PATHS);
    }
        
    /**
     * Gets the width of this map.
     *
     * @return The width of this map.
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Gets the height of this map.
     *
     * @return The height of this map.
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Checks if an (x,y) coordinate tuple is within a map of
     * specified width and height.
     *
     * @param x The x-coordinate of the position.
     * @param y The y-coordinate of the position.
     * @param width The width of the map.
     * @param height The height of the map.
     * @return True if the given position is within the bounds of the map.
     */
    public static boolean inBox(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Checks whether a position is valid (within the map limits).
     *
     * @param x The X coordinate to check.
     * @param y The Y coordinate to check.
     * @return True if the coordinates are valid.
     */
    public boolean isValid(int x, int y) {
        return Map.inBox(x, y, getWidth(), getHeight());
    }

    /**
     * Checks whether a position is valid (within the map limits).
     *
     * @param position The {@code Position} to check.
     * @return True if the position is valid.
     */
    public boolean isValid(Position position) {
        return isValid(position.getX(), position.getY());
    }

    /**
     * Initialize the tile shape.  This must be called *once* per map.
     *
     * @param width The new map width.
     * @param height The new map height.
     * @return An error message if setting the tile array was invalid, null if valid.
     */
    private String setTiles(int width, int height) {
        if (width <= 0 || height <= 0) {
            return "Bad map tile array size: (" + width + "," + height + ")";
        }
        this.width = width;
        this.height = height;
        this.tileArray = new Tile[width][height];
        this.tileList.clear();
        return null;
    }

    /**
     * Update the tile shape, however it is an error to change the map
     * size after the initial setting.
     *
     * @param width The new map width.
     * @param height The new map height.
     * @return An error message if setting the tile array was invalid, null if valid.
     */
    private String updateTiles(int width, int height) {
        // Initial setting
        if (this.width < 0 && this.height < 0) return setTiles(width, height);
        // Next time, the size must *not* change.
        if (this.width != width || this.height != height) {
            return "Attempted map resize (" + this.width + "," + this.height
                + " -> " + width + "," + height + ")";
        }
        return null;
    }

    /**
     * Gets the Tile at position (x, y).  'x' specifies a column and
     * 'y' specifies a row.  (0, 0) is the Tile at the top-left corner
     * of the Map.
     *
     * @param x The x-coordinate of the {@code Tile}.
     * @param y The y-coordinate of the {@code Tile}.
     * @return The {@code Tile} at (x, y), or null if the
     *     position is invalid.
     */
    public Tile getTile(int x, int y) {
        if (!isValid(x, y)) return null;
        return this.tileArray[x][y];
    }

    /**
     * Gets the Tile at a requested position.
     *
     * @param p The {@code Position} to query.
     * @return The {@code Tile} at the given position.
     */
    public Tile getTile(Position p) {
        return getTile(p.getX(), p.getY());
    }

    /**
     * Set the tile at the given coordinates.
     *
     * This can be done *once* per tile.  Currently that is enforced
     * by calling setTile() only from populateTiles() where the map is
     * being built or indirectly through updateTile() where the
     * absence of the tile is checked.
     *
     * @param x The x-coordinate of the {@code Tile}.
     * @param y The y-coordinate of the {@code Tile}.
     * @param tile The {@code Tile} to set.
     * @return True if the {@code Tile} was updated.
     */
    private boolean setTile(Tile tile, int x, int y) {
        if (tile == null) return false;
        this.tileArray[x][y] = tile;
        this.tileList.add(tile);
        return true;
    }

    /**
     * Update a tile in this map from the given tile.
     *
     * @param tile The {@code Tile} to update from.
     * @return True if the tile was updated.
     */
    private boolean updateTile(Tile tile) {
        if (tile == null) return false;
        final int x = tile.getX(), y = tile.getY();
        if (!isValid(x, y)) return false;
        Tile old = this.tileArray[x][y];
        if (old == null) return setTile(tile, x, y);
        old.copyIn(tile);
        return true;
    }
        
    /**
     * Get the layer.
     *
     * @return The maximum implemented layer on this map.
     */
    public final Layer getLayer() {
        return this.layer;
    }

    /**
     * Set the layer.
     *
     * @param newLayer The new maximum implemented layer for this map.
     */
    public final void setLayer(final Layer newLayer) {
        this.layer = newLayer;
    }

    /**
     * Get the minimum latitude.
     *
     * @return The minimum latitude of this map.
     */
    public final int getMinimumLatitude() {
        return this.minimumLatitude;
    }

    /**
     * Set the minimum latitude.
     *
     * @param newMinimumLatitude The new minimum latitude for this map.
     */
    public final void setMinimumLatitude(final int newMinimumLatitude) {
        this.minimumLatitude = newMinimumLatitude;
        calculateLatitudePerRow();
    }

    /**
     * Get the maximum latitude.
     *
     * @return The maximum latitude of this map.
     */
    public final int getMaximumLatitude() {
        return this.maximumLatitude;
    }

    /**
     * Set the maxmimum latitude.
     *
     * @param newMaximumLatitude The new maximum latitude for this map.
     */
    public final void setMaximumLatitude(final int newMaximumLatitude) {
        this.maximumLatitude = newMaximumLatitude;
        calculateLatitudePerRow();
    }

    /**
     * Get the latitude/row.
     *
     * @return The latitude change between rows.
     */
    public final float getLatitudePerRow() {
        return this.latitudePerRow;
    }

    /**
     * Recalculate the latitude/row from the current maximum and minimum.
     */
    private final void calculateLatitudePerRow() {
        this.latitudePerRow = 1f * (this.maximumLatitude - this.minimumLatitude) /
            (getHeight() - 1);
    }

    /**
     * Gets the latitude of the given map row.
     *
     * @param row The row to check.
     * @return The row latitude.
     */
    public int getLatitude(int row) {
        return this.minimumLatitude + (int) (row * this.latitudePerRow);
    }

    /**
     * Gets the map row with the given latitude.
     *
     * @param latitude The latitude to find.
     * @return The row closest to the supplied latitude.
     */
    public int getRow(int latitude) {
        return (int) ((latitude - this.minimumLatitude) / this.latitudePerRow);
    }

    /**
     * Gets the regions in this map.
     *
     * @return All the regions in this map.
     */
    public Collection<Region> getRegions() {
        synchronized (this.regions) {
            return new ArrayList<>(this.regions);
        }
    }

    /**
     * Adds a region to this map.
     *
     * @param region The {@code Region} to add.
     */
    public void addRegion(final Region region) {
        synchronized (this.regions) {
            this.regions.add(region);
        }
    }

    /**
     * Clear the regions list.
     */
    public void clearRegions() {
        synchronized (this.regions) {
            this.regions.clear();
        }
    }

    /**
     * Get the fixed regions indexed by key.
     *
     * @return A map of the fixed regions.
     */
    public java.util.Map<String, Region> getFixedRegions() {
        HashMap<String, Region> result = new HashMap<>();
        for (Region r : getRegions()) {
            String n = r.getKey();
            if (n != null) result.put(n, r);
        }
        return result;
    }

    /**
     * Gets a {@code Region} by name key.
     *
     * @param key The name key to lookup the region with.
     * @return The {@code Region} with the given name key, or
     *     null if none found.
     */
    public Region getRegionByKey(final String key) {
        return (key == null) ? null
            : find(getRegions(), matchKeyEquals(key, Region::getKey));
    }

    /**
     * Gets a {@code Region} by name.
     *
     * @param name The region name.
     * @return The {@code Region} with the given name, or null if
     *     not found.
     */
    public Region getRegionByName(final String name) {
        return (name == null) ? null
            : find(getRegions(), matchKeyEquals(name, Region::getName));
    }


    // Useful location and direction utilities
    
    /**
     * Are two locations non-null and either the same or at the same tile.
     * This routine is here because Location is an interface.
     *
     * @param l1 The first {@code Location}.
     * @param l2 The second {@code Location}.
     * @return True if the locations are the same or at the same tile.
     */
    public static final boolean isSameLocation(Location l1, Location l2) {
        return (l1 == null || l2 == null) ? false
            : (l1 == l2) ? true
            : (l1.getTile() == null) ? false
            : l1.getTile() == l2.getTile();
    }

    /**
     * Are two locations at least in the same contiguous land/sea-mass?
     * This routine is here because Location is an interface.
     *
     * @param l1 The first {@code Location}.
     * @param l2 The second {@code Location}.
     * @return True if the locations are the same or in the same land/sea-mass.
     */
    public static final boolean isSameContiguity(Location l1, Location l2) {
        return (l1 == null || l2 == null) ? false
            : (l1 == l2) ? true
            : (l1.getTile() == null || l2.getTile() == null) ? false
            : l1.getTile().isConnectedTo(l2.getTile());
    }            

    /**
     * Is a tile in the map in a polar region?
     *
     * @param tile The {@code Tile} to examine.
     * @return True if the tile is in a polar region.
     */
    public boolean isPolar(Tile tile) {
        return tile.getY() <= POLAR_HEIGHT
            || tile.getY() >= getHeight() - POLAR_HEIGHT - 1;
    }

    /**
     * Gets the direction a unit needs to move in
     * order to get from {@code t1} to {@code t2}
     *
     * @param t1 The tile to move from.
     * @param t2 The target tile if moving from {@code t1}
     *      in the direction returned by this method.
     * @return The direction you need to move from {@code t1}
     *      in order to reach {@code t2}, or null if the two
     *      specified tiles are not neighbours.
     */
    public Direction getDirection(Tile t1, Tile t2) {
        return (t1 == null || t2 == null) ? null
            : new Position(t1).getDirection(new Position(t2));
    }

    /**
     * Get the approximate direction from one tile to another.
     *
     * @param src The source {@code Tile}.
     * @param dst The destination {@code Tile}.
     * @return The approximate direction from source to direction, or null
     *     if source and destination are the same.
     */
    public static Direction getRoughDirection(Tile src, Tile dst) {
        int x = dst.getX() - src.getX();
        int y = dst.getY() - src.getY();
        if (x == 0 && y == 0) return null;
        double theta = Math.atan2(y, x) + Math.PI/2 + Math.PI/8;
        if (theta < 0) theta += 2 * Math.PI;
        return Direction.angleToDirection(theta);
    }

    /**
     * Gets the adjacent tile in a given direction from the given coordinates.
     *
     * @param x The x coordinate to work from.
     * @param y The y coordinate to work from.
     * @param direction The {@code Direction} to check.
     * @return The adjacent {@code Tile} in the specified
     *     direction, or null if invalid.
     */
    public Tile getAdjacentTile(int x, int y, Direction direction) {
        return getTile(direction.step(x, y));
    }

    /**
     * Gets the adjacent tile in a given direction from a given tile.
     *
     * @param tile The starting {@code Tile}.
     * @param direction The {@code Direction} to check.
     * @return The adjacent {@code Tile} in the specified
     *     direction, or null if invalid.
     */
    public Tile getAdjacentTile(Tile tile, Direction direction) {
        return getAdjacentTile(tile.getX(), tile.getY(), direction);
    }

    /**
     * Gets the distance between two tiles.
     *
     * @param t1 The first {@code Tile}.
     * @param t2 The second {@code Tile}.
     * @return The distance between the tiles.
     */
    public int getDistance(Tile t1, Tile t2) {
        return Position.getXYDistance(t1.getX(), t1.getY(),
                                      t2.getX(), t2.getY());
    }

    /**
     * Get the closest tile to a given one from a list of other tiles.
     *
     * @param tile The {@code Tile} to start from.
     * @param tiles The list of {@code Tile}s to check.
     * @return The closest tile found (may be null if the list is empty).
     */
    public Tile getClosestTile(Tile tile, Collection<Tile> tiles) {
        return minimize(tiles, cachingIntComparator(t -> getDistance(t, tile)));
    }

    /**
     * Select a random land tile on the map.
     *
     * @param random A {@code Random} number source.
     * @return A random land tile, or null if none found.
     */
    public Tile getRandomLandTile(Random random) {
        final int SLOSH = 10;
        int x = 0, y = 0, width = getWidth(), height = getHeight();
        if (width >= SLOSH) {
            width -= SLOSH;
            x += SLOSH/2;
        }
        if (height >= SLOSH) {
            height -= SLOSH;
            y += SLOSH/2;
        }
        x += randomInt(logger, "W", random, width);
        y += randomInt(logger, "H", random, height);
        for (Tile t : getCircleTiles(getTile(x, y), true, INFINITY)) {
            if (t.isLand()) return t;
        }
        return null;
    }


    // Support for various kinds of map iteration.

    /**
     * Get a list of all the tiles that match a predicate.
     *
     * @param predicate The <code>Predicate</code> to check.
     * @return A {@code List} of all matching {@code Tile}s.
     */
    public Set<Tile> getTileSet(Predicate<Tile> predicate) {
        Set<Tile> ret = new HashSet<>();
        for (Tile t : this.tileList) {
            if (predicate.test(t)) ret.add(t);
        }
        return ret;
    }

    /**
     * Get a list of all the tiles that match a predicate.
     *
     * @param predicate The <code>Predicate</code> to check.
     * @return A {@code List} of all matching {@code Tile}s.
     */
    public List<Tile> getTileList(Predicate<Tile> predicate) {
        List<Tile> ret = new ArrayList<>();
        for (Tile t : this.tileList) {
            if (predicate.test(t)) ret.add(t);
        }
        return ret;
    }

    /**
     * Perform an action on each tile.
     *
     * @param consumer The {@code Consumer} action to perform.
     */
    public void forEachTile(Consumer<Tile> consumer) {
        for (Tile t : this.tileList) consumer.accept(t);
    }

    /**
     * Perform an action on each tile that matches a predicate.
     *
     * @param predicate The {@code Predicate} to match.
     * @param consumer The {@code Consumer} action to perform.
     */
    public void forEachTile(Predicate<Tile> predicate,
                            Consumer<Tile> consumer) {
        for (Tile t : this.tileList) {
            if (predicate.test(t)) consumer.accept(t);
        }
    }

    /**
     * Populate the map.
     *
     * To be called to build a new map.
     *
     * @param func A <code>Function</code> that makes a new
     *    <code>Tile</code> for the supplied x,y coordinates.
     * @return True if the map was populated.
     */
    public boolean populateTiles(BiFunction<Integer, Integer, Tile> func) {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (!this.setTile(func.apply(x, y), x, y)) return false;
            }
        }
        return true;
    }

    /**
     * Iterates through a rectangular subpart of the Map.
     * Intentionally avoids calling methods doing redundant checks,
     * which would slow down map display.
     * 
     * @param x X-component of the position of first tile.
     * @param y Y-component of the position of first tile.
     * @param w Width of the rectangle.
     * @param h Height of the rectangle.
     * @param consumer Provides a function to call for each tile.
     */
    public void forSubMap(int x, int y, int w, int h,
                          Consumer<Tile> consumer) {
        for (Tile t : subMap(x, y, w, h)) consumer.accept(t);
    }

    /**
     * Collect the tiles in a rectangular portion of the map.
     * 
     * @param x X-component of the position of first tile.
     * @param y Y-component of the position of first tile.
     * @param w Width of the rectangle.
     * @param h Height of the rectangle.
     * @return A list of {@code Tile}s found (empty on error).
     */
    public List<Tile> subMap(int x, int y, int w, int h) {
        if (x < 0) {
            w += x;
            x = 0;
        }
        if (y < 0) {
            h += y;
            y = 0;
        }
        final int width = getWidth();
        final int height = getHeight();
        if (w <= 0 || h <= 0 || x > width || y > height)
            return Collections.<Tile>emptyList();
        if (x+w > width) w = width - x;
        if (y+h > height) h = height - y;
        List<Tile> ret = new ArrayList<>();
        for (int yi = y; yi < y+h; yi++) {
            for (int xi = x; xi < x+w; xi++) {
                ret.add(getTile(xi, yi));
            }
        }
        return ret;
    }

    /**
     * Get a shuffled list of all the tiles.
     *
     * @param random A pseudo-random number source.
     * @return A shuffled list of all the {@code Tile}s in this map.
     */
    public List<Tile> getShuffledTiles(Random random) {
        List<Tile> ret = new ArrayList<>(this.tileList);
        randomShuffle(logger, "All tile shuffle", ret, random);
        return ret;
    }
        
    /**
     * An iterator returning positions in a spiral starting at a given
     * center tile.  The center tile is never included in the returned
     * tiles, and all returned tiles are valid.
     */
    private final class CircleIterator implements Iterator<Tile> {

        /** The maximum radius. */
        private final int radius;
        /** The current radius of the iteration. */
        private int currentRadius;
        /** The current index in the circle with the current radius: */
        private int n;
        /** The current position in the circle. */
        private int x, y;


        /**
         * Create a new Circle Iterator.
         *
         * @param center The center {@code Tile} of the circle.
         * @param isFilled True to get all of the positions within the circle.
         * @param radius The radius of the circle.
         */
        public CircleIterator(Tile center, boolean isFilled, int radius) {
            if (center == null) {
                throw new RuntimeException("center must not be null: " + this);
            }
            this.radius = radius;
            n = 0;

            Position step;
            if (isFilled || radius == 1) {
                step = Direction.NE.step(center.getX(), center.getY());
                x = step.x;
                y = step.y;
                currentRadius = 1;
            } else {
                this.currentRadius = radius;
                x = center.getX();
                y = center.getY();
                for (int i = 1; i < radius; i++) {
                    step = Direction.N.step(x, y);
                    x = step.x;
                    y = step.y;
                }
                step = Direction.NE.step(x, y);
                x = step.x;
                y = step.y;
            }
            if (!isValid(x, y)) nextTile();
        }

        /**
         * Gets the current radius of the circle.
         *
         * @return The distance from the center tile this
         *     {@code CircleIterator} was initialized with.
         */
        public int getCurrentRadius() {
            return currentRadius;
        }

        /**
         * Finds the next position.
         */
        private void nextTile() {
            boolean started = n != 0;
            do {
                n++;
                final int width = currentRadius * 2;
                if (n >= width * 4) {
                    currentRadius++;
                    if (currentRadius > radius) {
                        x = y = UNDEFINED;
                        break;
                    } else if (!started) {
                        x = y = UNDEFINED;
                        break;
                    } else {
                        n = 0;
                        started = false;
                        Position step = Direction.NE.step(x, y);
                        x = step.x;
                        y = step.y;
                    }
                } else {
                    int i = n / width;
                    Direction direction;
                    switch (i) {
                    case 0:
                        direction = Direction.SE;
                        break;
                    case 1:
                        direction = Direction.SW;
                        break;
                    case 2:
                        direction = Direction.NW;
                        break;
                    case 3:
                        direction = Direction.NE;
                        break;
                    default:
                        throw new IllegalStateException("i=" + i + ", n=" + n
                            + ", width=" + width);
                    }
                    Position step = direction.step(x, y);
                    x = step.x;
                    y = step.y;
                }
            } while (!isValid(x, y));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return x != UNDEFINED && y != UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Tile next() {
            if (!hasNext()) {
                throw new NoSuchElementException("CircleIterator exhausted: " + n);
            }
            Tile result = getTile(x, y);
            nextTile();
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Gets a circle iterator.
     *
     * @param center The center {@code Tile} to iterate around.
     * @param isFilled True to get all of the positions in the circle.
     * @param radius The radius of circle.
     * @return The circle iterator.
     */
    public Iterator<Tile> getCircleIterator(Tile center, boolean isFilled,
        int radius) {
        return new CircleIterator(center, isFilled, radius);
    }

    /**
     * Gets an iterable for all the tiles in a circle using an
     * underlying CircleIterator.
     *
     * @param center The center {@code Tile} to iterate around.
     * @param isFilled True to get all of the positions in the circle.
     * @param radius The radius of circle.
     * @return An {@code Iterable} for a circle of tiles.
     */
    public Iterable<Tile> getCircleTiles(final Tile center,
        final boolean isFilled,
        final int radius) {
        return new Iterable<Tile>() {
            @Override
            public Iterator<Tile> iterator() {
                return getCircleIterator(center, isFilled, radius);
            }
        };
    }
        

    // Path-finding/searching infrastructure and routines

    /**
     * Simple interface to supply a heuristic to the A* routine.
     */
    private interface SearchHeuristic {
        int getValue(Tile tile);
    }

    /** A trivial search heuristic that always returns zero. */
    private SearchHeuristic trivialSearchHeuristic = (Tile t) -> 0;

    /**
     * Gets a search heuristic using the Manhatten distance to an end tile.
     *
     * @param endTile The {@code Tile} to aim for.
     * @return A new {@code SearchHeuristic} aiming for the end tile.
     */
    private SearchHeuristic getManhattenHeuristic(final Tile endTile) {
        return (Tile tile) -> tile.getDistanceTo(endTile);
    }

    /**
     * Destination argument test for path searches.  Find the actual
     * destination of a path.
     *
     * @param unit An optional {@code Unit} to search for.
     * @param end  The candidate end {@code Location}.
     * @return The actual end location.
     */
    private Location findRealEnd(Unit unit, Location end) {
        while (true) {
            if (end == null) {
                throw new RuntimeException("Null end for: " + unit);
            } else if (end instanceof Europe) {
                return end;
            } else if (end instanceof Map) {
                end = unit.getFullEntryLocation();
            } else if (end.getTile() != null) {
                return end.getTile();
            } else if (unit != null) {
                return unit.resolveDestination();
            } else {
                throw new RuntimeException("Invalid end: " + end);
            }
        }
    }

    /**
     * Gets the best (closest) path location for this unit to reach a
     * given tile from off the map.
     *
     * @param unit The {@code Unit} to check.
     * @param tile The target {@code Tile}.
     * @param carrier An optional carrier {@code Unit}to use.
     * @param costDecider An optional {@code CostDecider} to use.
     * @return A path to the best entry location tile to arrive on the
     *     map at, or null if none found.
     */
    private PathNode getBestEntryPath(Unit unit, Tile tile, Unit carrier,
                                      CostDecider costDecider) {
        if (costDecider == null)
            costDecider = CostDeciders.avoidSettlementsAndBlockingUnits();
        return searchMap(unit, tile, GoalDeciders.getHighSeasGoalDecider(),
                         costDecider, INFINITY, carrier, null, null);
    }

    /**
     * Gets the best (closest) entry location for this unit to reach a
     * given tile from off the map.
     *
     * @param unit The {@code Unit} to check.
     * @param tile The target {@code Tile}.
     * @param carrier An optional carrier {@code Unit}to use.
     * @param costDecider An optional {@code CostDecider} to use.
     * @return The best entry location tile to arrive on the map at, or null
     *     if none found.
     */
    public Tile getBestEntryTile(Unit unit, Tile tile, Unit carrier,
                                 CostDecider costDecider) {
        PathNode path = getBestEntryPath(unit, tile, carrier, costDecider);
        return (path == null) ? null : path.getLastNode().getTile();
    }

    /**
     * Find the quickest path for a unit (with optional carrier) from
     * a start tile to an end tile.
     *
     * @param unit The {@code Unit} to find the path for.
     * @param start The {@code Tile} in which the path starts from.
     * @param end The {@code Tile} at the end of the path.
     * @param carrier An optional naval carrier {@code Unit} to use.
     * @param costDecider An optional {@code CostDecider} for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @param lb An optional {@code LogBuilder} to log to.
     * @return A path starting at the start tile and ending at the end
     *     tile, or null if none found.
     */
    private PathNode findMapPath(Unit unit, Tile start, Tile end, Unit carrier,
                                 CostDecider costDecider, LogBuilder lb) {
        final Unit offMapUnit = (carrier != null) ? carrier
            : (unit != null && unit.isNaval()) ? unit
            : null;
        final GoalDecider gd = GoalDeciders.getLocationGoalDecider(end);
        final SearchHeuristic sh = getManhattenHeuristic(end);
        Unit embarkTo, endUnit;

        PathNode path;
        if (start.getContiguity() == end.getContiguity()) {
            // If the unit potentially could get to the destination
            // without a carrier, compare both with-carrier and
            // without-carrier paths.  The latter will usually be
            // faster, but not always, e.g. mounted units on a good
            // road system.
            path = searchMap(unit, start, gd, costDecider,
                             INFINITY, null, sh, lb);
            PathNode carrierPath = (carrier == null) ? null
                : searchMap(unit, start, gd, costDecider,
                            INFINITY, carrier, sh, lb);
            if (carrierPath != null
                && (path == null
                    || (path.getLastNode().getCost()
                        > carrierPath.getLastNode().getCost()))) {
                path = carrierPath;
            }

        } else if (offMapUnit != null) {
            // If there is an off-map unit then complex paths which
            // use settlements and inland lakes are possible, but hard
            // to capture with the contiguity test, so just allow the
            // search to proceed.
            path = searchMap(unit, start, gd, costDecider,
                             INFINITY, carrier, sh, lb);

        } else if (unit != null && unit.isOnCarrier()
            && !start.isLand() && end.isLand()
            && !start.getContiguityAdjacent(end.getContiguity()).isEmpty()) {
            // Special case where a land unit is trying to move off a
            // ship to adjacent land.
            path = searchMap(unit, start, gd, costDecider, INFINITY,
                             carrier, sh, lb);

        } else if (start.isLand()
            && !end.isLand() && (endUnit = end.getFirstUnit()) != null
            && unit != null && unit.getOwner().owns(endUnit)
            && !end.getContiguityAdjacent(start.getContiguity()).isEmpty()
            && (embarkTo = end.getCarrierForUnit(unit)) != null) {
            // Special case where a land unit is trying to move from
            // land to an adjacent ship.
            path = searchMap(unit, start,
                             GoalDeciders.getAdjacentLocationGoalDecider(end),
                             costDecider, INFINITY, null, null, lb);
            if (path != null) {
                PathNode last = path.getLastNode();
                last.next = new PathNode(embarkTo, 0, last.getTurns()+1, true,
                    last, null);
            }
        } else { // Otherwise, there is a connectivity failure.
            path = null;
        }
        return path;
    }

    /**
     * Finish processing a path.
     *
     * @param path The {@code PathNode} to finish.
     * @param unit The {@code Unit} that is travelling along the path.
     * @param lb An optional {@code LogBuilder} to log to.
     */
    private void finishPath(PathNode path, Unit unit, LogBuilder lb) {
        if (path != null) {
            // Add the turns remaining on the high seas.
            final int initialTurns = (!unit.isAtSea()) ? 0
                : ((unit.isOnCarrier()) ? unit.getCarrier() : unit)
                .getWorkLeft();
            if (initialTurns != 0) path.addTurns(initialTurns);

            if (lb != null) {
                lb.add("\nSuccess\n", path.fullPathToString());
            }
        }
        if (lb != null) lb.log(logger, Level.INFO);
    }
        
    /**
     * Find the quickest path for a unit (with optional carrier) from
     * a start location to an end location.
     *
     * @param unit The {@code Unit} to find the path for.
     * @param start The {@code Location} in which the path starts from.
     * @param end The {@code Location} at the end of the path.
     * @param carrier An optional naval carrier {@code Unit} to use.
     * @param costDecider An optional {@code CostDecider} for
     *     determining the movement costs (uses default cost deciders
     *     for the unit/s if not provided).
     * @param lb An optional {@code LogBuilder} to log to.
     * @return A path starting at the start location and ending at the
     *     end location, or null if none found.
     */
    public PathNode findPath(final Unit unit,
                             final Location start, final Location end,
                             final Unit carrier, CostDecider costDecider,
                             LogBuilder lb) {
        if (traceSearch) lb = new LogBuilder(1024);

        // Validate the arguments, reducing to either Europe or a Tile.
        final Location realEnd;
        try {
            realEnd = findRealEnd(unit, end);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Path fail: " + unit
                + " from " + start + " to " + end + " with " + carrier, iae);
        }
        // Get the unit that will be used for off-map travel.
        final Unit offMapUnit = (carrier != null) ? carrier : unit;

        PathNode p, path;
        Tile tile;
        if (realEnd instanceof Tile && !((Tile)realEnd).isExplored()) {
            // Do not allow finding a path into unexplored territory,
            // as we do not have the terrain type and thus can not
            // calculate costs, but relent if the unexplored tile borders
            // an explored one on the same contiguity.
            Tile closest = (start instanceof Tile)
                ? getClosestTile((Tile)start,
                    transform(((Tile)realEnd).getSurroundingTiles(1, 1),
                        t -> t.isExplored()
                            && isSameContiguity(t, start)))
                : null;
            path = (closest == null) ? null
                : this.findPath(unit, start, closest, carrier,
                                costDecider, lb);
            if (path != null) {
                PathNode last = path.getLastNode();
                last.next = new PathNode((Tile)realEnd, 0,
                    last.getTurns()+1, last.isOnCarrier(), last, null);
            }

        } else if (start instanceof Europe && realEnd instanceof Europe) {
            // 0: Europe->Europe: Create a trivial path.
            path = new PathNode(start, unit.getMovesLeft(), 0,
                false, null, null);

        } else if (start instanceof Europe && realEnd instanceof Tile) {
            // 1: Europe->Tile
            // Fail fast without an off map unit.
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) {
                path = null;

                // Find the best place to enter the map from Europe
            } else if ((p = getBestEntryPath(unit, (Tile)realEnd, carrier,
                        costDecider)) == null) {
                path = null;

                // Now search forward from there to get a path in the
                // right order (path costs are not symmetric).  There are
                // "expected" failures when rivers block due to foreign
                // ship movement.  There are also other failures which we
                // would like to log.  Try to filter out the first case.
            } else if ((path = findMapPath(unit,
                        (tile = p.getLastNode().getTile()), (Tile)realEnd,
                        carrier, costDecider, lb)) == null) {
                if (!((Tile)realEnd).isOnRiver()) {
                    LogBuilder l2 = new LogBuilder(512);
                    l2.add("Fail in findPath(", unit, ", ", tile,
                        ", ", realEnd, ", ", carrier, ")\n");
                    l2.addStackTrace();
                    l2.add(p.fullPathToString());
                    findMapPath(unit, tile, (Tile)realEnd,
                        carrier, costDecider, l2);
                    l2.log(logger, Level.WARNING);
                }
                path = null;

                // At the front of the path insert a node for the starting
                // location in Europe, correcting for the turns to sail to
                // the entry location.
            } else {
                path.addTurns(offMapUnit.getSailTurns());
                path.previous = new PathNode(start, unit.getMovesLeft(),
                    0, carrier != null, null, path);
                path = path.previous;
                if (carrier != null && unit.getLocation() != carrier) {
                    path.previous = new PathNode(start, unit.getMovesLeft(),
                        0, false, null, path);
                    path = path.previous;
                }
            }

        } else if (start instanceof Tile && realEnd instanceof Europe) {
            // 2: Tile->Europe
            // Fail fast if Europe is unattainable.
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) {
                path = null;
                
                // Search forwards to the high seas.
            } else if ((p = searchMap(unit, (Tile)start,
                        GoalDeciders.getHighSeasGoalDecider(),
                        costDecider, INFINITY, carrier, null, lb)) == null) {
                path = null;

            } else {
                PathNode last = p.getLastNode();
                last.next = new PathNode(realEnd, unit.getInitialMovesLeft(),
                    last.getTurns() + offMapUnit.getSailTurns(),
                    last.isOnCarrier(), last, null);
                path = p;
            }

        } else if (start instanceof Tile && realEnd instanceof Tile) {
            // 3: Tile->Tile
            Direction d;
            Unit.MoveType mt;
            // Short circuit if adjacent and blocked
            if (unit != null
                && (d = ((Tile)start).getDirection((Tile)realEnd)) != null
                && (mt = unit.getMoveType(d)).isLegal()
                && !mt.isProgress()) {
                path = new PathNode(start, unit.getMovesLeft(), 0,
                                    carrier != null, null, null);
                int cost = unit.getMoveCost((Tile)start, (Tile)realEnd,
                                            unit.getMovesLeft());
                path.next = new PathNode(realEnd, unit.getMovesLeft() - cost,
                                         0, false, path, null);
            } else {
                path = findMapPath(unit, (Tile)start, (Tile)realEnd,
                                   carrier, costDecider, lb);
            }
        } else {
            throw new IllegalStateException("Can not happen: " + start
                + ", " + realEnd);
        }

        finishPath(path, unit, lb);
        return path;
    }

    /**
     * Searches for a goal.
     * Assumes units in Europe return to their current entry location,
     * which is not optimal most of the time.
     * Returns the full path including the start and end locations.
     *
     * @param unit The {@code Unit} to find a path for.
     * @param start The {@code Location} to start the search from.
     * @param goalDecider The object responsible for determining whether a
     *     given {@code PathNode} is a goal or not.
     * @param costDecider An optional {@code CostDecider}
     *     responsible for determining the path cost.
     * @param maxTurns The maximum number of turns the given
     *     {@code Unit} is allowed to move.  This is the
     *     maximum search range for a goal.
     * @param carrier An optional naval carrier {@code Unit} to use.
     * @param lb An optional {@code LogBuilder} to log to.
     * @return The path to a goal, or null if none can be found.
     * @throws IllegalArgumentException If the unit is null, or the
     *     start location does not make sense, or the carrier/unit
     *     combination is bogus.
     */
    public PathNode search(final Unit unit, Location start,
                           final GoalDecider goalDecider,
                           final CostDecider costDecider,
                           final int maxTurns, final Unit carrier,
                           LogBuilder lb) {
        if (traceSearch) lb = new LogBuilder(1024);

        final Unit offMapUnit = (carrier != null) ? carrier : unit;
        
        PathNode p, path;
        if (start instanceof Europe) {
            // Fail fast if Europe is unattainable.
            if (offMapUnit == null
                || !offMapUnit.getType().canMoveToHighSeas()) {
                path = null;

                // This is suboptimal.  We do not know where to enter from
                // Europe, so start with the standard entry location...
            } else if ((p = searchMap(unit,
                        (Tile)offMapUnit.getFullEntryLocation(),
                        goalDecider, costDecider, maxTurns, carrier,
                        null, lb)) == null) {
                path = null;

                // ...then if we find a path, try to optimize it.  This
                // will lose if the initial search fails due to a turn limit.
                // FIXME: do something better.
            } else {
                path = this.findPath(unit, start, p.getLastNode().getTile(),
                                     carrier, costDecider, lb);
            }

        } else {
            path = searchMap(unit, start.getTile(), goalDecider,
                             costDecider, maxTurns, carrier, null, lb);
        }

        finishPath(path, unit, lb);
        return path;
    }

    /**
     * Gets the search tracing status.
     *
     * @return The search tracing status.
     */
    public boolean getSearchTrace() {
        return traceSearch;
    }

    /**
     * Sets the search tracing status.
     *
     * @param trace The new search tracing status.
     * @return The original search tracing status.
     */
    public boolean setSearchTrace(boolean trace) {
        boolean ret = traceSearch;
        traceSearch = trace;
        return ret;
    }

    /**
     * Was a carrier used previously on a path?
     *
     * Beware!  This is special case code for partially constructed
     * paths that do not yet have valid .next links, so we can not use the
     * generic PathNode routines.
     *
     * @param path The path the search.
     * @return True if the path includes a previous on-carrier node.
     */
    private boolean usedCarrier(PathNode path) {
        while (path != null) {
            if (path.isOnCarrier()) return true;
            path = path.previous;
        }
        return false;
    }

    /**
     * Internal class for evaluating a candidate move.
     */
    private static class MoveCandidate {

        private Unit unit;
        private final PathNode current;
        private final Location dst;
        private int movesLeft;
        private int turns;
        private final boolean onCarrier;
        private int cost;


        /**
         * Creates a new move candidate where a cost decider will be used
         * to work out the new moves and turns left.
         *
         * @param unit The {@code Unit} to move.
         * @param current The current position on the path.
         * @param dst The {@code Location} to move to.
         * @param movesLeft The initial number of moves left.
         * @param turns The initial number of turns.
         * @param onCarrier Will the new move be on a carrier.
         * @param decider The {@code CostDecider} to use.
         */
        public MoveCandidate(Unit unit, PathNode current, Location dst,
                             int movesLeft, int turns, boolean onCarrier,
                             CostDecider decider) {
            this.unit = unit;
            this.current = current;
            this.dst = dst;
            this.movesLeft = movesLeft;
            this.turns = turns;
            this.onCarrier = onCarrier;
            CostDecider cd = (decider != null) ? decider
                : CostDeciders.defaultCostDeciderFor(unit);
            this.cost = cd.getCost(unit, current.getLocation(), dst,
                                   movesLeft);
            if (this.cost == CostDecider.ILLEGAL_MOVE) {
                // This can happen "validly" if we try to route
                // through unexplored tiles.  We do *not* want to
                // disallow this completely --- there was a bug report
                // to the effect that surely you should be able to
                // route through short stretches of unknown to
                // explored areas on the other side.  However BR#3153
                // shows that we need to cost the unexplored tiles
                // conservatively, so for now, consume all moves left
                // add two extra turns.
                if (dst.getTile() != null && !dst.getTile().isExplored()) {
                    this.turns += 2;
                    this.movesLeft = 0;
                } else {
                    throw new RuntimeException("Invalid move candidate:"
                        + " for " + unit + " to " + dst);
                }
            }
            this.turns += cd.getNewTurns();
            this.movesLeft = cd.getMovesLeft();
            this.cost = PathNode.getNodeCost(this.turns, this.movesLeft);
        }

        /**
         * Get the cost.
         *
         * @return The current move cost.
         */
        public int getCost() {
            return this.cost;
        }

        /**
         * Handles the change of unit as a result of an embark.
         *
         * @param unit The {@code Unit} to embark.
         */
        public void embarkUnit(Unit unit) {
            this.unit = unit;
            this.movesLeft = unit.getInitialMovesLeft();
            this.turns++;
            this.cost = PathNode.getNodeCost(this.turns, this.movesLeft);
        }

        /**
         * Does this move candidate improve on a specified move.
         *
         * @param best The {@code PathNode} to compare against.
         * @return True if this candidate is an improvement.
         */
        public boolean canImprove(PathNode best) {
            return best == null || this.cost < best.getCost();
        }

        /**
         * Replace a given path with that of this candidate move.
         *
         * @param openMap The list of available nodes.
         * @param openMapQueue The queue of available nodes.
         * @param f The heuristic values for A*.
         * @param sh A {@code SearchHeuristic} to apply.
         */
        public void improve(HashMap<String, PathNode> openMap,
                            PriorityQueue<PathNode> openMapQueue,
                            HashMap<String, Integer> f,
                            SearchHeuristic sh) {
            PathNode best = openMap.get(dst.getId());
            if (best != null) {
                openMap.remove(dst.getId());
                openMapQueue.remove(best);
            }
            add(openMap, openMapQueue, f, sh);
        }

        /**
         * Add this candidate to the open map+queue.
         *
         * @param openMap The list of available nodes.
         * @param openMapQueue The queue of available nodes.
         * @param f The map of f-values.
         * @param sh A {@code SearchHeuristic} to apply.
         */
        public void add(HashMap<String, PathNode> openMap,
                        PriorityQueue<PathNode> openMapQueue,
                        HashMap<String, Integer> f,
                        SearchHeuristic sh) {
            int fcost = this.cost;
            if (this.dst.getTile() != null) {
                fcost += sh.getValue(this.dst.getTile());
            }
            f.put(this.dst.getId(), fcost);
            PathNode path = new PathNode(this.dst, this.movesLeft, this.turns,
                this.onCarrier, this.current, null);
            openMap.put(this.dst.getId(), path);
            openMapQueue.offer(path);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("[candidate unit=").append(unit)
                .append(" dst=").append(dst)
                .append(" movesLeft=").append(movesLeft)
                .append(" turns=").append(turns)
                .append(" onCarrier=").append(onCarrier)
                .append(" cost=").append(cost)
                .append(']');
            return sb.toString();
        }
    };

    /**
     * Searches for a path to a goal determined by the given
     * {@code GoalDecider}.
     *
     * Using A* with a List (closedMap) for marking the visited nodes
     * and using a PriorityQueue (openMapQueue) for getting the next
     * edge with the least cost.  This implementation could be
     * improved by having the visited attribute stored on each Tile in
     * order to avoid both of the HashMaps currently being used to
     * serve this purpose.
     *
     * If the SearchHeuristic is not supplied, then the algorithm
     * degrades gracefully to Dijkstra's algorithm.
     *
     * The data structure for the open list is a combined structure: using a
     * HashMap for membership tests and a PriorityQueue for getting the node
     * with the minimal f (cost+heuristics). This gives O(1) on membership
     * test and O(log N) for remove-best and insertions.
     *
     * @param unit The {@code Unit} to find a path for.
     * @param start The {@code Tile} to start the search from.
     * @param goalDecider The object responsible for determining whether a
     *     given {@code PathNode} is a goal or not.
     * @param costDecider An optional {@code CostDecider}
     *     responsible for determining the path cost.
     * @param maxTurns The maximum number of turns the given
     *     {@code Unit} is allowed to move. This is the
     *     maximum search range for a goal.
     * @param carrier An optional naval carrier {@code Unit} to use.
     * @param searchHeuristic An optional {@code SearchHeuristic}.
     * @param lb An optional {@code LogBuilder} to log to.
     * @return A path to a goal determined by the given
     *     {@code GoalDecider}.
     */
    private PathNode searchMap(final Unit unit, final Tile start,
                               final GoalDecider goalDecider,
                               final CostDecider costDecider,
                               final int maxTurns, final Unit carrier,
                               final SearchHeuristic searchHeuristic,
                               final LogBuilder lb) {
        final HashMap<String, PathNode> openMap = new HashMap<>();
        final HashMap<String, PathNode> closedMap = new HashMap<>();
        final HashMap<String, Integer> f = new HashMap<>();
        final PriorityQueue<PathNode> openMapQueue = new PriorityQueue<>(1024,
            Comparator.comparingInt(p -> f.get(p.getLocation().getId())));
        final SearchHeuristic sh = (searchHeuristic == null)
            ? trivialSearchHeuristic : searchHeuristic;
        final Unit offMapUnit = (carrier != null) ? carrier : unit;
        Unit currentUnit = (start.isLand())
            ? ((unit != null && unit.getLocation() == carrier
                    && start.hasSettlement()
                    && start.getSettlement().isConnectedPort())
                ? carrier : unit)
            : offMapUnit;
        if (lb != null) lb.add("Search trace(unit=", unit,
            ", from=", start,
            ", max=", ((maxTurns == INFINITY)?"-":Integer.toString(maxTurns)),
            ", carrier=", carrier, ")",
            "\n", net.sf.freecol.common.debug.FreeColDebugger.stackTraceToString());

        // Create the start node and put it on the open list.
        final PathNode firstNode = new PathNode(start,
            ((currentUnit != null) ? currentUnit.getMovesLeft() : -1),
            0, carrier != null && currentUnit == carrier, null, null);
        f.put(start.getId(), sh.getValue(start));
        openMap.put(start.getId(), firstNode);
        openMapQueue.offer(firstNode);

        PathNode best = null;
        int bestScore = INFINITY;
ok:     while (!openMap.isEmpty()) {
            // Choose the node with the lowest f.
            final PathNode currentNode = openMapQueue.poll();
            final Location currentLocation = currentNode.getLocation();
            openMap.remove(currentLocation.getId());
            if (lb != null) lb.add("\n  ", currentNode);

            // Reset current unit to that of this node.
            currentUnit = (currentNode.isOnCarrier()) ? carrier : unit;

            // Check for success.
            if (goalDecider.check(currentUnit, currentNode)) {
                if (lb != null) lb.add(" ***goal(",
                    currentNode.getCost(), ")***");
                best = goalDecider.getGoal();
                bestScore = best.getCost();
                if (!goalDecider.hasSubGoals()) break ok;
                continue;
            }

            // Valid candidate for the closed list.
            closedMap.put(currentLocation.getId(), currentNode);
            if (lb != null) lb.add(" closing");

            // Skip nodes that can not beat the current best path.
            if (bestScore < currentNode.getCost()) {
                if (lb != null) lb.add("...goal cost wins(",
                    bestScore, " < ", currentNode.getCost(), ")...");
                continue;
            }

            // Ignore nodes over the turn limit.
            if (currentNode.getTurns() > maxTurns) {
                if (lb != null) lb.add("...out-of-range");
                continue;
            }

            // Collect the parameters for the current node.
            final int currentMovesLeft = currentNode.getMovesLeft();
            final int currentTurns = currentNode.getTurns();
            final boolean currentOnCarrier = currentNode.isOnCarrier();

            final Tile currentTile = currentNode.getTile();
            if (currentTile == null) { // Must be in Europe.
                // FIXME: Do not consider tiles "adjacent" to Europe, yet.
                // There may indeed be cases where going to Europe and
                // coming back on the other side of the map is faster.
                if (lb != null) lb.add("...skip Europe");
                continue;
            }

            // Try the tiles in each direction
            PathNode closed;
            for (Tile moveTile : currentTile.getSurroundingTiles(1)) {
                // If the new tile is the tile we just visited, skip it.
                if (lb != null) lb.add("\n    ", moveTile);
                if (currentNode.previous != null
                    && currentNode.previous.getTile() == moveTile) {
                    if (lb != null) lb.add(" !prev");
                    continue;
                }

                // Skip neighbouring tiles already too expensive.
                closed = closedMap.get(moveTile.getId());
                if (closed != null) {
                    int cc = closed.getCost();
                    if (cc <= currentNode.getCost()) {
                        if (lb != null) lb.add(" !worse ", cc);
                        continue;
                    }
                }

                // Prepare to consider move validity
                Unit.MoveType umt = unit.getSimpleMoveType(currentTile,
                                                           moveTile);
                boolean unitMove = umt.isProgress();
                boolean carrierMove = carrier != null
                    && carrier.getSimpleMoveType(carrier.getTile(), moveTile).isProgress();
                if (lb != null) lb.add(" ", ((unitMove) ? "U"
                        : ((carrierMove) ? "C" : "")));
                MoveCandidate move;
                String stepLog;
                
                // Is this move to the goal?  Use fake high cost so
                // this does not become cached inside the goal decider
                // as the preferred path.
                boolean isGoal = goalDecider.check(unit,
                    new PathNode(moveTile, 0, INFINITY/2, false,
                                 currentNode, null));
                if (isGoal) {
                    if (lb != null) lb.add(" *goal*", umt);
                    if (unitMove) {
                        if (moveTile.hasSettlement() && currentOnCarrier && carrierMove) {
                            // If the goal has a settlement and the
                            // unit is travelling by carrier, dock the
                            // carrier.
                            move = new MoveCandidate(carrier, currentNode,
                                moveTile, currentMovesLeft, currentTurns,
                                true, CostDeciders.tileCost());
                        } else {
                            // Otherwise let the unit complete the path.
                            int left = (currentOnCarrier)
                                ? ((currentNode.embarkedThisTurn(currentTurns))
                                    ? 0
                                    : unit.getInitialMovesLeft())
                                : currentMovesLeft;
                            move = new MoveCandidate(unit, currentNode,
                                moveTile, left, currentTurns, false,
                                CostDeciders.tileCost());
                        }
                    } else {
                        // Handle some special cases where the move may not
                        // necessarily progress, but still can do useful work.
                        switch (umt) {
                        case ATTACK_UNIT:
                        case ATTACK_SETTLEMENT:
                        case ENTER_FOREIGN_COLONY_WITH_SCOUT:
                        case ENTER_INDIAN_SETTLEMENT_WITH_SCOUT:
                        case ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST:
                        case ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY:
                        case ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS:
                            // Can not move to the tile, but there is
                            // a valid interaction with the unit or
                            // settlement that is there.
                            move = new MoveCandidate(unit, currentNode,
                                moveTile, currentMovesLeft, currentTurns,
                                false, CostDeciders.tileCost());
                            unitMove = true;
                            break;
                        case EMBARK:
                            move = new MoveCandidate(unit, currentNode,
                                moveTile, currentMovesLeft, currentTurns,
                                true, CostDeciders.tileCost());
                            unitMove = true;
                            break;
                        case MOVE_NO_ATTACK_CIVILIAN:
                            // There is a settlement in the way, this
                            // path can never succeed.
                            if (moveTile.hasSettlement()) {
                                if (lb != null) lb.add(" !FAIL-SETTLEMENT");
                                if (!goalDecider.hasSubGoals()) break ok;
                                continue;
                            }
                            // There is a unit in the way.  Unless this
                            // unit can arrive there this turn, assume the
                            // condition is transient as long as the tile
                            // is not in a constrained position such as a
                            // small island or river.
                            if (currentNode.getTurns() <= 0
                                || moveTile.getAvailableAdjacentCount() < 3) {
                                if (lb != null) lb.add(" !FAIL-ATTACK");
                                if (!goalDecider.hasSubGoals()) break ok;
                                continue;
                            }
                            if (lb != null) lb.add(" blocked");
                            unitMove = true;
                            move = new MoveCandidate(unit, currentNode,
                                moveTile, currentMovesLeft, currentTurns,
                                false, CostDeciders.tileCost());
                            break;
                        default:
                            // Several cases here, these are understood:
                            // MOVE_NO_ATTACK_EMBARK:
                            //   Land unit trying to use water, which
                            //   can not work without a ship there
                            // MOVE_NO_ACCESS_WATER:
                            //   The unit can not disembark directly to
                            //   the goal along this path
                            // MOVE_NO_ATTACK_MARINE:
                            //   Ampibious attack disallowed, disembark to
                            //   reach the goal
                            // There will be more.
                            // We used to do---
                            //   if (!goalDecider.hasSubGoals()) break ok;
                            // --- here, like in the transient failure case
                            // above but we should not truncate other
                            // surrounding tiles.
                            if (lb != null) lb.add(" !FAIL-", umt);
                            continue;
                        }
                    }
                    assert move != null;
                    stepLog = "@";
                } else {
                    // Ordinary non-goal moves.
                    //
                    // Check for a carrier change at the new tile,
                    // creating a MoveCandidate for each case.
                    //
                    // Do *not* allow units to re-embark on the carrier.
                    // Note that embarking can actually increase the moves
                    // left because the carrier might be not have spent
                    // any moves yet that turn.
                    //
                    // Note that we always favour using the carrier if
                    // both carrier and non-carrier moves are possible,
                    // which can only be true moving into a settlement.
                    // Usually when moving into a settlement it will be
                    // useful to dock the carrier so it can collect new
                    // cargo.  OTOH if the carrier is just passing through
                    // the right thing is to keep the passenger on board.
                    // However, see the goal settlement exception above.
                    MoveStep step = (currentOnCarrier)
                        ? ((carrierMove) ? MoveStep.BYWATER
                            : (unitMove) ? MoveStep.DISEMBARK
                            : MoveStep.FAIL)
                        : (carrierMove && !usedCarrier(currentNode))
                        ? MoveStep.EMBARK
                        : (unitMove) ? ((unit.isNaval())
                            ? MoveStep.BYWATER
                            : MoveStep.BYLAND)
                        : MoveStep.FAIL;
                    switch (step) {
                    case BYLAND:
                        move = new MoveCandidate(unit, currentNode, moveTile, 
                            currentMovesLeft, currentTurns, false, costDecider);
                        break;
                    case BYWATER:
                        move = new MoveCandidate(offMapUnit, currentNode, moveTile,
                            currentMovesLeft, currentTurns, currentOnCarrier,
                            costDecider);
                        break;
                    case EMBARK:
                        move = new MoveCandidate(offMapUnit, currentNode, moveTile,
                            currentMovesLeft, currentTurns, true,
                            costDecider);
                        move.embarkUnit(carrier);
                        break;
                    case DISEMBARK:
                        move = new MoveCandidate(unit, currentNode, moveTile,
                            0, currentTurns, false, costDecider);
                        break;
                    case FAIL: default: // Loop on failure.
                        if (lb != null) lb.add("!");
                        continue;
                    }
                    stepLog = " " + step + "_";
                }
                assert move.getCost() >= 0;
                // Tighten the bounds on a previously seen case if possible
                if (closed != null) {
                    if (move.canImprove(closed)) {
                        closedMap.remove(moveTile.getId());
                        move.improve(openMap, openMapQueue, f, sh);
                        stepLog += "^" + Integer.toString(move.getCost());
                    } else {
                        stepLog += "v";
                    }
                } else {
                    if (move.canImprove(openMap.get(moveTile.getId()))){
                        move.improve(openMap, openMapQueue, f, sh);
                        stepLog += "+" + Integer.toString(move.getCost());
                    } else {
                        stepLog += "-";
                    }
                }
                if (lb != null) lb.add(stepLog);
            }
        }

        // Relink the path.  We omitted the .next link while constructing it.
        best = goalDecider.getGoal();
        if (best != null) {
            while (best.previous != null) {
                best.previous.next = best;
                best = best.previous;
            }
        }
        return best;
    }

    /**
     * Searches for a tile within a radius of a starting tile.
     *
     * Does not use a unit, and thus does not consider movement validity.
     *
     * @param start The starting {@code Tile}.
     * @param goalDecider A {@code GoalDecider} that chooses the goal,
     *     which must be capable of tolerating a null unit.
     * @param radius The maximum radius of tiles to search from the start.
     * @return The goal tile as determined by the, or null if none found.
     */
    public Tile searchCircle(final Tile start, final GoalDecider goalDecider,
        final int radius) {
        if (start == null || goalDecider == null || radius <= 0) return null;

        for (Tile t : getCircleTiles(start, true, radius)) {
            PathNode path = new PathNode(t, 0, start.getDistanceTo(t), false,
                null, null);
            if (goalDecider.check(null, path)
                && !goalDecider.hasSubGoals())
                break;
        }
        
        PathNode best = goalDecider.getGoal();
        return (best == null) ? null : best.getTile();
    }

    
    // Useful customers for tile iteration.

    /**
     * Searches for land within the given radius.
     *
     * @param x X-component of the position to search from.
     * @param y Y-component of the position to search from.
     * @param distance The radius in tiles that should be searched for land.
     * @return The first land tile found within the radius, or null if none
     *     found.
     */
    public Tile getLandWithinDistance(int x, int y, int distance) {
        for (Tile t : getCircleTiles(getTile(x, y), true, distance)) {
            if (t.isLand()) return t;
        }
        return null;
    }

    /**
     * Flood fills from a given {@code Position} p, based on
     * connectivity information encoded in boolmap
     *
     * @param boolmap The connectivity information for this floodfill.
     * @param x The starting x coordinate.
     * @param y The starting y coordinate.
     * @return A boolean[][] of the same size as boolmap, where "true"
     *      means the fill succeeded at that location.
     */
    public static boolean[][] floodFillBool(boolean[][] boolmap, int x, int y) {
        return floodFillBool(boolmap, x, y, Integer.MAX_VALUE);
    }

    /**
     * Flood fills from a given {@code Position} p, based on
     * connectivity information encoded in boolmap
     *
     * @param boolmap The connectivity information for this floodfill.
     * @param x The starting x coordinate.
     * @param y The starting y coordinate.
     * @param limit Limit to stop flood fill at.
     * @return A boolean[][] of the same size as boolmap, where "true"
     *      means the fill succeeded at that location.
     */
    public static boolean[][] floodFillBool(boolean[][] boolmap, int x, int y,
                                            int limit) {
        final int xmax = boolmap.length, ymax = boolmap[0].length;
        boolean[][] visited = new boolean[xmax][ymax];
        Queue<Position> q = new LinkedList<>();

        visited[x][y] = true;
        Position p = new Position(x, y);
        for (; p != null && --limit > 0; p = q.poll()) {
            for (Direction d : Direction.values()) {
                final Position np = new Position(p, d);
                if (!np.isValid(xmax, ymax)) continue;
                int nx = np.getX(), ny = np.getY();
                if (boolmap[nx][ny] && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    q.add(np);
                }
            }

        }
        return visited;
    }

    /**
     * Sets the contiguity identifier for all tiles.
     */
    public void resetContiguity() {
        // Create the water map.  It is an error for any tile not to
        // have a region at this point.
        final int xmax = getWidth(), ymax = getHeight();
        boolean[][] waterMap = new boolean[xmax][ymax];
        for (int y = 0; y < ymax; y++) {
            for (int x = 0; x < xmax; x++) {
                if (isValid(x, y)) {
                    waterMap[x][y] = !getTile(x,y).isLand();
                    Tile tile = getTile(x, y);
                    tile.setContiguity(-1);
                }
            }
        }

        // Flood fill each contiguous water region, setting the
        // contiguity number.
        int contig = 0;
        floodFill(contig, ymax, xmax, waterMap);

        // Complement the waterMap, it is now the land map.
        for (int y = 0; y < ymax; y++) {
            for (int x = 0; x < xmax; x++) {
                if (isValid(x, y)) waterMap[x][y] = !waterMap[x][y];
            }
        }

        // Flood fill again for each contiguous land region.
        floodFill(contig, ymax, xmax, waterMap);
    }

    /**
     * Performs fill flood action and calculations
     *
     * @param contig The contiguity number.
     * @param ymax The Y-value
     * @param xmax The X-value
     * @param waterMap The boolean array containing the x and y coordinates
     */
    private void floodFill(int contig, int ymax, int xmax, boolean[][] waterMap) {
        for (int y = 0; y < ymax; y++) {
            for (int x = 0; x < xmax; x++) {
                if (waterMap[x][y]) {
                    Tile tile = getTile(x, y);
                    if (tile.getContiguity() >= 0) continue;

                    boolean[][] found = floodFillBool(waterMap, x, y);
                    for (int yy = 0; yy < ymax; yy++) {
                        for (int xx = 0; xx < xmax; xx++) {
                            if (found[xx][yy]) {
                                Tile t = getTile(xx, yy);
                                if (t.getContiguity() < 0) {
                                    t.setContiguity(contig);
                                }
                            }
                        }
                    }
                    contig++;
                }
            }
        }
    }

    /**
     * Places the "high seas"-tiles on the border of this map.
     *
     * All other tiles previously of type High Seas will be set to Ocean.
     *
     * @param distToLandFromHighSeas The distance between the land
     *     and the high seas (given in tiles).
     * @param maxDistanceToEdge The maximum distance a high sea tile
     *     can have from the edge of the map.
     */
    public void resetHighSeas(int distToLandFromHighSeas,
        int maxDistanceToEdge) {
        final Specification spec = getSpecification();
        final TileType ocean = spec.getTileType("model.tile.ocean");
        final TileType highSeas = spec.getTileType("model.tile.highSeas");
        if (highSeas == null) {
            throw new RuntimeException("HighSeas TileType must exist: "+spec);
        }
        if (ocean == null) {
            throw new RuntimeException("Ocean TileType must exist: " + spec);
        }
        if (distToLandFromHighSeas < 0) {
            throw new RuntimeException("Land<->HighSeas distance can not be negative: " + distToLandFromHighSeas);
        }
        if (maxDistanceToEdge < 0) {
            throw new RuntimeException("Distance to edge can not be negative: " + maxDistanceToEdge);
        }

        // Reset all highSeas tiles to the default ocean type.
        forEachTile(t -> t.getType() == highSeas, t -> t.setType(ocean));

        final int width = getWidth(), height = getHeight();
        Tile t, seaL = null, seaR = null;
        int totalL = 0, totalR = 0, distanceL = -1, distanceR = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < maxDistanceToEdge && x < width
                     && isValid(x, y)
                     && (t = getTile(x, y)).getType() == ocean; x++) {
                Tile other = getLandWithinDistance(x, y,
                    distToLandFromHighSeas);
                if (other == null) {
                    t.setType(highSeas);
                    totalL++;
                } else {
                    int distance = t.getDistanceTo(other);
                    if (distanceL < distance) {
                        distanceL = distance;
                        seaL = t;
                    }
                }
            }
            for (int x = 0; x < maxDistanceToEdge && x < width
                     && isValid(width-1-x, y)
                     && (t = getTile(width-1-x, y)).getType() == ocean; x++) {
                Tile other = getLandWithinDistance(width-1-x, y,
                    distToLandFromHighSeas);
                if (other == null) {
                    t.setType(highSeas);
                    totalR++;
                } else {
                    int distance = t.getDistanceTo(other);
                    if (distanceR < distance) {
                        distanceR = distance;
                        seaR = t;
                    }
                }
            }
        }
        if (totalL <= 0 && seaL != null) {
            seaL.setType(highSeas);
            totalL++;
        }
        if (totalR <= 0 && seaR != null) {
            seaR.setType(highSeas);
            totalR++;
        }
        if (totalL <= 0 || totalR <= 0) {
            logger.warning("No high seas on "
                + ((totalL <= 0 && totalR <= 0) ? "either"
                    : (totalL <= 0) ? "left"
                    : (totalR <= 0) ? "right"
                    : "BOGUS") + " side of the map."
                + "  This can cause failures on small test maps.");
        }
    }

    /**
     * Sets the high seas count for all tiles connected to the high seas.
     * Any ocean tiles on the map vertical edges that do not have an
     * explicit false moveToEurope attribute are given a true one.
     *
     * Set all high seas counts negative, then start with a count of
     * zero for tiles with the moveToEurope attribute or of a type
     * with that ability.  Iterate outward by neighbouring tile,
     * incrementing the count on each pass, stopping at land.  Thus,
     * only the coastal land tiles will have a non-negative high seas
     * count.  This significantly speeds up the colony site evaluator,
     * as it does not have to try to find a path to Europe for each
     * tile.
     */
    public void resetHighSeasCount() {
        List<Tile> curr = new ArrayList<>();
        List<Tile> next = new ArrayList<>();
        int hsc = 0;
        for (Tile t : this.tileList) {
            t.setHighSeasCount(-1);
            if (!t.isLand()) {
                if ((t.getX() == 0 || t.getX() == getWidth()-1)
                    && t.getType() != null
                    && t.getType().isHighSeasConnected()
                    && !t.getType().isDirectlyHighSeasConnected()
                    && t.getMoveToEurope() == null) {
                    t.setMoveToEurope(Boolean.TRUE);
                }
                if (t.isDirectlyHighSeasConnected()) {
                    t.setHighSeasCount(0);
                    next.add(t);
                }
            }
        }
        while (!next.isEmpty()) {
            hsc++;
            curr.addAll(next);
            next.clear();
            while (!curr.isEmpty()) {
                Tile tile = curr.remove(0);
                // Deliberately using low level access to neighbours
                // rather than Tile.getSurroundingTiles() because that
                // relies on the map being attached to the game, which
                // is not necessarily true in the test suite.
                Position position = new Position(tile.getX(), tile.getY());
                for (Position p : transform(Direction.values(), alwaysTrue(),
                        d -> new Position(position, d))) {
                    if (isValid(p)) {
                        Tile t = getTile(p);
                        if (t.getHighSeasCount() < 0) {
                            t.setHighSeasCount(hsc);
                            if (!t.isLand()) next.add(t);
                        }
                    }
                }
            }
        }
    }

    /**
     * Reset layer to reflect what is actually there.
     */
    public void resetLayers() {
        boolean regions = false,
            rivers = false,
            lostCityRumours = false,
            resources = false,
            nativeSettlements = false;
        final int hgt = getHeight(), wid = getWidth();
        for (int y = 0; y < hgt; y++) {
            for (int x = 0; x < wid; x++) {
                Tile t = getTile(x, y);
                regions |= t.getRegion() != null;
                rivers |= t.hasRiver();
                lostCityRumours |= t.hasLostCityRumour();
                resources |= t.hasResource();
                nativeSettlements |= t.getSettlement() instanceof IndianSettlement;
            }
        }
        setLayer((rivers && lostCityRumours && resources && nativeSettlements)
            ? Layer.ALL
            : (nativeSettlements) ? Layer.NATIVES
            : (lostCityRumours) ? Layer.RUMOURS
            : (resources) ? Layer.RESOURCES
            : (rivers) ? Layer.RIVERS
            : (regions) ? Layer.REGIONS
            : Layer.TERRAIN);
    }

    /**
     * Fix the region parent/child relationships.
     */
    public void fixupRegions() {
        for (Region r : transform(getRegions(), r -> !r.isPacific())) {
            Region p = r.getParent();
            // Mountains and Rivers were setting their parent to the
            // discoverable land region they are created within.  Move them
            // up to being children of the geographic region.
            if (p != null && p.getDiscoverable() && r.getDiscoverable()) {
                p = p.getParent();
                r.setParent(p);
            }
            if (p != null && !p.getChildren().contains(r)) p.addChild(r);
        }
    }

    /**
     * Import a tile, possibly from another game.
     *
     * @param other The <code>Tile</code> to import.
     * @param x The x-coordinate of the new tile.
     * @param y The y-coordinate of the new tile.
     * @param layer The maximum layer to import.
     * @return The new imported <code>Tile</code>.
     */
    public Tile importTile(Tile other, int x, int y, Layer layer) {
        final Game game = getGame();
        Tile t = new Tile(game, other.getType(), x, y);
        if (other.getMoveToEurope() != null) {
            t.setMoveToEurope(other.getMoveToEurope());
        }
        if (other.getTileItemContainer() != null) {
            TileItemContainer container = new TileItemContainer(game, t);
            container.copyFrom(other.getTileItemContainer(), layer);
            t.setTileItemContainer(container);
        }
        // FIXME: handle regions
        return t;
    }

    /**
     * Scale the map into the specified size.
     *
     * This implementation uses a simple linear scaling, and
     * the isometric shape is not taken into account.
     *
     * FIXME: Find a better method for choosing a group of
     * adjacent tiles.  This group can then be merged into a
     * common tile by using the average value (for example: are
     * there a majority of ocean tiles?).
     *
     * @param width The width of the resulting map.
     * @param height The height of the resulting map.
     * @return A scaled version of this map.
     */
    public Map scale(final int width, final int height) {
        // FIXME: the scaling above can omit or double tiles, which will
        // break the river and road connections.
        // FIXME: tile ownership is borked again
        // FIXME: settlements?!? what settlements?
        // Note: can not use Tile.copyIn as that sets x,y
        final Game game = getGame();
        Map ret = new Map(game, width, height);
        final int oldWidth = getWidth();
        final int oldHeight = getHeight();
        ret.populateTiles((x, y) -> {
                final int oldX = (x * oldWidth) / width;
                final int oldY = (y * oldHeight) / height;
                // FIXME: This tile should be based on the average as
                // mentioned at the top of this method.
                return this.importTile(getTile(oldX, oldY), x, y, Layer.ALL);
            });
        ret.resetContiguity();
        ret.resetHighSeasCount();
        return ret;
    }
        
    
    // Interface Location
    // getId() inherited.

    /**
     * Gets the location tile.  Obviously not applicable to a Map.
     *
     * @return Null.
     */
    @Override
    public Tile getTile() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabel() {
        return StringTemplate.key("newWorld");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabelFor(Player player) {
        String name = player.getNewLandName();
        return (name == null) ? getLocationLabel() : StringTemplate.name(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Locatable locatable) {
        // Used to add units to their entry location.  Dropped as this
        // is handled explicitly in the server.
        if (locatable instanceof Unit) {
            throw new RuntimeException("Disabled Map.add(Unit): " + locatable);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Tile tile = locatable.getTile();
            if (tile != null) return tile.remove(locatable);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Locatable locatable) {
        return locatable instanceof Unit
            && locatable.getLocation() != null
            && locatable.getLocation().getTile() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAdd(Locatable locatable) {
        return locatable instanceof Unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCount() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Unit> getUnits() {
        return Stream.<Unit>empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Unit> getUnitList() {
        return Collections.<Unit>emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GoodsContainer getGoodsContainer() {
        return null; // Obviously irrelevant for a Map.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Settlement getSettlement() {
        return null; // Obviously irrelevant for a Map.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Colony getColony() {
        return null; // Obviously irrelevant for a Map.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndianSettlement getIndianSettlement() {
        return null; // Obviously irrelevant for a Map.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location up() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRank() {
        return Location.LOCATION_RANK_NOWHERE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        return "Map";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageIcon getLocationImage(int cellHeight, ImageLibrary library) {
        return new ImageIcon(library.getScaledImage(ImageLibrary.LOST_CITY_RUMOUR));
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        final int hgt = getHeight(), wid = getWidth();
        for (int y = 0; y < hgt; y++) {
            for (int x = 0; x < wid; x++) {
                Tile t = getTile(x, y);
                result = result.combine(t.checkIntegrity(fix, lb));
            }
        }
        return result;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Map o = copyInCast(other, Map.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        String err = updateTiles(o.getWidth(), o.getHeight());
        if (err != null) throw new RuntimeException("copyIn failure, " + err);
        // Allow creating new regions in first map update
        clearRegions();
        for (Region r : o.getRegions()) addRegion(game.update(r, true));
        // Use updateTile rather than copyIn to allow new tiles to be
        // populated in the first map update
        o.forEachTile(t -> this.updateTile(game.update(t, true)));
        
        this.layer = o.getLayer();
        this.minimumLatitude = o.getMinimumLatitude();
        this.maximumLatitude = o.getMaximumLatitude();
        this.latitudePerRow = o.getLatitudePerRow();
        return true;
    }


    // Serialization

    private static final String HEIGHT_TAG = "height";
    private static final String LAYER_TAG = "layer";
    private static final String MAXIMUM_LATITUDE_TAG = "maximumLatitude";
    private static final String MINIMUM_LATITUDE_TAG = "minimumLatitude";
    private static final String WIDTH_TAG = "width";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(WIDTH_TAG, getWidth());

        xw.writeAttribute(HEIGHT_TAG, getHeight());

        xw.writeAttribute(LAYER_TAG, layer);

        xw.writeAttribute(MINIMUM_LATITUDE_TAG, minimumLatitude);

        xw.writeAttribute(MAXIMUM_LATITUDE_TAG, maximumLatitude);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Region region : sort(getRegions())) region.toXML(xw);

        final int hgt = getHeight(), wid = getWidth();
        for (int y = 0; y < hgt; y++) {
            for (int x = 0; x < wid; x++) {
                getTile(x, y).toXML(xw);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        String err = updateTiles(xr.getAttribute(WIDTH_TAG, -1), xr.getAttribute(HEIGHT_TAG, -1));
        if (err != null) throw new XMLStreamException("Map.readAttributes failure, " + err);

        setLayer(xr.getAttribute(LAYER_TAG, Layer.class, Layer.ALL));

        minimumLatitude = xr.getAttribute(MINIMUM_LATITUDE_TAG, -90);

        maximumLatitude = xr.getAttribute(MAXIMUM_LATITUDE_TAG, 90);

        calculateLatitudePerRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // The tiles structure is large, and individually
        // overwriteable, so we do not clear it unlike most other containers.

        super.readChildren(xr);

        // Fix up settlement tile ownership in one hit here, avoiding
        // complications with Tile-internal cached tiles.
        forEachTile(t -> {
                Settlement s = t.getOwningSettlement();
                if (s != null) s.addTile(t);
            });

        // @compat 0.11.3
        // Maps with incorrect parent/child chains were occurring.
        fixupRegions();
        // end @compat 0.11.3
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (Region.TAG.equals(tag)) {
            addRegion(xr.readFreeColObject(game, Region.class));

        } else if (Tile.TAG.equals(tag)) {
            Tile t = xr.readFreeColObject(game, Tile.class);
            if (!updateTile(t)) {
                logger.warning("Tile update failure for: " + t);
            }                    

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
