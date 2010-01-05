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
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when embarking.
 */
public class EmbarkMessage extends Message {

    /**
     * The id of the unit embarking.
     */
    private String unitId;

    /**
     * The id of the carrier to embark onto.
     */
    private String carrierId;

    /**
     * An optional direction for the unit to move to find the carrier.
     */
    private String directionString;


    /**
     * Create a new <code>EmbarkMessage</code> with the
     * supplied unit, carrier and optional direction.
     *
     * @param unit The <code>Unit</code> to embark.
     * @param carrier The carrier <code>Unit</code> to embark on.
     * @param direction An option direction to embark in.
     */
    public EmbarkMessage(Unit unit, Unit carrier, Direction direction) {
        this.unitId = unit.getId();
        this.carrierId = carrier.getId();
        this.directionString = (direction == null) ? null
            : String.valueOf(direction);
    }

    /**
     * Create a new <code>EmbarkMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public EmbarkMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.carrierId = element.getAttribute("carrier");
        this.directionString = (!element.hasAttribute("direction")) ? null
            : element.getAttribute("direction");
    }

    /**
     * Handle a "embark"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the embarked unit,
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
        Unit carrier;
        try {
            carrier = server.getUnitSafely(carrierId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        Location sourceLocation = unit.getLocation();
        Tile sourceTile = null;
        Tile destinationTile = null;
        Direction direction;
        if (directionString == null) {
            if (sourceLocation != carrier.getLocation()) {
                return Message.clientError("Unit: " + unitId
                                           + " and carrier: " + carrierId
                                           + " are not co-located.");
            }
            direction = null;
        } else {
            // Units have to be on the map if a move is involved
            try {
                direction = Enum.valueOf(Direction.class, directionString);
            } catch (Exception e) {
                return Message.clientError(e.getMessage());
            }
            sourceTile = unit.getTile();
            if (sourceTile == null) {
                return Message.clientError("Unit is not on the map: " + unitId);
            }
            Map map = serverPlayer.getGame().getMap();
            destinationTile = map.getNeighbourOrNull(direction, sourceTile);
            if (destinationTile == null) {
                return Message.clientError("Could not find tile"
                                           + " in direction: " + direction
                                           + " from unit: " + unitId);
            }
            if (carrier.getTile() != destinationTile) {
                return Message.clientError("Carrier: " + carrierId
                                           + " is not next to unit: " + unitId);
            }
        }

        // Embark
        if (!server.getInGameController().embarkUnit(serverPlayer, unit, carrier)) {
            if (unit.isNaval()) {
                return Message.clientError("Naval unit " + unitId
                                           + " can not embark.");
            } else {
                return Message.clientError("No space available for unit " + unitId
                                           + " to embark.");
            }
        }


        // Return the updated carrier and the source location.
        // Splice in an animation if there was movement between tiles.
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        if (sourceTile != null && destinationTile != null) {
            Element animate = doc.createElement("animateMove");
            reply.appendChild(animate);
            animate.setAttribute("unit", unit.getId());
            animate.setAttribute("oldTile", sourceTile.getId());
            animate.setAttribute("newTile", destinationTile.getId());
        }
        Element update = doc.createElement("update");
        reply.appendChild(update);
        update.appendChild(carrier.toXMLElement(serverPlayer, doc));
        update.appendChild(((FreeColGameObject) sourceLocation).toXMLElement(serverPlayer, doc));
        return reply;
    }

    /**
     * Convert this EmbarkMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("carrier", carrierId);
        if (directionString != null) {
            result.setAttribute("direction", directionString);
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "embark".
     */
    public static String getXMLElementTagName() {
        return "embark";
    }
}
