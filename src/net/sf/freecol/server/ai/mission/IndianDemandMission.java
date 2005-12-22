
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Mission for demanding goods from a specified player.
*/
public class IndianDemandMission extends Mission {
    private static final Logger logger = Logger.getLogger(IndianDemandMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The <code>Colony</code> receiving the gift. */
    private Colony target;
    
    /** Whether this mission has been completed or not. */
    private boolean completed;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param target The <code>Colony</code> receiving the gift.    
    */
    public IndianDemandMission(AIMain aiMain, AIUnit aiUnit, Colony target) {
        super(aiMain, aiUnit);
        
        this.target = target;

        if (getUnit().getType() != Unit.BRAVE) {
            logger.warning("Only an indian brave can be given the mission: IndianDemandMission");
            throw new IllegalArgumentException("Only an indian brave can be given the mission: IndianDemandMission");
        }
    }

    
    /**
     * Loads a mission from the given element.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */    
    public IndianDemandMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    
    /**
    * Performs the mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        // Move to the target's colony and deliver
        Unit unit = getUnit();
        int r = moveTowards(connection, target.getTile());
        if (r >= 0 && getGame().getMap().getNeighbourOrNull(r, unit.getTile()) == target.getTile() &&
            unit.getMovesLeft() > 0) {
            // We have arrived.
            Element demandElement = Message.createNewRootElement("indianDemand");
            demandElement.setAttribute("unit", unit.getID());
            demandElement.setAttribute("settlement", target.getID());
            
            try {
                Element reply = connection.ask(demandElement);
                if (reply == null) {
                    // nothing to demand, for example
                    completed = true;
                    return;
                }
                boolean accepted =  Boolean.valueOf(reply.getAttribute("accepted")).booleanValue();
                Player enemy = target.getOwner();
                int tension = 0;
                if (accepted) {
                    tension = -(5 - enemy.getDifficulty()) * 50;
                    unit.getOwner().modifyTension(enemy, tension);
                } else {
                    tension = (enemy.getDifficulty() + 1) * 50;
                    unit.getOwner().modifyTension(enemy, tension);
                    if (unit.getOwner().getTension(enemy) >=
                        Player.TENSION_ANGRY) {
                        // if we didn't get what we wanted, attack
                        getAIUnit().setMission(new UnitSeekAndDestroyMission(getAIMain(),
                                                                             getAIUnit(),
                                                                             target));
                    }
                }

            } catch (IOException e) {
                logger.warning("Could not send \"demand\"-message!");
            }
                
            completed = true;
        }
    }


    
    /**
    * Checks if this mission is still valid to perform.
    *
    * <BR><BR>
    *
    * This mission will be invalidated when the demand has been delivered.
    *
    * @return <code>true</code> if this mission is still valid.
    */
    public boolean isValid() {
        // The last check is to ensure that the colony have not been burned to the ground.
        return (!completed && target != null && target.getTile().getColony() == target);
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
        element.setAttribute("target", target.getID());
        element.setAttribute("completed", Boolean.toString(completed));

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>IndianDemandMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        
        if (element.hasAttribute("target")) {
            target = (Colony) getGame().getFreeColGameObject(element.getAttribute("target"));
        } else {
            // For PRE-0.1.1-protocols
            target = null;
        }

        completed = Boolean.valueOf(element.getAttribute("completed")).booleanValue();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "indianDemandMission".
    */
    public static String getXMLElementTagName() {
        return "indianDemandMission";
    }
}
