package net.sf.freecol.server.generator;

import java.util.*;

import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.FreeColException;



/**
* Creates random maps and sets the starting locations for the players.
*/
public class MapGenerator {

    // The directions a Unit can move to.
    public static final int N = 0,
                            NE = 1,
                            E = 2,
                            SE = 3,
                            S = 4,
                            SW = 5,
                            W = 6,
                            NW = 7;

    // Deltas for moving to adjacent squares. Different due to the
    // isometric map. Starting north and going clockwise.
    private final int[] ODD_DX = {0, 1, 1, 1, 0, 0, -1, 0};
    private final int[] ODD_DY = {-2, -1, 0, 1, 2, 1, 0, -1};
    private final int[] EVEN_DX = {0, 0, 1, 0, 0, -1, -1, -1};
    private final int[] EVEN_DY = {-2, -1, 0, 1, 2, 1, 0, -1};

    private Random random = new Random();
    private Game game;
    private int width;
    private int height;



    public MapGenerator(Game game) {
        this.game = game;
    }





    public Map createMap(Vector players, int width, int height) throws FreeColException {
        this.width = width;
        this.height = height;

        LandGenerator landGenerator = new LandGenerator(width, height);
        boolean[][] landMap = landGenerator.createLandMap();
        
        TerrainGenerator terrainGenerator = new TerrainGenerator(landMap);
        Map map = terrainGenerator.createMap();


        
        createEuropeanUnits(map, width, height, players);

        return map;
    }
    
    
    
    
    
