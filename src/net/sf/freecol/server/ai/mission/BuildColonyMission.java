
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Mission for building a <code>Colony</code>.
 * 
 * <br /><br />
 * 
 * This mission can be used in two different ways:
 * <ul>
 *   <li>Build a colony at a specific location.</li>
 *   <li>Find a site for a colony and build it there.</li>
 * </ul>
 * 
 * This mission will be aborted in the former case if the value
 * gets below a given threshold, while a colony will always get
 * built (if there is sufficient space on the map) in the latter case.
 * Use the appropriate constructor to get the desired behaviour.
 * 
 * @see net.sf.freecol.common.model.Colony Colony
 */
public class BuildColonyMission extends Mission {
    private static final Logger logger = Logger.getLogger(BuildColonyMission.class.getName());
    
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    
    /** The <code>Tile</code> where the <code>Colony</code> should be built. */
    private Tile target;
    
    /** The value of the target <code>Tile</code>. */
    private int colonyValue;

    /** 
     * The mission will look for a new colony site, instead of aborting this mission,
     * if the colony value drop below the given level if this variable is set to 
     * <code>true</code>.
     */
    private boolean doNotGiveUp = false;
    
    private boolean colonyBuilt = false;
    
    
    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param target The <code>Tile</code> where the <code>Colony</code> should be built.
     * @param colonyValue The value of the <code>Tile</code> to build a <code>Colony</code>
     *        upon. This mission will be invalidated if <code>target.getColonyValue()</code>
     *        is less than this value.
     */
    public BuildColonyMission(AIMain aiMain, AIUnit aiUnit, Tile target, int colonyValue) {
        super(aiMain, aiUnit);
        
        this.target = target;
        this.colonyValue = colonyValue;
        
        if (target == null) {
            throw new NullPointerException("target == null");
        }
        
        if (!getUnit().isColonist()) {
            logger.warning("Only colonists can build a new Colony.");
            throw new IllegalArgumentException("Only colonists can build a new Colony.");
        }
    }
    
    /**
     * Creates a <code>BuildColonyMission</code> for the given <code>AIUnit</code>.
     * The mission will try to find the closest and best site for a colony, and build
     * the colony there. It will not stop until a {@link Colony} gets built.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public BuildColonyMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
        
        this.target = null;
        this.colonyValue = -1;
        this.doNotGiveUp = true;
        
        if (!getUnit().isColonist()) {
            logger.warning("Only colonists can build a new Colony.");
            throw new IllegalArgumentException("Only colonists can build a new Colony.");
        }
    }
    
    /**
     * Creates a new <code>BuildColonyMission</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public BuildColonyMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }
    
    
    /**
     * Performs this mission.
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        Unit unit = getUnit();
        
        if (!isValid()) {
            return;
        }
        
        if (getUnit().getTile() == null) {
            return;
        }
        
        if (target == null || doNotGiveUp && colonyValue > target.getColonyValue()) {
            target = findColonyLocation(getUnit());
            if (target == null) {
                doNotGiveUp = false;
                return;
            }
        }   
        
        // Move towards the target.
        if (getUnit().getTile() != null) {
            if (target != getUnit().getTile()) {
                int r = moveTowards(connection, target);
                if (r >= 0 && (unit.getMoveType(r) == Unit.MOVE || unit.getMoveType(r) == Unit.DISEMBARK)) {
                    move(connection, r);
                }
            }
            if (getUnit().canBuildColony() && target == getUnit().getTile() && getUnit().getMovesLeft() > 0) {
                Element buildColonyElement = Message.createNewRootElement("buildColony");
                buildColonyElement.setAttribute("name", unit.getOwner().getDefaultColonyName());
                buildColonyElement.setAttribute("unit", unit.getID());
                
                try {
                    connection.ask(buildColonyElement);
                    colonyBuilt = true;
                    getAIUnit().setMission(new WorkInsideColonyMission(getAIMain(), getAIUnit(), (AIColony) getAIMain().getAIObject(unit.getTile().getSettlement())));
                } catch (IOException e) {
                    logger.warning("Could not send \"buildColonyElement\"-message!");
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
        if (target == null) {
            if (getUnit().getLocation() instanceof Unit) {
                return (Tile) ((Unit) getUnit().getLocation()).getEntryLocation();
            } else {
                return (Tile) getUnit().getOwner().getEntryLocation();
            }
        }
        
        if (getUnit().getLocation() instanceof Unit) {
            return target;
        } else if (getUnit().getLocation().getTile() == target) {
            return null;
        } else if (getUnit().getTile() == null) {
            return target;
        } else if (getUnit().findPath(target) == null) {
            return target;
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
            return NORMAL_TRANSPORT_PRIORITY;
        } else {
            return 0;
        }
    }
    
    /**
     * Finds a site for a new colony.
     * 
     * @param unit The <code>Unit</code> to find a colony
     *      site for. This unit will be used for determining
     *      the path to a colony location and colony sites
     *      far away from the unit (in turns) will be less
     *      valuable.
     * @return A site suitable for a <code>Colony</code> or
     *      <code>null</code> if no such site could be found
     *      within a fixed distance from the <code>Unit</code>. 
     */
    public static Tile findColonyLocation(Unit unit) {
        Game game = unit.getGame();
        
        if (unit.getTile() == null) {
            return null;
        }
        
        Tile bestTile = null;
        int highestColonyValue = 0;
        
        Iterator it = game.getMap().getFloodFillIterator(unit.getTile().getPosition());
        for (int i=0; it.hasNext() && i<500; i++) {
            Tile tile = game.getMap().getTile((Map.Position) it.next());
            if (tile.getColonyValue() > 0) {
                if (tile != unit.getTile()) {
                    PathNode path;
                    if (unit.getLocation() instanceof Unit) {
                        Unit carrier = (Unit) unit.getLocation();
                        path = game.getMap().findPath(unit, carrier.getTile(), tile, carrier);
                    } else {
                        path = game.getMap().findPath(unit, unit.getTile(), tile);
                    }
                    
                    if (path != null) {
                        int newColonyValue = 10000 + tile.getColonyValue() + getNearbyColonyBonus(unit.getOwner(), tile) 
                                - path.getTotalTurns() 
                                * ((unit.getGame().getTurn().getNumber() < 10 && unit.getLocation() instanceof Unit) ? 25 : 4);
                        if (newColonyValue > highestColonyValue) {
                            highestColonyValue = newColonyValue;
                            bestTile = tile;
                        }
                    }
                } else {
                    int newColonyValue = 10000 + tile.getColonyValue() + getNearbyColonyBonus(unit.getOwner(), tile);
                    if (newColonyValue > highestColonyValue) {
                        highestColonyValue = newColonyValue;
                        bestTile = tile;
                    }
                }
            }
        }
        
        if (bestTile != null && bestTile.getColonyValue() > 0) {
            return bestTile;
        } else {
            return null;
        }
    }    
    
