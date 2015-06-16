/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.LinkedList;
import java.util.Queue;

import java.awt.Rectangle;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;
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
     * @param game The <code>Game</code> to create in.
     */
    public ServerRegion(Game game, String id) {
        super(game, id);
    }

    /**
     * Constructor for copying in a new region from an imported game.
     */
    public ServerRegion(Game game, Region region) {
        super(game);

        this.name = region.getName();
        this.nameKey = region.getNameKey();
        this.parent = null;
        this.claimable = region.isClaimable();
        this.discoverable = region.isDiscoverable();
        this.discoveredIn = region.getDiscoveredIn();
        this.discoveredBy = region.getDiscoveredBy();
        this.prediscovered = region.isPrediscovered();
        this.scoreValue = region.getScoreValue();
        this.type = region.getType();
    }

    /**
     * Creates a new server region.
     *
     * @param game The <code>Game</code> to create in.
     * @param key The stem key of the region
     * @param type The <code>RegionType</code> to use.
     * @param parent The <code>Region</code> to be the parent of this one.
     */
    public ServerRegion(Game game, String key, RegionType type,
                        Region parent) {
        super(game);

        this.nameKey = Messages.nameKey(key);
        this.type = type;
        this.parent = parent;
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
     * @return The bounding <code>Rectangle</code>.
     */
    public final Rectangle getBounds() {
        return bounds;
    }

    /**
     * Set the bounding rectangle for this region.
     *
     * @param newBounds A new bounding <code>Rectangle</code>.
     */
    public final void setBounds(final Rectangle newBounds) {
        this.bounds = newBounds;
    }

    /**
     * Get the center of the regions bounds.
     *
     * @return An two element array [x,y] of the center coordinate.
     */
    public int[] getCenter() {
        return new int[] { bounds.x + bounds.width/2,
                           bounds.y + bounds.height/2 };
    }

    /**
     * Does this region contain the center of another?
     *
     * @param other The other <code>ServerRegion</code> to check.
     * @return True if the center of the other region is within this one.
     */
    public boolean containsCenter(ServerRegion other) {
        int[] xy = other.getCenter();
        return bounds.contains(xy[0], xy[1]);
    }

    /**
     * Add the given tile to this region.
     *
     * @param tile A <code>Tile</code> to add.
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
     * @param player The discovering <code>Player</code>.
     * @param turn The discovery <code>Turn</code>.
     * @param newName The name of the region.
     * @param cs A <code>ChangeSet</code> to update.
     */
    public void csDiscover(Player player, Turn turn, String newName,
                           ChangeSet cs) {
        final int score = (isDiscoverable()
            && getSpecification().getBoolean(GameOptions.EXPLORATION_POINTS)) 
            ? this.scoreValue
            : 0;
        if (!hasName()) this.name = newName;
        for (Region r : discover(player, turn)) cs.add(See.all(), r);
        HistoryEvent h = new HistoryEvent(turn,
            HistoryEvent.HistoryEventType.DISCOVER_REGION, player)
                .addStringTemplate("%nation%", player.getNationName())
                .addName("%region%", newName);
        h.setScore(score);
        cs.addGlobalHistory(getGame(), h);
    }

    /**
     * Make the fixed ocean regions if they do not exist in a given map.
     *
     * @param map The <code>Map</code> to check.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return A list of <code>ServerRegion</code>s.
     */     
    public static void makeFixedOceans(Map map, LogBuilder lb) {
        final Game game = map.getGame();
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
        if (pacific != null && atlantic != null
            && northPacific != null && northAtlantic != null
            && southPacific != null && southAtlantic != null) return;

        if (pacific == null) {
            pacific = new ServerRegion(game,
                "model.region.pacific", RegionType.OCEAN, null);
            pacific.setDiscoverable(true);
            pacific.setScoreValue(PACIFIC_SCORE_VALUE);
            map.putRegion(pacific);
        }
        if (atlantic == null) {
            atlantic = new ServerRegion(game,
                "model.region.atlantic", RegionType.OCEAN, null);
            atlantic.setPrediscovered(true);
            atlantic.setDiscoverable(false);
            map.putRegion(atlantic);
        }
        if (northPacific == null) {
            northPacific = new ServerRegion(game,
                "model.region.northPacific", RegionType.OCEAN, pacific);
            northPacific.setDiscoverable(false);
            map.putRegion(northPacific);
        }
        if (southPacific == null) {
            southPacific = new ServerRegion(game,
                "model.region.southPacific", RegionType.OCEAN, pacific);
            southPacific.setDiscoverable(false);
            map.putRegion(southPacific);
        }
        if (northAtlantic == null) {
            northAtlantic = new ServerRegion(game,
                "model.region.northAtlantic", RegionType.OCEAN, atlantic);
            northAtlantic.setPrediscovered(true);
            northAtlantic.setDiscoverable(false);
            map.putRegion(northAtlantic);
        }
        if (southAtlantic == null) {
            southAtlantic = new ServerRegion(game,
                "model.region.southAtlantic", RegionType.OCEAN, atlantic);
            southAtlantic.setPrediscovered(true);
            southAtlantic.setDiscoverable(false);
            map.putRegion(southAtlantic);
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
        if (nNP <= 0) lb.add("No North Pacific tiles found.\n");
        if (nSP <= 0) lb.add("No South Pacific tiles found");
        if (nNA <= 0) lb.add("No North Atlantic tiles found");
        if (nSA <= 0) lb.add("No South Atlantic tiles found");
        lb.add("Ocean regions complete: ",
            nNP, " North Pacific, ",
            nSP, " South Pacific, ",
            nNA, " North Atlantic, ",
            nSP, " South Atlantic");
    }

    /**
     * Flood fill ocean regions.
     *
     * @param map The <code>Map</code> to fill in.
     * @param tile A valid starting <code>Tile</code>.
     * @param region A <code>ServerRegion</code> to fill with.
     * @param bounds A <code>Rectangle</code> that bounds the filling.
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

    /**
     * Makes sure we have the standard regions.
     *
     * @param map The <code>Map</code> to work on.
     * @return An array of the standard geographic regions.
     */
    public static ServerRegion[] makeStandardRegions(Map map) {
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
        northWest.setBounds(new Rectangle(0,0,thirdWidth,thirdHeight));
        northWest.setPrediscovered(true);
        ServerRegion north = (ServerRegion)map
            .getRegion("model.region.north");
        if (north == null) {
            north = new ServerRegion(game,
                "model.region.north", RegionType.LAND, null);
            map.putRegion(north);
        }
        north.setBounds(new Rectangle(thirdWidth,0,thirdWidth,thirdHeight));
        north.setPrediscovered(true);
        ServerRegion northEast = (ServerRegion)map
            .getRegion("model.region.northEast");
        if (northEast == null) {
            northEast = new ServerRegion(game,
                "model.region.northEast", RegionType.LAND, null);
            map.putRegion(northEast);
        }
        northEast.setBounds(new Rectangle(twoThirdWidth,0,thirdWidth,thirdHeight));
        northEast.setPrediscovered(true);
        ServerRegion west = (ServerRegion)map
            .getRegion("model.region.west");
        if (west == null) {
            west = new ServerRegion(game,
                "model.region.west", RegionType.LAND, null);
            map.putRegion(west);
        }
        west.setBounds(new Rectangle(0,thirdHeight,thirdWidth,thirdHeight));
        west.setPrediscovered(true);
        ServerRegion center = (ServerRegion)map
            .getRegion("model.region.center");
        if (center == null) {
            center = new ServerRegion(game,
                "model.region.center", RegionType.LAND, null);
            map.putRegion(center);
        }
        center.setBounds(new Rectangle(thirdWidth,thirdHeight,thirdWidth,thirdHeight));
        center.setPrediscovered(true);
        ServerRegion east = (ServerRegion)map
            .getRegion("model.region.east");
        if (east == null) {
            east = new ServerRegion(game,
                "model.region.east", RegionType.LAND, null);
            map.putRegion(east);
        }
        east.setBounds(new Rectangle(twoThirdWidth,thirdHeight,thirdWidth,thirdHeight));
        east.setPrediscovered(true);
        ServerRegion southWest = (ServerRegion)map
            .getRegion("model.region.southWest");
        if (southWest == null) {
            southWest = new ServerRegion(game,
                "model.region.southWest", RegionType.LAND, null);
            map.putRegion(southWest);
        }
        southWest.setBounds(new Rectangle(0,twoThirdHeight,thirdWidth,thirdHeight));
        southWest.setPrediscovered(true);
        ServerRegion south = (ServerRegion)map
            .getRegion("model.region.south");
        if (south == null) {
            south = new ServerRegion(game,
                "model.region.south", RegionType.LAND, null);
            map.putRegion(south);
        }
        south.setBounds(new Rectangle(thirdWidth,twoThirdHeight,thirdWidth,thirdHeight));
        south.setPrediscovered(true);
        ServerRegion southEast = (ServerRegion)map
            .getRegion("model.region.southEast");
        if (southEast == null) {
            southEast = new ServerRegion(game,
                "model.region.southEast", RegionType.LAND, null);
            map.putRegion(southEast);
        }
        southEast.setBounds(new Rectangle(twoThirdWidth,twoThirdHeight,thirdWidth,thirdHeight));
        southEast.setPrediscovered(true);
        return new ServerRegion[] {
            northWest, north, northEast,
            west, center, east,
            southWest, south, southEast };
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" ").append((name == null) ? "(null)" : name)
            .append(" ").append(nameKey).append(" ").append(type)
            .append(" ").append(size).append(" ").append(bounds)
            .append("]");
        return sb.toString();
    }
}
