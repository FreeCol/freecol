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

package net.sf.freecol.server.generator;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;

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

    public final static int POLAR_HEIGHT = 2;

    private final static Direction[] adjacentDirections = new Direction[] {
        Direction.NE, Direction.SE, Direction.SW, Direction.NW };

    private final MapGeneratorOptions mapGeneratorOptions;
     
    private boolean[][] map;
     
    private int width;
    private int height;

    private int preferredDistanceToEdge;
    private int numberOfLandTiles;



    /**
     * Creates a new <code>LandGenerator</code>.
     * 
     * @param mapGeneratorOptions The options to be
     *         used when creating a land map.
     * @see #createLandMap
     */
    public LandGenerator(MapGeneratorOptions mapGeneratorOptions) {
        this.mapGeneratorOptions = mapGeneratorOptions;
    }



    /**
     * Imports the land map from the given <code>Game</code>.
     * 
     * @param game The <code>Game</code> to get the land map from.
     * @return An array where <i>true</i> means land 
     * and <i>false</i> means ocean.
     */
    public static boolean[][] importLandMap(Game game) {
        boolean[][] map = new boolean[game.getMap().getWidth()][game.getMap().getHeight()];
        for (int i=0; i<map.length; i++) {
            for (int j=0; j<map[0].length; j++) {
                map[i][j] = game.getMap().getTile(i, j).isLand();
            }
        }
        return map;
    }



    /**
     * Creates a new land map.
     * 
     * @return An array where <i>true</i> means land 
     * and <i>false</i> means ocean.
     */
    public boolean[][] createLandMap() {
        width = mapGeneratorOptions.getWidth();
        height = mapGeneratorOptions.getHeight();         
        preferredDistanceToEdge = mapGeneratorOptions.getPrefDistToEdge();
        map = new boolean[width][height];
        numberOfLandTiles = 0;
         
        int x;
        int y;

        int minLandMass = mapGeneratorOptions.getLandMass();

        int minimumNumberOfTiles = (width * height * minLandMass) / 100;
        while (numberOfLandTiles < minimumNumberOfTiles) {
            do {
                x=((int) (Math.random() * (width-preferredDistanceToEdge*2))) + preferredDistanceToEdge;
                y=((int) (Math.random() * (height-preferredDistanceToEdge*2))) + preferredDistanceToEdge;
            } while (map[x][y]);

            setLand(x,y);
        }

        cleanMap();
        createPolarRegions();

        return map;
    }



    /**
     * Adds land to the first two and last two rows. 
     */
    private void createPolarRegions() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < POLAR_HEIGHT; y++) {
                map[x][y] = true;
            }
            int limit = height - 1 - POLAR_HEIGHT;
            for (int y = limit; y < height; y++) {
                map[x][y] = true;
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

        for (Direction direction : adjacentDirections) {
            Position n = Map.getAdjacent(p, direction);
            if (Map.isValid(n, width, height) && map[n.getX()][n.getY()]) {
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

        for (Direction direction : adjacentDirections) {
            Position n = Map.getAdjacent(p, direction);
            if (Map.isValid(n, width, height)) {
                gl(n.getX(), n.getY());
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
    private void gl(int i, int j) {
        if (map[i][j]) {
            return;
        }

        //Generate a comparison value:
        //Only if the number of adjacent land tiles is bigger than this value,
        //this tile will be set to land.
        //This value is part random, part based on position, that is:
        //-1 in the center of the map, and growing to
        //preferredDistanceToEdge (*2 for pole ends) at the maps edges.
        int r = ((int) (Math.random() * 8))
                + Math.max(-1,
                          (1+Math.max(preferredDistanceToEdge-Math.min(i,width-i),
                                    2*preferredDistanceToEdge-Math.min(j, height-j))));

        int sum = 0;
        Position p = new Position(i, j);

        for (Direction direction : Direction.values()) {
            Position n = Map.getAdjacent(p, direction);
            if (Map.isValid(n, width, height) && map[n.getX()][n.getY()]) {
                sum++;
            }
        }

        if (sum > r) {
            setLand(i,j);
        }
    }
    
    
    
}
