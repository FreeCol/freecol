/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.server.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.LogBuilder;


/**
 * The server version of a region.
 */
public class ServerRegion extends Region {

    /** Score for being first to discover the pacific. */
    public static final int PACIFIC_SCORE_VALUE = 100;

    /** The size of this Region (number of Tiles). */
    private int size = 0;

    /** A Rectangle that contains all points of the Region. */
    private Rectangle bounds = new Rectangle();


    /**
     * Trivial constructor for serialization.
     *
     * @param game The {@code Game} to create in.
     * @param id The object identifier.
     */
    public ServerRegion(Game game, String id) {
        super(game, id);
    }

    /**
     * Constructor for copying in a new region from an imported game.
     *
     * @param game The {@code Game} to create in.
     * @param region A {@code Region} to copy.
     */
    public ServerRegion(Game game, Region region) {
        super(game);

        this.name = region.getName();
        this.key = region.getKey();
        this.type = region.getType();
        this.parent = null; // Has to be fixed up elsewhere
        this.claimable = region.getClaimable();
        this.discoverable = region.getDiscoverable();
        this.discoveredIn = region.getDiscoveredIn();
        this.discoveredBy = region.getDiscoveredBy();
        this.scoreValue = region.getScoreValue();
    }

    /**
     * Create a new discoverable server region of a given type.
     *
     * @param game The {@code Game} to create in.
     * @param type The {@code RegionType} to use.
     */
    public ServerRegion(Game game, RegionType type) {
        this(game, null, type, null);

        this.setClaimable(type.getClaimable());
        this.setDiscoverable(true);
    }
    
    /**
     * Create a new fixed server region.
     *
     * @param map The {@code Map} to create in.
     * @param key The key for the region.
     * @param type The {@code RegionType} to use.
     * @param parent The {@code Region} to be the parent of this one.
     */
    private ServerRegion(Map map, String key, RegionType type,
                         Region parent) {
        this(map.getGame(), key, type, parent);

        map.addRegion(this);
    }

    /**
     * Create a new fixed server region.
     *
     * @param game The {@code Game} to create in.
     * @param key The key for the region.
     * @param type The {@code RegionType} to use.
     * @param parent The {@code Region} to be the parent of this one.
     */
    private ServerRegion(Game game, String key, RegionType type,
                         Region parent) {
        super(game);

        this.key = key;
        this.name = null;
        this.type = type;
        this.parent = parent;
        if (this.parent != null) this.parent.addChild(this);
        this.claimable = false;
        this.discoverable = false;
        this.discoveredIn = null;
        this.discoveredBy = null;
        this.scoreValue = 0;
    }


    /**
     * Get the number of tiles in this region.
     *
     * @return The number of tiles in this region.
     */
    public final int getSize() {
        return size;
    }

    /**
     * Set the number of tiles in this region.
     *
     * @param size The new number of tiles.
     */
    public final void setSize(final int size) {
        this.size = size;
    }

    /**
     * Get the bounding rectangle for this region.
     *
     * @return The bounding {@code Rectangle}.
     */
    public final Rectangle getBounds() {
        return bounds;
    }

    /**
     * Is this a geographic region?
     *
     * @return True if this is a geographic region.
     */
    public final boolean isGeographic() {
        return this.key != null && this.type == RegionType.LAND;
    }

    /**
     * Get the center of the regions bounds.
     *
     * @return An two member array [x,y] of the center coordinate.
     */
    public int[] getCenter() {
        return new int[] { bounds.x + bounds.width/2,
                           bounds.y + bounds.height/2 };
    }

    /**
     * Does this region contain the center of another?
     *
     * @param other The other {@code ServerRegion} to check.
     * @return True if the center of the other region is within this one.
     */
    public boolean containsCenter(ServerRegion other) {
        int[] xy = other.getCenter();
        return bounds.contains(xy[0], xy[1]);
    }

    /**
     * Add the given tile to this region.
     *
     * @param tile A {@code Tile} to add.
     */
    public void addTile(Tile tile) {
        tile.setRegion(this);
        size++;
        if (bounds.x == 0 && bounds.width == 0
            || bounds.y == 0 && bounds.height == 0) {
            bounds.setBounds(tile.getX(), tile.getY(), 0, 0);
        } else {
            bounds.add(tile.getX(), tile.getY());
        }
    }

