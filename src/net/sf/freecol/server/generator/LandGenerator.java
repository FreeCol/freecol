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

package net.sf.freecol.server.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.Utils;


/**
 * Class for creating a land map.
 *
 * <br><br>
 *
 * A land map is a two-dimensional array with the boolean
 * values:
 *
 * <ul>
 *     <li><code>true</code>: land</li>
 *     <li><code>false</code>: ocean</li>
 * </ul>
 */
public class LandGenerator {

    private static final Logger logger = Logger.getLogger(LandGenerator.class.getName());

    private final OptionGroup mapGeneratorOptions;
    private final Random random;

    private boolean[][] map;

    private int width;
    private int height;
    private int landMass;

    private int preferredDistanceToEdge;
    private int numberOfLandTiles;
    private int minimumNumberOfTiles;

    private int genType;

    /**
     * Creates a new <code>LandGenerator</code>.
     *
     * @param mapGeneratorOptions The options to be
     *         used when creating a land map.
     * @param random The <code>Random</code> number source to use.
     * @see #createLandMap
     */
    public LandGenerator(OptionGroup mapGeneratorOptions, Random random) {
        this.mapGeneratorOptions = mapGeneratorOptions;
        this.random = random;
    }


    /**
     * Imports the land map from the given <code>Game</code>.
     *
     * @param game The <code>Game</code> to get the land map from.
     * @return An array where <i>true</i> means land
     * and <i>false</i> means ocean.
     */
    public static boolean[][] importLandMap(Game game) {
        Map map = game.getMap();
        boolean[][] bmap = new boolean[map.getWidth()][map.getHeight()];
        for (int y = 0; y < bmap[0].length; y++) {
            for (int x = 0; x < bmap.length; x++) {
                if (map.isValid(x, y)) {
                    try {
                        bmap[x][y] = map.getTile(x, y).isLand();
                    } catch (Exception e) {
                        logger.severe("Import failure at tile (" + x
                            + ", " + y + ").");
                    }
                } else {
                    bmap[x][y] = false;
                }
            }
        }
        return bmap;
    }



    /**
     * Creates a new land map.
     *
     * @return An array where <i>true</i> means land
     *     and <i>false</i> means ocean.
     */
    public boolean[][] createLandMap() {
        //get values from mapGeneratorOptions
        width = mapGeneratorOptions.getInteger(MapGeneratorOptions.MAP_WIDTH);
        height = mapGeneratorOptions.getInteger(MapGeneratorOptions.MAP_HEIGHT);
        preferredDistanceToEdge = mapGeneratorOptions.getInteger(MapGeneratorOptions.PREFERRED_DISTANCE_TO_EDGE);
        landMass = mapGeneratorOptions.getInteger(MapGeneratorOptions.LAND_MASS);
        minimumNumberOfTiles = width * height * landMass / 100;
        genType = mapGeneratorOptions.getInteger(MapGeneratorOptions.LAND_GENERATOR_TYPE);

        //set other internal values
        map = new boolean[width][height];
        numberOfLandTiles = 0;

        //run one of different land generators,
        //based on setting in mapGeneratorOptions
        //"Classic" is the original FreeCol land generator
        switch (genType) {
            case MapGeneratorOptions.LAND_GENERATOR_CLASSIC:
                createClassicLandMap();
                break;
            case MapGeneratorOptions.LAND_GENERATOR_CONTINENT:
                addPolarRegions();
                //create one landmass of 75%, start it somewhere near the center
                int contsize = (minimumNumberOfTiles*75)/100;
                addLandmass(contsize,contsize, width/2, 
                    Utils.randomInt(logger, "Landmass", random, height/2)+height/4);
                //then create small islands to fill up
                while (numberOfLandTiles < minimumNumberOfTiles) {
                    addLandmass(15,25);
                }
                cleanMap();
                break;
            case MapGeneratorOptions.LAND_GENERATOR_ARCHIPELAGO:
                addPolarRegions();
                //create 5 islands of 10% each
                int archsize = (minimumNumberOfTiles*10)/100;
                for (int i=0;i<5;i++) {
                    addLandmass(archsize-10,archsize);
                }
                //then, fall into next case to generate small islands
            case MapGeneratorOptions.LAND_GENERATOR_ISLANDS:
                addPolarRegions();
                //creates only islands of 25..75 tiles
                while (numberOfLandTiles < minimumNumberOfTiles) {
                    int s = Utils.randomInt(logger, "Island", random, 50) + 25;
                    addLandmass(20,s);
                }
                cleanMap();
                break;
        }

        return map;
    }