    /**
     * Create two ships, one with a colonist, for each player, and
     * select suitable starting positions.
     * @param players List of players
     * @throws FreeColException if thrown by a called method
     */
    private void createEuropeanUnits(Map map, int width, int height, Vector players) throws FreeColException {
        int[] shipYPos = new int[4];
        for (int i = 0; i < 4; i++)
            shipYPos[i] = 0;
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = (ServerPlayer)players.elementAt(i);
            int y = random.nextInt(height - 20) + 10;
            int x = width - 1;
            while (isAShipTooClose(y, shipYPos)) {
                y = random.nextInt(height - 20) + 10;
            }
            shipYPos[i] = y;
            while (map.getTile(x - 1, y).getType() == Tile.HIGH_SEAS) {
                x--;
            }

            Tile startTile = map.getTile(x,y);
            player.setEntryLocation(startTile);

            int unitType;
            if (player.getNation() == ServerPlayer.DUTCH) {
                unitType = Unit.MERCHANTMAN;
            } else {
                unitType = Unit.CARAVEL;
            }

            Unit unit1 = new Unit(game, startTile, player, unitType, Unit.ACTIVE);
            Unit unit2 = new Unit(game, unit1, player, Unit.HARDY_PIONEER, Unit.SENTRY);
            Unit unit3 = new Unit(game, unit1, player, Unit.VETERAN_SOLDIER, Unit.SENTRY);
            Unit unit4 = new Unit(game, startTile, player, Unit.GALLEON, Unit.ACTIVE);
            Unit unit5 = new Unit(game, unit4, player, Unit.FREE_COLONIST, Unit.SENTRY);
        }
    }


    /**
     * Determine whether a proposed ship starting Y position is "too close"
     * to those already used.
     * @param proposedY Proposed ship starting Y position
     * @param usedYPositions List of already assigned positions
     * @return True if the proposed position is too close
     */
    private boolean isAShipTooClose(int proposedY,
                                        int[] usedYPositions) {
        for (int i = 0; i < 4 && usedYPositions[i] != 0; i++) {
            if (Math.abs(usedYPositions[i] - proposedY) < 8) {
                return true;
            }
        }
        return false;
    }


    /**
    * Gets the position adjacent to a given position, in a given
    * direction.
    *
    * @param position The position
    * @param direction The direction (N, NE, E, etc.)
    * @return Adjacent position
    */
    private Position getAdjacent(Position position, int direction) {
        int x = position.getX() + ((position.getY() & 1) != 0 ?
            ODD_DX[direction] : EVEN_DX[direction]);
        int y = position.getY() + ((position.getY() & 1) != 0 ?
            ODD_DY[direction] : EVEN_DY[direction]);
        return new Position(x, y);
    }

    

    private boolean isValid(int x, int y) {
        return x>=0 && x < width && y >= 0 && y < height;
    }

    private boolean isValid(Position p) {
        return isValid(p.getX(), p.getY());
    }

    protected class TerrainGenerator {

        private static final int DISTANCE_TO_LAND_FROM_HIGH_SEAS = 4;
        private static final int MAX_DISTANCE_TO_EDGE = 12;

        private boolean[][] landMap;
	
	private Random tileType;


        TerrainGenerator(boolean[][] landMap) {
            this.landMap = landMap;
        }

        private int getRandomTileType(int percent) {
          // Do by percent +- 10.
          int min = (percent - 10);
          int max = (percent + 10);
          if (min < 0) min = 0;
          if (max > 99) max = 99;
          int thisValue = tileType.nextInt(max - min) + min;
          thisValue /= 10; // Gives a number from 0-3.
	  int minWoD = 0;
	  int maxWoD = 99;
	  int dryOrWet = tileType.nextInt(maxWoD - minWoD) + minWoD;
	  dryOrWet /= 33;
          switch(thisValue) {
	    case 0: return Tile.ARCTIC;
	    case 1: case 2: switch (dryOrWet) {
	      case 0: return Tile.TUNDRA;
	      case 1: default: return Tile.TUNDRA;
	      case 2: return Tile.MARSH;
	    }
	    case 3: case 4: case 5: case 6: default: switch (dryOrWet) {
	      case 0: return Tile.PLAINS;
	      case 1: default: return Tile.PRAIRIE;
	      case 2: return Tile.SWAMP;
	    }
	    case 7: case 8: case 9: switch (dryOrWet) {
	      case 0: return Tile.GRASSLANDS;
	      case 1: default: return Tile.SAVANNAH;
	      case 2: return Tile.DESERT;
	    }
          }
        }

        public Map createMap() {
            Vector columns = new Vector(width);
	    tileType = new Random();
            for (int i = 0; i < width; i++) {
                Vector v = new Vector(height);
                for (int j = 0; j < height; j++) {
                    Tile t;

                    if (landMap[i][j]) {
                        t = new Tile(game, getRandomTileType(((Math.min(j, height - j) * 100) / height)), i, j);
                        if ((t.getType() != t.ARCTIC) && (tileType.nextInt(3) > 0)) t.setForested(true);
                    } else {
                        t = new Tile(game, Tile.OCEAN, i, j);
                    }

                    v.add(t);
                }
                columns.add(v);
            }

            Map map = new Map(game, columns);

            createHighSeas(map);

            return map;
        }



        private void createHighSeas(Map map) {
            for (int y = 0; y < height; y++) {
                for (int x=0; x<MAX_DISTANCE_TO_EDGE && !map.isLandWithinDistance(x, y, DISTANCE_TO_LAND_FROM_HIGH_SEAS); x++) {
                        map.getTile(x, y).setType(Tile.HIGH_SEAS);
                }

                for (int x=1; x<=MAX_DISTANCE_TO_EDGE && !map.isLandWithinDistance(width-x, y, DISTANCE_TO_LAND_FROM_HIGH_SEAS); x++) {
                        map.getTile(width-x, y).setType(Tile.HIGH_SEAS);
                }
            }
        }


    }


    protected class LandGenerator {


        private static final int PREFERRED_DISTANCE_TO_EDGE = 4;
        private static final int C = 2;

        private boolean[][] map;
        private boolean[][] visited;

        private int numberOfLandTiles = 0;


        LandGenerator(int width, int height) {
            map = new boolean[width][height];
            visited = new boolean[width][height];
        }





        boolean[][] createLandMap() {
            int x;
            int y;

            int minLandMass = 25;
            int minimumNumberOfTiles = (map.length * map[0].length * minLandMass - 2 * PREFERRED_DISTANCE_TO_EDGE) / 100;
            while (numberOfLandTiles < minimumNumberOfTiles) {
                do {
                    x=((int) (Math.random() * (map.length-PREFERRED_DISTANCE_TO_EDGE*2))) + PREFERRED_DISTANCE_TO_EDGE;
                    y=((int) (Math.random() * (map[0].length-PREFERRED_DISTANCE_TO_EDGE*4))) + PREFERRED_DISTANCE_TO_EDGE * 2;
                } while (map[x][y]);

                map[x][y] = true;
                setLand(x,y);
            }

            cleanMap();

            return map;
        }


        private boolean hasLandMass(int minLandMass) {
            return (numberOfLandTiles * 100) / (map.length * map[0].length) >= minLandMass;
        }

        private void cleanMap() {
            for (int y=0; y<map[0].length; y++) {
                for (int x=0; x<map.length; x++) {
                    if (isSingleTile(x, y)) {
                        map[x][y] = false;
                    }
                }
            }
        }


        private boolean isSingleTile(int x, int y) {
            Position p = new Position(x, y);

            for (int i=1; i<8;i+=2) {
                Position n = getAdjacent(p, i);
                if (isValid(n) && map[n.getX()][n.getY()]) {
                    return false;
                }
            }

            return true;
        }


        private int getEdge(int x, int y) {
            int u = Math.max(PREFERRED_DISTANCE_TO_EDGE-Math.min(x, map.length-x),
                             2*PREFERRED_DISTANCE_TO_EDGE-Math.min(y, map[0].length-y))
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

            for (int i=1; i<8;i+=2) {
                Position n = getAdjacent(p, i);
                if (isValid(n)) {
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

            for (int k=0; k<8;k++) {
                Position n = getAdjacent(p, k);
                if (isValid(n) && map[n.getX()][n.getY()]) {
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


}