    /**
     * Discover this region.
     *
     * @param player The discovering {@code Player}.
     * @param unit The discovering {@code Unit}.
     * @param turn The discovery {@code Turn}.
     * @param newName The name of the region.
     * @param cs A {@code ChangeSet} to update.
     */
    public void csDiscover(Player player, Unit unit, Turn turn,
                           String newName, ChangeSet cs) {
        if (!getDiscoverable()) return;
        final int score = (getSpecification().getBoolean(GameOptions.EXPLORATION_POINTS)) 
            ? this.scoreValue
            : 0;
        if (!hasName()) this.name = newName;
        for (Region r : discover(player, unit, turn)) cs.add(See.all(), r);
        HistoryEvent h = new HistoryEvent(turn,
            HistoryEvent.HistoryEventType.DISCOVER_REGION, player)
                .addStringTemplate("%nation%", player.getNationLabel())
                .addName("%region%", newName);
        h.setScore(score);
        cs.addGlobalHistory(getGame(), h);
    }
    
    /**
     * Determines how to separate the given map into three
     * parts (north, center and south) with equal amount of land tiles
     * for each.
     *  
     * @param map The map.
     * @return The indexes separating the three parts.
     */
    private static int[] findYForThirdsOfLand(Map map) {
        final int[] landTiles = new int[map.getHeight() - 1];
        long sum = 0;
        for (Tile t : map) {
            if (t.isLand() && !t.isPolar()) {
                landTiles[t.getY()]++;
                sum++;
            }
        }
        
        return findThirdsOfValues(landTiles, sum);
    }
    
    /**
     * Determines how to separate the given submap into three
     * parts (west, center and east) with equal amount of land tiles
     * for each.
     *  
     * @param map The map.
     * @param startY The starting Y-coordinate for the submap (inclusive).
     * @param endY The ending Y-coordinate for the submap (inclusive).
     * @return The indexes separating the three parts.
     */
    private static int[] findXForThirdsOfLand(Map map, int startY, int endY) {
        final List<Tile> subMap = map.subMap(0, startY, map.getWidth() - 1, endY - startY + 1);
        
        final int[] landTiles = new int[map.getWidth() - 1];
        long sum = 0;
        for (Tile t : subMap) {
            if (t.isLand() && !t.isPolar()) {
                landTiles[t.getX()]++;
                sum++;
            }
        }
        
        return findThirdsOfValues(landTiles, sum);
    }
    
    /**
     * Finds the indexes  for separating the given array into three parts.
     * @param timesTheValue An array containing the number of elements for
     *      each number (as given by the index).
     * @param sum The total sum.
     * @return The indexes separating the three parts.
     */
    private static int[] findThirdsOfValues(int[] timesTheValue, long sum) {
        final int[] result = new int[2];
        int accumulatedSum = 0;
        boolean needFirst = true;
        for (int i=0; i<timesTheValue.length; i++) {
            accumulatedSum += timesTheValue[i];
            if (needFirst && accumulatedSum >= sum / 3) {
                result[0] = i;
                needFirst = false;
            }
            if (accumulatedSum >= (2 * sum) / 3) {
                result[1] = i;
                break;
            }
        }
        return result;
    }
    
    private static Rectangle ensureWithinMap(Map map, Rectangle bounds) {
        final int capX = Math.max(0, bounds.x);
        final int capY = Math.max(0, bounds.y);
        final int capWidth = Math.min(map.getWidth() - 1, bounds.x + Math.min(map.getWidth() - 1, bounds.width)) - capX;
        final int capHeight = Math.min(map.getHeight() - 1, bounds.y + Math.min(map.getHeight() - 1, bounds.height)) - capY;
        
        return new Rectangle(capX, capY, capWidth, capHeight);
    }

