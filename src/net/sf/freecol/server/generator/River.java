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

package net.sf.freecol.server.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.server.model.ServerRegion;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * A river for the map generator.
 */
public class River {

    private static final Logger logger = Logger.getLogger(SimpleMapGenerator.class.getName());

    private final TileImprovementType riverType;

    /**
     * Possible direction changes for a river.
     * @see net.sf.freecol.common.model.Map
     */
    private static enum DirectionChange {
        STRAIGHT_AHEAD,
        RIGHT_TURN,
        LEFT_TURN;

        public Direction getNewDirection(Direction oldDirection) {
            switch(this) {
            case STRAIGHT_AHEAD:
                return oldDirection;
            case RIGHT_TURN:
                switch(oldDirection) {
                case NE:
                    return Direction.SE;
                case SE:
                    return Direction.SW;
                case SW:
                    return Direction.NW;
                case NW:
                    return Direction.NE;
                default:
                    return oldDirection;
                }
            case LEFT_TURN:
                switch(oldDirection) {
                case NE:
                    return Direction.NW;
                case SE:
                    return Direction.NE;
                case SW:
                    return Direction.SE;
                case NW:
                    return Direction.SW;
                default:
                    return oldDirection;
                }
            }
            return oldDirection;
        }
    }

    /**
     * Current direction the river is flowing in.
     */
    private Direction direction;

    /**
     * The map on which the river flows.
     */
    private final Map map;

    /**
     * A list of river sections.
     */
    private List<RiverSection> sections = new ArrayList<>();

    /**
     * The next river.
     */
    private River nextRiver = null;

    /**
     * The ServerRegion this River belongs to.
     */
    private ServerRegion region;

    /**
     * The random number source.
     */
    private final Random random;

    /**
     * A hashtable of position-river pairs.
     */
    private final java.util.Map<Tile, River> riverMap;

    /**
     * Whether the river is connected to the high seas.
     */
    private boolean connected = false;


    /**
     * Constructor.
     *
     * @param map The map on which the river flows.
     * @param riverMap A hashtable of position-river pairs.
     * @param region The region for this river.
     * @param random The <code>Random</code> number source to use.
     */
    public River(Map map, java.util.Map<Tile, River> riverMap,
                 ServerRegion region, Random random) {
        this.map = map;
        this.riverMap = riverMap;
        this.region = region;
        this.random = random;
        this.riverType = map.getSpecification()
            .getTileImprovementType("model.improvement.river");
        this.direction = getRandomMember(logger, "River", Direction.longSides,
                                         random);
        logger.fine("Starting new river flowing " + direction);
    }

    public List<RiverSection> getSections() {
        return sections;
    }

    /**
     * Returns the length of this river.
     *
     * @return the length of this river.
     */
    public int getLength() {
        return this.sections.size();
    }

    public RiverSection getLastSection() {
        return this.sections.get(sections.size() - 1);
    }

    /**
     * Get the <code>ServerRegion</code> value.
     *
     * @return a <code>ServerRegion</code> value
     */
    public final ServerRegion getRegion() {
        return region;
    }

    /**
     * Set the <code>ServerRegion</code> value.
     *
     * @param newServerRegion The new ServerRegion value.
     */
    public final void setRegion(final ServerRegion newServerRegion) {
        this.region = newServerRegion;
    }

    /**
     * Adds a new section to this river.
     *
     * @param tile The <code>Tile</code> where this section is located.
     * @param direction The <code>Direction</code> the river is flowing in.
     */
    public void add(Tile tile, Direction direction) {
        this.sections.add(new RiverSection(tile, direction));
    }

