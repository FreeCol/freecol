
package net.sf.freecol.server.ai.mission;

import net.sf.freecol.server.ai.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;

import org.w3c.dom.*;


/**
* A mission describes what a unit should do; attack, build colony, wander etc.
* Every {@link AIUnit} should have a mission. By extending this class,
* you create different missions.
*/
public abstract class Mission extends AIObject {
    private static final Logger logger = Logger.getLogger(Mission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    protected int NO_PATH_TO_TARGET = -2,
                  NO_MORE_MOVES_LEFT = -1;

    private AIUnit aiUnit;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public Mission(AIMain aiMain) {
        this(aiMain, null);
    }
    

    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public Mission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain);
        this.aiUnit = aiUnit;
    }

    
    /**
    * Moves the unit owning this mission towards the given <code>Tile</code>.
    * This is done in a loop until the tile is reached, there are no moves left or
    * that the path to the target cannot be found.
    *
    * @param tile The <code>Tile</code> the unit should move towards.
    * @return The dirction to take the final move (greater than or equal to zero),
    *         or {@link #NO_MORE_MOVES_LEFT} if there are no more moves left and
    *         {@link #NO_PATH_TO_TARGET} if there is no path to follow.
    *         If a direction is returned, it is guarantied that moving in that direction
    *         is not an {@link #ILLEGAL_MOVE}.
    */
    protected int moveTowards(Connection connection, Tile tile) {
        Map map = getAIMain().getGame().getMap();
        PathNode pathNode = getUnit().findPath(tile);
        if (pathNode != null) {
            int direction = pathNode.getDirection();
            int turns = pathNode.getTurns();
            while (pathNode != null && pathNode.getTile() != tile && pathNode.getTurns() == turns && getUnit().getMoveType(direction) == Unit.MOVE) {
                move(connection, direction);
                pathNode = pathNode.next;
            }
            if (pathNode.getTile() == tile && pathNode.getTurns() == turns && getUnit().getMoveType(direction) != Unit.ILLEGAL_MOVE) {
                return pathNode.getDirection();
            }
            return NO_MORE_MOVES_LEFT;
        } else {
            return NO_PATH_TO_TARGET;
        }
    }
    
    
    /**
    * Moves the unit owning this mission in the given direction.
    */
    protected void move(Connection connection, int direction) {
        Element moveElement = Message.createNewRootElement("move");
        moveElement.setAttribute("unit", getUnit().getID());
        moveElement.setAttribute("direction", Integer.toString(direction));

        try {
            connection.send(moveElement);
        } catch (IOException e) {
            logger.warning("Could not send \"move\"-message!");
        }
    }
    

    /**
    * Performs the mission. This method should be implemented by a subclass.
    * @param connection The <code>Connection</code> to the server.
    */
    public abstract void doMission(Connection connection);
    

    /**
    * Checks if this mission is still valid to perform.
    *
    * <BR><BR>
    *
    * A mission can be invalidated for a number of reasons. For example:
    * a seek-and-destroy mission can be invalidated in case the
    * relationship towards the targeted player improves.
    */
    public boolean isValid() {
        return true;
    }


    /**
    * Gets the unit this mission has been created for.
    */
    public Unit getUnit() {
        return aiUnit.getUnit();
    }


    /**
    * Gets the AI-unit this mission has been created for.
    */
    public AIUnit getAIUnit() {
        return aiUnit;
    }
    
    
    protected void setAIUnit(AIUnit aiUnit) {
        this.aiUnit = aiUnit;
    }
}
