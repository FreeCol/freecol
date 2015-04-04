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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when moving a unit across the high seas.
 */
public class MoveToMessage extends DOMMessage {

    /** The identifier of the object to be moved. */
    private final String unitId;

    /** The identifier of the destination to be moved to. */
    private final String destinationId;


    /**
     * Create a new <code>MoveToMessage</code> for the supplied unit
     * and destination.
     *
     * @param unit The <code>Unit</code> to move.
     * @param destination The <code>Location</code> to move to.
     */
    public MoveToMessage(Unit unit, Location destination) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.destinationId = destination.getId();
    }

    /**
     * Create a new <code>MoveToMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MoveToMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.destinationId = element.getAttribute("destination");
    }


    /**
     * Handle a "moveTo"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the moved unit, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = player.getGame();

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Location destination = game.findFreeColLocation(destinationId);
        if (destination == null) {
            return DOMMessage.clientError("Not a location: " + destinationId);
        }

        // Proceed to move.
        return server.getInGameController()
            .moveTo(serverPlayer, unit, destination);
    }

    /**
     * Convert this MoveToMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return DOMMessage.createMessage(getXMLElementTagName(),
            "unit", unitId,
            "destination", destinationId);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "moveTo".
     */
    public static String getXMLElementTagName() {
        return "moveTo";
    }
}
