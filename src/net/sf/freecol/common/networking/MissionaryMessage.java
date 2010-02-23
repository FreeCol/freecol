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

package net.sf.freecol.common.networking;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when a missionary establishes/denounces a mission.
 */
public class MissionaryMessage extends Message {
    /**
     * The id of the missionary.
     */
    private String unitId;

    /**
     * The direction to the settlement.
     */
    private String directionString;

    /**
     * Is this a denunciation?
     */
    private boolean denounce;

    /**
     * Create a new <code>MissionaryMessage</code> with the
     * supplied name.
     *
     * @param unit The missionary <code>Unit</code>.
     * @param direction The <code>Direction</code> to the settlement.
     * @param denounce True if this is a denunciation.
     */
    public MissionaryMessage(Unit unit, Direction direction, boolean denounce) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
        this.denounce = denounce;
    }

    /**
     * Create a new <code>MissionaryMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MissionaryMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unitId");
        this.directionString = element.getAttribute("direction");
        this.denounce = Boolean.valueOf(element.getAttribute("denounce")).booleanValue();
    }

    /**
     * Handle a "missionary"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An element containing the result of the mission operation,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (unit.getTile() == null) {
            return Message.clientError("Unit is not on the map: " + unitId);
        }
        Direction direction = Enum.valueOf(Direction.class, directionString);
        Game game = serverPlayer.getGame();
        Map map = game.getMap();
        Tile tile = map.getNeighbourOrNull(direction, unit.getTile());
        if (tile == null) {
            return Message.clientError("Could not find tile"
                                       + " in direction: " + direction
                                       + " from unit: " + unitId);
        }
        Settlement settlement = tile.getSettlement();
        if (settlement == null || !(settlement instanceof IndianSettlement)) {
            return Message.clientError("There is no native settlement at: "
                                       + tile.getId());
        }
        IndianSettlement indianSettlement = (IndianSettlement) settlement;
        Unit missionary = indianSettlement.getMissionary();
        if (denounce) {
            if (missionary == null) {
                return Message.clientError("Denouncing an empty mission at: "
                                           + indianSettlement.getId());
            } else if (missionary.getOwner() == player) {
                return Message.clientError("Denouncing our own missionary at: "
                                           + indianSettlement.getId());
            }
        } else {
            if (missionary != null) {
                return Message.clientError("Establishing extra mission at: "
                                           + indianSettlement.getId());
            }
        }

        // Valid, proceed to establish/denounce.
        InGameController igc = server.getInGameController();
        Location oldLocation = unit.getLocation();
        ModelMessage m;
        try {
            m = (denounce) ? igc.denounceMission(indianSettlement, unit)
                : igc.establishMission(indianSettlement, unit);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (oldLocation instanceof Tile) {
            igc.sendRemoveUnitToAll(serverPlayer, unit, (Tile) oldLocation);
        }
        if (!unit.isDisposed()) {
            settlement.getTile().updateIndianSettlementInformation(player);
            unit.setMovesLeft(0);
        }

        // Build the reply, updating the settlement for tension,
        // missionary presence and other information.
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        reply.appendChild(update);
        update.appendChild(indianSettlement.toXMLElement(player, doc));
        if (m != null) {
            Element messages = doc.createElement("addMessages");
            reply.appendChild(messages);
            messages.appendChild(m.toXMLElement(player, doc));
        }
        if (unit.isDisposed()) {
            Element remove = doc.createElement("remove");
            reply.appendChild(remove);
            unit.addToRemoveElement(remove);
        } else {
            update.appendChild(unit.toXMLElement(player, doc));
            if (oldLocation instanceof Tile) {
                update.appendChild(((Tile) oldLocation).toXMLElement(player, doc));
            } else if (oldLocation instanceof Unit) {
                update.appendChild(((Unit) oldLocation).toXMLElement(player, doc));
            }
        }
        return reply;
    }

    /**
     * Convert this MissionaryMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unitId", unitId);
        result.setAttribute("direction", directionString);
        result.setAttribute("denounce", Boolean.toString(denounce));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "missionary".
     */
    public static String getXMLElementTagName() {
        return "missionary";
    }
}