    /**
     * Increases the size of this river.
     *
     * @param lastSection The last section of the river flowing into this one.
     * @param tile The <code>Tile</code> of the confluence.
     */
    public void grow(RiverSection lastSection, Tile tile) {

        boolean found = false;

        for (RiverSection section : sections) {
            if (found) {
                section.grow();
            } else if (section.getTile().equals(tile)) {
                section.setBranch(lastSection.direction.getReverseDirection(),
                                  lastSection.getSize());
                section.grow();
                found = true;
            }
        }
        drawToMap(sections);
        if (nextRiver != null) {
            RiverSection section = sections.get(sections.size() - 1);
            Tile neighbor = section.getTile().getNeighbourOrNull(section.direction);
            nextRiver.grow(section, neighbor);
        }
    }

    /**
     * Returns true if the given tile is next to this river.
     *
     * @param tile A map tile.
     * @return true if the given tile is next to this river.
     */
    public boolean isNextToSelf(Tile tile) {
        return any(Direction.longSides,
            d -> this.contains(tile.getNeighbourOrNull(d)));
    }

    /**
     * Returns true if the given tile is next to a river, lake or sea.
     *
     * @param tile A map tile.
     * @return true if the given tile is next to a river, lake or sea.
     */
    public boolean isNextToWater(Tile tile) {
        return any(Direction.longSides,
            d -> {
                Tile t = tile.getNeighbourOrNull(d);
                return t != null && (!t.isLand() || t.hasRiver());
            });
    }

    /**
     * Returns true if this river already contains the given tile.
     *
     * @param tile A map tile.
     * @return true if this river already contains the given tile.
     */
    public boolean contains(Tile tile) {
        return any(getSections(), rs -> rs.getTile() == tile);
    }

    /**
     * Creates a river flowing from the given tile if possible.
     *
     * @param tile An origin map <code>Tile</code>.
     * @return True if a river was created, false otherwise.
     */
    public boolean flowFromSource(Tile tile) {
        TileImprovementType riverType =
            map.getSpecification().getTileImprovementType("model.improvement.river");
        if (!riverType.isTileTypeAllowed(tile.getType())) {
            // Mountains, ocean cannot have rivers
            logger.fine("Tile (" + tile + ") can not have a river.");
            return false;
        } else if (isNextToWater(tile)) {
            logger.fine("Tile (" + tile + ") is next to water.");
            return false;
        } else {
            logger.fine("Tile (" + tile + ") is suitable source.");
            return flow(tile);
        }
    }

    /**
     * Lets the river flow from the given tile.
     *
     * @param source A map tile.
     * @return true if a river was created, false otherwise.
     */
    private boolean flow(Tile source) {

        if (sections.size() % 2 == 0) {
            // get random new direction
            int length = DirectionChange.values().length;
            int index = randomInt(logger, "Flow", random, length);
            DirectionChange change = DirectionChange.values()[index];
            this.direction = change.getNewDirection(this.direction);
            logger.fine("Direction is now " + direction);
        }

        for (DirectionChange change : DirectionChange.values()) {
            Direction dir = change.getNewDirection(direction);
            Tile nextTile = source.getNeighbourOrNull(dir);
            if (nextTile == null) continue;

            // is the tile suitable for this river?
            if (!riverType.isTileTypeAllowed(nextTile.getType())) {
                // Mountains, ocean cannot have rivers
                logger.fine("Tile (" + nextTile + ") can not have a river.");
                continue;
            } else if (this.contains(nextTile)) {
                logger.fine("Tile (" + nextTile + ") is already in river.");
                continue;
            } else if (isNextToSelf(nextTile)) {
                logger.fine("Tile (" + nextTile + ") is next to the river.");
                continue;
            } else {
                // find out if an adjacent tile is next to water
                for (DirectionChange change2 : DirectionChange.values()) {
                    Direction lastDir = change2.getNewDirection(dir);
                    Tile t = nextTile.getNeighbourOrNull(lastDir);
                    if (t == null) continue;
                    if (t.isLand() && !t.hasRiver()) continue;

                    sections.add(new RiverSection(source, dir));
                    RiverSection lastSection = new RiverSection(nextTile,
                            lastDir);
                    sections.add(lastSection);

                    if (t.hasRiver() && t.isLand()) {
                        logger.fine("Tile (" + t + ") is next to another river.");
                        // increase the size of the other river
                        nextRiver = riverMap.get(t);
                        nextRiver.grow(lastSection, t);
                        // if the other river is connected, so is this one
                        connected |= nextRiver.connected;
                        // add this region to other river if too small
                        if (getLength() < 10) {
                            region = nextRiver.region;
                        }
                        drawToMap(sections);
                    } else {
                        // flow into the sea (or a lake)
                        logger.fine("Tile (" + t + ") is next to water.");
                        River someRiver = riverMap.get(t);
                        if (someRiver == null) {
                            sections.add(new RiverSection(t, lastDir.getReverseDirection()));
                            if (lastSection.getSize() < TileImprovement.FJORD_RIVER) {
                                createDelta(nextTile, lastDir, lastSection);
                            }
                        } else {
                            RiverSection waterSection = someRiver.getLastSection();
                            waterSection.setBranch(lastDir.getReverseDirection(),
                                TileImprovement.SMALL_RIVER);
                        }
                        connected |= t.isHighSeasConnected();
                        drawToMap(sections);
                    }
                    return true;
                }
                // not next to water
                logger.fine("Tile (" + nextTile + ") is suitable.");
                sections.add(new RiverSection(source, dir));
                return flow(nextTile);
            }
        }
        sections = new ArrayList<>();
        return false;
    }

