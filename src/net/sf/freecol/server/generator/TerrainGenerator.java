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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColObject;
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
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.model.ServerRegion;


/**
 * Class for making a <code>Map</code> based upon a land map.
 *
 * TODO dynamic lakes, mountains and hills
 */
public class TerrainGenerator {

    private static final Logger logger = Logger.getLogger(TerrainGenerator.class.getName());

    public static final int LAND_REGIONS_SCORE_VALUE = 1000;
    public static final int LAND_REGION_MIN_SCORE = 5;
    public static final int PACIFIC_SCORE_VALUE = 100;
    public static final int LAND_REGION_MAX_SIZE = 75;

    /**
     * The map options group.
     */
    private final OptionGroup mapOptions;

    /**
     * The pseudo-random number source to use.
     * Uses of the PRNG are usually logged in FreeCol, but the use is
     * so intense here and this code is called pre-game, so we intentionally
     * skip the logging here.
     */
    private final Random random;

    // Cache of land and ocean tile types.
    private ArrayList<TileType> landTileTypes = null;
    private ArrayList<TileType> oceanTileTypes = null;

    // Cache of geographic regions
    private ServerRegion[] geographicRegions = null;


    /**
     * Creates a new <code>TerrainGenerator</code>.
     *
     * @param mapGeneratorOptions The <code>OptionGroup</code>
     *     containing the options for the map generator.
     * @param random A <code>Random</code> number source.
     * @see #createMap
     */
    public TerrainGenerator(OptionGroup mapGeneratorOptions, Random random) {
        this.mapOptions = mapGeneratorOptions;
        this.random = random;
    }

    // Utilities

    // TODO: this might be useful elsewhere, too
    private int limitToRange(int value, int lower, int upper) {
        return Math.max(lower, Math.min(value, upper));
    }

    /**
     * Gets the approximate number of land tiles.
     *
     * @return The approximate number of land tiles
     */
    private int getApproximateLandCount() {
        return mapOptions.getInteger(MapGeneratorOptions.MAP_WIDTH)
            * mapOptions.getInteger(MapGeneratorOptions.MAP_HEIGHT)
            * mapOptions.getInteger(MapGeneratorOptions.LAND_MASS)
            / 100;
    }

    /**
     * Set a parent region from the geographic regions now we
     * know where each region is.
     *
     * @param sr The <code>ServerRegion</code> to find a parent for.
     */
    private void setGeographicRegion(ServerRegion sr) {
        if (geographicRegions == null) return;
        for (ServerRegion gr : geographicRegions) {
            Position cen = sr.getCenter();
            if (gr.getBounds().contains(cen.getX(), cen.getY())) {
                sr.setParent(gr);
                gr.addChild(sr);
                gr.setSize(gr.getSize() + sr.getSize());
                break;
            }
        }
    }

    /**
     * Creates a random tile for the specified position.
     *
     * @param game The <code>Game</code> to create the tile in.
     * @param x The tile x coordinate.
     * @param y The tile y coordinate.
     * @param landMap A boolean array defining where the land is.
     * @param latitude The tile latitude.
     * @return The created tile.
     */
    private Tile createTile(Game game, int x, int y, boolean[][] landMap,
                            int latitude) {
        return (landMap[x][y])
            ? new Tile(game, getRandomLandTileType(game, latitude), x, y)
            : new Tile(game, getRandomOceanTileType(game, latitude), x, y);
    }

    /**
     * Gets a random land tile type based on the latitude.
     *
     * @param game the Game
     * @param latitude The location of the tile relative to the north/south
     *     poles and equator:
     *     0 is the mid-section of the map (equator)
     *     +/-90 is on the bottom/top of the map (poles).
     * @return A suitable random land tile type.
     */
    private TileType getRandomLandTileType(Game game, int latitude) {
        if (landTileTypes == null) {
            landTileTypes = new ArrayList<TileType>();
            for (TileType type : game.getSpecification().getTileTypeList()) {
                if (type.isElevation() || type.isWater()) {
                    // do not generate elevated and water tiles at this time
                    // they are created separately
                    continue;
                }
                landTileTypes.add(type);
            }
        }
        return getRandomTileType(game, landTileTypes, latitude);
    }

    /**
     * Gets a random ocean tile type.
     *
     * @param game The <code>Game</code> to query for tile types.
     * @param latitude The latitude of the proposed tile.
     * @return A suitable random ocean tile type.
     */
    private TileType getRandomOceanTileType(Game game, int latitude) {
        if (oceanTileTypes == null) {
            oceanTileTypes = new ArrayList<TileType>();
            for (TileType type : game.getSpecification().getTileTypeList()) {
                if (type.isWater()
                    && type.isHighSeasConnected()
                    && !type.isDirectlyHighSeasConnected()) {
                    oceanTileTypes.add(type);
                }
            }
        }
        return getRandomTileType(game, oceanTileTypes, latitude);
    }

