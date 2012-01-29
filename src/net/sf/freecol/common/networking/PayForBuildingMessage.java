/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when paying for a building.
 */
public class PayForBuildingMessage extends DOMMessage {

    /**
     * The id of the colony that is building.
     */
    private String colonyId;

    /**
     * Create a new <code>PayForBuildingMessage</code> with the
     * supplied colony.
     *
     * @param colony The <code>Colony</code> that is building.
     */
    public PayForBuildingMessage(Colony colony) {
        this.colonyId = colony.getId();
    }

    /**
     * Create a new <code>PayForBuildingMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public PayForBuildingMessage(Game game, Element element) {
        this.colonyId = element.getAttribute("colony");
    }

    /**
     * Handle a "payForBuilding"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the payForBuildingd unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();

        Colony colony;
        if (game.getFreeColGameObject(colonyId) instanceof Colony) {
            colony = (Colony) game.getFreeColGameObject(colonyId);
        } else {
            return DOMMessage.clientError("Not a colony: " + colonyId);
        }
        if (!player.owns(colony)) {
            return DOMMessage.clientError("Not your colony: " + colonyId);
        }

        // Proceed to pay.
        return server.getInGameController()
            .payForBuilding(serverPlayer, colony);
    }

    /**
     * Convert this PayForBuildingMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("colony", colonyId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "payForBuilding".
     */
    public static String getXMLElementTagName() {
        return "payForBuilding";
    }
}
