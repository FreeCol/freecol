
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.TileImprovement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Mission for controlling a pioneer.
 * 
 * @see Unit#isPioneer
 */
public class PioneeringMission extends Mission {
	/* 
	 * TODO-LATER: "updateTileImprovement" should be called
	 *             only once (in the beginning of the turn).
	 */
	
    private static final Logger logger = Logger.getLogger(PioneeringMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private TileImprovement tileImprovement = null;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public PioneeringMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
    }


    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public PioneeringMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    
    
    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
        if (tileImprovement != null) {
        	tileImprovement.setPioneer(null);
        	tileImprovement = null;
        }
        super.dispose();
    }

    /**
     * Sets the <code>TileImprovement</code> which should
     * be the next target.
     * 
     * @param tileImprovement The <code>TileImprovement</code>.
     */
    public void setTileImprovement(TileImprovement tileImprovement) {
    	this.tileImprovement = tileImprovement;
    }

    private void updateTileImprovement() {
    	if (tileImprovement != null) {
    		return;
    	}
    	AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getID());
    	
    	TileImprovement bestChoice = null;
    	int bestValue = 0;
    	Iterator tiIterator = aiPlayer.getTileImprovementIterator();        	
    	while (tiIterator.hasNext()) {
    		TileImprovement ti = (TileImprovement) tiIterator.next();
    		if (ti.getPioneer() == null) {
    			PathNode path = null;
    			int value;
    			if (getUnit().getTile() != ti.getTarget()) {
    				path = getUnit().findPath(ti.getTarget());
    				if (path != null) {
    					value = ti.getValue() + 10000 - (path.getTotalTurns()*5);
    				} else {
    					value = ti.getValue();
    				}
    			} else {
    				value = ti.getValue() + 10000;
    			}
    			if (value > bestValue) {
    				bestChoice = ti;
    				bestValue = value;
    			}
    		}
    	}
    	
    	if (bestChoice != null) {
    		tileImprovement = bestChoice;
    		bestChoice.setPioneer(getAIUnit());
    	}    
    }
    
    /**
     * Performs this mission.
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {       
        if (!isValid()) {
            return;
        }
               
        if (!getUnit().isPioneer()) {
        	// TODO: Get tools from a Colony
        	return;
        }
        
        if (tileImprovement == null) {
        	updateTileImprovement();
        }

        if (getUnit().getState() != Unit.ACTIVE) {
        	return;
        }
        
        if (tileImprovement != null) {
        	if (getUnit().getTile() != null) {
        		if (getUnit().getTile() == tileImprovement.getTarget()) {
        	        Element changeStateElement = Message.createNewRootElement("changeState");
        	        changeStateElement.setAttribute("unit", getUnit().getID());
        	        changeStateElement.setAttribute("state", Integer.toString(tileImprovement.getType()));
                    try {
                        connection.sendAndWait(changeStateElement);
                    } catch (IOException e) {
                        logger.warning("Could not send message!");
                    }
        		} else {
        			PathNode pathToTarget = getUnit().findPath(tileImprovement.getTarget());
        			if (pathToTarget != null) {
        				int direction = moveTowards(connection, pathToTarget);
        				if (direction >= 0 && getUnit().getMoveType(direction) == Unit.MOVE) {
        					move(connection, direction);
        				}
        			}
        		}
        	}
        }        
    }


    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target {@link Tile} of the transport
     * or the target for the entire <code>Transportable</code>'s
     * mission. The target for the tansport is determined by
     * {@link TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */    
    public Tile getTransportDestination() {
    	updateTileImprovement();
    	if (tileImprovement == null) {
    		return null;
    	}
        if (getUnit().getLocation() instanceof Unit) {
            return tileImprovement.getTarget();
        } else if (getUnit().getTile() == tileImprovement.getTarget()) {
            return null;
        } else if (getUnit().getTile() == null || getUnit().findPath(tileImprovement.getTarget()) == null) {
            return tileImprovement.getTarget();
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
    	updateTileImprovement();
    	if (tileImprovement == null) {
    		return 0;
    	}    	
        if (getUnit().getLocation() instanceof Unit) {
            return NORMAL_TRANSPORT_PRIORITY;
        } else if (getUnit().getLocation().getTile() == tileImprovement.getTarget()) {
            return 0;
        } else if (getUnit().getTile() == null || getUnit().findPath(tileImprovement.getTarget()) == null) {
            return NORMAL_TRANSPORT_PRIORITY;
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
    	updateTileImprovement();    	
        //return tileImprovement != null;
    	// TODO: Remove the second test after code for getting tools has been added:
    	return (tileImprovement != null) && getUnit().isPioneer();
    }

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The unit.
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */    
    public static boolean isValid(AIUnit aiUnit) {
    	AIPlayer aiPlayer = (AIPlayer) aiUnit.getAIMain().getAIObject(aiUnit.getUnit().getOwner().getID());
    	Iterator tiIterator = aiPlayer.getTileImprovementIterator();        	
    	while (tiIterator.hasNext()) {
    		TileImprovement ti = (TileImprovement) tiIterator.next();
    		if (ti.getPioneer() == null) {
    			return true;
    		}
    	}
    	return false;
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
        element.setAttribute("tileImprovement", tileImprovement.getID());

        return element;
    }

    /**
     * Updates this object from an XML-representation of
     * a <code>WishRealizationMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        
        tileImprovement = (TileImprovement) getAIMain().getAIObject(element.getAttribute("tileImprovement"));
    }

    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "wishRealizationMission".
     */
    public static String getXMLElementTagName() {
        return "tileImprovementMission";
    }
}