    /**
     * Gets a tile type fitted to the regional requirements.
     *
     * TODO: Can be used for mountains and rivers too.
     *
     * @param game The <code>Game</code> to find the type in.
     * @param candidates A list of <code>TileType</code>s to use for
     *     calculations.
     * @param latitude The tile latitude.
     * @return A suitable <code>TileType</code>.
     */
    private TileType getRandomTileType(Game game, List<TileType> candidates,
                                       int latitude) {
        // decode options
        final int forestChance
            = mapOptions.getInteger(MapGeneratorOptions.FOREST_NUMBER);
        final int temperaturePreference
            = mapOptions.getInteger(MapGeneratorOptions.TEMPERATURE);

        // temperature calculation
        int poleTemperature = -20;
        int equatorTemperature= 40;
        switch(temperaturePreference) {
        case MapGeneratorOptions.TEMPERATURE_COLD:
            poleTemperature = -20;
            equatorTemperature = 25;
            break;
        case MapGeneratorOptions.TEMPERATURE_CHILLY:
            poleTemperature = -20;
            equatorTemperature = 30;
            break;
        case MapGeneratorOptions.TEMPERATURE_TEMPERATE:
            poleTemperature = -10;
            equatorTemperature = 35;
            break;
        case MapGeneratorOptions.TEMPERATURE_WARM:
            poleTemperature = -5;
            equatorTemperature = 40;
            break;
        case MapGeneratorOptions.TEMPERATURE_HOT:
            poleTemperature = 0;
            equatorTemperature = 40;
            break;
        }

        final Specification spec = game.getSpecification();
        int temperatureRange = equatorTemperature-poleTemperature;
        int localeTemperature = poleTemperature + (90 - Math.abs(latitude))
            * temperatureRange/90;
        int temperatureDeviation = 7; // +/- 7 degrees randomization
        localeTemperature += Utils.randomInt(logger, "Temperature", random,
                                             temperatureDeviation * 2)
            - temperatureDeviation;
        localeTemperature = limitToRange(localeTemperature, -20, 40);

        // humidity calculation
        int localeHumidity = spec.getRangeOption(MapGeneratorOptions.HUMIDITY)
            .getValue();
        int humidityDeviation = 20; // +/- 20% randomization
        localeHumidity += Utils.randomInt(logger, "Humidity", random,
                                          humidityDeviation * 2)
            - humidityDeviation;
        localeHumidity = limitToRange(localeHumidity, 0, 100);

        List<TileType> candidateTileTypes
            = new ArrayList<TileType>(candidates);

        // Filter the candidates by temperature.
        int i = 0;
        while (i < candidateTileTypes.size()) {
            TileType type = candidateTileTypes.get(i);
            if (!type.withinRange(TileType.RangeType.TEMPERATURE,
                                  localeTemperature)) {
                candidateTileTypes.remove(i);
                continue;
            }
            i++;
        }

        // Need to continue?
        switch (candidateTileTypes.size()) {
        case 0:
            throw new RuntimeException("No TileType for"
                + " temperature==" + localeTemperature);
        case 1:
            return candidateTileTypes.get(0);
        default:
            break;
        }

        // Filter the candidates by humidity.
        i = 0;
        while (i < candidateTileTypes.size()) {
            TileType type = candidateTileTypes.get(i);
            if (!type.withinRange(TileType.RangeType.HUMIDITY,
                                  localeHumidity)) {
                candidateTileTypes.remove(i);
                continue;
            }
            i++;
        }

        // Need to continue?
        switch (candidateTileTypes.size()) {
        case 0:
            throw new RuntimeException("No TileType for"
                + " humidity==" + localeHumidity);
        case 1:
            return candidateTileTypes.get(0);
        default:
            break;
        }

        // Filter the candidates by forest presence.
        boolean forested = Utils.randomInt(logger, "Forest", random, 100) < forestChance;
        i = 0;
        while (i < candidateTileTypes.size()) {
            TileType type = candidateTileTypes.get(i);
            if (type.isForested() != forested) {
                candidateTileTypes.remove(i);
                continue;
            }
            i++;
        }

        // Done
        switch (i = candidateTileTypes.size()) {
        case 0:
            throw new RuntimeException("No TileType for"
                + " forested==" + forested);
        case 1:
            return candidateTileTypes.get(0);
        default:
            return candidateTileTypes.get(Utils.randomInt(logger, "Forest tile",
                                                          random, i));
        }
    }

    /**
     * Select a random land tile on the map.
     *
     * @param map The <code>Map</code> to search in.
     * @param random A <code>Random</code> number source.
     * @return A random land tile, or null if none found.
     */
    public Tile getRandomLandTile(Map map, Random random) {
        int x = (map.getWidth() < 10)
            ? Utils.randomInt(logger, "W1", random, map.getWidth())
            : Utils.randomInt(logger, "W2", random, map.getWidth() - 10) + 5;
        int y = (map.getHeight() < 10)
            ? Utils.randomInt(logger, "H1", random, map.getHeight())
            : Utils.randomInt(logger, "H2", random, map.getHeight() - 10) + 5;
        for (Tile t : map.getCircleTiles(map.getTile(x, y), true,
                                         FreeColObject.INFINITY)) {
            if (t.isLand()) return t;
        }
        return null;
    }


    // Main functionality, create the map.

