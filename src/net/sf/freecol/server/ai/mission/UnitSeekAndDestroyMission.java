
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.server.ai.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import org.w3c.dom.*;


/**
* Mission for attacking a specific target, be it a Unit or a Settlement.
*/
public class UnitSeekAndDestroyMission extends Mission {
    private static final Logger logger = Logger.getLogger(UnitSeekAndDestroyMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * What are we trying to destroy? This can be a
     * Settlement or a Unit.
     */
    private FreeColGameObject target;

    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public UnitSeekAndDestroyMission(AIMain aiMain, AIUnit aiUnit, FreeColGameObject target) {
        super(aiMain, aiUnit);
        this.target = target; 
    }


    /**
    * Loads a mission from the given element.
    */
    public UnitSeekAndDestroyMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }
    


    /**
    * Performs the mission. This is done by searching for hostile units
    * that are located within one tile and attacking them. If no such units
    * are found, then wander in a random direction.
    *
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Tile thisTile = getUnit().getTile();
        Unit unit = getUnit();
        Map map = getGame().getMap();

        PathNode pathToTarget = null;
        if (unit.isOffensiveUnit() && getDestination() != null) {
            pathToTarget = getUnit().findPath(getDestination());
        }
        
        if (pathToTarget != null) {
            int direction = moveTowards(connection, pathToTarget);
            while (direction >= 0) {
                if (unit.getMoveType(direction) == Unit.ATTACK) {
                    Element element = Message.createNewRootElement("attack");
                    element.setAttribute("unit", unit.getID());
                    element.setAttribute("direction", Integer.toString(direction));

                    try {
                        Element reply = connection.ask(element);
                    } catch (IOException e) {
                        logger.warning("Could not send message!");
                    }
                    break;
                } else if (unit.getMoveType(direction) == Unit.MOVE) {
                    Element element = Message.createNewRootElement("move");
                    element.setAttribute("unit", unit.getID());
                    element.setAttribute("direction", Integer.toString(direction));

                    try {
                        connection.sendAndWait(element);
                    } catch (IOException e) {
                        logger.warning("Could not send message!");
                    }

                    // Hmmm... if we are an AI player, our Unit will be updated 
                    // immediately. Therefore we don't need to check a server
                    // response.
                    pathToTarget = getUnit().findPath(getDestination());
                    direction = moveTowards(connection, pathToTarget);
                } else {
                    break;
                }
            }
        }
    }

    /**
    * Check to see if this is a valid hostility with a valid target.
    */
    public boolean isValid() {
        Player owner = getUnit().getOwner();
        Player targetPlayer;

        if (target == null) return false;
        if (!(target instanceof Ownable)) return false;

        targetPlayer = ((Ownable)target).getOwner();
        int stance = owner.getStance(targetPlayer);

        return (stance == Player.WAR) && (owner.getTension(targetPlayer) >= Player.TENSION_CONTENT) && (getTarget() != null) && ((target instanceof Unit) || (target instanceof Settlement));
    }

    public FreeColGameObject getTarget() {
        return target;
    }

    public Tile getDestination() {
        if (target instanceof Unit) {
            return ((Unit)target).getTile();
        } else if (target instanceof Settlement) {
            return ((Settlement)target).getTile();
        } else {
            return null;
        }
    }


    public void setTarget(FreeColGameObject target) {
        this.target = target;
    }


    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());
        
        element.setAttribute("unit", getUnit().getID());
        element.setAttribute("target", getTarget().getID());

        return element;
    }


    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        setTarget((FreeColGameObject) getGame().getFreeColGameObject(element.getAttribute("target")));
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "unitSeekAndDestroyMission".
    */
    public static String getXMLElementTagName() {
        return "unitSeekAndDestroyMission";
    }
}
