package net.sf.freecol.server.generator;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Map.Position;


/**
 * A river for the map generator.
 */
public class River {

    private static final Logger logger = Logger.getLogger(MapGenerator.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * Directions a river may flow in.
     * @see net.sf.freecol.common.model.Map
     */
    private static int[] directions = {1, 3, 5, 7};

    /**
     * Possible direction changes for a river.
     * @see net.sf.freecol.common.model.Map
     */
    private static int[] change = { 0, 2, -2 };

    /**
     * Current direction the river is flowing in.
     */
    private int direction = 1;

    /**
     * The map on which the river flows.
     */
    private Map map;
    
    /**
     * A list of river sections.
     */
    private ArrayList<Section> sections = new ArrayList<Section>();

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

    public Section getLastSection() {
        return this.sections.get(sections.size() - 1);
    }

    /**
     * Adds a new section to this river.
     *
     * @param position Where this section is located.
     * @param direction The direction the river is flowing in.
     */
    public void add(Map.Position position, int direction) {
        this.sections.add(new Section(position, direction));
    }

    /**
     * Increases the size of this river.
     *
     * @param lastSection The last section of the river flowing into this one.
     * @param position The position of the confluence.
     */
    public void grow(Section lastSection, Map.Position position) {
        
        boolean found = false;
        Section section = null;
        
        Iterator<Section> sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            section = sectionIterator.next();
            if (found) {
                section.grow();
            } else if (section.position.equals(position)) {
                section.addBranch(oppositeDirection(lastSection.direction),
                        lastSection.size);
                section.grow();
                found = true;
            }
        }
        drawToMap();
        if (nextRiver != null) {
            nextRiver.grow(section, Map.getAdjacent(section.position, section.direction));
        }
    }

    /**
     * Returns true if the given position is next to this river.
     *
     * @param p A map position.
     * @return true if the given position is next to this river.
     */
    public boolean isNextToSelf(Map.Position p) {
        for (int i = 0; i < 4; i++) {
            Map.Position px = Map.getAdjacent(p, directions[i]);
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
        for (int i = 0; i < 4; i++) {
            Map.Position px = Map.getAdjacent(p, directions[i]);
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
        Iterator<Section> sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            Map.Position q = sectionIterator.next().position;
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
            logger.fine("Tile (" + tile.getType().getName() + ") at "
                        + position + " cannot have rivers.");
            return false;
/*
        if (!tile.isLand()) {
            logger.fine("Tile at " + position + " is water.");
            return false;
        } else if (tile.getAddition() != Tile.ADD_NONE) {
            logger.fine("Tile at " + position + " has additions.");
            return false;
        } else if (tile.getType() == Tile.DESERT) {
            logger.fine("Tile at " + position + " is desert.");
            return false;
        } else if (tile.getType() == Tile.ARCTIC) {
            logger.fine("Tile at " + position + " is arctic.");
            return false;
*/
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
        
        for (int i = 0; i < 3; i++) {
            int dir = newDirection(i);
            Map.Position newPosition = Map.getAdjacent(source, dir);
            Tile nextTile = map.getTile(newPosition);
            
            if (nextTile == null) {
                continue;
            }
            // is the tile suitable for this river?
            if (!nextTile.getType().canHaveRiver()) {
                logger.fine("Tile (" + nextTile.getType().getName() + ") at "
                            + newPosition + " cannot have rivers.");
                continue;
            /*  Depreciated
            if (nextTile.getAddition() == Tile.ADD_HILLS ||
                    nextTile.getAddition() == Tile.ADD_MOUNTAINS) {
                logger.fine("Tile at " + newPosition + " too high.");
                continue;
            */
            } else if (this.contains(newPosition)) {
                logger.fine("Tile at " + newPosition + " is already in river.");
                continue;
            } else if (isNextToSelf(newPosition)) {
                logger.fine("Tile at " + newPosition + " is next to the river.");
                continue;
            } else {
                // find out if an adjacent tile is next to water
                for (i = 0; i < 3; i++) {
                    int lastDir = newDirection(i, dir);
                    Map.Position px = Map.getAdjacent(newPosition, lastDir);
                    Tile tile = map.getTile(px);
                    if (tile == null || !tile.isLand() || tile.hasRiver()) {
                        
                        sections.add(new Section(source, dir));
                        Section lastSection = new Section(newPosition, lastDir);
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
                                sections.add(new Section(px, oppositeDirection(lastDir)));
                            } else {
                                Section waterSection = someRiver.getLastSection();
                                waterSection.addBranch(oppositeDirection(lastDir), 1);
                            }
                            drawToMap();
                        }
                        return true;
                    }
                }
                // this is not the case
                logger.fine("Tile at " + newPosition + " is suitable.");
                sections.add(new Section(source, dir));
                return flow(newPosition);
            }
        }
        sections = new ArrayList<Section>();
        return false;
    }
    
