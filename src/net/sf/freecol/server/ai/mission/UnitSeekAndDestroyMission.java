
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Mission for attacking a specific target, be it a Unit or a Settlement.
*/
public class UnitSeekAndDestroyMission extends Mission {
    private static final Logger logger = Logger.getLogger(UnitSeekAndDestroyMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * The object we are trying to destroy. This can be a
     * either <code>Settlement</code> or a <code>Unit</code>.
     */
    private FreeColGameObject target;

    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * 
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param target The object we are trying to destroy. This can be either a
    *        <code>Settlement</code> or a <code>Unit</code>.
    */
    public UnitSeekAndDestroyMission(AIMain aiMain, AIUnit aiUnit, FreeColGameObject target) {
        super(aiMain, aiUnit);
        this.target = target; 
    }


    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
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
        Unit unit = getUnit();

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
                        connection.ask(element);
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
    * @return <code>true</code> if this mission is valid.
    */
    public boolean isValid() {
        Player owner = getUnit().getOwner();
        Player targetPlayer;

        if (target == null) return false;
        if (!(target instanceof Ownable)) return false;

        targetPlayer = ((Ownable)target).getOwner();
        int stance = owner.getStance(targetPlayer);

        return (stance == Player.WAR) &&
            (owner.getTension(targetPlayer).getLevel() >= Tension.CONTENT) &&
            (getTarget() != null) &&
            ((target instanceof Unit) || (target instanceof Settlement));
    }

    
    /**
     * Gets the destination of this mission.
     * @return The <code>Tile</code> containing the target.
     */
    public Tile getDestination() {
        if (target instanceof Unit) {
            return ((Unit)target).getTile();
        } else if (target instanceof Settlement) {
            return ((Settlement)target).getTile();
        } else {
            return null;
        }
    }

    
    /**
     * Returns the object we are trying to destroy. 
     * 
     * @return The object which should be destroyed. 
     *      This can be either a <code>Settlement</code> 
     *      or a <code>Unit</code>.
     */    
    public FreeColGameObject getTarget() {
        return target;
    }    

    
    /**
     * Sets the object we are trying to destroy. 
     * 
     * @param target The object which should be destroyed. 
     *      This can be either a <code>Settlement</code> 
     *      or a <code>Unit</code>.
     */
    public void setTarget(FreeColGameObject target) {
        this.target = target;
    }


    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());
        
        element.setAttribute("unit", getUnit().getID());
        element.setAttribute("target", getTarget().getID());

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>UnitSeekAndDestroyMission</code>.
     * 
     * @param element The XML-representation.
     */    
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
