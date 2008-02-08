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
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Map.Position;

/**
 * Class for making a <code>Map</code> based upon a land map.
 */
public class TerrainGenerator {

    private static final Logger logger = Logger.getLogger(TerrainGenerator.class.getName());
    
    private final MapGeneratorOptions mapGeneratorOptions;

    private final Random random = new Random();

    private TileType ocean = FreeCol.getSpecification().getTileType("model.tile.ocean");

    public ArrayList<ArrayList<TileType>> latTileTypes = new ArrayList<ArrayList<TileType>>();

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
        final boolean importLandMap = (importGame != null) && getMapGeneratorOptions().getBoolean(MapGeneratorOptions.IMPORT_LAND_MAP);
                
        Tile[][] tiles = new Tile[width][height];
        for (int y = 0; y < height; y++) {
            int latitude = (Math.min(y, (height-1) - y) * 200) / height; // lat=0 for poles; lat=100 for equator
            for (int x = 0; x < width; x++) {
                Tile t;
                if (importTerrain && importGame.getMap().isValid(x, y)) {
                    Tile importTile = importGame.getMap().getTile(x, y);
                    if (importLandMap || importTile.isLand() == landMap[x][y]) {
                        t = new Tile(game, importTile.getType(), x, y);
                        // TileItemContainer copies everything including Resource unless importBonuses == false
                        if (importTile.getTileItemContainer()!=null)
                            t.getTileItemContainer().copyFrom(importTile.getTileItemContainer(), importBonuses);
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
            createHighSeas(map);
            createMountains(map);
            createRivers(map);
        }
    }

    private Tile createTile(Game game, int x, int y, boolean[][] landMap, int latitude) {
        
        final int height = landMap[0].length;
        int forestChance = getMapGeneratorOptions().getPercentageOfForests();
        if (y==0 || y==height-1 || y==1 || y==height-2) {
            // please no forests at the pole
            forestChance = 0;
        }
        Tile t;
        if (landMap[x][y]) {
            t = new Tile(game, getRandomLandTileType(latitude, forestChance), x, y);
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
     * @param latitude The location of the tile relative to the north/south poles and equator, 
     *        100% is the mid-section of the map (equator) 
     *        0% is on the top/bottom of the map (poles).
     * @param forestChance The percentage chance of forests in this area
     */
    private TileType getRandomLandTileType(int latitudePercent, int forestChance) {
        // decode options
        final int humidityPreference = getMapGeneratorOptions().getHumidity();
        final int temperaturePreference = getMapGeneratorOptions().getTemperature();

        // latRanges correspond to 0,1,2,3 from TileType.latitude (100-0)
        int equatorialZoneLimit=0, tropicalZoneLimit=0, temperateZoneLimit=0, borealZoneLimit=0;
        if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_COLD) {
            equatorialZoneLimit = 90;
            tropicalZoneLimit = 75;
            temperateZoneLimit = 50;
            borealZoneLimit = 0;
        } else if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_CHILLY) {
            equatorialZoneLimit = 85;
            tropicalZoneLimit = 70;
            temperateZoneLimit = 35;
            borealZoneLimit = 0;
        } else if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_TEMPERATE) {
            equatorialZoneLimit = 75;
            tropicalZoneLimit = 50;
            temperateZoneLimit = 25;
            borealZoneLimit = 0;
        } else if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_WARM) {
            equatorialZoneLimit = 70;
            tropicalZoneLimit = 45;
            temperateZoneLimit = 15;
            borealZoneLimit = 0;
        } else if (temperaturePreference==MapGeneratorOptions.TEMPERATURE_HOT) {
            equatorialZoneLimit = 65;
            tropicalZoneLimit = 35;
            temperateZoneLimit = 10;
            borealZoneLimit = 0;
        }
        int[] latRanges = { equatorialZoneLimit, tropicalZoneLimit, temperateZoneLimit, borealZoneLimit };
        // altRanges correspond to 1,2,3 from TileType.altitude (1-10)
        int[] altRanges = { 6, 8, 10};
        // Create the lists of TileType the first time you use it
        while (latTileTypes.size() < latRanges.length) {
            latTileTypes.add(new ArrayList<TileType>());
        }
        // convert the latitude percentage into a classification index
        // latitudeIndex = 0 is for the equator
        // latitudeIndex = 3 is for the poles
        int latitudeIndex = latRanges.length - 1;
        for (int i = 0; i < latRanges.length; i++) {
            if (latRanges[i] < latitudePercent) {
                latitudeIndex = i;
                break;
            }
        }
        // Fill the list of latitude TileTypes the first time you use it
        if (latTileTypes.get(latitudeIndex).size() == 0) {
            for (TileType tileType : FreeCol.getSpecification().getTileTypeList()) {
                if (tileType.getId().equals("model.tile.hills") ||
                    tileType.getId().equals("model.tile.mountains")) {
                    // do not generate hills or mountains at this time
                    // they will be created explicitly later
                    continue;
                }
                if (!tileType.isWater() && tileType.withinRange(TileType.RangeType.LATITUDE, latitudeIndex)) {
                    // Within range, add it
                    latTileTypes.get(latitudeIndex).add(tileType);
                }
            }
            if (latTileTypes.get(latitudeIndex).size() == 0) {
                // If it is still 0 after adding all relevant types, throw error
                throw new RuntimeException("No TileType within latitude == " + latitudeIndex);
            }
        }
        
        // Scope the type of tiles to be used and choose one
        TileType chosen = null;
        //List<TileType> acceptable = latTileTypes.get(latitudeIndex).clone();
        List<TileType> acceptable = new ArrayList<TileType>();
        acceptable.addAll(latTileTypes.get(latitudeIndex));
        // Choose based on altitude
        int altitude = random.nextInt(10);
        for (int i = 0; i < 3; i++) {
            if (altRanges[i] > altitude) {
                altitude = i;
                break;
            }
        }
        Iterator<TileType> it = acceptable.iterator();
        while (it.hasNext()) {
            TileType t = it.next();
            if (t.withinRange(TileType.RangeType.ALTITUDE, altitude)) {
                if (acceptable.size() == 1) {
                    chosen = t;
                    break;
                }
                it.remove();
            }
        }
        // Choose based on forested/unforested
        if (chosen == null) {
            boolean forested = random.nextInt(100) < forestChance;
            it = acceptable.iterator();
            while (it.hasNext()) {
                TileType t = it.next();
                if (t.isForested() != forested) {
                    if (acceptable.size() == 1) {
                        chosen = t;
                        break;
                    }
                    it.remove();
                }
            }
        }
        // Choose based on humidity - later use MapGeneratorOptions to help define these
        if (chosen == null) {
            int humidity = random.nextInt(7) - 3;   // To get -3 to 3, 0 inclusive
            it = acceptable.iterator();
            while (it.hasNext()) {
                TileType t = it.next();
                if (!t.withinRange(TileType.RangeType.HUMIDITY, humidity)) {
                    if (acceptable.size() == 1) {
                        chosen = t;
                        break;
                    }
                    it.remove();
                }
            }
        }
        // Choose based on temperature - later use MapGeneratorOptions to help define these
        if (chosen == null) {
            int temperature = random.nextInt(7) - 3;   // To get -3 to 3, 0 inclusive
            it = acceptable.iterator();
            while (it.hasNext()) {
                TileType t = it.next();
                if (!t.withinRange(TileType.RangeType.TEMPERATURE, temperature)) {
                    if (acceptable.size() == 1) {
                        chosen = t;
                        break;
                    }
                    it.remove();
                }
            }
        }
        // All scoped, if none have been selected by elimination, randomly choose one
        if (chosen == null) {
            chosen = acceptable.get(random.nextInt(acceptable.size()));
        }
        return chosen;
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

        Region pacific = map.getRegion("model.region.pacific");
        Region northPacific = map.getRegion("model.region.northPacific");
        northPacific.setParent(pacific);
        Region southPacific = map.getRegion("model.region.southPacific");
        southPacific.setParent(pacific);
        Region atlantic = map.getRegion("model.region.atlantic");
        Region northAtlantic = map.getRegion("model.region.northAtlantic");
        northAtlantic.setParent(atlantic);
        Region southAtlantic = map.getRegion("model.region.southAtlantic");
        southAtlantic.setParent(atlantic);

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x=0; x<maxDistanceToEdge && 
                          x<map.getWidth() && 
                          !map.isLandWithinDistance(x, y, distToLandFromHighSeas); x++) {
                if (map.isValid(x, y)) {
                    map.getTile(x, y).setType(highSeas);
                    if (y < map.getHeight() / 2) {
                        map.getTile(x, y).setRegion(northPacific);
                    } else {
                        map.getTile(x, y).setRegion(southPacific);
                    }
                }
            }