    /**
     * Creates a <code>Map</code> for the given <code>Game</code>.
     *
     * The <code>Map</code> is added to the <code>Game</code> after
     * it is created.
     *
     * @param game The <code>Game</code> to add the map to.
     * @param landMap Determines whether there should be land or ocean
     *     on a given tile.  This array also specifies the size of the
     *     map that is going to be created.
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
     * @param game The <code>Game</code> to add the map to.
     * @param importGame The <code>Game</code> to import information form.
     * @param landMap Determines whether there should be land or ocean
     *     on a given tile.  This array also specifies the size of the
     *     map that is going to be created.
     * @see Map
     */
    public void createMap(Game game, Game importGame, boolean[][] landMap) {
        final Specification spec = game.getSpecification();
        final int width = landMap.length;
        final int height = landMap[0].length;
        final boolean importTerrain = (importGame != null)
            && mapOptions.getBoolean(MapGeneratorOptions.IMPORT_TERRAIN);
        final boolean importBonuses = (importGame != null)
            && mapOptions.getBoolean(MapGeneratorOptions.IMPORT_BONUSES);

        boolean mapHasLand = false;
        Tile[][] tiles = new Tile[width][height];
        Map map = new Map(game, tiles);
        int minimumLatitude = mapOptions
            .getInteger(MapGeneratorOptions.MINIMUM_LATITUDE);
        int maximumLatitude = mapOptions
            .getInteger(MapGeneratorOptions.MAXIMUM_LATITUDE);
        // make sure the values are in range
        minimumLatitude = limitToRange(minimumLatitude, -90, 90);
        maximumLatitude = limitToRange(maximumLatitude, -90, 90);
        map.setMinimumLatitude(Math.min(minimumLatitude, maximumLatitude));
        map.setMaximumLatitude(Math.max(minimumLatitude, maximumLatitude));

        java.util.Map<String, ServerRegion> regionMap
            = new HashMap<String, ServerRegion>();
        if (importTerrain) { // Import the regions
            String ids = "";
            for (Region r : importGame.getMap().getRegions()) {
                ServerRegion region = new ServerRegion(game, r);
                map.putRegion(region);
                regionMap.put(r.getId(), region);
                ids += " " + region.getNameKey();
            }
            for (Region r : importGame.getMap().getRegions()) {
                ServerRegion region = regionMap.get(r.getId());
                Region x = r.getParent();
                if (x != null) x = regionMap.get(x.getId());
                region.setParent(x);
                for (Region c : r.getChildren()) {
                    x = regionMap.get(c.getId());
                    if (x != null) region.addChild(x);
                }
            }
            logger.info("Imported regions: " + ids);
        }

        List<Tile> fixRegions = new ArrayList<Tile>();
        for (int y = 0; y < height; y++) {
            int latitude = map.getLatitude(y);
            for (int x = 0; x < width; x++) {
                if (landMap[x][y]) mapHasLand = true;
                Tile t, importTile = null;
                if (importTerrain
                    && importGame.getMap().isValid(x, y)
                    && (importTile = importGame.getMap().getTile(x, y)) != null
                    && importTile.isLand() == landMap[x][y]) {
                    String id = importTile.getType().getId();
                    t = new Tile(game, spec.getTileType(id), x, y);
                    if (importTile.getMoveToEurope() != null) {
                        t.setMoveToEurope(importTile.getMoveToEurope());
                    }
                    if (importTile.getTileItemContainer() != null) {
                        TileItemContainer container
                            = new TileItemContainer(game, t);
                        // TileItemContainer copies every natural item
                        // including Resource unless importBonuses ==
                        // false Rumors and roads are not copied
                        container.copyFrom(importTile.getTileItemContainer(),
                            importBonuses, true);
                        t.setTileItemContainer(container);
                    }
                    Region r = importTile.getRegion();
                    if (r == null) {
                        fixRegions.add(t);
                    } else {
                        ServerRegion ours = regionMap.get(r.getId());
                        if (ours == null) {
                            logger.warning("Could not set tile region "
                                + r.getId() + " for tile: " + t);
                            fixRegions.add(t);
                        } else {
                            ours.addTile(t);
                        }
                    }
                } else {
                    t = createTile(game, x, y, landMap, latitude);
                }
                tiles[x][y] = t;
            }
        }
        game.setMap(map);
        geographicRegions = getStandardRegions(map);

        if (importTerrain) {
            if (!fixRegions.isEmpty()) { // Fix the tiles missing regions.
                createOceanRegions(map);
                createLakeRegions(map);
                createLandRegions(map);
            }
        } else {
            createOceanRegions(map);
            createHighSeas(map);
            if (mapHasLand) {
                createMountains(map);
                createRivers(map);
                createLakeRegions(map);
                createLandRegions(map);
            }
        }

        // Add the bonuses only after the map is completed.
        // Otherwise we risk creating resources on fields where they
        // don't belong (like sugar in large rivers or tobaco on hills).
        for (Tile tile : map.getAllTiles()) {
            perhapsAddBonus(tile, !importBonuses);
            if (!tile.isLand()) {
                encodeStyle(tile);
            }
        }

        map.resetContiguity();
        map.resetHighSeasCount();
    }

