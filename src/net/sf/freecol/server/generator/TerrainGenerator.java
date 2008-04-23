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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Region.RegionType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.server.model.ServerRegion;

/**
 * Class for making a <code>Map</code> based upon a land map.
 */
public class TerrainGenerator {

    private static final Logger logger = Logger.getLogger(TerrainGenerator.class.getName());
    
    public static final int PACIFIC_SCORE_VALUE = 100;

    private final MapGeneratorOptions mapGeneratorOptions;

    private final Random random = new Random();

    private TileType ocean = FreeCol.getSpecification().getTileType("model.tile.ocean");

    private ArrayList<TileType> terrainTileTypes = null;

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
     * @param landMap Determines whether there should be land
     *                or ocean on a given tile. This array also
     *                specifies the size of the map that is going
     *                to be created.
     * @see Map
     */
    public void createMap(Game game, boolean[][] landMap) {
        createMap(game, null, landMap);
    }
    
    /**
     * Creates a <code>Map</code> for the given <code>Game</code>.
     * 
     * The <code>Map</code> is added to the <code>Game</code> after
     * it is created.
     * 
     * @param game The game. 
     * @param importGame The game to import information form.
     * @param landMap Determines whether there should be land
     *                or ocean on a given tile. This array also
     *                specifies the size of the map that is going
     *                to be created.
     * @see Map
     */
    public void createMap(Game game, Game importGame, boolean[][] landMap) {
        final int width = landMap.length;
        final int height = landMap[0].length;
        
        final boolean importTerrain = (importGame != null) && getMapGeneratorOptions().getBoolean(MapGeneratorOptions.IMPORT_TERRAIN);
        final boolean importBonuses = (importGame != null) && getMapGeneratorOptions().getBoolean(MapGeneratorOptions.IMPORT_BONUSES);

        boolean mapHasLand = false;
        Tile[][] tiles = new Tile[width][height];
        for (int y = 0; y < height; y++) {
            int latitude = (Math.min(y, (height-1) - y) * 200) / height; // lat=0 for poles; lat=100 for equator
            for (int x = 0; x < width; x++) {
                if (landMap[x][y]) {
                    mapHasLand = true;
                }
                Tile t;
                if (importTerrain && importGame.getMap().isValid(x, y)) {
                    Tile importTile = importGame.getMap().getTile(x, y);
                    if (importTile.isLand() == landMap[x][y]) {
                        t = new Tile(game, importTile.getType(), x, y);
                        // TileItemContainer copies everything including Resource unless importBonuses == false
                        if (importTile.getTileItemContainer() != null) {
                            t.getTileItemContainer().copyFrom(importTile.getTileItemContainer(), importBonuses);
                        }
                        if (!importBonuses) {
                            // In which case, we may add a Bonus Resource
                            perhapsAddBonus(t, landMap);
                        }
                    } else {
                        t = createTile(game, x, y, landMap, latitude);
                    }
                } else {
                    t = createTile(game, x, y, landMap, latitude);
                }
                tiles[x][y] = t;
            }
        }

        Map map = new Map(game, tiles);
        game.setMap(map);

        if (!importTerrain) {
            createRegions(map);
            createHighSeas(map);
	    if (mapHasLand) {
		createMountains(map);
		createRivers(map);
	    }
        }
    }

    private Tile createTile(Game game, int x, int y, boolean[][] landMap, int latitude) {
        
        Tile t;
        if (landMap[x][y]) {
            t = new Tile(game, getRandomLandTileType(latitude), x, y);
        } else {
            t = new Tile(game, ocean, x, y);
        }
        perhapsAddBonus(t, landMap);
        
        return t;
    }

