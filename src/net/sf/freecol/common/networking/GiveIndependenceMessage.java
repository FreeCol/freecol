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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when a player gives independence.
 */
public class GiveIndependenceMessage extends Message {

    /**
     * The ID of the player to be given independence.
     */
    private String playerId;

    /**
     * Create a new <code>GiveIndependenceMessage</code> for the given
     * player.
     *
     * @param player The <code>Player</code> to be granted independence.
     */
    public GiveIndependenceMessage(Player player) {
        this.playerId = player.getId();
    }

    /**
     * Create a new <code>GiveIndependenceMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public GiveIndependenceMessage(Game game, Element element) {
        this.playerId = element.getAttribute("player");
    }

    /**
     * Handle a "giveIndependence"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> the message is from.
     *
     * @return An <code>Element</code> containing an update of the REF
     *         and rebel player or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = serverPlayer.getGame();
        ServerPlayer independent;

        if (playerId == null || playerId.length() == 0) {
            return Message.clientError("Player ID must not be empty");
        } else if (game.getFreeColGameObjectSafely(playerId) instanceof Player) {
            independent = (ServerPlayer) game.getFreeColGameObjectSafely(playerId);
        } else {
            return Message.clientError("Not a player ID: " + playerId);
        }
        if (independent.getREFPlayer() != player) {
            return Message.clientError("Cannot give independence to a country we do not own.");
        }

        // Grant
        List<ModelMessage> indeps = independent.giveIndependence(serverPlayer);
        serverPlayer.changeRelationWithPlayer(independent, Stance.PEACE);

        // Full player update is inelegant, but we do this at most, once.
        Connection independentConnection = independent.getConnection();
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        reply.appendChild(update);
        update.appendChild(independent.toXMLElement(doc));
        Element messages = doc.createElement("addMessages");
        reply.appendChild(messages);
        for (ModelMessage m : indeps) {
            m.addToOwnedElement(messages, player);
        }
        try {
            independentConnection.send(reply);
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        // Send everyone else the common view of the player
        reply = Message.createNewRootElement("update");
        doc = reply.getOwnerDocument();
        reply.appendChild(independent.toXMLElement(player, doc, false, false));
        server.getServer().sendToAll(reply, independentConnection);
        return null;
    }

    /**
     * Convert this GiveIndependenceMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("player", playerId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "giveIndependence".
     */
    public static String getXMLElementTagName() {
        return "giveIndependence";
    }
}
