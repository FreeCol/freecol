package net.sf.freecol.server.generator;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
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
     * Random number generator.
     */
    private static Random random = new Random();

    /**
     * Directions a river may flow in. @see net.sf.freecol.common.model.Map.
     */
    private static int[] directions = {1, 3, 5, 7};

    /**
     * Possible direction changes for a river. @see net.sf.freecol.common.model.Map.
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
    private List sections = new ArrayList();

    /**
     * The next river.
     */
    private River nextRiver = null;

    /**
     * A hashtable of position-river pairs.
     */
    private Hashtable riverMap;


    /**
     * Constructor.
     *
     * @param map The map on which the river flows.
     */
    public River(Map map, Hashtable riverMap) {

        this.map = map;
        this.riverMap = riverMap;
        logger.info("Starting new river");
    }

    /**
     * Returns the length of this river.
     *
     * @return the length of this river.
     */
    public int getLength() {
        return this.sections.size();
    }

    /**
     * Adds a new section to this river.
     *
     * @param position Where this section is located.
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

        Iterator sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            section = (Section) sectionIterator.next();
            if (found) {
                section.grow();
            } else if (section.position.equals(position)) {
                section.addBranch(oppositeDirection(lastSection.direction));
                section.grow();
                found = true;
            }
        }
        drawToMap();
        if (nextRiver != null) {
            nextRiver.grow(section, map.getAdjacent(section.position, section.direction));
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
	    Map.Position px = map.getAdjacent(p, directions[i]);
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
	    Map.Position px = map.getAdjacent(p, directions[i]);
            if (!map.getTile(px).isLand() || map.getTile(px).hasRiver()) {
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

	Iterator sectionIterator = sections.iterator();
	while (sectionIterator.hasNext()) {
	    Map.Position q = (Map.Position) ((Section) sectionIterator.next()).position;
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
        if (!tile.isLand()) {
            logger.info("Tile at " + position + " is water.");
            return false;
        } else if (tile.getAddition() != Tile.ADD_NONE) {
            logger.info("Tile at " + position + " has additions.");
            return false;
        } else if (tile.getType() == Tile.DESERT) {
            logger.info("Tile at " + position + " is desert.");
            return false;
        } else if (tile.getType() == Tile.ARCTIC) {
            logger.info("Tile at " + position + " is arctic.");
            return false;
        } else if (isNextToWater(position)) {
            logger.info("Tile at " + position + " is next to water.");
            return false;
        } else {
            logger.info("Tile at " + position + " is suitable source.");
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
	    logger.info("Direction is now " + direction);
	}

	for (int i = 0; i < 3; i++) {
	    // add Map.NUMBER_OF_DIRECTIONS to ensure direction > 0
	    int dir = newDirection(i);
	    Map.Position newPosition = map.getAdjacent(source, dir);
	    Tile nextTile = map.getTile(newPosition);

            // is the tile suitable for this river?
	    if (nextTile.getAddition() == Tile.ADD_HILLS ||
                nextTile.getAddition() == Tile.ADD_MOUNTAINS) {
		logger.info("Tile at " + newPosition + " too high.");
		continue;
	    } else if (this.contains(newPosition)) {
		logger.info("Tile at " + newPosition + " is already in river.");
		continue;
	    } else if (isNextToSelf(newPosition)) {
		logger.info("Tile at " + newPosition + " is next to the river.");
		continue;
	    } else {
                // find out if an adjacent tile is next to water
                for (i = 0; i < 3; i++) {
                    int lastDir = newDirection(i, dir);
                    Map.Position px = map.getAdjacent(newPosition, lastDir);
                    Tile tile = map.getTile(px);
                    if (tile == null || !tile.isLand() || tile.hasRiver()) {

                        sections.add(new Section(source, dir));
                        Section lastSection = new Section(newPosition, lastDir);
                        sections.add(lastSection);

                        if (tile.hasRiver()) {
                            logger.info("Point " + newPosition + " is next to another river.");
                            drawToMap();
                            // increase the size of another river
                            nextRiver = (River) riverMap.get(px);
                            nextRiver.grow(lastSection, px);
                        } else {
                            // flow into the sea (or a lake)
                            logger.info("Point " + newPosition + " is next to water.");
                            sections.add(new Section(px, oppositeDirection(lastDir)));
                            drawToMap();
                        }
                        return true;
                    }
                }
                // this is not the case
                logger.info("Tile at " + newPosition + " is suitable.");
                sections.add(new Section(source, dir));
                return flow(newPosition);
	    }
	}
        sections = new ArrayList();
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
            offset = change[random.nextInt(3)];
        } else {
            offset = change[index];
        }
	return (currentDirection + offset + Map.NUMBER_OF_DIRECTIONS) %
            Map.NUMBER_OF_DIRECTIONS;
    }

    /**
     * Returns the opposite of the direction given.
     * 
     * @param direction The direction to reverse.
     * @return The opposite of the direction given.
     */
    private int oppositeDirection(int direction) {
        return (direction + Map.NUMBER_OF_DIRECTIONS/2) %
            Map.NUMBER_OF_DIRECTIONS;
    }

    /**
     * Draws the completed river to the map.
     */
    private void drawToMap() {

        int oldDirection = -1;

        Iterator sectionIterator = sections.iterator();
        while (sectionIterator.hasNext()) {
            Section section = (Section) sectionIterator.next();
            riverMap.put(section.position, this);
            if (oldDirection != -1) {
                section.addBranch(oldDirection);
            }
            Tile tile = map.getTile(section.position);
            switch (section.size) {
            case 1:
                tile.addRiver(Tile.ADD_RIVER_MINOR, section.getBranches());
                logger.info("Added minor river to tile at " + section.position);
                break;
            case 2:
                tile.addRiver(Tile.ADD_RIVER_MAJOR, section.getBranches());
                logger.info("Added major river to tile at " + section.position);
                break;
            default:
                tile.setType(Tile.OCEAN);
                logger.info("Created fjord at " + section.position);
            }
            oldDirection = oppositeDirection(section.direction);
	}
    }

    /**
     * A river section.
     */
    private class Section {
        private int[] base = {1, 2, 4, 8};

        public Map.Position position;
        public int size = 1;
        public int direction = -1;
        public int branch[] = {0, 0, 0, 0};

        public Section(Map.Position position, int direction) {
            this.position = position;
            this.direction = direction;
            this.branch[direction/2] = 1;
        }

        public void addBranch(int direction) {
            this.branch[direction/2] = 1;
        }

        /**
         * Returns the type of river.
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
        }

    }


}
