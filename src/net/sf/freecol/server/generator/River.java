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
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Map.Position;


/**
 * A river for the map generator.
 */
public class River {

    private static final Logger logger = Logger.getLogger(MapGenerator.class.getName());

    private static final Direction[] allDirections = Map.getDirectionArray();

    /**
     * Directions a river may flow in.
     * @see net.sf.freecol.common.model.Map
     */
    private static final Direction[] directions = {
        Direction.NE, Direction.SE, Direction.SW, Direction.NW};

    /**
     * Possible direction changes for a river.
     * @see net.sf.freecol.common.model.Map
     */
    private static final int STRAIGHT_AHEAD = 0;
    private static final int RIGHT_TURN = 2; // right angle turn (90 degrees) clockwise
    private static final int LEFT_TURN = -2; // right angle turn (90 degrees) counter clockwise
    private static int[] directionChanges = { STRAIGHT_AHEAD, RIGHT_TURN, LEFT_TURN };  

    /**
     * Current direction the river is flowing in.
     */
    private Direction direction = Direction.NE;
    
    /**
     * The map on which the river flows.
     */
    private Map map;
    
    /**
     * A list of river sections.
     */
    private ArrayList<RiverSection> sections = new ArrayList<RiverSection>();

    /**
     * The next river.
     */
    private River nextRiver = null;

    /**
     * A hashtable of position-river pairs.
     */
    private Hashtable<Position, River> riverMap;


    /**
     * Constructor.
     *
     * @param map The map on which the river flows.
     * @param riverMap A hashtable of position-river pairs.
     */
    public River(Map map, Hashtable<Position, River> riverMap) {
        this.map = map;
        this.riverMap = riverMap;
        logger.fine("Starting new river");
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
     * Adds a new section to this river.
     *
     * @param position Where this section is located.
     * @param direction The direction the river is flowing in.
     */
    public void add(Map.Position position, Direction direction) {
        this.sections.add(new RiverSection(position, direction));
    }

    /**
     * Increases the size of this river.
     *
     * @param lastSection The last section of the river flowing into this one.
     * @param position The position of the confluence.
     */
    public void grow(RiverSection lastSection, Map.Position position) {
        
        boolean found = false;
        RiverSection section = null;
        
        Iterator<RiverSection> sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            section = sectionIterator.next();
            if (found) {
                section.grow();
            } else if (section.getPosition().equals(position)) {
                section.setBranch(lastSection.direction.getReverseDirection(),
                        lastSection.getSize());
                section.grow();
                found = true;
            }
        }
        drawToMap();
        if (nextRiver != null) {
            Position neighbor = Map.getAdjacent(section.getPosition(), section.direction);
            nextRiver.grow(section, neighbor);
        }
    }

