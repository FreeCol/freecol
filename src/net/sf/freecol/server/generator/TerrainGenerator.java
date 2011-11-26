/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Region.RegionType;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.model.ServerRegion;
import net.sf.freecol.common.util.RandomChoice;


/**
 * Class for making a <code>Map</code> based upon a land map.
 */
public class TerrainGenerator {

    private static final Logger logger = Logger.getLogger(TerrainGenerator.class.getName());

    public static final int LAND_REGIONS_SCORE_VALUE = 1000;
    public static final int LAND_REGION_MIN_SCORE = 5;
    public static final int PACIFIC_SCORE_VALUE = 100;

    public static final int LAND_REGION_MAX_SIZE = 75;

    public static final Direction[] corners = new Direction[] {
        Direction.N, Direction.E, Direction.S, Direction.W
    };

    private final OptionGroup mapGeneratorOptions;
    private final Random random;

    private TileType lake;
    private TileImprovementType riverType;
    private TileImprovementType fishBonusLandType;
    private TileImprovementType fishBonusRiverType;

    private ArrayList<TileType> terrainTileTypes;
    private ArrayList<TileType> oceanTileTypes;

    // TODO dynamic lakes, mountains and hills

    /**
     * Creates a new <code>TerrainGenerator</code>.
     *
     * @param mapGeneratorOptions The options.
     * @param random A <code>Random</code> number source.
     * @see #createMap
     */
    public TerrainGenerator(OptionGroup mapGeneratorOptions, Random random) {
        this.mapGeneratorOptions = mapGeneratorOptions;
        this.random = random;
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
        Specification spec = game.getSpecification();
        lake = spec.getTileType("model.tile.lake");
        riverType = spec.getTileImprovementType("model.improvement.river");
        fishBonusLandType = spec.getTileImprovementType("model.improvement.fishBonusLand");
        fishBonusRiverType = spec.getTileImprovementType("model.improvement.fishBonusRiver");

        final int width = landMap.length;
        final int height = landMap[0].length;

        final boolean importTerrain = (importGame != null)
            && getMapGeneratorOptions().getBoolean(MapGeneratorOptions.IMPORT_TERRAIN);
        final boolean importBonuses = (importGame != null)
            && getMapGeneratorOptions().getBoolean(MapGeneratorOptions.IMPORT_BONUSES);

        boolean mapHasLand = false;
        Tile[][] tiles = new Tile[width][height];
        Map map = new Map(game, tiles);
        int minimumLatitude = getMapGeneratorOptions()
            .getInteger(MapGeneratorOptions.MINIMUM_LATITUDE);
        int maximumLatitude = getMapGeneratorOptions()
            .getInteger(MapGeneratorOptions.MAXIMUM_LATITUDE);
        // make sure the values are in range
        minimumLatitude = Math.max(-90, Math.min(90, minimumLatitude));
        maximumLatitude = Math.max(-90, Math.min(90, maximumLatitude));
        map.setMinimumLatitude(Math.min(minimumLatitude, maximumLatitude));
        map.setMaximumLatitude(Math.max(minimumLatitude, maximumLatitude));

        for (int y = 0; y < height; y++) {
            int latitude = map.getLatitude(y);
            for (int x = 0; x < width; x++) {
                if (landMap[x][y]) {
                    mapHasLand = true;
                }
                Tile t;
                if (importTerrain && importGame.getMap().isValid(x, y)) {
                    Tile importTile = importGame.getMap().getTile(x, y);
                    if (importTile.isLand() == landMap[x][y]) {
                        t = new Tile(game, spec.getTileType(importTile.getType().getId()), x, y);
                        if (importTile.getMoveToEurope() != null) {
                            t.setMoveToEurope(importTile.getMoveToEurope());
                        }
                        if (importTile.getTileItemContainer() != null) {
                            TileItemContainer container = new TileItemContainer(game, t);
                            // TileItemContainer copies every natural item
                            //including Resource unless importBonuses == false
                            // Rumors and roads are not copied
                            container.copyFrom(importTile.getTileItemContainer(), importBonuses, true);
                            t.setTileItemContainer(container);
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

        game.setMap(map);

        if (!importTerrain) {
            createOceanRegions(map);
            createHighSeas(map);
            if (mapHasLand) {
                createMountains(map);
                findLakes(map);
                // Create the rivers *after* the land regions as
                // rivers are child regions of the surrounding lands.
                createLandRegions(map);
                createRivers(map);
            }
        }

        // Add the bonuses only after the map is completed.
        // Otherwise we risk creating resources on fields where they
        // don't belong (like sugar in large rivers or tobaco on hills).
        for (Tile tile: map.getAllTiles()) {
            perhapsAddBonus(tile, !importBonuses);
            if (!tile.isLand()) {
                encodeStyle(tile);
            }
        }
    }

    public static void encodeStyle(Tile ocean) {
        EnumMap<Direction, Boolean> connections =
            new EnumMap<Direction, Boolean>(Direction.class);

        // corners
        for (Direction d : corners) {
            Tile tile = ocean.getNeighbourOrNull(d);
            connections.put(d, (tile != null && tile.isLand()));
        }
        // edges
        for (Direction d : Direction.longSides) {
            Tile tile = ocean.getNeighbourOrNull(d);
            if (tile != null && tile.isLand()) {
                connections.put(d, true);
                // ignore adjacent corners
                connections.put(d.getNextDirection(), false);
                connections.put(d.getPreviousDirection(), false);
            } else {
                connections.put(d, false);
            }
        }
        int result = 0;
        int index = 0;
        for (Direction d : corners) {
            if (connections.get(d)) {
                result += (int) Math.pow(2, index);
            }
            index++;
        }
        for (Direction d : Direction.longSides) {
            if (connections.get(d)) {
                result += (int) Math.pow(2, index);
            }
            index++;
        }
        ocean.setStyle(result);
    }

    private Tile createTile(Game game, int x, int y, boolean[][] landMap, int latitude) {
        Tile t;
        if (landMap[x][y]) {
            t = new Tile(game, getRandomLandTileType(game, latitude), x, y);
        } else {
            t = new Tile(game, getRandomOceanTileType(game, latitude), x, y);
        }

        return t;
    }


    /**
     * Adds a terrain bonus with a probability determined by the
     * <code>MapGeneratorOptions</code>.
     *
     * @param t a <code>Tile</code> value
     * @param generateBonus a <code>boolean</code> value
     */
    private void perhapsAddBonus(Tile t, boolean generateBonus) {
        if (t.isLand()) {
            if (generateBonus
                && random.nextInt(100) < getMapGeneratorOptions().getInteger("model.option.bonusNumber")) {
                // Create random Bonus Resource
                t.addResource(createResource(t));
            }
        } else {
            int adjacentLand = 0;
            boolean adjacentRiver = false;
            for (Direction direction : Direction.values()) {
                Tile otherTile = t.getNeighbourOrNull(direction);
                if (otherTile != null && otherTile.isLand()) {
                    adjacentLand++;
                    if (otherTile.hasRiver()) {
                        adjacentRiver = true;
                    }
                }
            }

            //In Col1, ocean tiles with less than 3 land neighbours produce 2 fish,
            //all others produce 4 fish
            if (adjacentLand > 2) {
                t.add(new TileImprovement(t.getGame(), t, fishBonusLandType));
            }

            //In Col1, the ocean tile in front of a river mouth would
            //get an additional +1 bonus
            //TODO: This probably has some false positives, means river tiles
            //that are NOT a river mouth next to this tile!
            if (!t.hasRiver() && adjacentRiver) {
                t.add(new TileImprovement(t.getGame(), t, fishBonusRiverType));
            }

            if (t.getType().isConnected()) {
                if (generateBonus && adjacentLand > 1 && random.nextInt(10 - adjacentLand) == 0) {
                    t.addResource(createResource(t));
                }
            } else {
                if (random.nextInt(100) < getMapGeneratorOptions().getInteger("model.option.bonusNumber")) {
                    // Create random Bonus Resource
                    t.addResource(createResource(t));
                }
            }
        }
    }


    public Resource createResource(Tile tile) {
        if (tile == null) {
            return null;
        }
        ResourceType resourceType = RandomChoice.getWeightedRandom(null, null,
            random, tile.getType().getWeightedResources());
        if (resourceType == null) {
            return null;
        }
        int minValue = resourceType.getMinValue();
        int maxValue = resourceType.getMaxValue();
        int quantity = (minValue == maxValue) ? maxValue :
            (minValue + random.nextInt(maxValue - minValue + 1));
        return new Resource(tile.getGame(), tile, resourceType, quantity);
    }


    /**
     * Gets the <code>MapGeneratorOptions</code>.
     * @return The <code>MapGeneratorOptions</code> being used
     *      when creating terrain.
     */
    private OptionGroup getMapGeneratorOptions() {
        return mapGeneratorOptions;
    }

    /**
     *
     * @param game
     * @param latitude
     * @return
     */
    private TileType getRandomOceanTileType(Game game, int latitude) {
    	// create the main list of TileTypes the first time, and reuse it afterwards
        if (oceanTileTypes==null) {
        	oceanTileTypes = new ArrayList<TileType>();
            for (TileType tileType : game.getSpecification().getTileTypeList()) {
                if (tileType.isWater() && tileType.isConnected()
                	&& !tileType.hasAbility("model.ability.moveToEurope")) {
                	oceanTileTypes.add(tileType);
                }

            }
        }

        return getRandomTileType(game, oceanTileTypes, latitude);
    }

    /**
     * Gets a random land tile type based on the given percentage.
     *
     * @param game the Game
     * @param latitude The location of the tile relative to the north/south
     *        poles and equator:
     *          0 is the mid-section of the map (equator)
     *          +/-90 is on the bottom/top of the map (poles).
     */
    private TileType getRandomLandTileType(Game game, int latitude) {
        // create the main list of TileTypes the first time, and reuse it afterwards
        if (terrainTileTypes==null) {
            terrainTileTypes = new ArrayList<TileType>();
            for (TileType tileType : game.getSpecification().getTileTypeList()) {
                if (tileType.isElevation() || tileType.isWater()) {
                    // do not generate elevated and water tiles at this time
                    // they are created separately
                    continue;
                }
                terrainTileTypes.add(tileType);
            }
        }

        return getRandomTileType(game, terrainTileTypes, latitude);
    }

    /**
     * Returns a TileType, that fits to the regional requirements.
     * <br/><br/>
     * TODO: Can be used for mountains and rivers too.
     *
     * @param game The game.
     * @param candidates A list of <tt>TileType</tt>s to use for calculations.
     * @param latitude The latitude.
     * @return A <tt>TileType</tt> that fits to the regional requirements.
     */
    private TileType getRandomTileType(Game game, List<TileType> candidates, int latitude) {
    	// decode options
        final int forestChance = getMapGeneratorOptions().getInteger("model.option.forestNumber");
        final int temperaturePreference = getMapGeneratorOptions().getInteger("model.option.temperature");

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
        int localeTemperature = poleTemperature + (90 - Math.abs(latitude))
            * temperatureRange/90;
        int temperatureDeviation = 7; // +/- 7 degrees randomization
        localeTemperature += random.nextInt(temperatureDeviation*2)-temperatureDeviation;
        if (localeTemperature>40)
            localeTemperature = 40;
        if (localeTemperature<-20)
            localeTemperature = -20;

        // humidity calculation
        int localeHumidity = game.getSpecification().getRangeOption(MapGeneratorOptions.HUMIDITY).getValue();
        int humidityDeviation = 20; // +/- 20% randomization
        localeHumidity += random.nextInt(humidityDeviation*2) - humidityDeviation;
        if (localeHumidity<0)
            localeHumidity = 0;
        if (localeHumidity>100)
            localeHumidity = 100;

        /*
         * Make and use a backup of the specified list, because we are modifying the list here.
         */
        List<TileType> candidateTileTypes = new ArrayList<TileType>(candidates);
        /*
         * There will be a check for forested later, but there might be tiles like
         * oceans/mountains that do not support forests. Therefore shuffle now,
         * because in this case, we will just use the last one standing.
         */
        Collections.shuffle(candidateTileTypes, this.random);

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

        // Choose based on forested/unforested if possible
        boolean forested = random.nextInt(100) < forestChance;
        it = candidateTileTypes.iterator();
        while (it.hasNext()) {
            TileType t = it.next();
            if (t.isForested() != forested) {
            	/*
            	 * Fix for land/ocean/mountain tiles that do not support forests. In this case
            	 * return the last one standing. (shuffled before to get random tiles here)
            	 */
            	if (it.hasNext()) {
            		it.remove();
            	}
            }
        }
        if (candidateTileTypes.size() == 1) {
            return candidateTileTypes.get(0);
        } else if (candidateTileTypes.size()==0) {
            throw new RuntimeException("No TileType for temperature==" + localeTemperature
                    +" and humidity==" + localeHumidity + " and forested=="+forested);
        }

        /*
         * Return the first one, if more than one is left after termination.
		 * Note, that we shuffled the list already.
         */
        return candidateTileTypes.get(0);
    }


    /**
     * Creates ocean map regions in the given Map. At the moment, the ocean
     * is divided into two by two regions.
     *
     * @param map a <code>Map</code> value
     */
    void createOceanRegions(Map map) {
        Game game = map.getGame();

        ServerRegion pacific =
            new ServerRegion(game, "model.region.pacific", RegionType.OCEAN);
        ServerRegion northPacific =
            new ServerRegion(game, "model.region.northPacific", RegionType.OCEAN, pacific);
        ServerRegion southPacific =
            new ServerRegion(game, "model.region.southPacific", RegionType.OCEAN, pacific);
        ServerRegion atlantic =
            new ServerRegion(game, "model.region.atlantic", RegionType.OCEAN);
        ServerRegion northAtlantic =
            new ServerRegion(game, "model.region.northAtlantic", RegionType.OCEAN, atlantic);
        ServerRegion southAtlantic =
            new ServerRegion(game, "model.region.southAtlantic", RegionType.OCEAN, atlantic);

        for (ServerRegion region : new ServerRegion[] {
                northPacific, southPacific,
                atlantic, northAtlantic, southAtlantic }) {
            region.setPrediscovered(true);
            map.setRegion(region);
        }

        map.setRegion(pacific);
        pacific.setDiscoverable(true);
        pacific.setScoreValue(PACIFIC_SCORE_VALUE);

        // Fill the ocean regions by first filling the quadrants individually,
        // then allow the quadrants to overflow into their horizontally
        // opposite quadrant, then finally into the whole map.
        // This correctly handles cases like:
        //
        //   NP NP NP NA NA NA      NP NP NP NA NA NA
        //   NP L  L  L  L  NA      NP L  L  NA L  NA
        //   NP L  NA NA NA NA  or  NP L  NA NA L  NA
        //   SP L  SA SA SA SA      SP L  NA L  L  SA
        //   SP L  L  L  L  SA      SP L  L  L  L  SA
        //   SP SP SP SA SA SA      SP SP SP SA SA SA
        //
        // or multiple such incursions across the nominal quadrant divisions.
        //
        int maxx = map.getWidth();
        int midx = maxx / 2;
        int maxy = map.getHeight();
        int midy = maxy / 2;
        Position pNP = new Position(0,      midy-1);
        Position pSP = new Position(0,      midy+1);
        Position pNA = new Position(maxx-1, midy-1);
        Position pSA = new Position(maxx-1, midy+1);

        Rectangle rNP = new Rectangle(0,0,       midx,midy);
        Rectangle rSP = new Rectangle(0,midy,    midx,maxy);
        Rectangle rNA = new Rectangle(midx,0,    maxx,midy);
        Rectangle rSA = new Rectangle(midx,midy, maxx,maxy);
        fillOcean(map, pNP, northPacific,  rNP);
        fillOcean(map, pSP, southPacific,  rSP);
        fillOcean(map, pNA, northAtlantic, rNA);
        fillOcean(map, pSA, southAtlantic, rSA);

        Rectangle rN = new Rectangle(0,0,    maxx,midy);
        Rectangle rS = new Rectangle(0,midy, maxx,maxy);
        fillOcean(map, pNP, northPacific,  rN);
        fillOcean(map, pSP, southPacific,  rS);
        fillOcean(map, pNA, northAtlantic, rN);
        fillOcean(map, pSA, southAtlantic, rS);

        Rectangle rAll = new Rectangle(0,0, maxx,maxy);
        fillOcean(map, pNP, northPacific,  rAll);
        fillOcean(map, pSP, southPacific,  rAll);
        fillOcean(map, pNA, northAtlantic, rAll);
        fillOcean(map, pSA, southAtlantic, rAll);
    }

    /**
     * Flood fill ocean regions.
     *
     * @param map The <code>Map</code> to fill in.
     * @param p A valid starting <code>Position</code>.
     * @param region A <code>Region</code> to fill with.
     * @param bounds A <code>Rectangle</code> that bounds the filling.
     */
    private void fillOcean(Map map, Position p, Region region,
                           Rectangle bounds) {
        Queue<Position> q = new LinkedList<Position>();
        boolean[][] visited = new boolean[map.getWidth()][map.getHeight()];
        visited[p.getX()][p.getY()] = true;
        q.add(p);

        while ((p = q.poll()) != null) {
            Tile tile = map.getTile(p);
            tile.setRegion(region);

            for (Direction direction : Direction.values()) {
                Position n = p.getAdjacent(direction);
                if (map.isValid(n)
                    && !visited[n.getX()][n.getY()]
                    && bounds.contains(n.getX(), n.getY())) {
                    visited[n.getX()][n.getY()] = true;
                    Tile next = map.getTile(n);
                    if ((next.getRegion() == null || next.getRegion() == region)
                        && !next.isLand()) {
                        q.add(n);
                    }
                }
            }
        }
    }


    /**
     * Creates land map regions in the given Map.
     * First, the arctic/antarctic regions are defined, based on <code>LandGenerator.POLAR_HEIGHT</code>
     * For the remaining land tiles, one region per contiguous landmass is created.
     * @param map a <code>Map</code> value
     */
    void createLandRegions(Map map) {
        Game game = map.getGame();

        //Create arctic/antarctic regions first
        ServerRegion arctic =
            new ServerRegion(game, "model.region.arctic", RegionType.LAND);
        ServerRegion antarctic =
            new ServerRegion(game, "model.region.antarctic", RegionType.LAND);

        map.setRegion(arctic);
        arctic.setPrediscovered(true);
        map.setRegion(antarctic);
        antarctic.setPrediscovered(true);

        int arcticHeight = Map.POLAR_HEIGHT;
        int antarcticHeight = map.getHeight() - Map.POLAR_HEIGHT - 1;

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < arcticHeight; y++) {
                if (map.isValid(x, y)) {
                    Tile tile = map.getTile(x, y);
                    if (tile.isLand()) {
                        arctic.addTile(tile);
                    }
                }
            }
            for (int y = antarcticHeight; y < map.getHeight(); y++) {
                if (map.isValid(x, y)) {
                    Tile tile = map.getTile(x, y);
                    if (tile.isLand()) {
                        antarctic.addTile(tile);
                    }
                }
            }
        }

        //Then, create "geographic" land regions
        //These regions are used by the MapGenerator to place Indian Settlements
        //Note that these regions are "virtual", i.e. having a bounding box,
        //but containing no tiles
        int thirdWidth = map.getWidth()/3;
        int twoThirdWidth = 2 * thirdWidth;
        int thirdHeight = map.getHeight()/3;
        int twoThirdHeight = 2 * thirdHeight;

        ServerRegion northWest =
            new ServerRegion(game, "model.region.northWest", RegionType.LAND);
        northWest.setBounds(new Rectangle(0,0,thirdWidth,thirdHeight));
        ServerRegion north =
            new ServerRegion(game, "model.region.north", RegionType.LAND);
        north.setBounds(new Rectangle(thirdWidth,0,thirdWidth,thirdHeight));
        ServerRegion northEast =
            new ServerRegion(game, "model.region.northEast", RegionType.LAND);
        northEast.setBounds(new Rectangle(twoThirdWidth,0,map.getWidth()-twoThirdWidth,thirdHeight));

        ServerRegion west =
            new ServerRegion(game, "model.region.west", RegionType.LAND);
        west.setBounds(new Rectangle(0,thirdHeight,thirdWidth,thirdHeight));
        ServerRegion center =
            new ServerRegion(game, "model.region.center", RegionType.LAND);
        center.setBounds(new Rectangle(thirdWidth,thirdHeight,thirdWidth,thirdHeight));
        ServerRegion east =
            new ServerRegion(game, "model.region.east", RegionType.LAND);
        east.setBounds(new Rectangle(twoThirdWidth,thirdHeight,map.getWidth()-twoThirdWidth,thirdHeight));

        ServerRegion southWest =
            new ServerRegion(game, "model.region.southWest", RegionType.LAND);
        southWest.setBounds(new Rectangle(0,twoThirdHeight,thirdWidth,map.getHeight()-twoThirdHeight));
        ServerRegion south =
            new ServerRegion(game, "model.region.south", RegionType.LAND);
        south.setBounds(new Rectangle(thirdWidth,twoThirdHeight,thirdWidth,map.getHeight()-twoThirdHeight));
        ServerRegion southEast =
            new ServerRegion(game, "model.region.southEast", RegionType.LAND);
        southEast.setBounds(new Rectangle(twoThirdWidth,twoThirdHeight,map.getWidth()-twoThirdWidth,map.getHeight()-twoThirdHeight));

        ServerRegion[] geographicRegions = new ServerRegion[] {
            northWest, north, northEast, west, center,
            east, southWest, south, southEast
        };
        for (ServerRegion region : geographicRegions) {
            region.setDiscoverable(false);
            map.setRegion(region);
        }

        //last, create "explorable" land regions
        int continents = 0;
        boolean[][] landmap = new boolean[map.getWidth()][map.getHeight()];
        int[][] continentmap = new int[map.getWidth()][map.getHeight()];

        //initialize both maps
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                continentmap[x][y] = 0;
                if (map.isValid(x, y)) {
                    Tile tile = map.getTile(x, y);
                    boolean isMountainRange = false;
                    if (tile.getRegion() != null) {
                        isMountainRange = (tile.getRegion().getType() == RegionType.MOUNTAIN);
                    }
                    if (tile.isLand()) {
                        //exclude arctic/antarctic tiles and Mountain Ranges
                        //Note: Great Rivers are excluded by tile.isLand()
                        if ((y<arcticHeight) || (y>=antarcticHeight) || isMountainRange) {
                            landmap[x][y] = false;
                        } else {
                            landmap[x][y] = true;
                        }
                    } else {
                        landmap[x][y] = false;
                    }
                }
            }
        }

        //floodfill, so that we end up with individual landmasses numbered in continentmap[][]
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (landmap[x][y]) { //found a new continent/island
                    continents++;
                    boolean[][] continent = floodFill(landmap,new Position(x,y));

                    for (int yy = 0; yy < map.getHeight(); yy++) {
                        for (int xx = 0; xx < map.getWidth(); xx++) {
                            if (continent[xx][yy]) {
                                continentmap[xx][yy] = continents;
                                landmap[xx][yy] = false;
                            }
                        }
                    }
                }
            }
        }
        logger.info("Number of individual landmasses is " + continents);

