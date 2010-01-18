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

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when disembarking.
 */
public class DisembarkMessage extends Message {
    /**
     * The id of the object disembarking.
     */
    private String unitId;

    /**
     * Create a new <code>DisembarkMessage</code> with the
     * supplied name.
     *
     * @param unit The <code>Unit</code> that is disembarking.
     */
    public DisembarkMessage(Unit unit) {
        this.unitId = unit.getId();
    }

    /**
     * Create a new <code>DisembarkMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DisembarkMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
    }

    /**
     * Handle a "disembark"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the disembarkd unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (!(unit.getLocation() instanceof Unit)) {
            return Message.clientError("Unit " + unitId
                                       + " at " + unit.getLocation().getId()
                                       + " which is not a carrier.");
        }

        // Do the disembark.
        Unit carrier = (Unit) unit.getLocation();
        if (!server.getInGameController().disembarkUnit(serverPlayer, unit)) {
            return Message.clientError("Unable to disembark " + unitId
                                       + " from " + carrier.getId());
        }

        // Only have to update the new location, as it contains the carrier.
        Element reply = Message.createNewRootElement("update");
        Document doc = reply.getOwnerDocument();
        if (carrier.isInEurope()) {
            Europe europe = (Europe) carrier.getLocation();
            reply.appendChild(europe.toXMLElement(player, doc));
        } else {
            Tile tile = (Tile) carrier.getLocation();
            reply.appendChild(tile.toXMLElement(player, doc));
        }
        return reply;
    }

    /**
     * Convert this DisembarkMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "disembark".
     */
    public static String getXMLElementTagName() {
        return "disembark";
    }
}
