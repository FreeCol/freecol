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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.i18n.Messages;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * The directions a Unit can move to. Includes deltas for moving
 * to adjacent squares, which are required due to the isometric
 * map. Starting north and going clockwise.
*/
public enum Direction implements Named {
    N  ( 0, -2,  0, -2),
    NE ( 1, -1,  0, -1),
    E  ( 1,  0,  1,  0),
    SE ( 1,  1,  0,  1),
    S  ( 0,  2,  0,  2),
    SW ( 0,  1, -1,  1),
    W  (-1,  0, -1,  0),
    NW ( 0, -1, -1, -1);

    public final static int NUMBER_OF_DIRECTIONS = values().length;

    public static final List<Direction> allDirections
        = makeUnmodifiableList(Direction.N, Direction.NE,
                               Direction.E, Direction.SE,
                               Direction.S, Direction.SW,
                               Direction.W, Direction.NW);

    public static final List<Direction> longSides
        = makeUnmodifiableList(Direction.NE, Direction.SE,
                               Direction.SW, Direction.NW);

    public static final List<Direction> corners
        = makeUnmodifiableList(Direction.N, Direction.E,
                               Direction.S, Direction.W);
    
    /** The direction increments. */
    private final int oddDX;
    private final int oddDY;
    private final int evenDX;
    private final int evenDY;


    /**
     * Create a new direction with the given increments.
     *
     * @param oddDX Delta X/odd.
     * @param oddDY Delta y/odd.
     * @param evenDX Delta X/even.
     * @param evenDY Delta y/even.
     */
    Direction(int oddDX, int oddDY, int evenDX, int evenDY) {
        this.oddDX = oddDX;
        this.oddDY = oddDY;
        this.evenDX = evenDX;
        this.evenDY = evenDY;
    }


    /**
     * Step the x and y coordinates in this direction.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return The map position after the step.
     */
    public Map.Position step(int x, int y) {
        return ((y & 1) != 0)
            ? new Map.Position(x + oddDX, y + oddDY)
            : new Map.Position(x + evenDX, y + evenDY);
    }

    /**
     * Gets this direction rotated by n places.
     *
     * @param n The number of places to rotate
     *     (-#directions <= n <= #directions).
     * @return The rotated direction.
     */
    private Direction rotate(int n) {
        return values()[(ordinal() + n + NUMBER_OF_DIRECTIONS)
                        % NUMBER_OF_DIRECTIONS];
    }

    /**
     * Get the next direction after this one (clockwise).
     *
     * @return The next <code>Direction</code>.
     */
    public Direction getNextDirection() {
        return rotate(1);
    }

    /**
     * Get the previous direction after this one (anticlockwise).
     *
     * @return The previous <code>Direction</code>.
     */
    public Direction getPreviousDirection() {
        return rotate(-1);
    }

    /**
     * Gets the reverse direction of this one.
     *
     * @return The reverse <code>Direction</code>.
     */
    public Direction getReverseDirection() {
        return rotate(NUMBER_OF_DIRECTIONS/2);
    }

    /**
     * Gets the direction with east-west part mirrored.
     *
     * @return The mirrored <code>Direction</code>.
     */
    public Direction getEWMirroredDirection() {
        switch(this) {
        case NW: return Direction.NE;
        case W: return Direction.E;
        case SW: return Direction.SE;
        case NE: return Direction.NW;
        case E: return Direction.W;
        case SE: return Direction.SW;
        default: return this;
        }
    }

    /**
     * Gets a random Direction.
     *
     * @param logMe A string to log with the random results.
     * @param logger A <code>Logger</code> to log to.
     * @param random A <code>Random</code> number source.
     * @return A random <code>Direction</code> value.
     */
    public static Direction getRandomDirection(String logMe, Logger logger,
                                               Random random) {
        return values()[randomInt(logger, logMe, random,
                                  NUMBER_OF_DIRECTIONS)];
    }

    /**
     * Creates an array of the eight directions in a random order.
     *
     * @param logMe A string to log with the random results.
     * @param logger A <code>Logger</code> to log to.
     * @param random A <code>Random</code> number source.
     * @return An array of the <code>Direction</code>s in a random order.
     */
    public static Direction[] getRandomDirections(String logMe, Logger logger,
                                                  Random random) {
        List<Direction> directions = new ArrayList<>(allDirections);
        randomShuffle(logger, logMe, directions, random);
        return directions.toArray(new Direction[0]);
    }

    /**
     * Creates an array of the directions in an order that favours
     * a supplied one.  Entry 0 will be the supplied direction,
     * entry 1+2 will be those immediately to the left and right
     * of it (chosen randomly), and so on until the last entry
     * will be the complete reverse of the supplied direction.
     * 
     * Useful if we want to travel in a particular direction, but
     * if this fails to be able to try the closest other
     * directions to the original one in order.
     *
     * @param logMe A string to log with the random results.
     * @param logger A <code>Logger</code> to log to.
     * @param random A <code>Random</code> number source.
     * @return An array of the <code>Direction</code>s favouring this one.
     */
    public Direction[] getClosestDirections(String logMe, Logger logger,
                                            Random random) {
        // Will need 3 bits of randomness --- 2 directions are known,
        // need one bit to randomize each remaining pair.
        final int nbits = (NUMBER_OF_DIRECTIONS - 2) / 2;
        final int r = randomInt(logger, logMe, random, 1 << nbits);

        Direction[] ret = new Direction[NUMBER_OF_DIRECTIONS];
        ret[0] = this;

        int step = 1, mask = 1;
        for (int i = 1; i < NUMBER_OF_DIRECTIONS - 1; i += 2) {
            Direction dr = this.rotate(step);
            Direction dl = this.rotate(NUMBER_OF_DIRECTIONS - step);
            ret[i] = ((r & mask) == 0) ? dr : dl;
            ret[i+1] = ((r & mask) == 0) ? dl : dr;
            step += 1;
            mask *= 2;
        }

        ret[NUMBER_OF_DIRECTIONS-1] = this.getReverseDirection();
        return ret;
    }

    /**
     * Convert an angle (radians) to a direction.
     *
     * @param angle The angle to convert.
     * @return An equivalent <code>Direction</code>.
     */
    public static Direction angleToDirection(double angle) {
        return Direction.values()[(int)Math.floor(angle / (Math.PI/4))];
    }

    /**
     * Get a message key for this direction.
     *
     * @return A suitable message key.
     */
    public String getKey() {
        return "direction." + this; // Deliberately retain upper case
    }


    // Implement Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return Messages.nameKey("model." + getKey());
    }
}
