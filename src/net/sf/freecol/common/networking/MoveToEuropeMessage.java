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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when moving a unit to Europe.
 */
public class MoveToEuropeMessage extends DOMMessage {

    /**
     * The id of the object to be moved.
     */
    private String unitId;

    /**
     * Create a new <code>MoveToEuropeMessage</code> for the supplied unit.
     *
     * @param unit The <code>Unit</code> to move.
     */
    public MoveToEuropeMessage(Unit unit) {
        this.unitId = unit.getId();
    }

    /**
     * Create a new <code>MoveToEuropeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MoveToEuropeMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
    }

    /**
     * Handle a "moveToEurope"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the moved unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        if (!serverPlayer.canMoveToEurope()) {
            return DOMMessage.clientError("Player " + player.getId()
                + " can not move units to Europe.");
        }
        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        if (!unit.canMoveToEurope()) {
            return DOMMessage.clientError("Unit can not move to Europe: "
                + unitId);
        }
        if (unit.isAtSea() || unit.getTile() != null) {
            ; // OK
        } else {
            return DOMMessage.clientError("Unit must be in the new world"
                + " or on the high seas to move to Europe: " + unitId);
        }

        // Proceed to move.
        return server.getInGameController()
            .moveToEurope(serverPlayer, unit);
    }

    /**
     * Convert this MoveToEuropeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", this.unitId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "moveToEurope".
     */
    public static String getXMLElementTagName() {
        return "moveToEurope";
    }
}