    /**
     * Make the fixed regions if they do not exist in a given map.
     *
     * @param map The {@code Map} to check.
     * @param lb A {@code LogBuilder} to log to.
     * @return A list of fixed server regions.
     */     
    public static List<ServerRegion> requireFixedRegions(Map map, LogBuilder lb) {
        final java.util.Map<String, Region> fixed = map.getFixedRegions();
        List<ServerRegion> result = new ArrayList<>(16);

        lb.add("Add regions ");
        // Create arctic/antarctic regions first, but only if they do
        // not exist in on the map already.  This allows for example
        // the imported Caribbean map to have arctic/antarctic regions
        // defined but with no tiles assigned to them, thus they will
        // not be seen on the map.  Generated games though will not
        // have the region defined, and so will create it here.
        final int arcticHeight = Map.POLAR_HEIGHT;
        ServerRegion arctic = (ServerRegion)fixed.get("model.region.arctic");
        if (arctic == null) {
            arctic = new ServerRegion(map, "model.region.arctic",
                                      RegionType.LAND, null);
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < arcticHeight; y++) {
                    if (map.isValid(x, y)) {
                        Tile tile = map.getTile(x, y);
                        if (tile.isLand()) arctic.addTile(tile);
                    }
                }
            }
            lb.add("+arctic");
        }
        result.add(arctic);
        final int antarcticHeight = map.getHeight() - Map.POLAR_HEIGHT - 1;
        ServerRegion antarctic = (ServerRegion)fixed.get("model.region.antarctic");
        if (antarctic == null) {
            antarctic = new ServerRegion(map, "model.region.antarctic",
                                         RegionType.LAND, null);
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = antarcticHeight; y < map.getHeight(); y++) {
                    if (map.isValid(x, y)) {
                        Tile tile = map.getTile(x, y);
                        if (tile.isLand()) antarctic.addTile(tile);
                    }
                }
            }
            lb.add("+antarctic");
        }
        result.add(antarctic);

        // Then, create "geographic" land regions.  These regions are
        // used by the MapGenerator to place Indian Settlements.  Note
        // that these regions are "virtual", i.e. having a bounding
        // box, but contain no tiles directly.
        final int[] yDelimiterThirds = findYForThirdsOfLand(map);
        
        int[] xDelimiterThirds = findXForThirdsOfLand(map, 0, yDelimiterThirds[0]);
        
        map.removeRegionsByKey("model.region.northWest");
        final ServerRegion northWest = new ServerRegion(map, "model.region.northWest", RegionType.LAND, null);
        northWest.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                0,
                0,
                xDelimiterThirds[0],
                yDelimiterThirds[0]
                )));
        map.addRegion(northWest);

        map.removeRegionsByKey("model.region.north");
        final ServerRegion north = new ServerRegion(map, "model.region.north",RegionType.LAND, null);
        north.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                xDelimiterThirds[0],
                0,
                xDelimiterThirds[1] - xDelimiterThirds[0],
                yDelimiterThirds[0]
                )));
        map.addRegion(north);

        map.removeRegionsByKey("model.region.northEast");
        final ServerRegion northEast = new ServerRegion(map, "model.region.northEast", RegionType.LAND, null);
        northEast.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                xDelimiterThirds[1],
                0,
                Integer.MAX_VALUE,
                yDelimiterThirds[0]
                )));
        map.addRegion(northEast);

        
        xDelimiterThirds = findXForThirdsOfLand(map, yDelimiterThirds[0], yDelimiterThirds[1]);
        
        map.removeRegionsByKey("model.region.west");
        final ServerRegion west = new ServerRegion(map, "model.region.west", RegionType.LAND, null);
        west.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                0,
                yDelimiterThirds[0],
                xDelimiterThirds[0],
                yDelimiterThirds[1] - yDelimiterThirds[0]
                )));
        map.addRegion(west);

        map.removeRegionsByKey("model.region.center");
        final ServerRegion center = new ServerRegion(map, "model.region.center", RegionType.LAND, null);
        center.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                xDelimiterThirds[0],
                yDelimiterThirds[0],
                xDelimiterThirds[1] - xDelimiterThirds[0],
                yDelimiterThirds[1] - yDelimiterThirds[0]
                )));
        map.addRegion(center);

        map.removeRegionsByKey("model.region.east");
        final ServerRegion east = new ServerRegion(map, "model.region.east", RegionType.LAND, null);
        east.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                xDelimiterThirds[1],
                yDelimiterThirds[0],
                Integer.MAX_VALUE,
                yDelimiterThirds[1] - yDelimiterThirds[0]
                )));
        map.addRegion(east);

        
        xDelimiterThirds = findXForThirdsOfLand(map, yDelimiterThirds[1], map.getHeight() - 1);
        
        map.removeRegionsByKey("model.region.southWest");
        final ServerRegion southWest = new ServerRegion(map, "model.region.southWest", RegionType.LAND, null);
        southWest.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                0,
                yDelimiterThirds[1],
                xDelimiterThirds[0],
                Integer.MAX_VALUE
                )));
        map.addRegion(southWest);

        map.removeRegionsByKey("model.region.south");
        final ServerRegion south = new ServerRegion(map, "model.region.south", RegionType.LAND, null);
        south.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                xDelimiterThirds[0],
                yDelimiterThirds[1],
                xDelimiterThirds[1] - xDelimiterThirds[0],
                Integer.MAX_VALUE
                )));
        map.addRegion(south);

        map.removeRegionsByKey("model.region.southEast");
        final ServerRegion southEast = new ServerRegion(map, "model.region.southEast", RegionType.LAND, null);
        southEast.bounds.setBounds(ensureWithinMap(map, new Rectangle(
                xDelimiterThirds[1],
                yDelimiterThirds[1],
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
                )));
        map.addRegion(southEast);

        boolean allOceans = true;
        ServerRegion pacific = (ServerRegion)fixed.get("model.region.pacific");
        if (pacific == null) {
            pacific = new ServerRegion(map, "model.region.pacific",
                                       RegionType.OCEAN, null);
            pacific.setDiscoverable(true);
            pacific.setScoreValue(PACIFIC_SCORE_VALUE);
            allOceans = false;
            lb.add("+pacific");
        }
        result.add(pacific);
        ServerRegion northPacific = (ServerRegion)fixed.get("model.region.northPacific");
        if (northPacific == null) {
            northPacific = new ServerRegion(map, "model.region.northPacific",
                                            RegionType.OCEAN, pacific);
            northPacific.setDiscoverable(false); // discovers parent
            allOceans = false;
            lb.add("+northPacific");
        }
        result.add(northPacific);
        ServerRegion southPacific = (ServerRegion)fixed.get("model.region.southPacific");
        if (southPacific == null) {
            southPacific = new ServerRegion(map, "model.region.southPacific",
                                            RegionType.OCEAN, pacific);
            southPacific.setDiscoverable(false); // discovers parent
            allOceans = false;
            lb.add("+southPacific");
        }
        result.add(southPacific);

        ServerRegion atlantic = (ServerRegion)fixed.get("model.region.atlantic");
        if (atlantic == null) {
            atlantic = new ServerRegion(map, "model.region.atlantic",
                                        RegionType.OCEAN, null);
            atlantic.setDiscoverable(false);
            allOceans = false;
            lb.add("+atlantic");
        }
        result.add(atlantic);
        ServerRegion northAtlantic = (ServerRegion)fixed.get("model.region.northAtlantic");
        if (northAtlantic == null) {
            northAtlantic = new ServerRegion(map, "model.region.northAtlantic",
                                             RegionType.OCEAN, atlantic);
            northAtlantic.setDiscoverable(false);
            allOceans = false;
            lb.add("+northAtlantic");
        }
        result.add(northAtlantic);
        ServerRegion southAtlantic = (ServerRegion)fixed.get("model.region.southAtlantic");
        if (southAtlantic == null) {
            southAtlantic = new ServerRegion(map, "model.region.southAtlantic",
                                             RegionType.OCEAN, atlantic);
            southAtlantic.setDiscoverable(false);
            allOceans = false;
            lb.add("+southAtlantic");
        }
        result.add(southAtlantic);
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
        if (!allOceans) {
            final int maxx = map.getWidth();
            final int midx = maxx / 2;
            final int maxy = map.getHeight();
            final int midy = maxy / 2;
            Tile tNP = null, tSP = null, tNA = null, tSA = null, t;
            for (int y = midy-1; y >= 0; y--) {
                if (tNP == null && !(t = map.getTile(0, y)).isLand()) tNP = t;
                if (tNA == null && !(t = map.getTile(maxx-1, y)).isLand()) tNA = t;
                if (tNP != null && tNA != null) break;
            }
            for (int y = midy; y < maxy; y++) {
                if (tSP == null && !(t = map.getTile(0, y)).isLand()) tSP = t;
                if (tSA == null && !(t = map.getTile(maxx-1, y)).isLand()) tSA = t;
                if (tSP != null && tSA != null) break;
            }
            int nNP = 0, nSP = 0, nNA = 0, nSA = 0;
            
            Rectangle rNP = new Rectangle(0,0,       midx,midy);
            Rectangle rSP = new Rectangle(0,midy,    midx,maxy-midy);
            Rectangle rNA = new Rectangle(midx,0,    maxx-midx,midy);
            Rectangle rSA = new Rectangle(midx,midy, maxx-midx,maxy-midy);
            if (tNP != null) nNP += fillOcean(map, tNP, northPacific,  rNP);
            if (tSP != null) nSP += fillOcean(map, tSP, southPacific,  rSP);
            if (tNA != null) nNA += fillOcean(map, tNA, northAtlantic, rNA);
            if (tSA != null) nSA += fillOcean(map, tSA, southAtlantic, rSA);

            Rectangle rN = new Rectangle(0,0,    maxx,midy);
            Rectangle rS = new Rectangle(0,midy, maxx,maxy-midy);
            if (tNP != null) nNP += fillOcean(map, tNP, northPacific,  rN);
            if (tSP != null) nSP += fillOcean(map, tSP, southPacific,  rS);
            if (tNA != null) nNA += fillOcean(map, tNA, northAtlantic, rN);
            if (tSA != null) nSA += fillOcean(map, tSA, southAtlantic, rS);

            Rectangle rAll = new Rectangle(0,0, maxx,maxy);
            if (tNP != null) nNP += fillOcean(map, tNP, northPacific,  rAll);
            if (tSP != null) nSP += fillOcean(map, tSP, southPacific,  rAll);
            if (tNA != null) nNA += fillOcean(map, tNA, northAtlantic, rAll);
            if (tSA != null) nSA += fillOcean(map, tSA, southAtlantic, rAll);
            lb.add(" filled ocean regions ",
                nNP, " North Pacific, ",
                nSP, " South Pacific, ",
                nNA, " North Atlantic, ",
                nSA, " South Atlantic.\n");
        }
        return result;
    }

    /**
     * Flood fill ocean regions.
     *
     * @param map The {@code Map} to fill in.
     * @param tile A valid starting {@code Tile}.
     * @param region A {@code ServerRegion} to fill with.
     * @param bounds A {@code Rectangle} that bounds the filling.
     * @return The number of tiles filled.
     */
    private static int fillOcean(Map map, Tile tile, ServerRegion region,
                                 Rectangle bounds) {
        Queue<Tile> q = new LinkedList<>();
        int n = 0;
        boolean[][] visited = new boolean[map.getWidth()][map.getHeight()];
        visited[tile.getX()][tile.getY()] = true;
        q.add(tile);

        while ((tile = q.poll()) != null) {
            region.addTile(tile);
            n++;

            for (Direction direction : Direction.values()) {
                Tile t = map.getAdjacentTile(tile, direction);
                if (t != null
                    && !visited[t.getX()][t.getY()]
                    && bounds.contains(t.getX(), t.getY())) {
                    visited[t.getX()][t.getY()] = true;
                    if ((t.getRegion() == null || t.getRegion() == region)
                        && !t.isLand()) {
                        q.add(t);
                    }
                }
            }
        }
        return n;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(super.toString());
        sb.setLength(sb.length() - 1);
        sb.append(' ').append(size).append(' ').append(bounds)
            .append(']');
        return sb.toString();
    }
}
