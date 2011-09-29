/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
 * Mission for building a <code>Colony</code>.
 *
 * This mission can be used in two different ways:
 * <ul>
 * <li>Build a colony at a specific location.</li>
 * <li>Find a site for a colony and build it there.</li>
 * </ul>
 *
 * This mission will be aborted in the former case if the value gets
 * below a given threshold, while a colony will always get built (if
 * there is sufficient space on the map) in the latter case. Use the
 * appropriate constructor to get the desired behaviour.
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
     * {@link net.sf.freecol.common.model.Colony} gets built.
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
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
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
        Player player = unit.getOwner();

        if (!isValid()) {
            return;
        }

        if (unit.getTile() == null) {
            return;
        }

        // Check target is valid and value has not degraded.
        int newValue = (target == null) ? -1 : player.getColonyValue(target);
        if (newValue <= 0 || newValue < colonyValue) {
            if (!doNotGiveUp
                || (target = findColonyLocation(getUnit())) == null) {
                doNotGiveUp = false;
                return;
            }
            colonyValue = player.getColonyValue(target);
        }

        // Move towards the target.
        if (unit.getTile() != null) {
            if (target != unit.getTile()) {
                Direction r = moveTowards(target);
                if (r == null || !moveButDontAttack(r)) return;
            }
            if (unit.canBuildColony()
                && target == unit.getTile()
                && unit.getMovesLeft() > 0) {
                if (target.getOwner() == null) {
                    ; // All is well
                } else if (target.getOwner() == player) {
                    // Already ours, clear users
                    Colony colony = (Colony) target.getOwningSettlement();
                    if (colony != null
                        && colony.getColonyTile(target) != null) {
                        colony.getColonyTile(target).relocateWorkers();
                    }
                } else { // Not our tile, claim it first
                    int price = player.getLandPrice(target);
                    if (price < 0) { // Someone has got in first
                        target = null;
                        return;
                    }
                    if (price > 0 && !player.checkGold(price)
                        && getAIRandom().nextInt(4) == 0) {
                        // CHEAT: add gold so player can buy the land
                        player.modifyGold(price);
                    }
                    if (!AIMessage.askClaimLand(connection, target, null,
                            (player.checkGold(price))
                                                ? price
                                                : NetworkConstants.STEAL_LAND)
                        || target.getOwner() != player) {
                        target = null; // Claim failed, try a different tile
                        return;
                    }
                }
                if (AIMessage.askBuildColony(getAIUnit(), Player.ASSIGN_SETTLEMENT_NAME)
                    && target.getSettlement() != null) {
                    colonyBuilt = true;
                    AIColony aiColony = getAIMain().getAIColony(target.getColony());
                    getAIUnit().setMission(new WorkInsideColonyMission(getAIMain(), getAIUnit(), aiColony));
                } else {
                    logger.warning("Could not build an AI colony on tile "
                                   + target.getPosition().toString());
                }
            }
        }
    }

    /**
     * Returns the destination for this
     * <code>Transportable</code>. This can either be the target
     * {@link net.sf.freecol.common.model.Tile} of the transport or
     * the target for the entire <code>Transportable</code>'s
     * mission. The target for the transport is determined by {@link
     * TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Tile getTransportDestination() {
        if (target == null) {
            return ((getUnit().isOnCarrier()) ? ((Unit) getUnit().getLocation()) : getUnit()).getFullEntryLocation();
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
     * Finds a site for a new colony.  Favour closer sites.
     *
     * @param unit The <code>Unit</code> to find a colony site for.
     * @return A suitable tile for a new colony.
     */
    public static Tile findColonyLocation(Unit unit) {
        Game game = unit.getGame();
        Map map = game.getMap();
        Player player = unit.getOwner();

        Tile startTile = null;
        Unit carrier = null;
        if (unit.isOnCarrier()) {
            carrier = (Unit) unit.getLocation();
            startTile = carrier.getTile();
        } else if (unit.isInEurope() || unit.isAtSea()) {
            startTile = unit.getFullEntryLocation();
        } else {
            startTile = unit.getTile();
        }
        if (startTile == null) {
            logger.warning("findColonyLocation failed, unit " + unit.getId()
                           + " not on the map");
            return null;
        }

        // If no colonies, do not fail.
        boolean noFail = player.getSettlements().size() == 0;
        boolean gameStart = game.getTurn().getNumber() < 20;
        final int maxNumberofTiles = 400;
        int tileCounter = 0;
        Tile bestTile = null;
        float bestValue = 0.0f;
        Iterator<Position> it = map.getCircleIterator(startTile.getPosition(),
                                                      true, 12);
        while (it.hasNext()) {
            // Stop after checking a fixed number of tiles unless
            // this is the first/sole colony, in which case continue
            // until we found *some* location, except if there is no
            // carrier available which may mean we are marooned on
            // land with no available sites.
            if (++tileCounter >= maxNumberofTiles) {
                if (!noFail || bestTile != null || carrier == null) break;
            }
 
            Tile tile = map.getTile(it.next());
            // No initial polar colonies
            if (gameStart && map.isPolar(tile)) {
                continue;
            }

            // Can we acquire the tile?
            switch (player.canClaimToFoundSettlementReason(tile)) {
            case NONE: case NATIVES:
                break;
            default:
                continue;
            }

            // Score is proportional to tile value and inversely proportional
            // to distance.
            int val = unit.getOwner().getColonyValue(tile);
            if (val <= 0) continue;

            // Work out the number of turns to the target tile.
            float len = 1.0f;
            if (tile != startTile) {
                PathNode path = (carrier == null)
                    ? map.findPath(unit, startTile, tile)
                    : map.findPath(unit, startTile, tile, carrier);
                if (path == null) continue;
                len += path.getTotalTurns();
            }

            float value = val / len;
            if (value > bestValue) {
                bestValue = value;
                bestTile = tile;
            }
        }
        logger.finest("findColonyLocation(" + unit.getId()
                      + ") found tile: " + bestTile);
        return bestTile;
    }

    /**
     * Checks if this mission is still valid to perform.
     *
     * This mission will be invalidated when the colony has been built
     * or if the <code>target.getColonyValue()</code> decreases.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        return super.isValid() && !colonyBuilt
            && (doNotGiveUp
                || (target != null
                    && target.getSettlement() == null
                    && colonyValue <= getUnit().getOwner().getColonyValue(target)));
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


    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXML(out, getXMLElementTagName());
    }

    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        writeAttribute(out, "target", target);
        out.writeAttribute("doNotGiveUp", Boolean.toString(doNotGiveUp));
        out.writeAttribute("colonyBuilt", Boolean.toString(colonyBuilt));
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     *
     * @param in The input stream with the XML.
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        final String targetStr = in.getAttributeValue(null, "target");
        if (targetStr != null) {
            target = (Tile) getGame().getFreeColGameObject(targetStr);
        } else {
            target = null;
        }

        final String doNotGiveUpStr = in.getAttributeValue(null,
            "doNotGiveUp");
        if (doNotGiveUpStr != null) {
            doNotGiveUp = Boolean.valueOf(doNotGiveUpStr).booleanValue();
        } else {
            doNotGiveUp = false;
        }
        colonyBuilt = Boolean.valueOf(in.getAttributeValue(null, "colonyBuilt")).booleanValue();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "buildColonyMission".
     */
    public static String getXMLElementTagName() {
        return "buildColonyMission";
    }
}
