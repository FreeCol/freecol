
package net.sf.freecol.server.ai;

import java.util.*;

import net.sf.freecol.server.ai.mission.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.*;
import net.sf.freecol.server.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.*;


/**
* Objects of this class contains AI-information for a single {@link Player}.
*/
public class AIPlayer extends AIObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * The FreeColGameObject this AIObject contains AI-information for:
    */
    private ServerPlayer player;


    /**
    * Creates a new <code>AIPlayer</code>.
    *
    * @param aiMain The main AI-class.
    * @param player The player that should be associated with this
    *        <code>AIPlayer</code>.
    */
    public AIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain);

        this.player = player;
    }


    /**
    * Creates a new <code>AIPlayer</code> and reads the information
    * from the given <code>Element</code>.
    *
    * @param aiMain The main AI-class.
    * @param element The XML-element containing information.
    */
    public AIPlayer(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    /**
    * Tells this <code>AIPlayer</code> to make decisions.
    * The <code>AIPlayer</code> is done doing work this turn
    * when this method returns.
    */
    public void startWorking() {
        secureSettlements();

        // Make every unit perform their mission:
        Iterator aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            aiUnit.doMission(getConnection());
        }
    }


    /**
    * Takes the necessary actions to secure the settlements.
    * This is done by making new military units or to give
    * existing units new missions.
    */
    public void secureSettlements() {
        Map map = player.getGame().getMap();

        if (!player.isEuropean()) {
            // Determines if we need to move a brave out of the settlement.
            Iterator it = player.getIndianSettlementIterator();
            while (it.hasNext()) {
                IndianSettlement is = (IndianSettlement) it.next();
                
                if (is.getUnitCount() > 2) {
                    int defenders = is.getTile().getUnitCount();
                    int threat = 0;

                    Iterator positionIterator = map.getCircleIterator(is.getTile().getPosition(), true, 2);
                    while (positionIterator.hasNext()) {
                        Tile t = map.getTile((Map.Position) positionIterator.next());
                        if (t.getFirstUnit() != null) {
                            if (t.getFirstUnit().getOwner() == player) {
                                defenders++;
                            } else {
                                if (player.getTension(t.getFirstUnit().getOwner()) >= Player.TENSION_ADD_MAJOR) {
                                    threat = 2;
                                } else if (player.getTension(t.getFirstUnit().getOwner()) >= Player.TENSION_ADD_MINOR){
                                    threat = 1;
                                }
                            }
                        }
                    }

                    if (threat > defenders) {
                        Unit newDefender = is.getFirstUnit();
                        newDefender.setState(Unit.ACTIVE);                        
                        newDefender.setLocation(is.getTile());
                        AIUnit newDefenderAI = (AIUnit) getAIMain().getAIObject(newDefender);
                        newDefenderAI.setMission(new UnitWanderHostileMission(newDefenderAI));
                    }
                }
            }
        }
    }


    /**
    * Returns an iterator over all the <code>AIUnit</code>s
    * owned by this player.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getAIUnitIterator() {
        ArrayList au = new ArrayList();

        Iterator unitsIterator = player.getUnitIterator();
        while (unitsIterator.hasNext()) {
            Unit theUnit = (Unit) unitsIterator.next();
            au.add(getAIMain().getAIObject(theUnit.getID()));
        }

        return au.iterator();
    }

    
    /**
    * Gets the connection to the server.
    *
    * @return The connection that can be used when communication
    *         with the server.
    */
    public DummyConnection getConnection() {
        return ((DummyConnection) player.getConnection()).getOtherConnection();
    }


    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", player.getID());

        return element;
    }

    
    public void readFromXMLElement(Element element) {
        player = (ServerPlayer) getAIMain().getFreeColGameObject(element.getAttribute("ID"));
    }    
    
    
    /**
    * Returns the tag name of the root element representing this object.
    * @return the tag name.
    */
    public static String getXMLElementTagName() {
        return "aiPlayer";
    }
}
