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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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

    /** Target distance from land to the map edge. */
    private int preferredDistanceToEdge;

    /** Minimum number of land tiles on the map. */
    private int minimumNumberOfTiles;


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
     * Create a land map by importing it from a given game.
     *
     * @param game The <code>Game</code> to get the land map from.
     */
    public LandMap(Game game) {
        this(game.getMap().getWidth(), game.getMap().getHeight());

        final Map map = game.getMap();
        boolean[][] bmap = new boolean[width][height];
        int n = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bmap[x][y] = (map.isValid(x, y))
                    ? map.getTile(x, y).isLand()
                    : false;
                if (bmap[x][y]) n++;
            }
        }
        this.map = bmap;
        this.numberOfLandTiles = n;
    }

    /**
     * Create a new land map using parameters from a supplied map
     * generator options option group.
     *
     * @param mgo The map generator <code>OptionGroup</code> to use.
     * @param random A pseudo random number source.
     */
    public LandMap(OptionGroup mgo, Random random) {
        this(mgo.getInteger(MapGeneratorOptions.MAP_WIDTH),
             mgo.getInteger(MapGeneratorOptions.MAP_HEIGHT));

        int preferredDistanceToEdge
            = mgo.getInteger(MapGeneratorOptions.PREFERRED_DISTANCE_TO_EDGE);
        int minimumNumberOfTiles = mgo.getInteger(MapGeneratorOptions.LAND_MASS)
            * width * height / 100;
        int gen = mgo.getInteger(MapGeneratorOptions.LAND_GENERATOR_TYPE);
        SelectOption so = (SelectOption)
            mgo.getOption(MapGeneratorOptions.LAND_GENERATOR_TYPE);
        logger.info("Using land generator " + so.getItemValues().get(gen));
        generate(gen, preferredDistanceToEdge, minimumNumberOfTiles,
                 random);
    }


    /**
     * Get the width of the land map.
     *
     * @return The map width.
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of the land map.
     *
     * @return The map height.
     */
    public int getHeight() {
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
        return x >= 0 && x < this.width && y >= 0 && y < this.height;
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


    // Internals

    /**
     * Generate the land map using the given generator type.
     *
     * @param type The generator type.
     * @param minimumNumberOfTiles The minimum land tiles to generate.
     * @param preferredDistanceToEdge The preferred distance to the map edge.
     * @param random A pseudo random number source.
     */
    private void generate(int type, int preferredDistanceToEdge,
                          int minimumNumberOfTiles, Random random) {
        switch (type) {
        case MapGeneratorOptions.LAND_GENERATOR_CLASSIC:
            createClassicLandMap(preferredDistanceToEdge, minimumNumberOfTiles,
                                 random);
            break;
        case MapGeneratorOptions.LAND_GENERATOR_CONTINENT:
            // Create one landmass of 75%, start it somewhere near the
            // center, then fill up with small islands.
            addPolarRegions();
            int contsize = (minimumNumberOfTiles * 75) / 100;
            addLandMass(contsize, contsize, width/2, height/4
                        + randomInt(logger, "Landmass", random, height/2),
                        preferredDistanceToEdge, random);
            while (numberOfLandTiles < minimumNumberOfTiles) {
                addLandMass(15, 25, -1, -1, preferredDistanceToEdge, random);
            }
            break;
        case MapGeneratorOptions.LAND_GENERATOR_ARCHIPELAGO:
            // Create 5 islands of 10% each, then delegate to the Islands
            // generator.
            addPolarRegions();
            int archsize = (minimumNumberOfTiles * 10) / 100;
            for (int i = 0; i < 5; i++) {
                addLandMass(archsize - 5, archsize + 5, -1, -1,
                            preferredDistanceToEdge, random);
            }
            // Fall through
        case MapGeneratorOptions.LAND_GENERATOR_ISLANDS:
            // Create islands of 25..75 tiles.
            addPolarRegions();
            while (numberOfLandTiles < minimumNumberOfTiles) {
                int s = randomInt(logger, "Island", random, 50) + 25;
                addLandMass(25, s, -1, -1,
                            preferredDistanceToEdge, random);
            }
            break;
        }
        cleanMap();
    }

    /**
     * Create the standard FreeCol land map.
     */
    private void createClassicLandMap(int preferredDistanceToEdge,
                                      int minimumNumberOfTiles, Random random) {
        int x, y;
        while (numberOfLandTiles < minimumNumberOfTiles) {
            int failCounter = 0;
            do {
                x = (randomInt(logger, "ClassicW", random,
                               width - preferredDistanceToEdge * 4))
                    + preferredDistanceToEdge * 2;
                y = (randomInt(logger, "ClassicH", random,
                               height - preferredDistanceToEdge * 4))
                    + preferredDistanceToEdge * 2;
                failCounter++;
                // If landmass% is set too high, this loop may fail to
                // find a free tile.  Decrease necessary minimum over
                // time, so that this process will eventually succeed.
                if (failCounter > 100) {
                    failCounter = 0;
                    minimumNumberOfTiles--;
                    break;
                }
            } while (map[x][y]);
            setLand(x, y, preferredDistanceToEdge, random);
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
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < Map.POLAR_HEIGHT; y++) {
                if (!map[x][y]) {
                    map[x][y] = true;
                    numberOfLandTiles++;
                }
            }
            int limit = height - 1 - Map.POLAR_HEIGHT;
            for (int y = limit; y < height; y++) {
                if (!map[x][y]) {
                    map[x][y] = true;
                    numberOfLandTiles++;
                }
            }
        }
    }

    /**
     * Remove any 1x1 islands on the map.
     */
    private void cleanMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isSingleTile(x, y)) map[x][y] = false;
            }
        }
    }

    /**
     * Do the given coordinates correspond to a location in the land map
     * with no adjoining land?
     *
     * @param x The x coordinate to check.
     * @param y The y coordinate to check.
     */
    private boolean isSingleTile(int x, int y) {
        final Position p = new Position(x, y);
        return none(Direction.values(), d -> {
                Position n = new Position(p, d);
                return n.isValid(width, height) && map[n.getX()][n.getY()];
            });
    }

    /**
     * Sets a given map position to land.
     *
     * Calls #growLand(int,int) for all valid adjacent map positions,
     * which may recursively call setLand for these.
     *
     * @param x The x coordinate of the new land.
     * @param y The y coordinate of the new land.
     * @param preferredDistanceToEdge The preferred distance to the map edge.
     * @param random A pseudo random number source.
     * @return The number of tiles set to land.
     */
    private int setLand(int x, int y, int preferredDistanceToEdge,
                        Random random) {
        if (map[x][y]) return 0;

        int ret = 1;
        map[x][y] = true;
        numberOfLandTiles++;

        Position p = new Position(x, y);
        for (Direction direction : Direction.longSides) {
            Position n = new Position(p, direction);
            if (n.isValid(width, height)) {
                ret += growLand(n.getX(), n.getY(), preferredDistanceToEdge,
                                random);
            }
        }
        return ret;
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
     * @param preferredDistanceToEdge The preferred distance to the map edge.
     * @param random A pseudo random number source.
     * @return The number of tiles set to land.
     */
    private int growLand(int x, int y, int preferredDistanceToEdge,
                         Random random) {
        if (map[x][y]) return 0; // Already land

        // Generate a comparison value:
        // Only if the number of adjacent land tiles is bigger than this value,
        // this tile will be set to land.
        // This value is part random, part based on position, that is:
        // -1 in the center of the map, and growing to
        // preferredDistanceToEdge (*2 for pole ends) at the maps edges.
        int r = randomInt(logger, "Grow", random, 8)
            + Math.max(-1,
                (1 + Math.max(preferredDistanceToEdge - Math.min(x, width-x),
                    2 * preferredDistanceToEdge - Math.min(y, height-y))));

        int sum = 0;
        Position p = new Position(x, y);
        for (Direction direction : Direction.values()) {
            Position n = new Position(p, direction);
            if (n.isValid(width, height) && map[n.getX()][n.getY()]) sum++;
        }

        return (sum > r) ? setLand(x, y, preferredDistanceToEdge, random)
            : 0;
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
     * @param preferredDistanceToEdge The preferred distance to the map edge.
     * @param random A pseudo random number source.
     * @return The number of tiles added.
     */
    private int addLandMass(int minSize, int maxSize, int x, int y,
                            int preferredDistanceToEdge, Random random) {
        int size = 0;
        boolean[][] newLand = new boolean[width][height];

        // Pick a starting position that is sea without neighbouring land.
        if (x < 0 || y < 0) {
            do {
                x = randomInt(logger, "LandW", random, 
                              width - preferredDistanceToEdge * 2)
                    + preferredDistanceToEdge;
                y = randomInt(logger, "LandH", random,
                              height - preferredDistanceToEdge * 2)
                    + preferredDistanceToEdge;
            } while (map[x][y] || !isSingleTile(x, y));
        }

        newLand[x][y] = true;
        size++;

        // Add all valid neighbour positions to list
        List<Position> l = new ArrayList<>();
        Position p = new Position(x, y);
        for (Direction direction : Direction.longSides) {
            Position n = new Position(p, direction);
            if (n.isValid(width, height)
                && isSingleTile(n.getX(), n.getY())
                && n.getX() > preferredDistanceToEdge
                && n.getX() < width-preferredDistanceToEdge) {
                l.add(n);
            }
        }

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

                for (Direction direction : Direction.longSides) {
                    Position n = new Position(p, direction);
                    if (n.isValid(width, height)
                        && isSingleTile(n.getX(), n.getY())
                        && n.getX() > preferredDistanceToEdge
                        && n.getX() < width-preferredDistanceToEdge) {
                        l.add(n);
                    }
                }
            }
        }

        // Add generated land to map if sufficiently large
        if (size >= minSize) {
            for (x = 0; x < width; x++) {
                for (y = 0; y < height; y++) {
                    if (newLand[x][y] == true) {
                        map[x][y] = true;
                        numberOfLandTiles++;
                    }
                }
            }
        }
        return (size >= minSize) ? size : 0;
    }
}