    private void createDelta(Tile tile, Direction direction, RiverSection section) {
        delta(tile, direction, section, DirectionChange.LEFT_TURN.getNewDirection(direction));
        delta(tile, direction, section, DirectionChange.RIGHT_TURN.getNewDirection(direction));
    }

    private void delta(Tile tile, Direction direction, RiverSection section, Direction d) {
        Tile t = tile.getNeighbourOrNull(d);
        if (!t.isLand()) {
            List<RiverSection> deltaSections = new ArrayList<>();
            section.setBranch(d, TileImprovement.SMALL_RIVER);
            deltaSections.add(new RiverSection(tile, d.getReverseDirection()));
            drawToMap(deltaSections);
        } else if (riverType.isTileTypeAllowed(t.getType())) {
            Tile t2 = t.getNeighbourOrNull(direction);
            if (!t2.isLand() && randomInt(logger, "Delta", random, 2) == 0) {
                List<RiverSection> deltaSections = new ArrayList<>();
                section.setBranch(d, TileImprovement.SMALL_RIVER);
                RiverSection rs = new RiverSection(t, direction);
                rs.setBranch(d.getReverseDirection(), TileImprovement.SMALL_RIVER);
                deltaSections.add(rs);
                rs = new RiverSection(t2, direction.getReverseDirection());
                deltaSections.add(rs);
                drawToMap(deltaSections);
            }
        }

    }

    /**
     * Draws the completed river to the map.
     */
    private void drawToMap(List<RiverSection> sections) {
        RiverSection oldSection = null;

        for (RiverSection section : sections) {
            riverMap.put(section.getTile(), this);
            if (oldSection != null) {
                section.setBranch(oldSection.direction.getReverseDirection(),
                                  oldSection.getSize());
            }
            Tile tile = section.getTile();
            if (tile.isLand()) {
                if (section.getSize() >= TileImprovement.FJORD_RIVER) {
                    TileType greatRiver = map.getSpecification().getTileType("model.tile.greatRiver");
                    tile.changeType(greatRiver);
                    // changing the type resets the improvements
                    //container.addRiver(section.getSize(), section.encodeStyle());
                    logger.fine("Added fjord (magnitude: " + section.getSize() +
                                ") to tile: " + section.getTile());
                } else if (section.getSize() > TileImprovement.NO_RIVER) {
                    String style = section.encodeStyle();
                    tile.addRiver(section.getSize(), style);
                    logger.fine("Added river"
                        + "(magnitude: " + section.getSize()
                        + " style: " + style);
                }
                region.addTile(tile);
                oldSection = section;
            }
        }
    }
}
