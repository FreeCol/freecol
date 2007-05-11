package net.sf.freecol.server.generator;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Map.Position;

/**
 * Class for making a <code>Map</code> based upon a land map.
 */
public class TerrainGenerator {
    private static final Logger logger = Logger.getLogger(TerrainGenerator.class.getName());
    
    private final MapGeneratorOptions mapGeneratorOptions;
    private final Random random = new Random();


    /**
     * Creates a new <code>TerrainGenerator</code>.
     * 
     * @see #createMap
     */
    public TerrainGenerator(MapGeneratorOptions mapGeneratorOptions) {
        this.mapGeneratorOptions = mapGeneratorOptions;
    }

    
    /**
     * Creates a <code>Map</code> for the given <code>Game</code>.
     * 
     * The <code>Map</code> is added to the <code>Game</code> after
     * it is created.
     * 
     * @param game The game. 
     * @param landMap Determines wether there should be land
     *                or ocean on a given tile. This array also
     *                specifies the size of the map that is going
     *                to be created.
     * @see Map
     */
    public void createMap(Game game, boolean[][] landMap) {
        final int width = landMap.length;
        final int height = landMap[0].length;
        
        Vector<Vector<Tile>> columns = new Vector<Vector<Tile>>(getMapGeneratorOptions().getWidth());
        for (int i = 0; i < width; i++) {
            Vector<Tile> v = new Vector<Tile>(height);
            for (int j = 0; j < height; j++) {
                Tile t;

                if (landMap[i][j]) {
                    t = new Tile(game, getRandomTileType(((Math.min(j, height - j) * 200) / height)), i, j);

                    if ((t.getType() != Tile.ARCTIC) && 
                        (random.nextInt(100) < getMapGeneratorOptions().getPercentageOfForests())) {
                        t.setForested(true);
                    } else if (t.getType() != Tile.ARCTIC) {
                        int k = random.nextInt(16);
                        if (k < 1) {
                            t.setAddition(Tile.ADD_MOUNTAINS);
                        } else if (k < 2) {
                            t.setAddition(Tile.ADD_HILLS);
                        }
                    }

                    // Terrain bonus:
                    if (random.nextInt(100) < getMapGeneratorOptions().getPercentageOfBonusTiles()) {
                        t.setBonus(true);
                    }
                } else {
                    t = new Tile(game, Tile.OCEAN, i, j);

                    // Terrain bonus:
                    int adjacentLand = 0;
                    for (int k=0; k<8; k++) {
                        Position mp = Map.getAdjacent(t.getPosition(), k);
                        final boolean valid = Map.isValid(mp, width,
                                height);
                        if (valid && landMap[mp.getX()][mp.getY()]) {
                            adjacentLand++;
                        }
                    }

                    if (adjacentLand > 1 && random.nextInt(10 - adjacentLand) == 0) {
                        t.setBonus(true);
                    }
                }

                v.add(t);
            }
            columns.add(v);
        }

        Map map = new Map(game, columns);

        createHighSeas(map);
        createMountains(map);
        createRivers(map);

        game.setMap(map);
    }
    
    /**
     * Gets the <code>MapGeneratorOptions</code>.
     * @return The <code>MapGeneratorOptions</code> being used
     *      when creating terrain.
     */
    private MapGeneratorOptions getMapGeneratorOptions() {
        return mapGeneratorOptions;
    }

    /**
     * Gets a random tile type based on the given percentage.
     *
     * @param percent The location of the tile, where 100% is the center on
     *        the y-axis and 0% is on the top/bottom of the map.
     */
    private int getRandomTileType(int percent) {
        int thisValue = Math.max(((percent - random.nextInt(20) - 1)) / 10, 0);

        int minWoD = 0;
        int maxWoD = 99;
        int dryOrWet = random.nextInt(maxWoD - minWoD) + minWoD;
        dryOrWet /= 33;
        switch(thisValue) {
        case 0: return Tile.ARCTIC;
        case 1: case 2: switch (dryOrWet) {
            case 0: return Tile.TUNDRA;
            case 1: default: return Tile.TUNDRA;
            case 2: return Tile.MARSH;
        }
        case 3: case 4: case 5: default: switch (dryOrWet) {
            case 0: return Tile.DESERT;
            case 1: default: return Tile.PLAINS;
            case 2: return Tile.PRAIRIE;
        }
        case 6: case 7: case 8: case 9: switch (dryOrWet) {
            case 0: return Tile.GRASSLANDS;
            case 1: default: return Tile.SAVANNAH;
            case 2: return Tile.SWAMP;
        }
        }
    }

    /**
     * Places "high seas"-tiles on the border of the given map.
     * @param map The <code>Map</code> to create high seas on.
     */
    private void createHighSeas(Map map) {
        createHighSeas(
            map,
            getMapGeneratorOptions().getDistLandHighSea(),
            getMapGeneratorOptions().getMaxDistToEdge()
        );
    }
    
