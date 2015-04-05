/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

import org.w3c.dom.Element;


/**
 * The message sent when embarking.
 */
public class EmbarkMessage extends DOMMessage {

    /** The identifier of the unit embarking. */
    private final String unitId;

    /** The identifier of the carrier to embark onto. */
    private final String carrierId;

    /** An optional direction for the unit to move to find the carrier. */
    private final String directionString;


    /**
     * Create a new <code>EmbarkMessage</code> with the
     * supplied unit, carrier and optional direction.
     *
     * @param unit The <code>Unit</code> to embark.
     * @param carrier The carrier <code>Unit</code> to embark on.
     * @param direction An option direction to embark in.
     */
    public EmbarkMessage(Unit unit, Unit carrier, Direction direction) {
        super(getXMLElementTagName());

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
        super(getXMLElementTagName());

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
     * @return An update containing the embarked unit, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        ServerUnit unit;
        try {
            unit = player.getOurFreeColGameObject(unitId, ServerUnit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Unit carrier;
        try {
            carrier = player.getOurFreeColGameObject(carrierId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Location sourceLocation = unit.getLocation();
        if (directionString == null) {
            // Locations must be the same, or the source is also a
            // carrier in the same location as the carrier, or they
            // must be on the same tile.
            if (!carrier.isAtLocation(sourceLocation)) {
                return DOMMessage.clientError("Unit " + unitId
                    + " at " + sourceLocation.getId()
                    + " and carrier " + carrierId
                    + " at " + carrier.getLocation().getId()
                    + " are not co-located.");
            }
        } else {
            // Units have to be on the map and have moves left if a
            // move is involved.
            if (unit.getMovesLeft() <= 0) {
                return DOMMessage.clientError("Unit has no moves left: "
                    + unitId);
            }

            Tile destinationTile = null;
            try {
                destinationTile = unit.getNeighbourTile(directionString);
            } catch (Exception e) {
                return DOMMessage.clientError(e.getMessage());
            }
            if (carrier.getTile() != destinationTile) {
                return DOMMessage.clientError("Carrier: " + carrierId
                    + " is not at destination tile: " + destinationTile);
            }
        }

        // Proceed to embark
        return server.getInGameController()
            .embarkUnit(serverPlayer, unit, carrier);
    }

    /**
     * Convert this EmbarkMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "unit", unitId,
            "carrier", carrierId);
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