    /**
     * Returns true if the given position is next to this river.
     *
     * @param p A map position.
     * @return true if the given position is next to this river.
     */
    public boolean isNextToSelf(Map.Position p) {
        for (Direction direction : directions) {
            Map.Position px = Map.getAdjacent(p, direction);
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
    public boolean isNextToWater(Map.Position p) {
        for (Direction direction : directions) {
            Map.Position px = Map.getAdjacent(p, direction);
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
    public boolean contains(Map.Position p) {
        Iterator<RiverSection> sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            Map.Position q = sectionIterator.next().getPosition();
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
    public boolean flowFromSource(Map.Position position) {
        Tile tile = map.getTile(position);
        if (!tile.getType().canHaveRiver()) {
            // Mountains, ocean cannot have rivers
            logger.fine("Tile (" + tile.getType().getName() + ") at "
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
    private boolean flow(Map.Position source) {
        //Tile t = map.getTile(source);
        
        if (sections.size() % 2 == 0) {
            // get random new direction
            this.direction = newDirection(-1);
            logger.fine("Direction is now " + direction);
        }
        
        for (int i = 0; i < directionChanges.length; i++) {
            Direction dir = newDirection(i);
            Map.Position newPosition = Map.getAdjacent(source, dir);
            Tile nextTile = map.getTile(newPosition);
            
            if (nextTile == null) {
                continue;
            }
            // is the tile suitable for this river?
            if (!nextTile.getType().canHaveRiver()) {
                // Mountains, ocean cannot have rivers
                logger.fine("Tile (" + nextTile.getType().getName() + ") at "
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
                for (i = 0; i < directionChanges.length; i++) {
                    Direction lastDir = newDirection(i, dir);
                    Map.Position px = Map.getAdjacent(newPosition, lastDir);
                    Tile tile = map.getTile(px);
                    if (tile != null && (!tile.isLand() || tile.hasRiver())) {
                        
                        sections.add(new RiverSection(source, dir));
                        RiverSection lastSection = new RiverSection(newPosition, lastDir);
                        sections.add(lastSection);
                        
                        if (tile.hasRiver()) {
                            logger.fine("Point " + newPosition + " is next to another river.");
                            drawToMap();
                            // increase the size of another river
                            nextRiver = riverMap.get(px);
                            nextRiver.grow(lastSection, px);
                        } else {
                            // flow into the sea (or a lake)
                            logger.fine("Point " + newPosition + " is next to water.");
                            River someRiver = riverMap.get(px);
                            if (someRiver == null) {
                                sections.add(new RiverSection(px, lastDir.getReverseDirection()));
                            } else {
                                RiverSection waterSection = someRiver.getLastSection();
                                waterSection.setBranch(lastDir.getReverseDirection(),
                                                       TileImprovement.SMALL_RIVER);
                            }
                            drawToMap();
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
     * Returns a new direction, but never the opposite of the current
     * direction.
     *
     * @param index The index of the direction change.
     * @return A new direction. @see net.sf.freecol.common.model.Map.
     */
    private Direction newDirection(int index) {
        return newDirection(index, this.direction);
    }

    /**
     * Returns a new direction, but never the opposite of the given
     * direction.
     *
     * @param index The index of the direction change.
     * @param currentDirection The current direction.
     * @return A new direction. @see net.sf.freecol.common.model.Map.
     */
    private Direction newDirection(int index, Direction currentDirection) {
        int offset = 0;
        if (index < 0 || index >= directionChanges.length) {
            PseudoRandom random =
                map.getGame().getModelController().getPseudoRandom();
            offset = directionChanges[random.nextInt(directionChanges.length)];
        } else {
            offset = directionChanges[index];
        }
        index = (currentDirection.ordinal() + offset + allDirections.length) 
            % allDirections.length;
        return allDirections[index];
    }

    /**
     * Draws the completed river to the map.
     */
    private void drawToMap() {
        RiverSection oldSection = null;
        
        Iterator<RiverSection> sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            RiverSection section = sectionIterator.next();
            riverMap.put(section.getPosition(), this);
            if (oldSection != null) {
                section.setBranch(oldSection.direction.getReverseDirection(),
                        oldSection.getSize());
            }
            Tile tile = map.getTile(section.getPosition());
            
            if (section.getSize() == TileImprovement.SMALL_RIVER || 
                section.getSize() == TileImprovement.LARGE_RIVER) {
                // tile.addRiver(Tile.ADD_RIVER_MAJOR, section.getBranches()); // Depreciated
                // tile.addRiver will process the neighbouring branches as well 
                tile.addRiver(section.getSize());
                logger.fine("Added river (magnitude: " + section.getSize() + ") to tile at " + section.getPosition());
            }
            else if (section.getSize() >= TileImprovement.FJORD_RIVER) {
                TileType ocean = FreeCol.getSpecification().getTileType("model.tile.ocean");
                tile.setType(ocean);
                logger.fine("Created fjord at " + section.getPosition());
            }
            oldSection = section;
        }
    }
}
