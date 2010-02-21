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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The message sent when the client requests abandoning of a colony.
 */
public class AbandonColonyMessage extends Message {

    /**
     * The colony to abandon.
     */
    String colonyId;


    /**
     * Create a new <code>AbandonColonyMessage</code> with the specified
     * colony.
     *
     * @param colony The <code>Colony</code> to abandon.
     */
    public AbandonColonyMessage(Colony colony) {
        this.colonyId = colony.getId();
    }

    /**
     * Create a new <code>AbandonColonyMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public AbandonColonyMessage(Game game, Element element) {
        this.colonyId = element.getAttribute("colony");
    }

    /**
     * Handle a "abandonColony"-message.
     *
     * @param server The <code>FreeColServer</code> handling the request.
     * @param player The <code>Player</code> abandoning the colony.
     * @param connection The <code>Connection</code> the message is from.
     *
     * @return An update <code>Element</code> defining the new colony
     *         and updating its surrounding tiles,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        Game game = player.getGame();
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Colony colony;
        if (game.getFreeColGameObject(colonyId) instanceof Colony) {
            colony = (Colony) game.getFreeColGameObject(colonyId);
        } else {
            return Message.clientError("Not a colony: " + colonyId);
        }
        if (player != colony.getOwner()) {
            return Message.clientError("Player does not own colony: " + colonyId);
        }
        if (colony.getUnitCount() != 0) {
            return Message.clientError("Attempt to abandon colony " + colonyId
                                       + " with non-zero unit count "
                                       + Integer.toString(colony.getUnitCount()));
        }

        // Remember these before destroying the colony
        String name = colony.getName();
        Tile tile = colony.getTile();
        int radius = colony.getRadius();

        // Proceed to abandon
        colony.dispose();
        HistoryEvent h = new HistoryEvent(game.getTurn().getNumber(), HistoryEvent.EventType.ABANDON_COLONY)
            .addName("%colony%", name);
        player.getHistory().add(h);
        server.getInGameController().sendUpdatedTileToAll(tile, serverPlayer);
        // TODO: clean up trade routes?

        // Reply, updating the surrounding tiles now owned by the colony.
        // TODO: Player.settlements is still being fixed on the client side.
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        reply.appendChild(update);
        update.appendChild(tile.toXMLElement(player, doc));
        Map map = game.getMap();
        for (Tile t : map.getSurroundingTiles(tile, radius)) {
            update.appendChild(t.toXMLElement(player, doc));
        }
        Element history = doc.createElement("addHistory");
        reply.appendChild(history);
        history.appendChild(h.toXMLElement(player, doc));
        return reply;
    }

    /**
     * Convert this AbandonColonyMessage to XML.
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
     * @return "abandonColony".
     */
    public static String getXMLElementTagName() {
        return "abandonColony";
    }
}
