/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementStyle;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.server.model.ServerRegion;


/**
 * A river for the map generator.
 */
public class River {

    private static final Logger logger = Logger.getLogger(SimpleMapGenerator.class.getName());

    private TileImprovementType riverType;

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
    private Map map;

    /**
     * A list of river sections.
     */
    private List<RiverSection> sections = new ArrayList<RiverSection>();

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
    private java.util.Map<Position, River> riverMap;

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
    public River(Map map, java.util.Map<Position, River> riverMap,
                 ServerRegion region, Random random) {
        this.map = map;
        this.riverMap = riverMap;
        this.region = region;
        this.random = random;
        this.riverType = map.getSpecification()
            .getTileImprovementType("model.improvement.river");
        int index = random.nextInt(Direction.longSides.length);
        direction = Direction.longSides[index];
        logger.fine("Starting new river flowing " + direction.toString());
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
     * @param position Where this section is located.
     * @param direction The direction the river is flowing in.
     */
    public void add(Position position, Direction direction) {
        this.sections.add(new RiverSection(position, direction));
    }

    /**
     * Increases the size of this river.
     *
     * @param lastSection The last section of the river flowing into this one.
     * @param position The position of the confluence.
     */
    public void grow(RiverSection lastSection, Position position) {

        boolean found = false;

        for (RiverSection section : sections) {
            if (found) {
                section.grow();
            } else if (section.getPosition().equals(position)) {
                section.setBranch(lastSection.direction.getReverseDirection(),
                        lastSection.getSize());
                section.grow();
                found = true;
            }
        }
        drawToMap(sections);
        if (nextRiver != null) {
            RiverSection section = sections.get(sections.size() - 1);
            Position neighbor = section.getPosition().getAdjacent(section.direction);
            nextRiver.grow(section, neighbor);
        }
    }

    /**
     * Returns true if the given position is next to this river.
     *
     * @param p A map position.
     * @return true if the given position is next to this river.
     */
    public boolean isNextToSelf(Position p) {
        for (Direction direction : Direction.longSides) {
            Position px = p.getAdjacent(direction);
            if (this.contains(px)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given position is next to a river, lake or sea.
     *
     * @param p A map position.
     * @return true if the given position is next to a river, lake or sea.
     */
    public boolean isNextToWater(Position p) {
        for (Direction direction : Direction.longSides) {
            Position px = p.getAdjacent(direction);
            final Tile tile = map.getTile(px);
            if (tile == null) {
                continue;
            }
            if (!tile.isLand() || tile.hasRiver()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this river already contains the given position.
     *
     * @param p A map position.
     * @return true if this river already contains the given position.
     */
    public boolean contains(Position p) {
        Iterator<RiverSection> sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            Position q = sectionIterator.next().getPosition();
            if (p.equals(q)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a river flowing from the given position if possible.
     *
     * @param position A map position.
     * @return true if a river was created, false otherwise.
     */
    public boolean flowFromSource(Position position) {
        TileImprovementType riverType =
            map.getSpecification().getTileImprovementType("model.improvement.river");
        Tile tile = map.getTile(position);
        if (!tile.getType().canHaveImprovement(riverType)) {
            // Mountains, ocean cannot have rivers
            logger.fine("Tile (" + tile.getType().toString() + ") at "
                        + position + " cannot have rivers.");
            return false;
        } else if (isNextToWater(position)) {
            logger.fine("Tile at " + position + " is next to water.");
            return false;
        } else {
            logger.fine("Tile at " + position + " is suitable source.");
            return flow(position);
        }
    }

    /**
     * Lets the river flow from the given position.
     *
     * @param source A map position.
     * @return true if a river was created, false otherwise.
     */
    private boolean flow(Position source) {

        if (sections.size() % 2 == 0) {
            // get random new direction
            int length = DirectionChange.values().length;
            int index = random.nextInt(length);
            DirectionChange change = DirectionChange.values()[index];
            this.direction = change.getNewDirection(this.direction);
            logger.fine("Direction is now " + direction);
        }

        for (DirectionChange change : DirectionChange.values()) {
            Direction dir = change.getNewDirection(direction);
            Position newPosition = source.getAdjacent(dir);
            Tile nextTile = map.getTile(newPosition);

            if (nextTile == null) {
                continue;
            }
            // is the tile suitable for this river?
            if (!nextTile.getType().canHaveImprovement(riverType)) {
                // Mountains, ocean cannot have rivers
                logger.fine("Tile (" + nextTile.getType().toString() + ") at "
                            + newPosition + " cannot have rivers.");
                continue;
            } else if (this.contains(newPosition)) {
                logger.fine("Tile at " + newPosition + " is already in river.");
                continue;
            } else if (isNextToSelf(newPosition)) {
                logger.fine("Tile at " + newPosition + " is next to the river.");
                continue;
            } else {
                // find out if an adjacent tile is next to water
                for (DirectionChange change2 : DirectionChange.values()) {
                    Direction lastDir = change2.getNewDirection(dir);
                    Position px = newPosition.getAdjacent(lastDir);
                    Tile tile = map.getTile(px);
                    if (tile != null && (!tile.isLand() || tile.hasRiver())) {

                        sections.add(new RiverSection(source, dir));
                        RiverSection lastSection = new RiverSection(newPosition, lastDir);
                        sections.add(lastSection);

                        if (tile.hasRiver() && tile.isLand()) {
                            logger.fine("Point " + newPosition + " is next to another river.");
                            // increase the size of the other river
                            nextRiver = riverMap.get(px);
                            nextRiver.grow(lastSection, px);
                            // if the other river is connected, so is this one
                            connected |= nextRiver.connected;
                            // add this region to other river if too small
                            if (getLength() < 10) {
                                region = nextRiver.region;
                            }
                            drawToMap(sections);
                        } else {
                            // flow into the sea (or a lake)
                            logger.fine("Point " + newPosition + " is next to water.");
                            River someRiver = riverMap.get(px);
                            if (someRiver == null) {
                                sections.add(new RiverSection(px, lastDir.getReverseDirection()));
                                if (lastSection.getSize() < TileImprovement.FJORD_RIVER) {
                                    createDelta(newPosition, lastDir, lastSection);
                                }
                            } else {
                                RiverSection waterSection = someRiver.getLastSection();
                                waterSection.setBranch(lastDir.getReverseDirection(),
                                                       TileImprovement.SMALL_RIVER);
                            }
                            connected |= tile.isHighSeasConnected();
                            drawToMap(sections);
                        }
                        return true;
                    }
                }
                // this is not the case
                logger.fine("Tile at " + newPosition + " is suitable.");
                sections.add(new RiverSection(source, dir));
                return flow(newPosition);
            }
        }
        sections = new ArrayList<RiverSection>();
        return false;
    }

    /**
     * Describe <code>createDelta</code> method here.
     *
     * @param position a <code>Position</code> value
     * @param direction a <code>Direction</code> value
     */
    private void createDelta(Position position, Direction direction, RiverSection section) {
        delta(position, direction, section, DirectionChange.LEFT_TURN.getNewDirection(direction));
        delta(position, direction, section, DirectionChange.RIGHT_TURN.getNewDirection(direction));
    }

    private void delta(Position position, Direction direction, RiverSection section, Direction d) {
        Position p = position.getAdjacent(d);
        Tile tile = map.getTile(p);
        if (!tile.isLand()) {
            List<RiverSection> deltaSections = new ArrayList<RiverSection>();
            section.setBranch(d, TileImprovement.SMALL_RIVER);
            deltaSections.add(new RiverSection(p, d.getReverseDirection()));
            drawToMap(deltaSections);
        } else if (tile.getType().canHaveImprovement(riverType)) {
            Position p2 = p.getAdjacent(direction);
            Tile tile2 = map.getTile(p2);
            if (!tile2.isLand() && random.nextInt(2) == 0) {
                List<RiverSection> deltaSections = new ArrayList<RiverSection>();
                section.setBranch(d, TileImprovement.SMALL_RIVER);
                RiverSection rs = new RiverSection(p, direction);
                rs.setBranch(d.getReverseDirection(), TileImprovement.SMALL_RIVER);
                deltaSections.add(rs);
                rs = new RiverSection(p2, direction.getReverseDirection());
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
            riverMap.put(section.getPosition(), this);
            if (oldSection != null) {
                section.setBranch(oldSection.direction.getReverseDirection(),
                        oldSection.getSize());
            }
            Tile tile = map.getTile(section.getPosition());
            if (tile.isLand()) {
                if (section.getSize() >= TileImprovement.FJORD_RIVER) {
                    TileType greatRiver = map.getSpecification().getTileType("model.tile.greatRiver");
                    tile.setType(greatRiver);
                    // changing the type resets the improvements
                    //container.addRiver(section.getSize(), section.encodeStyle());
                    logger.fine("Added fjord (magnitude: " + section.getSize() +
                                ") to tile at " + section.getPosition());
                } else if (section.getSize() > TileImprovement.NO_RIVER) {
                    TileItemContainer container = tile.getTileItemContainer();
                    if (container == null) {
                        container = new TileItemContainer(tile.getGame(), tile);
                        tile.setTileItemContainer(container);
                    }
                    container.addRiver(section.getSize(), TileImprovementStyle.getInstance(section.encodeStyle()));
                    logger.fine("Added river (magnitude: " + section.getSize() +
                                ") to tile at " + section.getPosition());
                }
                region.addTile(tile);
                oldSection = section;
            }
        }
    }
}