    private void createClassicLandMap() {
        int x;
        int y;

        while (numberOfLandTiles < minimumNumberOfTiles) {
            int failCounter=0;
            do {
                x = (Utils.randomInt(logger, "ClassicW", random, width-preferredDistanceToEdge*4)) + preferredDistanceToEdge*2;
                y = (Utils.randomInt(logger, "ClassicH", random, height-preferredDistanceToEdge*4)) + preferredDistanceToEdge*2;
                failCounter++;
                //if landmass% is set to high, this loop may fail to find a free tile.
                //decrease necessary minimum over time, so that this process
                //will eventually come to an end.
                if (failCounter>100) {
                    failCounter=0;
                    minimumNumberOfTiles--;
                    break;
                }
            } while (map[x][y]);

            setLand(x,y);
        }

        addPolarRegions();
        cleanMap();
    }



    /**
     * Tries to create a new land mass (unconnected to existing land masses)
     * of size=maxsize, and adds it to the current map if it is
     * at least size=minsize.
     */
    private void addLandmass(int minsize, int maxsize, int x, int y) {
        int size = 0;
        boolean[][] newland = new boolean[width][height];

        //pick a starting position that is sea without neighbouring land
        if (x<0 || y<0) {
            do {
                x = Utils.randomInt(logger, "LandW", random, 
                                    width-preferredDistanceToEdge*2)
                    + preferredDistanceToEdge;
                y = Utils.randomInt(logger, "LandH", random,
                                    height-preferredDistanceToEdge*2)
                    + preferredDistanceToEdge;
            } while (map[x][y] || !isSingleTile(x,y));
        }

        newland[x][y] = true;
        size++;

        //add all valid neighbour positions to list
        List<Position>l = new ArrayList<Position>();
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

        //get a random position from the list,
        //set it to land,
        //add its valid neighbours to the list
        while (size < maxsize && l.size()>0) {
            int i = Utils.randomInt(logger, "Lsiz", random, l.size());
            p = l.remove(i);

            if (!newland[p.getX()][p.getY()]) {
                newland[p.getX()][p.getY()] = true;
                size++;

                //add all valid neighbour positions to list
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

        //add generated island to map
        for (x=0; x<width; x++) {
            for (y=0; y<height; y++) {
                if (newland[x][y] == true) {
                    map[x][y] = true;
                    numberOfLandTiles++;
                }
            }
        }
    }

    private void addLandmass(int minsize, int maxsize) {
        addLandmass(minsize, maxsize, -1, -1);
    }

    /**
     * Adds land to the first two and last two rows.
     */
    private void addPolarRegions() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < Map.POLAR_HEIGHT; y++) {
                if (map[x][y] == false) {
                    map[x][y] = true;
                    numberOfLandTiles++;
                }
            }
            int limit = height - 1 - Map.POLAR_HEIGHT;
            for (int y = limit; y < height; y++) {
                if (map[x][y] == false) {
                    map[x][y] = true;
                    numberOfLandTiles++;
                }
            }
        }
    }



    /**
     * Removes any 1x1 islands on the map.
     */
    private void cleanMap() {
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (isSingleTile(x, y)) {
                    map[x][y] = false;
                }
            }
        }
    }



    /**
     * Returns <i>true</i> if there are no adjacent land
     * to the given coordinates.
     */
    private boolean isSingleTile(int x, int y) {
        Position p = new Position(x, y);

        for (Direction direction : Direction.values()) {
            Position n = new Position(p, direction);
            if (n.isValid(width, height) && map[n.getX()][n.getY()]) {
                return false;
            }
        }

        return true;
    }



    /**
     * Sets a given map position to land.
     * Calls #gl(int,int) for all valid adjacent map positions, which may
     * recursively call setLand for these.
     */
    private void setLand(int x, int y) {
        if (map[x][y]) {
            return;
        }

        map[x][y] = true;
        numberOfLandTiles++;

        Position p = new Position(x, y);

        for (Direction direction : Direction.longSides) {
            Position n = new Position(p, direction);
            if (n.isValid(width, height)) {
                growLand(n.getX(), n.getY());
            }
        }
    }



    /**
     * Determines, based on position, number of adjacent land tiles and some
     * random factor, whether a given map position should be set to land.
     * This is called for all valid map positions adjacent to a position
     * that has been set to land by #setLand(int,int), and may recursively call
     * setLand for the current position.
     */
    private void growLand(int i, int j) {
        if (map[i][j]) {
            return;
        }

        //Generate a comparison value:
        //Only if the number of adjacent land tiles is bigger than this value,
        //this tile will be set to land.
        //This value is part random, part based on position, that is:
        //-1 in the center of the map, and growing to
        //preferredDistanceToEdge (*2 for pole ends) at the maps edges.
        int r = Utils.randomInt(logger, "Grow", random, 8)
                + Math.max(-1,
                          (1+Math.max(preferredDistanceToEdge-Math.min(i,width-i),
                                    2*preferredDistanceToEdge-Math.min(j, height-j))));

        int sum = 0;
        Position p = new Position(i, j);

        for (Direction direction : Direction.values()) {
            Position n = new Position(p, direction);
            if (n.isValid(width, height) && map[n.getX()][n.getY()]) {
                sum++;
            }
        }

        if (sum > r) {
            setLand(i,j);
        }
    }



}