    /**
     * Returns a new direction, but never the opposite of the current
     * direction.
     *
     * @param index The index of the direction change.
     * @return A new direction. @see net.sf.freecol.common.model.Map.
     */
    private int newDirection(int index) {
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
    private int newDirection(int index, int currentDirection) {
        int offset = 0;
        if (index < 0 || index > 2) {
            PseudoRandom random =
                map.getGame().getModelController().getPseudoRandom();
            offset = change[random.nextInt(3)];
        } else {
            offset = change[index];
        }
        return (currentDirection + offset + Map.NUMBER_OF_DIRECTIONS) 
                % Map.NUMBER_OF_DIRECTIONS;
    }

    /**
     * Returns the opposite of the direction given.
     * 
     * @param direction The direction to reverse.
     * @return The opposite of the direction given.
     */
    private int oppositeDirection(int direction) {
        return (direction + Map.NUMBER_OF_DIRECTIONS/2)
                % Map.NUMBER_OF_DIRECTIONS;
    }

    /**
     * Draws the completed river to the map.
     */
    private void drawToMap() {
        Section oldSection = null;
        
        Iterator<Section> sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            Section section = sectionIterator.next();
            riverMap.put(section.position, this);
            if (oldSection != null) {
                section.addBranch(oppositeDirection(oldSection.direction),
                        oldSection.size);
            }
            Tile tile = map.getTile(section.position);
            
            switch (section.size) {
            case 1:
            case 2:
                // tile.addRiver(Tile.ADD_RIVER_MAJOR, section.getBranches()); // Depreciated
                // tile.addRiver will process the neighbouring branches as well 
                tile.addRiver(section.size);
                logger.fine("Added river (magnitude: " + section.size + ") to tile at " + section.position);
                break;
            default:
                TileType ocean = FreeCol.getSpecification().getTileType("model.tile.ocean");
                tile.setType(ocean);
                logger.fine("Created fjord at " + section.position);
            }
            oldSection = section;
        }
    }
/*  Depreciated
    public static int updateRiver(int oldRiver, int direction, int addition) {
        //System.out.println("old = " + oldRiver + ", direction = " + direction +
        //", addition = " + addition);
        int[] base = {0, 1, 0, 3, 0, 9, 0, 27};
        if (base[direction] == 0) {
            // ignore these directions
            return oldRiver;
        }
        int branch = 0;
        if (addition == Tile.ADD_RIVER_MINOR) {
            branch = 1;
        } else if (addition == Tile.ADD_RIVER_MAJOR) {
            branch = 2;
        }

        int tmpRiver = oldRiver;
        int value = 0;
        for (int index = base.length - 1; index > direction ; index -= 2) {
            value = tmpRiver / base[index];
            tmpRiver -= value * base[index];
        }
        value = tmpRiver / base[direction];
        //System.out.println("value = " + value + ", tmpRiver = " + tmpRiver);
        if (value == branch) {
            // no changes
            return oldRiver;
        } else {
            int newRiver = oldRiver + (branch - value) * base[direction];
            //System.out.println("new = " + newRiver);
            return newRiver;
        }
    }
        
        
*/
    /**
     * A river section.
     */
    private class Section {
        private int[] base = {1, 3, 9, 27};

        public Map.Position position;
        public int size = 1;
        public int direction = -1;
        public int branch[] = {0, 0, 0, 0};

        public Section(Map.Position position, int direction) {
            this.position = position;
            this.direction = direction;
            this.branch[direction/2] = 1;
        }

        public void addBranch(int direction, int size) {
            if (size != 1) {
                size = 2;
            }
            this.branch[direction/2] = size;
        }

        /**
         * Returns the type of river.
         * @return The type of river.
         */
        public int getBranches() {
            int result = 0;
            for (int i = 0; i < 4; i++) {
                result += branch[i] * base[i];
            }
            return result;
        }

        /**
         * Increases the size of this section by one.
         */
        public void grow() {
            this.size++;
            this.branch[direction/2] = 2;
        }

    }
}
