/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;

/**
 * Mission for building a <code>Colony</code>.
 * 
 * <br />
 * <br />
 * 
 * This mission can be used in two different ways:
 * <ul>
 * <li>Build a colony at a specific location.</li>
 * <li>Find a site for a colony and build it there.</li>
 * </ul>
 * 
 * This mission will be aborted in the former case if the value gets below a
 * given threshold, while a colony will always get built (if there is sufficient
 * space on the map) in the latter case. Use the appropriate constructor to get
 * the desired behaviour.
 * 
 * @see net.sf.freecol.common.model.Colony Colony
 */
public class BuildColonyMission extends Mission {

    private static final Logger logger = Logger.getLogger(BuildColonyMission.class.getName());

    /** The <code>Tile</code> where the <code>Colony</code> should be built. */
    private Tile target;

    /** The value of the target <code>Tile</code>. */
    private int colonyValue;

    /**
     * The mission will look for a new colony site, instead of aborting this
     * mission, if the colony value drop below the given level if this variable
     * is set to <code>true</code>.
     */
    private boolean doNotGiveUp = false;

    private boolean colonyBuilt = false;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param target The <code>Tile</code> where the <code>Colony</code>
     *            should be built.
     * @param colonyValue The value of the <code>Tile</code> to build a
     *            <code>Colony</code> upon. This mission will be invalidated
     *            if <code>target.getColonyValue()</code> is less than this
     *            value.
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
     * Creates a <code>BuildColonyMission</code> for the given
     * <code>AIUnit</code>. The mission will try to find the closest and best
     * site for a colony, and build the colony there. It will not stop until a
     * {@link Colony} gets built.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
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
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public BuildColonyMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>BuildColonyMission</code> and reads the given
     * element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see AIObject#readFromXML
     */
    public BuildColonyMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }

    /**
     * Performs this mission.
     * 
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

        if (target == null || doNotGiveUp
            && (colonyValue != getUnit().getOwner().getColonyValue(target) || target.getSettlement() != null)) {
            target = findColonyLocation(getUnit());
            if (target == null) {
                doNotGiveUp = false;
                return;
            } else {
                colonyValue = getUnit().getOwner().getColonyValue(target);
            }
        }

        // Move towards the target.
        if (getUnit().getTile() != null) {
            if (target != getUnit().getTile()) {
                Direction r = moveTowards(connection, target);
                moveButDontAttack(connection, r);
            }
            if (getUnit().canBuildColony() && target == getUnit().getTile()
                && getUnit().getMovesLeft() > 0) {
                String name = Messages.getDefaultSettlementName(unit.getOwner(), false);
                Element reply = null;

                try {
                    reply = connection.ask(new BuildColonyMessage(name, unit).toXMLElement());
                } catch (IOException e) {
                    logger.warning("Could not send BuildColony message.");
                }
                if (reply != null && reply.getTagName() != "error") {
                    colonyBuilt = true;
                    Settlement settlement = unit.getTile().getSettlement();
                    AIColony aiColony = (AIColony) getAIMain().getAIObject(settlement);
                    getAIUnit().setMission(new WorkInsideColonyMission(getAIMain(), getAIUnit(), aiColony));
                } else {
                    logger.warning("Could not build an AI colony on tile "+getUnit().getTile().getPosition().toString());
                }
            }
        }
    }

    /**
     * Returns the destination for this <code>Transportable</code>. This can
     * either be the target {@link Tile} of the transport or the target for the
     * entire <code>Transportable</code>'s mission. The target for the
     * transport is determined by {@link TransportMission} in the latter case.
     * 
     * @return The destination for this <code>Transportable</code>.
     */
    public Tile getTransportDestination() {
        if (target == null) {
            if (getUnit().isOnCarrier()) {
                return (Tile) ((Unit) getUnit().getLocation()).getEntryLocation();
            } else {
                return (Tile) getUnit().getOwner().getEntryLocation();
            }
        }

        if (getUnit().isOnCarrier()) {
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
     * Returns the priority of getting the unit to the transport destination.
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
     * @param unit The <code>Unit</code> to find a colony site for. This unit
     *            will be used for determining the path to a colony location and
     *            colony sites far away from the unit (in turns) will be less
     *            valuable.
     * @return A site suitable for a <code>Colony</code> or <code>null</code>
     *         if no such site could be found within a fixed distance from the
     *         <code>Unit</code>.
     */
    public static Tile findColonyLocation(Unit unit) {
        
        Game game = unit.getGame();
        
        Tile startTile = null;
        if (unit.isOnCarrier()) {
            Unit carrier = (Unit) unit.getLocation();
            startTile = carrier.getTile();
        } else if (unit.getLocation() instanceof Europe) {
            startTile = (Tile) unit.getEntryLocation();
        } else {
            startTile = unit.getTile();
        }
            
        if (startTile == null) {
            return null;
        }

        Tile bestTile = null;
        int highestColonyValue = 0;
        int maxNumberofTiles = 500;
        int tileCounter = 0;

        //if called during the first few turns of the game,
        //and our unit may be the starting unit (==isOnCarrier())
        //make sure to find _some_ starting position        
        boolean gameStart = false;
        if (unit.getGame().getTurn().getNumber() < 10 && unit.isOnCarrier()) {
            gameStart = true;
        }
        
        Iterator<Position> it = game.getMap().getFloodFillIterator(startTile.getPosition());
        
        while (it.hasNext()) {
            Tile tile = game.getMap().getTile(it.next());
            if (tile.getOwner() != null) continue;
            int newColonyValue = -1;
            int tileColonyValue = unit.getOwner().getColonyValue(tile);
            
            if (tileColonyValue > 0
            	&& (tileColonyValue + 10000) > highestColonyValue) {
            	// tileColonyValue + 10000 is the highest possible ColonyValue for this  tile
                if (tile != startTile) {
                    PathNode path;

                    if (unit.isOnCarrier()) {
                        Unit carrier = (Unit) unit.getLocation();
                        path = game.getMap().findPath(unit, startTile, tile, carrier);
                    } else {
                        path = game.getMap().findPath(unit, startTile, tile);
                    }
                    if (path != null) {
                        newColonyValue = 10000
                            + tileColonyValue
                            - path.getTotalTurns()
                            * ((unit.getGame().getTurn().getNumber() < 10
                                && unit.isOnCarrier()) ? 25 : 4);
                    }
                } else {
                    newColonyValue = 10000 + tileColonyValue;
                }
                if (newColonyValue > highestColonyValue) {
                    highestColonyValue = newColonyValue;
                    bestTile = tile;
                }
            }
            //break after checking a fixed number of tiles
            //unless this may be the first colony,
            //in which case we'll continue until we found _some_ location.
            if ((++tileCounter >= maxNumberofTiles)
                && (!gameStart || bestTile!= null)) break;
        }
        if (bestTile == null) {
            logger.info("Unit " + unit.getId() + " unsuccessfully searched for colony spot");
        }
        return bestTile;
    }

    /**
     * Checks if this mission is still valid to perform.
     * 
     * <BR>
     * <BR>
     * 
     * This mission will be invalidated when the colony has been built or if the
     * <code>target.getColonyValue()</code> decreases.
     * 
     * @return <code>true</code> if this mission is still valid to perform and
     *         <code>false</code> otherwise.
     */
    public boolean isValid() {
        return (!colonyBuilt && (doNotGiveUp || target != null
                                 && target.getSettlement() == null
                                 && colonyValue <= getUnit().getOwner().getColonyValue(target)));
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("unit", getUnit().getId());
        if (target != null) {
            out.writeAttribute("target", target.getId());
        }
        out.writeAttribute("doNotGiveUp", Boolean.toString(doNotGiveUp));
        out.writeAttribute("colonyBuilt", Boolean.toString(colonyBuilt));

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * 
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));

        final String targetStr = in.getAttributeValue(null, "target");
        if (targetStr != null) {
            target = (Tile) getGame().getFreeColGameObject(targetStr);
        } else {
            target = null;
        }

        final String doNotGiveUpStr = in.getAttributeValue(null, "doNotGiveUp");
        if (doNotGiveUpStr != null) {
            doNotGiveUp = Boolean.valueOf(doNotGiveUpStr).booleanValue();
        } else {
            doNotGiveUp = false;
        }
        colonyBuilt = Boolean.valueOf(in.getAttributeValue(null, "colonyBuilt")).booleanValue();
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return The <code>String</code> "buildColonyMission".
     */
    public static String getXMLElementTagName() {
        return "buildColonyMission";
    }

    /**
     * Gets debugging information about this mission. This string is a short
     * representation of this object's state.
     * 
     * @return The <code>String</code>: "(x, y) z" or "(x, y) z!" where
     *         <code>x</code> and <code>y</code> is the coordinates of the
     *         target tile for this mission, and <code>z</code> is the value
     *         of building the colony. The exclamation mark is added if the unit
     *         should continue searching for a colony site if the targeted site
     *         is lost.
     */
    public String getDebuggingInfo() {
        final String targetName = (target != null) ? target.getPosition().toString() : "unassigned";
        return targetName + " " + colonyValue + (doNotGiveUp ? "!" : "");
    }
}
