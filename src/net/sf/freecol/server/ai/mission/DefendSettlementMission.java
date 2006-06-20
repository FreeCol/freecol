
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Mission for defending a <code>Settlement</code>.
* @see net.sf.freecol.common.model.Settlement Settlement
*/
public class DefendSettlementMission extends Mission {
    /*
     * TODO: This Mission should later use sub-missions for 
     *       eliminating threats etc.
     */
    private static final Logger logger = Logger.getLogger(DefendSettlementMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The <code>Settlement</code> to be protected. */
    private Settlement settlement;
    
    //private Mission subMission;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param settlement The <code>Settlement</code> to defend.
    * @exception NullPointerException if <code>aiUnit == null</code> or
    *        <code>settlement == null</code>. 
    */
    public DefendSettlementMission(AIMain aiMain, AIUnit aiUnit, Settlement settlement) {
        super(aiMain, aiUnit);

        this.settlement = settlement;
        
        if (settlement == null) {
            logger.warning("settlement == null");
            throw new NullPointerException("settlement == null");
        }        
    }

    /**
     * Creates a new <code>DefendSettlementMission</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public DefendSettlementMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    
    /**
    * Performs this mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Unit unit = getUnit();
        Map map = unit.getGame().getMap();
        
        if (!isValid()) {
            return;
        }
        
        if (getUnit().getTile() == null) {
            return;
        }
        
        if (unit.isOffensiveUnit()) {
            Unit bestTarget = null;
            int bestDifference = Integer.MIN_VALUE;
            int bestDirection = -1;
            
            int[] directions = map.getRandomDirectionArray();
            for (int i=0; i<Map.NUMBER_OF_DIRECTIONS; i++) {
                int direction = directions[i];
                Tile t = map.getNeighbourOrNull(direction, unit.getTile());
                if (t != null
                        && t.getDefendingUnit(unit) != null
                        && t.getDefendingUnit(unit).getOwner().getStance(unit.getOwner()) == Player.WAR
                        && unit.getMoveType(direction) == Unit.ATTACK) {
                    Unit enemyUnit = t.getDefendingUnit(unit);
                    int enemyAttack = enemyUnit.getOffensePower(unit);
                    int weAttack = unit.getOffensePower(enemyUnit);
                    int enemyDefend = enemyUnit.getDefensePower(unit);
                    int weDefend = unit.getDefensePower(enemyUnit);

                    int difference = weAttack / (weAttack + enemyDefend) - enemyAttack / (enemyAttack + weDefend);                  
                    if (difference > bestDifference) {
                        if (difference > 0 || weAttack > enemyDefend) {
                            bestDifference = difference;
                            bestTarget = enemyUnit;
                            bestDirection = direction;
                        }
                    }
                }
            }
            
            if (bestTarget != null) {
                Element element = Message.createNewRootElement("attack");
                element.setAttribute("unit", unit.getID());
                element.setAttribute("direction", Integer.toString(bestDirection));

                try {
                    connection.ask(element);
                } catch (IOException e) {
                    logger.warning("Could not send message!");
                }               
                return;
            }
        }
            
        if (getUnit().getTile() != settlement.getTile()) {
            // Move towards the target.
            int r = moveTowards(connection, settlement.getTile());
            if (r >= 0 && (unit.getMoveType(r) == Unit.MOVE || unit.getMoveType(r) == Unit.DISEMBARK)) {
                move(connection, r);
            }
        } else {
            if (getUnit().getState() != Unit.FORTIFY) {
                Element changeStateElement = Message.createNewRootElement("changeState");
                changeStateElement.setAttribute("unit", getUnit().getID());
                changeStateElement.setAttribute("state", Integer.toString(Unit.FORTIFY));
                try {
                    connection.sendAndWait(changeStateElement);
                } catch (IOException e) {
                    logger.warning("Could not send message!");
                }               
            }
        }
    }    

    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target {@link Tile} of the transport
     * or the target for the entire <code>Transportable</code>'s
     * mission. The target for the transport is determined by
     * {@link TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */    
     public Tile getTransportDestination() {
         if (getUnit().getLocation() instanceof Unit) {
             return settlement.getTile();
         } else if (getUnit().getLocation().getTile() == settlement.getTile()) {
             return null;
         } else if (getUnit().getTile() == null || getUnit().findPath(settlement.getTile()) == null) {
             return settlement.getTile();
         } else {
             return null;
         }
     }


     /**
     * Returns the priority of getting the unit to the
     * transport destination.
     *
     * @return The priority.
     */
     public int getTransportPriority() {
        if (getTransportDestination() != null) {
            return NORMAL_TRANSPORT_PRIORITY + 5;
        } else {
            return 0;
        }
     }
     
    /**
     * Checks if this mission is still valid to perform.
     *
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */
    public boolean isValid() {
        return !settlement.isDisposed()
                && settlement.getOwner() == getUnit().getOwner()
                && getUnit().isDefensiveUnit();
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
        element.setAttribute("settlement", settlement.getID());

        return element;
    }

    /**
     * Updates this object from an XML-representation of
     * a <code>DefendSettlementMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        
        settlement = (Settlement) getGame().getFreeColGameObject(element.getAttribute("settlement"));
        if (settlement == null) {
            logger.warning("settlement == null");
            throw new NullPointerException("settlement == null");
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "defendSettlementMission".
     */
    public static String getXMLElementTagName() {
        return "defendSettlementMission";
    }
}