            for (int x=1; x<=maxDistanceToEdge && 
                          x<=map.getWidth()-1 &&
                          !map.isLandWithinDistance(map.getWidth()-x, y, distToLandFromHighSeas); x++) {
                if (map.isValid(map.getWidth()-x, y)) {
                    map.getTile(map.getWidth()-x, y).setType(highSeas);
                    if (y < map.getHeight() / 2) {
                        map.getTile(map.getWidth()-x, y).setRegion(northAtlantic);
                    } else {
                        map.getTile(map.getWidth()-x, y).setRegion(southAtlantic);
                    }
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
        TileType hills = null, mountains = null;
        for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
            if (t.getId().equals("model.tile.hills") && hills == null) {
                hills = t;
                if (mountains != null)
                    break;
            } else if (t.getId().equals("model.tile.mountains") && mountains == null) {
                mountains = t;
                if (hills != null)
                    break;
            }
        }
        if (hills == null || mountains == null) {
            throw new RuntimeException("Both Hills and Mountains TileTypes must be defined");
        }
        
        // generate the mountain ranges
        int counter = 0;
        for (int tries = 0; tries < 100; tries++) {
            if (counter < number) {
                Position p = map.getRandomLandPosition();
                if (p == null)
                    continue;
                Tile startTile = map.getTile(p);
                if (startTile==null || !startTile.isLand())
                    continue;
                if (startTile.getType() == hills || startTile.getType() == mountains) {
                    // already a high ground
                    continue;
                }
                boolean isMountainRangeCloseBy = false;
                Iterator<Position> it = map.getCircleIterator(p, true, 3);
                while (it.hasNext()) {
                    Tile neighborTile = map.getTile(it.next());
                    if (neighborTile.isLand() && neighborTile.getType() == mountains) {
                        isMountainRangeCloseBy = true;
                        break;
                    }
                }
                if (isMountainRangeCloseBy) {
                    // do not add a mountain range too close to another
                    continue;
                }
                boolean isWaterCloseBy = false;
                it = map.getCircleIterator(p, true, 2);
                while (it.hasNext()) {
                    Tile neighborTile = map.getTile(it.next());
                    if (!neighborTile.isLand()) {
                        isWaterCloseBy = true;
                        break;
                    }
                }
                if (isWaterCloseBy) {
                    // do not add a mountain range too close to the ocean/lake
                    // this helps with good locations for building colonies on shore
                    continue;
                }
                Direction direction = Direction.values()[random.nextInt(8)];
                int length = maximumLength - random.nextInt(maximumLength/2);
                logger.info("Direction of mountain range is " + direction +
                        ", length of mountain range is " + length);
                for (int index = 0; index < length; index++) {
                    p = Map.getAdjacent(p, direction);
                    Tile nextTile = map.getTile(p);
                    if (nextTile == null || !nextTile.isLand()) 
                        continue;
                    nextTile.setType(mountains);
                    counter++;
                    it = map.getCircleIterator(p, false, 1);
                    while (it.hasNext()) {
                        Tile neighborTile = map.getTile(it.next());
                        if (neighborTile==null || !neighborTile.isLand() || neighborTile.getType()==mountains)
                            continue;
                        int r = random.nextInt(8);
                        if (r == 0) {
                            neighborTile.setType(mountains);
                            counter++;
                        } else if (r > 2) {
                            neighborTile.setType(hills);
                        }
                    }
                }
            }
        }
        logger.info("Added " + counter + " mountain range tiles.");
        
        // and sprinkle a few random hills/mountains here and there
        number = (int)(getMapGeneratorOptions().getNumberOfMountainTiles()*randomHillsRatio);
        counter = 0;
        for (int tries = 0; tries < 1000; tries++) {
            if (counter < number) {
                Position p = map.getRandomLandPosition();
                if (p == null)
                    continue;
                Tile t = map.getTile(p);
                if (t==null || !t.isLand())
                    continue;
                if (t.getType() == hills || t.getType() == mountains) {
                    // already a high ground
                    continue;
                }
                boolean isMountainRangeCloseBy = false;
                Iterator<Position> it = map.getCircleIterator(p, true, 3);
                while (it.hasNext()) {
                    Tile neighborTile = map.getTile(it.next());
                    if (neighborTile.isLand() && neighborTile.getType() == mountains) {
                        isMountainRangeCloseBy = true;
                        break;
                    }
                }
                if (isMountainRangeCloseBy) {
                    // do not add hills too close to a mountain range
                    // this would defeat the purpose of adding random hills
                    continue;
                }
                boolean isWaterCloseBy = false;
                it = map.getCircleIterator(p, true, 1);
                while (it.hasNext()) {
                    Tile neighborTile = map.getTile(it.next());
                    if (!neighborTile.isLand()) {
                        isWaterCloseBy = true;
                        break;
                    }
                }
                if (isWaterCloseBy) {
                    // do not add hills too close to the ocean/lake
                    // this helps with good locations for building colonies on shore
                    continue;
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
            River river = new River(map, riverMap);
            for (int tries = 0; tries < 100; tries++) {
                Position position = new Position(random.nextInt(map.getWidth()),
                                                 random.nextInt(map.getHeight()));
                if (position.getY()==0 || position.getY()==map.getHeight()-1 ||
                    position.getY()==1 || position.getY()==map.getHeight()-2) {
                    // please no rivers in polar regions
                    continue;
                }
                // check the river source/spring is not too close to the ocean
                boolean isWaterCloseBy = false;
                Iterator<Position> it = map.getCircleIterator(position, true, 2);
                while (it.hasNext()) {
                    Tile neighborTile = map.getTile(it.next());
                    if (!neighborTile.isLand()) {
                        isWaterCloseBy = true;
                        break;
                    }
                }
                if (isWaterCloseBy) {
                    // do not start a new river too close to the ocean
                    continue;
                }
                if (riverMap.get(position) == null) {
                    // no river here yet
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
