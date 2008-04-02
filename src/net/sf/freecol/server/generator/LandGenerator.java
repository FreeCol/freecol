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

    private static final int C = 2;

    public static final int POLAR_WIDTH = 2;

    private static final Direction[] adjacentDirections = new Direction[] {
        Direction.NE, Direction.SE, Direction.SW, Direction.NW };

    private final MapGeneratorOptions mapGeneratorOptions;
     
    private boolean[][] map;
    private boolean[][] visited;
     
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
    public boolean[][] importLandMap(Game game) {
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
        visited = new boolean[width][height];
        numberOfLandTiles = 0;
         
        int x;
        int y;

        int minLandMass = mapGeneratorOptions.getLandMass();

        int minimumNumberOfTiles = (map.length * map[0].length * minLandMass - 2 * preferredDistanceToEdge) / 100;
        while (numberOfLandTiles < minimumNumberOfTiles) {
            do {
                x=((int) (Math.random() * (map.length-preferredDistanceToEdge*2))) + preferredDistanceToEdge;
                y=((int) (Math.random() * (map[0].length-preferredDistanceToEdge*4))) + preferredDistanceToEdge * 2;
            } while (map[x][y]);

            map[x][y] = true;
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
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < POLAR_WIDTH; y++) {
                map[x][y] = true;
            }
            int limit = map[0].length - 1 - POLAR_WIDTH;
            for (int y = limit; y < map[0].length; y++) {
                map[x][y] = true;
            }
        }
    }


    /**
     * Removes any 1x1 islands on the map.
     */
    private void cleanMap() {
        for (int y=0; y<map[0].length; y++) {
            for (int x=0; x<map.length; x++) {
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


    private int getEdge(int x, int y) {
        int u = Math.max(preferredDistanceToEdge-Math.min(x, map.length-x),
                         2*preferredDistanceToEdge-Math.min(y, map[0].length-y))
            + C;

        return (u>0 ? u : 0);
    }


    private void setLand(int x, int y) {
        if (visited[x][y]) {
            return;
        }

        visited[x][y] = true;
        numberOfLandTiles++;

        Position p = new Position(x, y);

        for (Direction direction : adjacentDirections) {
            Position n = Map.getAdjacent(p, direction);
            if (Map.isValid(n, width, height)) {
                gl(n.getX(), n.getY());
            }
        }
    }


    private void gl(int i, int j) {
        if (visited[i][j]) {
            return;
        }

        int r = ((int) (Math.random() * 8)) + 1 - C + getEdge(i,j);

        int sum = -1;

        Position p = new Position(i, j);

        for (Direction direction : Direction.values()) {
            Position n = Map.getAdjacent(p, direction);
            if (Map.isValid(n, width, height) && map[n.getX()][n.getY()]) {
                sum++;
            }
        }

        if (sum >= r) {
            if (!map[i][j]) {
                map[i][j] = true;
                setLand(i,j);
            }
        }
    }
}