    /**
     * Creates ocean map regions in the given Map.
     *
     * Be careful to tolerate regions pre-existing from an imported game.
     * At the moment, the ocean is divided into two by two regions.
     *
     * @param map The <code>Map</code> to work on.
     */
    private void createOceanRegions(Map map) {
        Game game = map.getGame();
        ServerRegion pacific = (ServerRegion)map
            .getRegion("model.region.pacific");
        ServerRegion northPacific = (ServerRegion)map
            .getRegion("model.region.northPacific");
        ServerRegion southPacific = (ServerRegion)map
            .getRegion("model.region.southPacific");
        ServerRegion atlantic = (ServerRegion)map
            .getRegion("model.region.atlantic");
        ServerRegion northAtlantic = (ServerRegion)map
            .getRegion("model.region.northAtlantic");
        ServerRegion southAtlantic = (ServerRegion)map
            .getRegion("model.region.southAtlantic");
        int present = 0;
        if (pacific == null) {
            pacific = new ServerRegion(game,
                "model.region.pacific", RegionType.OCEAN, null);
            pacific.setDiscoverable(true);
            map.putRegion(pacific);
            pacific.setScoreValue(PACIFIC_SCORE_VALUE);
        }
        if (northPacific == null) {
            northPacific = new ServerRegion(game,
                "model.region.northPacific", RegionType.OCEAN, pacific);
            northPacific.setDiscoverable(false);
            map.putRegion(northPacific);
        } else present++;
        if (southPacific == null) {
            southPacific = new ServerRegion(game,
                "model.region.southPacific", RegionType.OCEAN, pacific);
            southPacific.setDiscoverable(false);
            map.putRegion(southPacific);
        } else present++;
        if (atlantic == null) {
            atlantic = new ServerRegion(game,
                "model.region.atlantic", RegionType.OCEAN, null);
            atlantic.setPrediscovered(true);
            atlantic.setDiscoverable(false);
            map.putRegion(atlantic);
        }
        if (northAtlantic == null) {
            northAtlantic = new ServerRegion(game,
                "model.region.northAtlantic", RegionType.OCEAN, atlantic);
            northAtlantic.setPrediscovered(true);
            northAtlantic.setDiscoverable(false);
            map.putRegion(northAtlantic);
        } else present++;
        if (southAtlantic == null) {
            southAtlantic = new ServerRegion(game,
                "model.region.southAtlantic", RegionType.OCEAN, atlantic);
            southAtlantic.setPrediscovered(true);
            southAtlantic.setDiscoverable(false);
            map.putRegion(southAtlantic);
        } else present++;

        if (present == 4) {
            // All the ocean regions were defined, no need to regenerate.
            return;
        }

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
        final int maxx = map.getWidth();
        final int midx = maxx / 2;
        final int maxy = map.getHeight();
        final int midy = maxy / 2;

        Position pNP = null, pSP = null, pNA = null, pSA = null;
        for (int y = midy-1; y >= 0; y--) {
            if (pNP == null && !map.getTile(0, y).isLand()) {
                pNP = new Position(0, y);
            }
            if (pNA == null && !map.getTile(maxx-1, y).isLand()) {
                pNA = new Position(maxx-1, y);
            }
            if (pNP != null && pNA != null) break;
        }
        for (int y = midy; y < maxy; y++) {
            if (pSP == null && !map.getTile(0, y).isLand()) {
                pSP = new Position(0, y);
            }
            if (pSA == null && !map.getTile(maxx-1, y).isLand()) {
                pSA = new Position(maxx-1, y);
            }
            if (pSP != null && pSA != null) break;
        }
        int nNP = 0, nSP = 0, nNA = 0, nSA = 0;

        Rectangle rNP = new Rectangle(0,0,       midx,midy);
        Rectangle rSP = new Rectangle(0,midy,    midx,maxy);
        Rectangle rNA = new Rectangle(midx,0,    maxx,midy);
        Rectangle rSA = new Rectangle(midx,midy, maxx,maxy);
        if (pNP != null) nNP += fillOcean(map, pNP, northPacific,  rNP);
        if (pSP != null) nSP += fillOcean(map, pSP, southPacific,  rSP);
        if (pNA != null) nNA += fillOcean(map, pNA, northAtlantic, rNA);
        if (pSA != null) nSA += fillOcean(map, pSA, southAtlantic, rSA);

        Rectangle rN = new Rectangle(0,0,    maxx,midy);
        Rectangle rS = new Rectangle(0,midy, maxx,maxy);
        if (pNP != null) nNP += fillOcean(map, pNP, northPacific,  rN);
        if (pSP != null) nSP += fillOcean(map, pSP, southPacific,  rS);
        if (pNA != null) nNA += fillOcean(map, pNA, northAtlantic, rN);
        if (pSA != null) nSA += fillOcean(map, pSA, southAtlantic, rS);

        Rectangle rAll = new Rectangle(0,0, maxx,maxy);
        if (pNP != null) nNP += fillOcean(map, pNP, northPacific,  rAll);
        if (pSP != null) nSP += fillOcean(map, pSP, southPacific,  rAll);
        if (pNA != null) nNA += fillOcean(map, pNA, northAtlantic, rAll);
        if (pSA != null) nSA += fillOcean(map, pSA, southAtlantic, rAll);

        if (nNP <= 0) logger.warning("No North Pacific tiles found");
        if (nSP <= 0) logger.warning("No South Pacific tiles found");
        if (nNA <= 0) logger.warning("No North Atlantic tiles found");
        if (nSA <= 0) logger.warning("No South Atlantic tiles found");
        logger.info("Ocean regions complete: "
            + nNP + " North Pacific, "
            + nSP + " South Pacific, "
            + nNA + " North Atlantic, "
            + nSP + " South Atlantic");
    }

    /**
     * Flood fill ocean regions.
     *
     * @param map The <code>Map</code> to fill in.
     * @param p A valid starting <code>Position</code>.
     * @param region A <code>ServerRegion</code> to fill with.
     * @param bounds A <code>Rectangle</code> that bounds the filling.
     * @return The number of tiles filled.
     */
    private int fillOcean(Map map, Position p, ServerRegion region,
                          Rectangle bounds) {
        Queue<Position> q = new LinkedList<Position>();
        int n = 0;
        boolean[][] visited = new boolean[map.getWidth()][map.getHeight()];
        visited[p.getX()][p.getY()] = true;
        q.add(p);

        while ((p = q.poll()) != null) {
            Tile tile = map.getTile(p);
            region.addTile(tile);
            n++;

            for (Direction direction : Direction.values()) {
                Position next = p.getAdjacent(direction);
                if (map.isValid(next)
                    && !visited[next.getX()][next.getY()]
                    && bounds.contains(next.getX(), next.getY())) {
                    visited[next.getX()][next.getY()] = true;
                    Tile t = map.getTile(next);
                    if ((t.getRegion() == null || t.getRegion() == region)
                        && !t.isLand()) {
                        q.add(next);
                    }
                }
            }
        }
        return n;
    }

