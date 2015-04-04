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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when the client requests setting a unit destination.
 */
public class SetDestinationMessage extends DOMMessage {

    /** The object identifier of the unit whose destination is to be set. */
    private final String unitId;

    /** The object identifier of the unit destination or null. */
    private final String destinationId;


    /**
     * Create a new <code>SetDestinationMessage</code> with the supplied unit
     * and destination.
     *
     * @param unit The <code>Unit</code> whose destination is to be set
     * @param destination The destination to set (may be null)
     */
    public SetDestinationMessage(Unit unit, Location destination) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.destinationId = (destination == null) ? null
            : destination.getId();
    }

    /**
     * Create a new <code>SetDestinationMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SetDestinationMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.destinationId = (element.hasAttribute("destination"))
            ? element.getAttribute("destination")
            : null;
    }


    /**
     * Handle a "setDestination"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> the message is from.
     * @return An update containing the unit with the new destination,
     *     or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = serverPlayer.getGame();

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        // destination == null is OK.
        Location destination = (destinationId == null) ? null
            : game.findFreeColLocation(destinationId);

        // Set destination
        return server.getInGameController()
            .setDestination(serverPlayer, unit, destination);
    }

    /**
     * Convert this SetDestinationMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "unit", unitId);
        if (destinationId != null) {
            result.setAttribute("destination", destinationId);
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "setDestination".
     */
    public static String getXMLElementTagName() {
        return "setDestination";
    }
}