    private static int getNearbyColonyBonus(Player owner, Tile tile) {
        Game game = tile.getGame();
        Map map = game.getMap();
        
        Iterator it = map.getCircleIterator(tile.getPosition(), false, 3);
        while (it.hasNext()) {
            Tile ct = map.getTile((Map.Position) it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 45;
            }
        }
        
        it = map.getCircleIterator(tile.getPosition(), false, 4);
        while (it.hasNext()) {
            Tile ct = map.getTile((Map.Position) it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 25;
            }
        }       
        
        it = map.getCircleIterator(tile.getPosition(), false, 5);
        while (it.hasNext()) {
            Tile ct = map.getTile((Map.Position) it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 20;
            }
        }           
        
        it = map.getCircleIterator(tile.getPosition(), false, 4);
        while (it.hasNext()) {
            Tile ct = map.getTile((Map.Position) it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 15;
            }
        }       
        
        return 0;
    }
    
    
    /**
     * Checks if this mission is still valid to perform.
     *
     * <BR><BR>
     *
     * This mission will be invalidated when the colony has been built or
     * if the <code>target.getColonyValue()</code> decreases.
     *
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */
    public boolean isValid() {
        return (!colonyBuilt && (doNotGiveUp 
                || target != null 
                && colonyValue <= target.getColonyValue() + getNearbyColonyBonus(getUnit().getOwner(), target)));
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
        element.setAttribute("doNotGiveUp", Boolean.toString(doNotGiveUp));
        element.setAttribute("colonyBuilt", Boolean.toString(colonyBuilt));
        
        return element;
    }
    
    /**
     * Updates this object from an XML-representation of
     * a <code>BuildColonyMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        
        target = (Tile) getGame().getFreeColGameObject(element.getAttribute("target"));
        if (element.hasAttribute("doNotGiveUp")) {
            doNotGiveUp = Boolean.valueOf(element.getAttribute("doNotGiveUp")).booleanValue();
        } else {
            doNotGiveUp = false;
        }
        colonyBuilt = Boolean.valueOf(element.getAttribute("colonyBuilt")).booleanValue();      
    }   
    
    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "buildColonyMission".
     */
    public static String getXMLElementTagName() {
        return "buildColonyMission";
    }
}
