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

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.SelectOption;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * A class to encapsulate a binary land map.
 */
public class LandMap {

    private static final Logger logger = Logger.getLogger(LandMap.class.getName());

    /** The map width. */
    private final int width;

    /** The map height. */
    private final int height;

    /** The land map.  True means land. */
    private boolean[][] map;

    /** Number of land tiles on the map. */
    private int numberOfLandTiles;


    /**
     * Create a new land map with specified dimensions.
     *
     * @param width The map width.
     * @param height The map height.
     */
    public LandMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.map = new boolean[this.width][this.height];
        this.numberOfLandTiles = 0;
    }

    /**
     * Create a land map by importing it from a given map.
     *
     * @param map The {@code Map} to get the land map from.
     */
    public LandMap(Map map) {
        this(map.getWidth(), map.getHeight());

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.map[x][y] = (map.isValid(x, y))
                    ? map.getTile(x, y).isLand()
                    : false;
                if (this.map[x][y]) this.numberOfLandTiles++;
            }
        }
    }

    /**
     * Create a new land map using parameters from a supplied map
     * generator options option group.
     *
     * @param mgo The map generator {@code OptionGroup} to use.
     * @param random A pseudo random number source.
     */
    public LandMap(OptionGroup mgo, Random random) {
        this(mgo.getInteger(MapGeneratorOptions.MAP_WIDTH),
             mgo.getInteger(MapGeneratorOptions.MAP_HEIGHT));

        int distanceToEdge
            = mgo.getInteger(MapGeneratorOptions.PREFERRED_DISTANCE_TO_EDGE);
        int minNumberOfTiles = mgo.getInteger(MapGeneratorOptions.LAND_MASS)
            * getWidth() * getHeight() / 100;
        int gen = mgo.getSelection(MapGeneratorOptions.LAND_GENERATOR_TYPE);
        logger.info("Using land generator "
            + mgo.getSelectionName(MapGeneratorOptions.LAND_GENERATOR_TYPE)
            + " to make " + width + "x" + height + " map"
            + " with distance-to-edge=" + distanceToEdge
            + " and min-tile#=" + minNumberOfTiles);
        generate(gen, distanceToEdge, minNumberOfTiles, random);
    }


    /**
     * Get the width of the land map.
     *
     * @return The map width.
     */
    public final int getWidth() {
        return this.width;
    }

    /**
     * Get the height of the land map.
     *
     * @return The map height.
     */
    public final int getHeight() {
        return this.height;
    }

    /**
     * Is an xy coordinate valid on this map?
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return True if there coordinate is valid.
     */
    public boolean isValid(int x, int y) {
        return x >= 0  && y >= 0 && x < getWidth() && y < getHeight();
    }

    /**
     * Is there land on this map at a given xy coordinate?
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return True if there is land present.
     */
    public boolean isLand(int x, int y) {
        return (isValid(x, y)) ? this.map[x][y] : false;
    }

    /**
     * Is there any land in this land map?
     *
     * @return True if any land is present.
     */
    public boolean hasLand() {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; y < this.width; x++) {
                if (this.map[x][y]) return true;
            }
        }
        return false;
    }

    /**
     * Set a map position to land, and increase the land tile count.
     *
     * @param x The x coordinate of the new land.
     * @param y The y coordinate of the new land.
     * @return True if the land tile was set.
     */
    private boolean setLand(int x, int y) {
        if (isLand(x, y)) return false;
        this.map[x][y] = true;
        this.numberOfLandTiles++;
        return true;
    }
        
    /**
     * Sets a given map position to land and grow outward.
     *
     * Calls #growLand(int,int) for all valid adjacent map positions,
     * which may recursively call setLand for these.
     *
     * @param x The x coordinate of the new land.
     * @param y The y coordinate of the new land.
     * @param distanceToEdge The preferred distance to the map edge.
     * @param random A pseudo random number source.
     */
    private void setLand(int x, int y, int distanceToEdge,
                         Random random) {
        if (!setLand(x, y)) return;

        Position p = new Position(x, y);
        for (Direction direction : Direction.longSides) {
            Position n = new Position(p, direction);
            if (n.isValid(getWidth(), getHeight())) {
                growLand(n.getX(), n.getY(), distanceToEdge, random);
            }
        }
    }


    // Internals

    /**
     * Generate the land map using the given generator type.
     *
     * @param type The generator type.
     * @param minNumberOfTiles The minimum land tiles to generate.
     * @param distanceToEdge The preferred distance to the map edge.
     * @param random A pseudo random number source.
     */
    private final void generate(int type, int distanceToEdge,
                                int minNumberOfTiles, Random random) {
        switch (type) {
        case MapGeneratorOptions.LAND_GENERATOR_CLASSIC:
            createClassicLandMap(distanceToEdge, minNumberOfTiles, random);
            break;
        case MapGeneratorOptions.LAND_GENERATOR_CONTINENT:
            // Create one landmass of 75%, start it somewhere near the
            // center, then fill up with small islands.
            addPolarRegions();
            int contsize = (minNumberOfTiles * 75) / 100;
            addLandMass(contsize, contsize, getWidth()/2, getHeight()/4
                        + randomInt(logger, "Landmass", random, getHeight()/2),
                        distanceToEdge, random);
            while (this.numberOfLandTiles < minNumberOfTiles) {
                addLandMass(15, 25, -1, -1, distanceToEdge, random);
            }
            break;
        case MapGeneratorOptions.LAND_GENERATOR_ARCHIPELAGO:
            // Create 5 islands of 10% each, then delegate to the Islands
            // generator.
            addPolarRegions();
            int archsize = (minNumberOfTiles * 10) / 100;
            for (int i = 0; i < 5; i++) {
                addLandMass(archsize - 5, archsize + 5, -1, -1,
                            distanceToEdge, random);
            }
            // Fall through
        case MapGeneratorOptions.LAND_GENERATOR_ISLANDS:
            // Create islands of 25..75 tiles.
            addPolarRegions();
            while (this.numberOfLandTiles < minNumberOfTiles) {
                int s = randomInt(logger, "Island", random, 50) + 25;
                addLandMass(25, s, -1, -1, distanceToEdge, random);
            }
            break;
        }
        cleanMap();
    }

    /**
     * Create the standard FreeCol land map.
     *
     * @param distanceToEdge The nominal edge clearance.
     * @param minNumberOfTiles Lower bound for the tiles to create.
     * @param random A pseudo-random number source.
     */
    private void createClassicLandMap(int distanceToEdge, int minNumberOfTiles,
                                      Random random) {
        final int edg = distanceToEdge * 2;
        final int wid = getWidth() - edg * 2;
        final int hgt = getHeight() - edg * 2;
        int x, y;
        while (this.numberOfLandTiles < minNumberOfTiles) {
            int failCounter = 0;
            do {
                x = edg + randomInt(logger, "ClassicW", random, wid);
                y = edg + randomInt(logger, "ClassicH", random, hgt);
                failCounter++;
                // If landmass% is set too high, this loop may fail to
                // find a free tile.  Decrease necessary minimum over
                // time, so that this process will eventually succeed.
                if (failCounter > 100) {
                    failCounter = 0;
                    minNumberOfTiles--;
                    break;
                }
            } while (isLand(x, y));
            setLand(x, y, distanceToEdge, random);
        }
        addPolarRegions();
    }

    /**
     * Add land to the polar map rows at the top and bottom of the map,
     * with height determined by Map.POLAR_HEIGHT.
     *
     * FIXME: Make POLAR_HEIGHT an option.
     */
    private void addPolarRegions() {
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < Map.POLAR_HEIGHT; y++) setLand(x, y);
            int limit = this.height - 1 - Map.POLAR_HEIGHT;
            for (int y = limit; y < this.height; y++) setLand(x, y);
        }
    }

    /**
     * Remove any 1x1 islands on the map.
     */
    private void cleanMap() {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (!hasAdjacentLand(x, y)) this.map[x][y] = false;
            }
        }
    }

    /**
     * Do the given coordinates correspond to a location in the land map
     * with adjacent land?
     *
     * Note: does not check the tile at the coordinates itself.
     *
     * @param x The x coordinate to check.
     * @param y The y coordinate to check.
     * @return True if this tile has adjacent land.
     */
    private boolean hasAdjacentLand(int x, int y) {
        final Position p = new Position(x, y);
        final Predicate<Direction> landPred = d -> {
            Position n = new Position(p, d);
            return isLand(n.getX(), n.getY());
        };
        return any(Direction.values(), landPred);
    }

    /**
     * Get the positions surrounding a central position that are potential
     * valid land positions.
     *
     * @param position The central {@code Position} to work from.
     * @param distanceToEdge The preferred distance to the map edge.
     * @return A list of suitable {@code Position}s.
     */
    private List<Position> newPositions(Position position,
                                        int distanceToEdge) {
        final Predicate<Position> landPred = p ->
            (p.isValid(getWidth(), getHeight())
                && !hasAdjacentLand(p.getX(), p.getY())
                && p.getX() > distanceToEdge
                && p.getX() < getWidth() - distanceToEdge);
        final Function<Direction, Position> positionMapper = d ->
            new Position(position, d);
        return transform(map(Direction.longSides, positionMapper), landPred);
    }

    /**
     * Determines, based on position, number of adjacent land tiles
     * and some random factor, whether a given map position should be
     * set to land.  This is called for all valid map positions
     * adjacent to a position that has been set to land by
     * #setLand(int,int), and may recursively call setLand for the
     * current position.
     *
     * @param x The x coordinate to grow land at.
     * @param y The y coordinate to grow land at.
     * @param distanceToEdge The preferred distance to the map edge.
     * @param random A pseudo random number source.
     */
    private void growLand(int x, int y, int distanceToEdge, Random random) {
        if (isLand(x, y)) return; // Already land

        // Generate a comparison value:
        // Only if the number of adjacent land tiles is bigger than this value,
        // this tile will be set to land.
        // This value is part random, part based on position, that is:
        // -1 in the center of the map, and growing to
        // distanceToEdge (*2 for pole ends) at the maps edges.
        int r = randomInt(logger, "Grow", random, 8)
            + Math.max(-1,
                (1 + Math.max(distanceToEdge - Math.min(x, getWidth()-x),
                    2 * distanceToEdge - Math.min(y, getHeight()-y))));

        final Position p = new Position(x, y);
        final Predicate<Direction> landPred = d -> {
            Position n = new Position(p, d);
            return isLand(n.getX(), n.getY());
        };
        if (count(Direction.values(), landPred) > r) {
            setLand(x, y, distanceToEdge, random);
        }
    }

    /**
     * Create a new land mass (unconnected to existing land masses) of
     * size up to maxSize, and adds it to the current map if it is at
     * least minSize.
     *
     * @param minSize Minimum number of tiles in the land mass.
     * @param maxSize Maximum number of tiles in the land mass.
     * @param x Optional starting x coordinate (chosen randomly if negative).
     * @param y Optional starting y coordinate (chosen randomly if negative).
     * @param distanceToEdge The preferred distance to the map edge.
     * @param random A pseudo random number source.
     * @return The number of tiles added.
     */
    private int addLandMass(int minSize, int maxSize, int x, int y,
                            int distanceToEdge, Random random) {
        int size = 0;
        boolean[][] newLand = new boolean[getWidth()][getHeight()];

        // Pick a starting position that is sea without neighbouring land.
        if (x < 0 || y < 0) {
            final int wid = getWidth() - distanceToEdge * 2;
            final int hgt = getHeight() - distanceToEdge * 2;
            do {
                x = distanceToEdge + randomInt(logger, "LandW", random, wid);
                y = distanceToEdge + randomInt(logger, "LandH", random, hgt);
            } while (isLand(x, y) || hasAdjacentLand(x, y));
        }

        newLand[x][y] = true;
        size++;

        // Add all valid neighbour positions to list
        Position p = new Position(x, y);
        List<Position> l = newPositions(p, distanceToEdge);

        // Get a random position from the list,
        // set it to land,
        // add its valid neighbours to the list
        int enough = minSize + randomInt(logger, "LandSize", random,
                                         maxSize - minSize + 1);
        while (size < enough && !l.isEmpty()) {
            int i = randomInt(logger, "Lsiz", random, l.size());
            p = l.remove(i);

            if (!newLand[p.getX()][p.getY()]) {
                newLand[p.getX()][p.getY()] = true;
                size++;
                l.addAll(newPositions(p, distanceToEdge));
            }
        }

        // Add generated land to map if sufficiently large
        if (size >= minSize) {
            for (x = 0; x < this.width; x++) {
                for (y = 0; y < this.height; y++) {
                    if (newLand[x][y]) setLand(x, y);
                }
            }
        }
        return (size >= minSize) ? size : 0;
    }
}