    /**
     * Adds a terrain bonus with a probability determined by the
     * <code>MapGeneratorOptions</code>.
     * 
     * @param t The Tile.
     * @param landMap The landMap.
     */
    private void perhapsAddBonus(Tile t, boolean[][] landMap) {
        if (t.isLand()) {
            if (random.nextInt(100) < getMapGeneratorOptions().getPercentageOfBonusTiles()) {
                // Create random Bonus Resource
                t.setResource(t.getType().getRandomResourceType());
            }
        } else {
            int adjacentLand = 0;
            for (Direction direction : Direction.values()) {
                Position mp = Map.getAdjacent(t.getPosition(), direction);
                final boolean valid = Map.isValid(mp, landMap.length, landMap[0].length);
                if (valid && landMap[mp.getX()][mp.getY()]) {
                    adjacentLand++;
                }
            }

            if (adjacentLand > 1 && random.nextInt(10 - adjacentLand) == 0) {
                t.setResource(t.getType().getRandomResourceType());
            }
        }
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
     * Gets a random land tile type based on the given percentage.
     *
     * @param latitudePercent The location of the tile relative to the north/south poles and equator, 
     *        100% is the mid-section of the map (equator) 
     *        0% is on the top/bottom of the map (poles).
     */
    private TileType getRandomLandTileType(int latitudePercent) {
        // decode options
        final int forestChance = getMapGeneratorOptions().getPercentageOfForests();
        final int humidityPreference = getMapGeneratorOptions().getHumidity();
        final int temperaturePreference = getMapGeneratorOptions().getTemperature();
        
        // create the main list of TileTypes the first time, and reuse it afterwards
        if (terrainTileTypes==null) {
            terrainTileTypes = new ArrayList<TileType>();
            for (TileType tileType : FreeCol.getSpecification().getTileTypeList()) {
                if (tileType.getId().equals("model.tile.hills") ||
                    tileType.getId().equals("model.tile.mountains") ||
                    tileType.isWater()) {
                    // do not generate hills or mountains at this time
                    // they are created separately
                    continue;
                }
                terrainTileTypes.add(tileType);
            }
        }

        // temperature calculation
        int poleTemperature = -20;
        int equatorTemperature= 40;
        if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_COLD) {
            poleTemperature = -20;
            equatorTemperature = 25;
        } else if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_CHILLY) {
            poleTemperature = -20;
            equatorTemperature = 30;
        } else if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_TEMPERATE) {
            poleTemperature = -10;
            equatorTemperature = 35;
        } else if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_WARM) {
            poleTemperature = -5;
            equatorTemperature = 40;
        } else if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_HOT) {
            poleTemperature = 0;
            equatorTemperature = 40;
        }
        int temperatureRange = equatorTemperature-poleTemperature;
        int localeTemperature = poleTemperature + latitudePercent*temperatureRange/100;
        int temperatureDeviation = 7; // +/- 7 degrees randomization
        localeTemperature += random.nextInt(temperatureDeviation*2)-temperatureDeviation;
        if (localeTemperature>40)
            localeTemperature = 40;
        if (localeTemperature<-20)
            localeTemperature = -20;
        
        // humidity calculation
        int averageHumidity = 50;
        if (humidityPreference==MapGeneratorOptions.HUMIDITY_VERY_DRY) {
            averageHumidity = 25;
        } else if (humidityPreference==MapGeneratorOptions.HUMIDITY_DRY) {
            averageHumidity = 35;
        } else if (humidityPreference==MapGeneratorOptions.HUMIDITY_NORMAL) {
            averageHumidity = 40;
        } else if (humidityPreference==MapGeneratorOptions.HUMIDITY_WET) {
            averageHumidity = 55;
        } else if (humidityPreference==MapGeneratorOptions.HUMIDITY_VERY_WET) {
            averageHumidity = 60;
        }
        int localeHumidity = averageHumidity;
        int humidityDeviation = 20; // +/- 20% randomization
        localeHumidity += random.nextInt(humidityDeviation*2) - humidityDeviation;
        if (localeHumidity<0) 
            localeHumidity = 0;
        if (localeHumidity>100)
            localeHumidity = 100;
        
        // initialize list of candidates with all terrain TileTypes
        ArrayList<TileType> candidateTileTypes = new ArrayList<TileType>();
        candidateTileTypes.addAll(terrainTileTypes);
        
        // remove those that do not match the current temperature
        Iterator<TileType> it = candidateTileTypes.iterator();
        while (it.hasNext()) {
            TileType t = it.next();
            if (!t.withinRange(TileType.RangeType.TEMPERATURE, localeTemperature)) {
                it.remove();
            }
        }
        if (candidateTileTypes.size() == 1) {
            return candidateTileTypes.get(0);
        } else if (candidateTileTypes.size()==0) {
            throw new RuntimeException("No TileType for temperature==" + localeTemperature );
        }
        
        // remove those that do not match the current humidity
        it = candidateTileTypes.iterator();
        while (it.hasNext()) {
            TileType t = it.next();
            if (!t.withinRange(TileType.RangeType.HUMIDITY, localeHumidity)) {
                it.remove();
            }
        }
        if (candidateTileTypes.size() == 1) {
            return candidateTileTypes.get(0);
        } else if (candidateTileTypes.size()==0) {
            throw new RuntimeException("No TileType for temperature==" + localeTemperature 
                    +" and humidity==" + localeHumidity);
        }
 
        // Choose based on forested/unforested
        boolean forested = random.nextInt(100) < forestChance;
        it = candidateTileTypes.iterator();
        while (it.hasNext()) {
            TileType t = it.next();
            if (t.isForested() != forested) {
                it.remove();
            }
        }
        if (candidateTileTypes.size() == 1) {
            return candidateTileTypes.get(0);
        } else if (candidateTileTypes.size()==0) {
            throw new RuntimeException("No TileType for temperature==" + localeTemperature 
                    +" and humidity==" + localeHumidity + " and forested=="+forested);
        }
        
        // All scoped, if none have been selected by elimination, randomly choose one
        TileType chosen = null;
        if (chosen == null) {
            chosen = candidateTileTypes.get(random.nextInt(candidateTileTypes.size()));
        }
        return chosen;
    }


    /**
     * Creates map regions in the given Map. At the moment, the land
     * is divided into three by three regions, and the ocean into two
     * by two regions.
     *
     * @param map a <code>Map</code> value
     */
    private void createRegions(Map map) {

        Game game = map.getGame();

        ServerRegion PACIFIC =
            new ServerRegion(game, "model.region.pacific", RegionType.OCEAN);
        ServerRegion NORTH_PACIFIC =
            new ServerRegion(game, "model.region.northPacific", RegionType.OCEAN, PACIFIC);
        ServerRegion SOUTH_PACIFIC =
            new ServerRegion(game, "model.region.southPacific", RegionType.OCEAN, PACIFIC);
        ServerRegion ATLANTIC =
            new ServerRegion(game, "model.region.atlantic", RegionType.OCEAN);
        ServerRegion NORTH_ATLANTIC =
            new ServerRegion(game, "model.region.northAtlantic", RegionType.OCEAN, ATLANTIC);
        ServerRegion SOUTH_ATLANTIC =
            new ServerRegion(game, "model.region.southAtlantic", RegionType.OCEAN, ATLANTIC);

        ServerRegion CENTER =
            new ServerRegion(game, "model.region.center", RegionType.LAND);
        ServerRegion NORTH =
            new ServerRegion(game, "model.region.north", RegionType.LAND);
        ServerRegion SOUTH =
            new ServerRegion(game, "model.region.south", RegionType.LAND);
        ServerRegion EAST =
            new ServerRegion(game, "model.region.east", RegionType.LAND);
        ServerRegion WEST =
            new ServerRegion(game, "model.region.west", RegionType.LAND);
        ServerRegion NORTH_EAST =
            new ServerRegion(game, "model.region.northEast", RegionType.LAND);
        ServerRegion NORTH_WEST =
            new ServerRegion(game, "model.region.northWest", RegionType.LAND);
        ServerRegion SOUTH_EAST =
            new ServerRegion(game, "model.region.southEast", RegionType.LAND);
        ServerRegion SOUTH_WEST =
            new ServerRegion(game, "model.region.southWest", RegionType.LAND);

        for (ServerRegion region : new ServerRegion[] { PACIFIC, NORTH_PACIFIC, SOUTH_PACIFIC,
                                                        ATLANTIC, NORTH_ATLANTIC, SOUTH_ATLANTIC,
                                                        NORTH_WEST, NORTH, NORTH_EAST,
                                                        WEST, CENTER, EAST,
                                                        SOUTH_WEST, SOUTH, SOUTH_EAST } ) {
            if (region.getType() == RegionType.OCEAN) {
                region.setPrediscovered(true);
            } else {
                region.setDiscoverable(true);
            }
            map.setRegion(region);
        }
        PACIFIC.setDiscoverable(true);
        PACIFIC.setScoreValue(PACIFIC_SCORE_VALUE);


        int halfHeight = map.getHeight()/2;
        int halfWidth = map.getWidth()/2;
        int thirdHeight = map.getHeight()/3;
        int twoThirdHeight = 2 * thirdHeight;
        int thirdWidth = map.getWidth()/3;
        int twoThirdWidth = 2 * thirdWidth;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (map.isValid(x, y)) {
                    Tile tile = map.getTile(x, y);
                    if (tile.isLand()) {
                        if (y < thirdHeight) {
                            if (x < thirdWidth) {
                                NORTH_WEST.addTile(tile);
                            } else if (x < twoThirdWidth) {
                                NORTH.addTile(tile);
                            } else {
                                NORTH_EAST.addTile(tile);
                            }
                        } else if (y < twoThirdHeight) {
                            if (x < thirdWidth) {
                                WEST.addTile(tile);
                            } else if (x < twoThirdWidth) {
                                CENTER.addTile(tile);
                            } else {
                                EAST.addTile(tile);
                            }
                        } else {
                            if (x < thirdWidth) {
                                SOUTH_WEST.addTile(tile);
                            } else if (x < twoThirdWidth) {
                                SOUTH.addTile(tile);
                            } else {
                                SOUTH_EAST.addTile(tile);
                            }
                        }
                    } else {
                        if (y < halfHeight) {
                            if (x < halfWidth) {
                                NORTH_PACIFIC.addTile(tile);
                            } else {
                                NORTH_ATLANTIC.addTile(tile);
                            }
                        } else {
                            if (x < halfWidth) {
                                SOUTH_PACIFIC.addTile(tile);
                            } else {
                                SOUTH_ATLANTIC.addTile(tile);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Places "high seas"-tiles on the border of the given map.
     * @param map The <code>Map</code> to create high seas on.
     */
    private void createHighSeas(Map map) {
        createHighSeas(map,
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
        
        // lookup ocean TileTypes from xml specification
        TileType ocean = null, highSeas = null;
        for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
            if (t.isWater()) {
                if (t.canSailToEurope()) {
                    if (highSeas == null) {
                        highSeas = t;
                        if (ocean != null) {
                            break;
                        }
                    }
                } else {
                    if (ocean == null) {
                        ocean = t;
                        if (highSeas != null) {
                            break;
                        }
                    }
                }
            }
        }
        if (highSeas == null || ocean == null) {
            throw new RuntimeException("Both Ocean and HighSeas TileTypes must be defined");
        }
        
        // reset all water tiles to default ocean type, and remove regions
        for (Tile t : map.getAllTiles()) {
            t.setRegion(null);
            if (t.getType() == highSeas) {
                t.setType(ocean);
            }
        }
        
        // recompute the new high seas layout
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

        TileType highSeas = null;
        for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
            if (t.isWater()) {
                if (t.canSailToEurope()) {
                    highSeas = t;
                    break;
                }
            }
        }
        if (highSeas == null) {
            throw new RuntimeException("HighSeas TileType is defined by the 'sail-to-europe' attribute");
        }

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x=0; x<maxDistanceToEdge && 
                          x<map.getWidth() && 
                          !map.isLandWithinDistance(x, y, distToLandFromHighSeas); x++) {
                if (map.isValid(x, y)) {
                    map.getTile(x, y).setType(highSeas);
                }
            }

            for (int x=1; x<=maxDistanceToEdge && 
                          x<=map.getWidth()-1 &&
                          !map.isLandWithinDistance(map.getWidth()-x, y, distToLandFromHighSeas); x++) {
                if (map.isValid(map.getWidth()-x, y)) {
                    map.getTile(map.getWidth()-x, y).setType(highSeas);
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
        float randomHillsRatio = 0.5f;
        // 50% of user settings will be allocated for random hills here and there
        // the rest will be allocated for large mountain ranges
        int maximumLength = Math.max(getMapGeneratorOptions().getWidth(), getMapGeneratorOptions().getHeight()) / 10;
        int number = (int)(getMapGeneratorOptions().getNumberOfMountainTiles()*(1-randomHillsRatio));
        logger.info("Number of land tiles is " + getMapGeneratorOptions().getLand() +
                    ", number of mountain tiles is " + number);
        logger.fine("Maximum length of mountain ranges is " + maximumLength);
        
        // lookup the resources from specification
        TileType hills = FreeCol.getSpecification().getTileType("model.tile.hills");
        TileType mountains = FreeCol.getSpecification().getTileType("model.tile.mountains");
        if (hills == null || mountains == null) {
            throw new RuntimeException("Both Hills and Mountains TileTypes must be defined");
        }
        
        // generate the mountain ranges
        int counter = 0;
        nextTry: for (int tries = 0; tries < 100; tries++) {
            if (counter < number) {
                Position p = map.getRandomLandPosition();
                if (p == null) {
                    // this can only happen if the map contains no land
                    return;
                }
                Tile startTile = map.getTile(p);
                if (startTile.getType() == hills || startTile.getType() == mountains) {
                    // already a high ground
                    continue;
                }

                // do not start a mountain range too close to another
                Iterator<Position> it = map.getCircleIterator(p, true, 3);
                while (it.hasNext()) {
                    if (map.getTile(it.next()).getType() == mountains) {
                        continue nextTry;
                    }
                }

                // do not add a mountain range too close to the ocean/lake
                // this helps with good locations for building colonies on shore
                it = map.getCircleIterator(p, true, 2);
                while (it.hasNext()) {
                    if (!map.getTile(it.next()).isLand()) {
                        continue nextTry;
                    }
                }

                ServerRegion mountainRegion = new ServerRegion(map.getGame(),
                                                               "model.region.mountain" + tries,
                                                               Region.RegionType.MOUNTAIN,
                                                               startTile.getRegion());
                mountainRegion.setDiscoverable(true);
                mountainRegion.setClaimable(true);
                // TODO: make this depend on size, or other feature?
                mountainRegion.setScoreValue(10);
                map.setRegion(mountainRegion);
                Direction direction = map.getRandomDirection();
                int length = maximumLength - random.nextInt(maximumLength/2);
                logger.info("Direction of mountain range is " + direction +
                        ", length of mountain range is " + length);
                for (int index = 0; index < length; index++) {
                    p = Map.getAdjacent(p, direction);
                    Tile nextTile = map.getTile(p);
                    if (nextTile == null || !nextTile.isLand()) 
                        continue;
                    nextTile.setType(mountains);
                    mountainRegion.addTile(nextTile);
                    counter++;
                    it = map.getCircleIterator(p, false, 1);
                    while (it.hasNext()) {
                        Tile neighborTile = map.getTile(it.next());
                        if (neighborTile==null || !neighborTile.isLand() || neighborTile.getType()==mountains)
                            continue;
                        int r = random.nextInt(8);
                        if (r == 0) {
                            neighborTile.setType(mountains);
                            mountainRegion.addTile(neighborTile);
                            counter++;
                        } else if (r > 2) {
                            neighborTile.setType(hills);
                            mountainRegion.addTile(neighborTile);
                        }
                    }
                }
            }
        }
        logger.info("Added " + counter + " mountain range tiles.");
        
        // and sprinkle a few random hills/mountains here and there
        number = (int)(getMapGeneratorOptions().getNumberOfMountainTiles()*randomHillsRatio);
        counter = 0;
        nextTry: for (int tries = 0; tries < 1000; tries++) {
            if (counter < number) {
                Position p = map.getRandomLandPosition();
                /* this can't happen
                if (p == null)
                    continue;
                Tile t = map.getTile(p);
                if (t==null || !t.isLand())
                    continue;
                */
                Tile t = map.getTile(p);
                if (t.getType() == hills || t.getType() == mountains) {
                    // already a high ground
                    continue;
                }
                // do not add hills too close to a mountain range
                // this would defeat the purpose of adding random hills
                Iterator<Position> it = map.getCircleIterator(p, true, 3);
                while (it.hasNext()) {
                    if (map.getTile(it.next()).getType() == mountains) {
                        continue nextTry;
                    }
                }

                // do not add hills too close to the ocean/lake
                // this helps with good locations for building colonies on shore
                it = map.getCircleIterator(p, true, 1);
                while (it.hasNext()) {
                    if (!map.getTile(it.next()).isLand()) {
                        continue nextTry;
                    }
                }

                int k = random.nextInt(4);
                if (k == 0) {
                    // 25% chance of mountain
                    t.setType(mountains);
                } else {
                    // 75% chance of hill
                    t.setType(hills);
                }
                counter++;
            }
        }
        logger.info("Added " + counter + " random hills tiles.");
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
            nextTry: for (int tries = 0; tries < 100; tries++) {
                Position position = map.getRandomLandPosition();
                if (!map.getTile(position).getType().canHaveRiver()) {
                    continue;
                }
                // check the river source/spring is not too close to the ocean
                Iterator<Position> it = map.getCircleIterator(position, true, 2);
                while (it.hasNext()) {
                    Tile neighborTile = map.getTile(it.next());
                    if (!neighborTile.isLand()) {
                        continue nextTry;
                    }
                }
                if (riverMap.get(position) == null) {
                    // no river here yet
                    ServerRegion riverRegion = new ServerRegion(map.getGame(),
                                                                "model.region.river" + i,
                                                                Region.RegionType.RIVER,
                                                                map.getTile(position).getRegion());
                    riverRegion.setDiscoverable(true);
                    riverRegion.setClaimable(true);
                    // TODO: make this depend on size, or other feature?
                    riverRegion.setScoreValue(10);
                    map.setRegion(riverRegion);
                    River river = new River(map, riverMap, riverRegion);
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