    /**
     * Places "high seas"-tiles on the border of the given map.
     * 
     * All other tiles previously of type {@link Tile#HIGH_SEAS}
     * will be set to {@link Tile#OCEAN}.
     * 
     * @param map The <code>Map</code> to create high seas on.
     * @param distToLandFromHighSeas The distance between the land
     *      and the high seas (given in tiles).
     * @param maxDistanceToEdge The maximum distance a high sea tile
     *      can have from the edge of the map.
     */
    public static void determineHighSeas(Map map,
            int distToLandFromHighSeas,
            int maxDistanceToEdge) {
        for (Tile t : map.getAllTiles()) {
            if (t.getType() == Tile.HIGH_SEAS) {
                t.setType(Tile.OCEAN);
            }
        }
        createHighSeas(map, distToLandFromHighSeas, maxDistanceToEdge);
    }
    
    /**
     * Places "high seas"-tiles on the border of the given map.
     * 
     * @param map The <code>Map</code> to create high seas on.
     * @param distToLandFromHighSeas The distance between the land
     *      and the high seas (given in tiles).
     * @param maxDistanceToEdge The maximum distance a high sea tile
     *      can have from the edge of the map.
     */
    private static void createHighSeas(Map map,
            int distToLandFromHighSeas,
            int maxDistanceToEdge) {
        
        if (distToLandFromHighSeas < 0
                || maxDistanceToEdge < 0) {
            throw new IllegalArgumentException("The integer arguments cannot be negative.");
        }
        
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x=0; x<maxDistanceToEdge && !map.isLandWithinDistance(x, y, distToLandFromHighSeas); x++) {
                if (map.isValid(x, y)) {
                    map.getTile(x, y).setType(Tile.HIGH_SEAS);
                }
            }

            for (int x=1; x<=maxDistanceToEdge && !map.isLandWithinDistance(map.getWidth()-x, y, distToLandFromHighSeas); x++) {
                if (map.isValid(map.getWidth()-x, y)) {
                    map.getTile(map.getWidth()-x, y).setType(Tile.HIGH_SEAS);
                }
            }
        }
    }

    /**
     * Creates mountain ranges on the given map.  The number and size
     * of mountain ranges depends on the map size.
     *
     * @param map The map to use.
     */
    private void createMountains(Map map) {
        int maximumLength = Math.max(getMapGeneratorOptions().getWidth(), getMapGeneratorOptions().getHeight()) / 10;
        int number = getMapGeneratorOptions().getNumberOfMountainTiles();
        int counter = 0;
        logger.info("Number of land tiles is " + getMapGeneratorOptions().getLand() +
                    ", number of mountain tiles is " + number);
        logger.fine("Maximum length of mountain ranges is " + maximumLength);
        for (int tries = 0; tries < 100; tries++) {
            if (counter < number) {
                Position p = map.getRandomLandPosition();
                if (p != null
                        && map.getTile(p).isLand()) {
                    int direction = random.nextInt(8);
                    int length = maximumLength - random.nextInt(maximumLength/2);
                    logger.info("Direction of mountain range is " + direction +
                            ", length of mountain range is " + length);
                    for (int index = 0; index < length; index++) {
                        p = Map.getAdjacent(p, direction);
                        Tile t = map.getTile(p);
                        if (t != null && t.isLand()) {
                            t.setAddition(Tile.ADD_MOUNTAINS);
                            counter++;
                            Iterator<Position> it = map.getCircleIterator(p, false, 1);
                            while (it.hasNext()) {
                                t = map.getTile(it.next());
                                if (t.isLand() &&
                                        t.getAddition() != Tile.ADD_MOUNTAINS) {
                                    int r = random.nextInt(8);
                                    if (r == 0) {
                                        t.setAddition(Tile.ADD_MOUNTAINS);
                                        counter++;
                                    } else if (r > 2) {
                                        t.setAddition(Tile.ADD_HILLS);
                                    }
                                }
                            }
                        }
                    }
                    // break;
                }
            }
        }
        logger.info("Added " + counter + " mountain tiles.");
    }
    
    /**
     * Creates rivers on the given map. The number of rivers depends
     * on the map size.
     *
     * @param map The map to create rivers on.
     */
    private void createRivers(Map map) {
        int number = getMapGeneratorOptions().getNumberOfRivers();
        int counter = 0;
        Hashtable<Position, River> riverMap = new Hashtable<Position, River>();

        for (int i = 0; i < number; i++) {
            River river = new River(map, riverMap);
            for (int tries = 0; tries < 100; tries++) {
                Position position = new Position(random.nextInt(map.getWidth()),
                                                 random.nextInt(map.getHeight()));

                if (riverMap.get(position) == null) {
                    if (river.flowFromSource(position)) {
                        logger.fine("Created new river with length " + river.getLength());
                        counter++;
                        break;
                    } else {
                        logger.fine("Failed to generate river.");
                    }
                }
            }
        }

        logger.info("Created " + counter + " rivers of maximum " + number + ".");
    }
}