    /**
     * Makes sure we have the standard regions.
     *
     * @param map The <code>Map</code> to work on.
     * @return An array of the standard geographic regions.
     */
    private ServerRegion[] getStandardRegions(Map map) {
        final Game game = map.getGame();

        // Create arctic/antarctic regions first, but only if they do
        // not exist in on the map already.  This allows for example
        // the imported Caribbean map to have arctic/antarctic regions
        // defined but with no tiles assigned to them, thus they will
        // not be seen on the map.  Generated games though will not have
        // the region defined, and so will create it here.
        final int arcticHeight = Map.POLAR_HEIGHT;
        final int antarcticHeight = map.getHeight() - Map.POLAR_HEIGHT - 1;
        ServerRegion arctic = (ServerRegion)map
            .getRegion("model.region.arctic");
        if (arctic == null) {
            arctic = new ServerRegion(game,
                "model.region.arctic", RegionType.LAND, null);
            arctic.setPrediscovered(true);
            map.putRegion(arctic);
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < arcticHeight; y++) {
                    if (map.isValid(x, y)) {
                        Tile tile = map.getTile(x, y);
                        if (tile.isLand()) arctic.addTile(tile);
                    }
                }
            }
        }
        ServerRegion antarctic = (ServerRegion)map
            .getRegion("model.region.antarctic");
        if (antarctic == null) {
            antarctic = new ServerRegion(game,
                "model.region.antarctic", RegionType.LAND, null);
            antarctic.setPrediscovered(true);
            map.putRegion(antarctic);
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = antarcticHeight; y < map.getHeight(); y++) {
                    if (map.isValid(x, y)) {
                        Tile tile = map.getTile(x, y);
                        if (tile.isLand()) antarctic.addTile(tile);
                    }
                }
            }
        }

        // Then, create "geographic" land regions.  These regions are
        // used by the MapGenerator to place Indian Settlements.  Note
        // that these regions are "virtual", i.e. having a bounding
        // box, but containing no tiles directly.
        final int thirdWidth = map.getWidth()/3;
        final int twoThirdWidth = 2 * thirdWidth;
        final int thirdHeight = map.getHeight()/3;
        final int twoThirdHeight = 2 * thirdHeight;
        ServerRegion northWest = (ServerRegion)map
            .getRegion("model.region.northWest");
        if (northWest == null) {
            northWest = new ServerRegion(game,
                "model.region.northWest", RegionType.LAND, null);
            map.putRegion(northWest);
        }
        northWest.setBounds(new Rectangle(0,0,
                thirdWidth,thirdHeight));
        northWest.setPrediscovered(true);
        ServerRegion north = (ServerRegion)map
            .getRegion("model.region.north");
        if (north == null) {
            north = new ServerRegion(game,
                "model.region.north", RegionType.LAND, null);
            map.putRegion(north);
        }
        north.setBounds(new Rectangle(thirdWidth,0,
                twoThirdWidth,thirdHeight));
        north.setPrediscovered(true);
        ServerRegion northEast = (ServerRegion)map
            .getRegion("model.region.northEast");
        if (northEast == null) {
            northEast = new ServerRegion(game,
                "model.region.northEast", RegionType.LAND, null);
            map.putRegion(northEast);
        }
        northEast.setBounds(new Rectangle(twoThirdWidth,0,
                map.getWidth(),thirdHeight));
        northEast.setPrediscovered(true);
        ServerRegion west = (ServerRegion)map
            .getRegion("model.region.west");
        if (west == null) {
            west = new ServerRegion(game,
                "model.region.west", RegionType.LAND, null);
            map.putRegion(west);
        }
        west.setBounds(new Rectangle(0,thirdHeight,
                thirdWidth,twoThirdHeight));
        west.setPrediscovered(true);
        ServerRegion center = (ServerRegion)map
            .getRegion("model.region.center");
        if (center == null) {
            center = new ServerRegion(game,
                "model.region.center", RegionType.LAND, null);
            map.putRegion(center);
        }
        center.setBounds(new Rectangle(thirdWidth,thirdHeight,
                twoThirdWidth,twoThirdHeight));
        center.setPrediscovered(true);
        ServerRegion east = (ServerRegion)map
            .getRegion("model.region.east");
        if (east == null) {
            east = new ServerRegion(game,
                "model.region.east", RegionType.LAND, null);
            map.putRegion(east);
        }
        east.setBounds(new Rectangle(twoThirdWidth,thirdHeight,
                map.getWidth(),twoThirdHeight));
        east.setPrediscovered(true);
        ServerRegion southWest = (ServerRegion)map
            .getRegion("model.region.southWest");
        if (southWest == null) {
            southWest = new ServerRegion(game,
                "model.region.southWest", RegionType.LAND, null);
            map.putRegion(southWest);
        }
        southWest.setBounds(new Rectangle(0,twoThirdHeight,
                thirdWidth,map.getHeight()));
        southWest.setPrediscovered(true);
        ServerRegion south = (ServerRegion)map
            .getRegion("model.region.south");
        if (south == null) {
            south = new ServerRegion(game,
                "model.region.south", RegionType.LAND, null);
            map.putRegion(south);
        }
        south.setBounds(new Rectangle(thirdWidth,twoThirdHeight,
                twoThirdWidth,map.getHeight()));
        south.setPrediscovered(true);
        ServerRegion southEast = (ServerRegion)map
            .getRegion("model.region.southEast");
        if (southEast == null) {
            southEast = new ServerRegion(game,
                "model.region.southEast", RegionType.LAND, null);
            map.putRegion(southEast);
        }
        southEast.setBounds(new Rectangle(twoThirdWidth,twoThirdHeight,
                map.getWidth(),map.getHeight()));
        southEast.setPrediscovered(true);
        return new ServerRegion[] {
            northWest, north, northEast,
            west, center, east,
            southWest, south, southEast };
    }

    /**
     * Creates land map regions in the given Map.
     *
     * First, the arctic/antarctic regions are defined, based on
     * <code>LandGenerator.POLAR_HEIGHT</code>.
     *
     * For the remaining land tiles, one region per contiguous
     * landmass is created.
     *
     * @param map The <code>Map</code> to work on.
     */
    private void createLandRegions(Map map) {
        Game game = map.getGame();

        // Create "explorable" land regions
        int continents = 0;
        boolean[][] landmap = new boolean[map.getWidth()][map.getHeight()];
        int[][] continentmap = new int[map.getWidth()][map.getHeight()];
        int landsize = 0;

        // Initialize both maps
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                continentmap[x][y] = 0;
                landmap[x][y] = false;
                if (map.isValid(x, y)) {
                    Tile tile = map.getTile(x, y);
                    // Exclude existing regions (arctic/antarctic, mountains,
                    // rivers).
                    landmap[x][y] = tile.isLand()
                        && tile.getRegion() == null;
                    if (tile.isLand()) landsize++;
                }
            }
        }

        // Flood fill, so that we end up with individual landmasses
        // numbered in continentmap[][]
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (landmap[x][y]) { // Found a new region.
                    continents++;
                    boolean[][] continent = Map.floodFill(landmap,
                        new Position(x,y));

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

        // Get landmass sizes
        int[] continentsize = new int[continents+1];
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                continentsize[continentmap[x][y]]++;
            }
        }

        // Go through landmasses, split up those too big
        int oldcontinents = continents;
        for (int c = 1; c <= oldcontinents; c++) {
            // c starting at 1, c=0 is all excluded tiles
            if (continentsize[c] > LAND_REGION_MAX_SIZE) {
                boolean[][] splitcontinent
                    = new boolean[map.getWidth()][map.getHeight()];
                Position splitposition = new Position(0,0);

                for (int x = 0; x < map.getWidth(); x++) {
                    for (int y = 0; y < map.getHeight(); y++) {
                        if (continentmap[x][y] == c) {
                            splitcontinent[x][y] = true;
                            splitposition = new Position(x,y);
                        } else {
                            splitcontinent[x][y] = false;
                        }
                    }
                }

                while (continentsize[c] > LAND_REGION_MAX_SIZE) {
                    int targetsize = LAND_REGION_MAX_SIZE;
                    if (continentsize[c] < 2*LAND_REGION_MAX_SIZE) {
                        targetsize = continentsize[c]/2;
                    }
                    continents++; //index of the new region in continentmap[][]
                    boolean[][] newregion
                        = Map.floodFill(splitcontinent, splitposition,
                                        targetsize);
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
        int landIndex = 1;
        for (int c = 1; c <= continents; c++) {
            // c starting at 1, c=0 is all water tiles
            String id;
            do {
                id = "model.region.land" + Integer.toString(landIndex++);
            } while (map.getRegion(id) != null);
            landregions[c] = new ServerRegion(map.getGame(), id,
                Region.RegionType.LAND, null);
            landregions[c].setDiscoverable(true);
            map.putRegion(landregions[c]);
        }

        // Add tiles to ServerRegions
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (continentmap[x][y] > 0) {
                    Tile tile = map.getTile(x, y);
                    landregions[continentmap[x][y]].addTile(tile);
                }
            }
        }

        for (int c = 1; c <= continents; c++) {
            ServerRegion sr = landregions[c];

            // Set exploration points for land regions based on size
            int score = Math.max((int)(((float)sr.getSize() / landsize)
                                       * LAND_REGIONS_SCORE_VALUE),
                                 LAND_REGION_MIN_SCORE);
            sr.setScoreValue(score);
            setGeographicRegion(sr);
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
     *
     * @param map The <code>Map</code> to create high seas on.
     */
    private void createHighSeas(Map map) {
        OptionGroup opt = mapOptions;
        createHighSeas(map, opt.getInteger(MapGeneratorOptions.DISTANCE_TO_HIGH_SEA),
                       opt.getInteger(MapGeneratorOptions.MAXIMUM_DISTANCE_TO_EDGE));
    }

    /**
     * Places "high seas"-tiles on the border of the given map.
     *
     * Public so it can be called from an action when the distance
     * parameters are changed.
     *
     * All other tiles previously of type High Seas will be set to Ocean.
     *
     * @param map The <code>Map</code> to create high seas on.
     * @param distToLandFromHighSeas The distance between the land
     *      and the high seas (given in tiles).
     * @param maxDistanceToEdge The maximum distance a high sea tile
     *      can have from the edge of the map.
     */
    public static void determineHighSeas(Map map, int distToLandFromHighSeas,
                                         int maxDistanceToEdge) {
        final Specification spec = map.getSpecification();
        final TileType ocean = spec.getTileType("model.tile.ocean");
        final TileType highSeas = spec.getTileType("model.tile.highSeas");
        if (highSeas == null || ocean == null) {
            throw new RuntimeException("Both Ocean and HighSeas TileTypes must be defined");
        }

        // Reset all highSeas tiles to the default ocean type.
        for (Tile t : map.getAllTiles()) {
            if (t.getType() == highSeas) t.setType(ocean);
        }

        // Recompute the new high seas layout.
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
    private static void createHighSeas(Map map, int distToLandFromHighSeas,
                                       int maxDistanceToEdge) {
        if (distToLandFromHighSeas < 0 || maxDistanceToEdge < 0) {
            throw new IllegalArgumentException("The integer arguments cannot be negative.");
        }

        final Specification spec = map.getSpecification();
        final TileType ocean = spec.getTileType("model.tile.ocean");
        final TileType highSeas = spec.getTileType("model.tile.highSeas");
        if (highSeas == null) {
            throw new RuntimeException("TileType highSeas must be defined.");
        }

        Tile t, seaL = null, seaR = null;
        int totalL = 0, totalR = 0, distanceL = -1, distanceR = -1;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < maxDistanceToEdge && x < map.getWidth()
                     && map.isValid(x, y)
                     && (t = map.getTile(x, y)).getType() == ocean; x++) {
                Tile other = map.getLandWithinDistance(x, y,
                                                       distToLandFromHighSeas);
                if (other == null) {
                    t.setType(highSeas);
                    totalL++;
                } else {
                    int distance = t.getDistanceTo(other);
                    if (distanceL < distance) {
                        distanceL = distance;
                        seaL = t;
                    }
                }
            }
            for (int x = 0; x < maxDistanceToEdge && x < map.getWidth()
                     && map.isValid(map.getWidth()-1-x, y)
                     && (t = map.getTile(map.getWidth()-1-x, y))
                     .getType() == ocean; x++) {
                Tile other = map.getLandWithinDistance(map.getWidth()-1-x, y,
                                                       distToLandFromHighSeas);
                if (other == null) {
                    t.setType(highSeas);
                    totalR++;
                } else {
                    int distance = t.getDistanceTo(other);
                    if (distanceR < distance) {
                        distanceR = distance;
                        seaR = t;
                    }
                }
            }
        }
        if (totalL <= 0 && seaL != null) {
            seaL.setType(highSeas);
            totalL++;
        }
        if (totalR <= 0 && seaR != null) {
            seaR.setType(highSeas);
            totalR++;
        }
        if (totalL <= 0 || totalR <= 0) {
            logger.warning("No high seas on "
                + ((totalL <= 0 && totalR <= 0) ? "either"
                    : (totalL <= 0) ? "left"
                    : (totalR <= 0) ? "right"
                    : "BOGUS") + " side of the map."
                + "  This can cause failures on small test maps.");
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
        // 50% of user settings will be allocated for random hills
        // here and there the rest will be allocated for large
        // mountain ranges
        int maximumLength
            = Math.max(mapOptions.getInteger(MapGeneratorOptions.MAP_WIDTH),
                mapOptions.getInteger(MapGeneratorOptions.MAP_HEIGHT)) / 10;
        int number = (int)((getApproximateLandCount()
                / mapOptions.getInteger(MapGeneratorOptions.MOUNTAIN_NUMBER))
            * (1 - randomHillsRatio));
        logger.info("Number of mountain tiles is " + number);
        logger.fine("Maximum length of mountain ranges is " + maximumLength);

        // lookup the resources from specification
        final Specification spec = map.getSpecification();
        TileType hills = spec.getTileType("model.tile.hills");
        TileType mountains = spec.getTileType("model.tile.mountains");
        if (hills == null || mountains == null) {
            throw new RuntimeException("Both Hills and Mountains TileTypes must be defined");
        }

        // Generate the mountain ranges
        int counter = 0;
        nextTry: for (int tries = 0; tries < 100; tries++) {
            if (counter < number) {
                Tile startTile = getRandomLandTile(map, random);
                if (startTile == null) return; // No land found!

                if (startTile.getType() == hills
                    || startTile.getType() == mountains) {
                    // already a high ground
                    continue;
                }

                // do not start a mountain range too close to another
                for (Tile t : startTile.getSurroundingTiles(3)) {
                    if (t.getType() == mountains) continue nextTry;
                }

                // Do not add a mountain range too close to the
                // ocean/lake this helps with good locations for
                // building colonies on shore
                for (Tile t : startTile.getSurroundingTiles(2)) {
                    if (!t.isLand()) continue nextTry;
                }

                ServerRegion mountainRegion = new ServerRegion(map.getGame(),
                    "model.region.mountain" + tries,
                    Region.RegionType.MOUNTAIN, startTile.getRegion());
                mountainRegion.setDiscoverable(true);
                mountainRegion.setClaimable(true);
                map.putRegion(mountainRegion);
                Direction direction = Direction.getRandomDirection("getLand",
                                                                   random);
                int length = maximumLength
                    - Utils.randomInt(logger, "MLen", random, maximumLength/2);
                for (int index = 0; index < length; index++) {
                    Tile nextTile = startTile.getNeighbourOrNull(direction);
                    if (nextTile == null || !nextTile.isLand()) continue;
                    nextTile.setType(mountains);
                    mountainRegion.addTile(nextTile);
                    counter++;
                    for (Tile neighbour : nextTile.getSurroundingTiles(1)) {
                        if (!neighbour.isLand()
                            || neighbour.getType() == mountains) continue;
                        int r = Utils.randomInt(logger, "MSiz", random, 8);
                        if (r == 0) {
                            neighbour.setType(mountains);
                            mountainRegion.addTile(neighbour);
                            counter++;
                        } else if (r > 2) {
                            neighbour.setType(hills);
                            mountainRegion.addTile(neighbour);
                        }
                    }
                }
                int scoreValue = 2 * mountainRegion.getSize();
                mountainRegion.setScoreValue(scoreValue);
                logger.fine("Created mountain region (direction " + direction
                    + ", length " + length
                    + ", size " + mountainRegion.getSize()
                    + ", score value " + scoreValue + ").");
            }
        }
        logger.info("Added " + counter + " mountain range tiles.");

        // and sprinkle a few random hills/mountains here and there
        number = (int) (getApproximateLandCount() * randomHillsRatio)
            / mapOptions.getInteger(MapGeneratorOptions.MOUNTAIN_NUMBER);
        counter = 0;
        nextTry: for (int tries = 0; tries < 1000; tries++) {
            if (counter < number) {
                Tile t = getRandomLandTile(map, random);
                if (t == null) return;

                if (t.getType() == hills || t.getType() == mountains) {
                    continue; // Already on high ground
                }

                // Do not add hills too close to a mountain range
                // this would defeat the purpose of adding random hills.
                for (Tile tile: t.getSurroundingTiles(3)) {
                    if (tile.getType() == mountains) continue nextTry;
                }

                // Do not add hills too close to the ocean/lake this
                // helps with good locations for building colonies on
                // shore.
                for (Tile tile: t.getSurroundingTiles(1)) {
                    if (!tile.isLand()) continue nextTry;
                }

                // 25% mountains, 75% hills
                boolean m = Utils.randomInt(logger, "MorH", random, 4) == 0;
                t.setType((m) ? mountains : hills);
                counter++;
            }
        }
        logger.info("Added " + counter + " random hills tiles.");
    }

    /**
     * Creates rivers on the given map. The number of rivers depends
     * on the map size.
     *
     * @param map The <code>Map</code> to create rivers on.
     */
    private void createRivers(Map map) {
        final Specification spec = map.getSpecification();
        final TileImprovementType riverType
            = spec.getTileImprovementType("model.improvement.river");
        final int number = getApproximateLandCount()
            / mapOptions.getInteger(MapGeneratorOptions.RIVER_NUMBER);
        int counter = 0;
        HashMap<Position, River> riverMap = new HashMap<Position, River>();
        List<River> rivers = new ArrayList<River>();

        for (int i = 0; i < number; i++) {
            nextTry: for (int tries = 0; tries < 100; tries++) {
                Tile tile = getRandomLandTile(map, random);
                if (tile == null) return;

                if (!tile.getType().canHaveImprovement(riverType)) {
                    continue;
                }

                // check the river source/spring is not too close to the ocean
                for (Tile neighborTile : tile.getSurroundingTiles(2)) {
                    if (!neighborTile.isLand()) {
                        continue nextTry;
                    }
                }
                if (riverMap.get(tile.getPosition()) == null) {
                    // no river here yet
                    ServerRegion riverRegion = new ServerRegion(map.getGame(),
                        "model.region.river" + i, Region.RegionType.RIVER,
                        tile.getRegion());
                    riverRegion.setDiscoverable(true);
                    riverRegion.setClaimable(true);
                    River river = new River(map, riverMap, riverRegion, random);
                    if (river.flowFromSource(tile.getPosition())) {
                        logger.fine("Created new river with length "
                            + river.getLength());
                        map.putRegion(riverRegion);
                        rivers.add(river);
                        counter++;
                    } else {
                        logger.fine("Failed to generate river.");
                    }
                    break;
                }
            }
        }
        logger.info("Created " + counter + " rivers of maximum " + number);

        for (River river : rivers) {
            ServerRegion region = river.getRegion();
            int scoreValue = 0;
            for (RiverSection section : river.getSections()) {
                scoreValue += section.getSize();
            }
            scoreValue *= 2;
            region.setScoreValue(scoreValue);
            logger.fine("Created river region (length " + river.getLength()
                + ", score value " + scoreValue + ").");
        }
    }

    /**
     * Finds all the lake regions.
     *
     * @param map The <code>Map</code> to work on.
     */
    private void createLakeRegions(Map map) {
        Game game = map.getGame();
        final TileType lakeType = map.getSpecification()
            .getTileType("model.tile.lake");

        // Create the water map, and find any tiles that are water but
        // not part of any region (such as the oceans).  These are
        // lake tiles.
        List<Tile> lakes = new ArrayList<Tile>();
        StringBuilder sb = new StringBuilder("Lakes at:");
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile;
                if (map.isValid(x, y)
                    && !(tile = map.getTile(x, y)).isLand()
                    && map.getTile(x, y).getRegion() == null) {
                    lakes.add(tile);
                    sb.append(" ").append(Integer.toString(x))
                        .append(",").append(Integer.toString(y));
                }
            }
        }
        logger.fine(sb.toString());

        // Make lake regions from unassigned lake tiles.
        int lakeCount = 0;
        while (!lakes.isEmpty()) {
            Tile tile = lakes.get(0);
            if (tile.getRegion() != null) continue;

            String id;
            while (game.getFreeColGameObject(id = "model.region.inlandLake"
                    + lakeCount) != null) lakeCount++;
            ServerRegion lakeRegion = new ServerRegion(game, id,
                                                       RegionType.LAKE, null);
            setGeographicRegion(lakeRegion);
            map.putRegion(lakeRegion);
            // Pretend lakes are discovered with the surrounding terrain?
            lakeRegion.setPrediscovered(false);
            List<Tile> todo = new ArrayList<Tile>();
            todo.add(tile);
            while (!todo.isEmpty()) {
                Tile t = todo.remove(0);
                if (lakes.contains(t)) {
                    t.setRegion(lakeRegion);
                    t.setType(lakeType);
                    lakes.remove(t);
                    todo.addAll(t.getSurroundingTiles(1, 1));
                }
            }
        }
    }

    /**
     * Adds a terrain bonus with a probability determined by the
     * <code>MapGeneratorOptions</code>.
     *
     * @param t The <code>Tile</code> to add bonuses to.
     * @param generateBonus Generate the bonus or not.
     */
    private void perhapsAddBonus(Tile t, boolean generateBonus) {
        final Specification spec = t.getSpecification();
        TileImprovementType fishBonusLandType
            = spec.getTileImprovementType("model.improvement.fishBonusLand");
        TileImprovementType fishBonusRiverType
            = spec.getTileImprovementType("model.improvement.fishBonusRiver");
        final int bonusNumber
            = mapOptions.getInteger(MapGeneratorOptions.BONUS_NUMBER);
        if (t.isLand()) {
            if (generateBonus
                && Utils.randomInt(logger, "Land Resource", random, 100) < bonusNumber) {
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

            // In Col1, ocean tiles with less than 3 land neighbours
            // produce 2 fish, all others produce 4 fish
            if (adjacentLand > 2) {
                t.add(new TileImprovement(t.getGame(), t, fishBonusLandType));
            }

            // In Col1, the ocean tile in front of a river mouth would
            // get an additional +1 bonus
            // TODO: This probably has some false positives, means
            // river tiles that are NOT a river mouth next to this tile!
            if (!t.hasRiver() && adjacentRiver) {
                t.add(new TileImprovement(t.getGame(), t, fishBonusRiverType));
            }

            if (t.getType().isHighSeasConnected()) {
                if (generateBonus && adjacentLand > 1
                    && Utils.randomInt(logger, "Sea resource", random,
                                       10 - adjacentLand) == 0) {
                    t.addResource(createResource(t));
                }
            } else {
                if (Utils.randomInt(logger, "Water resource", random,
                                    100) < bonusNumber) {
                    // Create random Bonus Resource
                    t.addResource(createResource(t));
                }
            }
        }
    }

    /**
     * Create a random resource on a tile.
     *
     * @param tile The <code>Tile</code> to create the resource on.
     * @return The created resource, or null if it is not possible.
     */
    private Resource createResource(Tile tile) {
        if (tile == null) return null;
        ResourceType resourceType = RandomChoice.getWeightedRandom(null, null,
            tile.getType().getWeightedResources(), random);
        if (resourceType == null) return null;
        int minValue = resourceType.getMinValue();
        int maxValue = resourceType.getMaxValue();
        int quantity = (minValue == maxValue) ? maxValue
            : (minValue + Utils.randomInt(logger, "Rsiz", random, 
                                          maxValue - minValue + 1));
        return new Resource(tile.getGame(), tile, resourceType, quantity);
    }

    /**
     * Sets the style of the tiles.
     * Only relevant to water tiles for now.
     * Public because it is used in the river generator.
     *
     * @param tile The <code>Tile</code> to set the style of.
     */
    public static void encodeStyle(Tile tile) {
        EnumMap<Direction, Boolean> connections
            = new EnumMap<Direction, Boolean>(Direction.class);

        // corners
        for (Direction d : Direction.corners) {
            Tile t = tile.getNeighbourOrNull(d);
            connections.put(d, t != null && t.isLand());
        }
        // edges
        for (Direction d : Direction.longSides) {
            Tile t = tile.getNeighbourOrNull(d);
            if (t != null && t.isLand()) {
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
        for (Direction d : Direction.corners) {
            if (connections.get(d)) result += (int)Math.pow(2, index);
            index++;
        }
        for (Direction d : Direction.longSides) {
            if (connections.get(d)) result += (int)Math.pow(2, index);
            index++;
        }
        tile.setStyle(result);
    }
}