        //get landmass sizes
        int[] continentsize = new int[continents+1];
        int landsize = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                continentsize[continentmap[x][y]]++;
                if (continentmap[x][y]>0) {
                    landsize++;
                }
            }
        }

        // Go through landmasses, split up those too big
        int oldcontinents = continents;
        for (int c = 1; c <= oldcontinents; c++) {
            //c starting at 1, c=0 is all water tiles
            if (continentsize[c]>LAND_REGION_MAX_SIZE) {
                boolean[][] splitcontinent = new boolean[map.getWidth()][map.getHeight()];
                Position splitposition = new Position(0,0);

                for (int x = 0; x < map.getWidth(); x++) {
                    for (int y = 0; y < map.getHeight(); y++) {
                        if (continentmap[x][y]==c) {
                            splitcontinent[x][y] = true;
                            splitposition = new Position(x,y);
                        } else {
                            splitcontinent[x][y] = false;
                        }
                    }
                }

                while (continentsize[c]>LAND_REGION_MAX_SIZE) {
                    int targetsize = LAND_REGION_MAX_SIZE;
                    if (continentsize[c] < 2*LAND_REGION_MAX_SIZE) {
                        targetsize = continentsize[c]/2;
                    }
                    continents++; //index of the new region in continentmap[][]

                    boolean[][] newregion = floodFill(splitcontinent, splitposition, targetsize);
                    for (int x = 0; x < map.getWidth(); x++) {
                        for (int y = 0; y < map.getHeight(); y++) {
                            if (newregion[x][y]) {
                                continentmap[x][y] = continents;
                                splitcontinent[x][y] = false;
                                continentsize[c]--;
                            }
                            if (splitcontinent[x][y]) {
                                splitposition = new Position(x,y);
                            }
                        }
                    }
                }
            }
        }
        logger.info("Number of land regions being created: " + continents);

        // Create ServerRegions for all land regions
        ServerRegion[] landregions = new ServerRegion[continents+1];
        for (int c = 1; c <= continents; c++) {
            //c starting at 1, c=0 is all water tiles
            landregions[c]
                = new ServerRegion(map.getGame(), "model.region.land" + c,
                                   Region.RegionType.LAND);
            landregions[c].setDiscoverable(true);
            map.setRegion(landregions[c]);
        }

        // Add tiles to ServerRegions
        landsize = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (continentmap[x][y] > 0) {
                    Tile tile = map.getTile(x, y);
                    landregions[continentmap[x][y]].addTile(tile);
                    landsize++;
                }
            }
        }

        for (int c = 1; c <= continents; c++) {
            ServerRegion sr = landregions[c];

            // Set exploration points for land regions based on size
            int score = Math.max((int)(((float) sr.getSize() / landsize)
                                       * LAND_REGIONS_SCORE_VALUE),
                                 LAND_REGION_MIN_SCORE);
            sr.setScoreValue(score);

            // Set a parent region from the geographic regions now we
            // know where each region is.
            for (ServerRegion gr : geographicRegions) {
                Position cen = sr.getCenter();
                if (gr.getBounds().contains(cen.getX(), cen.getY())) {
                    sr.setParent(gr);
                    gr.addChild(sr);
                    gr.setSize(gr.getSize() + sr.getSize());
                    break;
                }
            }
            logger.fine("Created land region " + sr.getNameKey()
                        + " (size " + sr.getSize()
                        + ", score " + sr.getScoreValue()
                        + ", parent " + ((sr.getParent() == null) ? "(null)"
                                         : sr.getParent().getNameKey())
                        + ")");
        }

        for (ServerRegion gr : geographicRegions) {
            logger.fine("Geographic region " + gr.getNameKey()
                        + " (size " + gr.getSize()
                        + ", children " + gr.getChildren().size()
                        + ")");
        }
    }


    /**
     * Places "high seas"-tiles on the border of the given map.
     * @param map The <code>Map</code> to create high seas on.
     */
    private void createHighSeas(Map map) {
        createHighSeas(map, getMapGeneratorOptions().getInteger("model.option.distanceToHighSea"),
                       getMapGeneratorOptions().getInteger("model.option.maximumDistanceToEdge"));
    }


    /**
     * Places "high seas"-tiles on the border of the given map.
     *
     * All other tiles previously of type High Seas
     * will be set to Ocean.
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
        for (TileType t : map.getSpecification().getTileTypeList()) {
            if (t.isWater()) {
                if (t.hasAbility("model.ability.moveToEurope")) {
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
        for (TileType t : map.getSpecification().getTileTypeList()) {
            if (t.isWater()) {
                if (t.hasAbility("model.ability.moveToEurope")) {
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
     * Returns the approximate number of land tiles.
     *
     * @return the approximate number of land tiles
     */
    private int getLand() {
        return mapGeneratorOptions.getInteger("model.option.mapWidth")
            * mapGeneratorOptions.getInteger("model.option.mapHeight")
            * mapGeneratorOptions.getInteger("model.option.landMass")
            / 100;
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
        int maximumLength = Math.max(getMapGeneratorOptions().getInteger("model.option.mapWidth"),
                                     getMapGeneratorOptions().getInteger("model.option.mapHeight")) / 10;
        int number = (int) ((getLand() / getMapGeneratorOptions().getInteger("model.option.mountainNumber"))
                            * (1 - randomHillsRatio));
        logger.info("Number of mountain tiles is " + number);
        logger.fine("Maximum length of mountain ranges is " + maximumLength);

        // lookup the resources from specification
        TileType hills = map.getSpecification().getTileType("model.tile.hills");
        TileType mountains = map.getSpecification().getTileType("model.tile.mountains");
        if (hills == null || mountains == null) {
            throw new RuntimeException("Both Hills and Mountains TileTypes must be defined");
        }

        // generate the mountain ranges
        int counter = 0;
        nextTry: for (int tries = 0; tries < 100; tries++) {
            if (counter < number) {
                Position p = map.getRandomLandPosition(random);
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
                for (Tile t: startTile.getSurroundingTiles(3)) {
                    if (t.getType() == mountains) {
                        continue nextTry;
                    }
                }

                // do not add a mountain range too close to the ocean/lake
                // this helps with good locations for building colonies on shore
                for (Tile t: startTile.getSurroundingTiles(2)) {
                    if (!t.isLand()) {
                        continue nextTry;
                    }
                }

                ServerRegion mountainRegion = new ServerRegion(map.getGame(),
                                                               "model.region.mountain" + tries,
                                                               Region.RegionType.MOUNTAIN,
                                                               startTile.getRegion());
                mountainRegion.setDiscoverable(true);
                mountainRegion.setClaimable(true);
                map.setRegion(mountainRegion);
                Direction direction = Direction.getRandomDirection(random);
                int length = maximumLength - random.nextInt(maximumLength/2);
                for (int index = 0; index < length; index++) {
                    p = p.getAdjacent(direction);
                    Tile nextTile = map.getTile(p);
                    if (nextTile == null || !nextTile.isLand())
                        continue;
                    nextTile.setType(mountains);
                    mountainRegion.addTile(nextTile);
                    counter++;
                    Iterator<Position> it = map.getCircleIterator(p, false, 1);
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
                int scoreValue = 2 * mountainRegion.getSize();
                mountainRegion.setScoreValue(scoreValue);
                logger.fine("Created mountain region (direction " + direction +
                            ", length " + length + ", size " + mountainRegion.getSize() +
                            ", score value " + scoreValue + ").");
            }
        }
        logger.info("Added " + counter + " mountain range tiles.");

        // and sprinkle a few random hills/mountains here and there
        number = (int) (getLand() * randomHillsRatio)
            / getMapGeneratorOptions().getInteger("model.option.mountainNumber");
        counter = 0;
        nextTry: for (int tries = 0; tries < 1000; tries++) {
            if (counter < number) {
                Position p = map.getRandomLandPosition(random);
                Tile t = map.getTile(p);
                if (t.getType() == hills || t.getType() == mountains) {
                    // already a high ground
                    continue;
                }
                // do not add hills too close to a mountain range
                // this would defeat the purpose of adding random hills
                for (Tile tile: t.getSurroundingTiles(3)) {
                    if (tile.getType() == mountains) {
                        continue nextTry;
                    }
                }

                // do not add hills too close to the ocean/lake
                // this helps with good locations for building colonies on shore
                for (Tile tile: t.getSurroundingTiles(1)) {
                    if (!tile.isLand()) {
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
        int number = getLand() / getMapGeneratorOptions().getInteger("model.option.riverNumber");
        int counter = 0;
        HashMap<Position, River> riverMap = new HashMap<Position, River>();
        List<River> rivers = new ArrayList<River>();

        for (int i = 0; i < number; i++) {
            nextTry: for (int tries = 0; tries < 100; tries++) {
                Position position = map.getRandomLandPosition(random);
                if (!map.getTile(position).getType().canHaveImprovement(riverType)) {
                    continue;
                }
                // check the river source/spring is not too close to the ocean

                for (Tile neighborTile: map.getTile(position).getSurroundingTiles(2)) {
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
                    River river = new River(map, riverMap, riverRegion, random);
                    if (river.flowFromSource(position)) {
                        logger.fine("Created new river with length " + river.getLength());
                        map.setRegion(riverRegion);
                        rivers.add(river);
                        counter++;
                        break;
                    } else {
                        logger.fine("Failed to generate river.");
                    }
                }
            }
        }

        logger.info("Created " + counter + " rivers of maximum " + number + ".");

        for (River river : rivers) {
            ServerRegion region = river.getRegion();
            int scoreValue = 0;
            for (RiverSection section : river.getSections()) {
                scoreValue += section.getSize();
            }
            scoreValue *= 2;
            region.setScoreValue(scoreValue);
            logger.fine("Created river region (length " + river.getLength() +
                        ", score value " + scoreValue + ").");
        }
    }


    private void findLakes(Map map) {
        Game game = map.getGame();

        //Currently, all inland lakes belong to one prediscovered region
        //TODO: Create separate, discoverable lake regions
        ServerRegion inlandlakes =
            new ServerRegion(game, "model.region.inlandlakes", RegionType.LAKE);
        map.setRegion(inlandlakes);
        inlandlakes.setPrediscovered(true);

        Position p = null;

        // Search for a reachable water tile on the vertical edge.
        // No need to search on the horizental edges as they will
        // be covered by the polar regions.
        for (int x : new int[] {0, map.getWidth() - 1}) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null && tile.getType() != null && !tile.isLand()) {
                    p = new Position(x, y);
                    break;
                }
            }
        }

        if (p == null) {
            // This should not happen!
            logger.warning("Find lakes: unable to find entry point.");
            return;
        }

        boolean[][] watermap = new boolean[map.getWidth()][map.getHeight()];
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                watermap[x][y] = !map.getTile(x,y).isLand();
            }
        }

        boolean[][] visited = floodFill(watermap,p);

        // Every water tile we did not reach is a lake.
        for (int y=0; y < map.getHeight(); y++) {
            for (int x=0; x < map.getWidth(); x++) {
                if (watermap[x][y] && !visited[x][y]) {
                    Tile tile = map.getTile(x, y);
                    if (tile != null) {
                        tile.setType(lake);
                        inlandlakes.addTile(tile);
                    }
                }
            }
        }
    }

    /**
     * Floodfills from a given <code>Position</code> p,
     * based on connectivity information encoded in boolmap
     *
     * @param boolmap The connectivity information for this floodfill
     * @param p The starting position
     * @param limit Limit to stop floodfill at
     * @return A boolean[][] of the same size as boolmap, where "true" means was floodfilled
     */
    private boolean[][] floodFill(boolean[][] boolmap, Position p, int limit) {
        // Starting from position p, find all tiles connected to it on boolmap
        // using a floodfill algorithm and BFS.
        Queue<Position>q = new LinkedList<Position>();

        boolean[][] visited = new boolean[boolmap.length][boolmap[0].length];
        visited[p.getX()][p.getY()] = true;
        limit--;
        do {
            for (Direction direction : Direction.values()) {
                Position n = p.getAdjacent(direction);
                if (Map.isValid(n,boolmap.length,boolmap[0].length) && boolmap[n.getX()][n.getY()] && !visited[n.getX()][n.getY()] && limit > 0) {
                    visited[n.getX()][n.getY()] = true;
                    limit--;
                    q.add(n);
                }
            }

            p = q.poll();
        } while ((p != null) && (limit > 0));
        return visited;
    }

    private boolean[][] floodFill(boolean[][] boolmap, Position p) {
        return floodFill (boolmap, p, java.lang.Integer.MAX_VALUE);
    }

}
