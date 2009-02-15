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

import org.w3c.dom.Element;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when the client requests setting a unit destination.
 */
public class SetDestinationMessage extends Message {

    /**
     * The ID of the unit whose destination is to be set.
     **/
    String unitId;

    /**
     * The ID of the unit destination or null.
     */
    String destinationId;


    /**
     * Create a new <code>SetDestinationMessage</code> with the supplied unit
     * and destination.
     *
     * @param unit The <code>Unit</code> whose destination is to be set
     * @param destination The destination to set (may be null)
     */
    public SetDestinationMessage(Unit unit, Location destination) {
        this.unitId = unit.getId();
        this.destinationId = (destination == null) ? null : destination.getId();
    }

    /**
     * Create a new <code>SetDestinationMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SetDestinationMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.destinationId = element.getAttribute("destination");
    }

    /**
     * Handle a "setDestination"-message.
     *
     * @param connection The <code>Connection</code> the message was received on.
     * @param element The element containing the request.
     *
     * @return Null.
     * @throw IllegalStateException if there is a problem with the message
     *        arguments..
     */
    public Element handle(FreeColServer server, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = serverPlayer.getGame();
        Unit unit;
        Location destination;

        if (unitId == null || unitId.length() == 0) {
            throw new IllegalStateException("unitId must not be empty.");
        }
        if (!(game.getFreeColGameObject(unitId) instanceof Unit)) {
            throw new IllegalStateException("Not a unit ID: " + unitId);
        }
        unit = (Unit) game.getFreeColGameObject(unitId);
        if (unit.getOwner() != serverPlayer) {
            throw new IllegalStateException("Not the owner of building unit: "
                                            + unitId);
        }
        if (destinationId == null) {
            destination = null;
        } else if (!(game.getFreeColGameObject(destinationId) instanceof Location)) {
            throw new IllegalStateException("Not a location ID: " + destinationId);
        } else {
            destination = (Location) game.getFreeColGameObject(destinationId);
        }
        unit.setDestination(destination);
        return null;
    }

    /**
     * Convert this SetDestinationMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
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
