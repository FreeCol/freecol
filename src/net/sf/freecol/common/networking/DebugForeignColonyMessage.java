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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The message sent when extracting debugging information for a colony.
 */
public class DebugForeignColonyMessage extends Message {

    /**
     * The ID of the tile the colony is on.
     */
    String tileId;

    /**
     * Create a new <code>DebugForeignColonyMessage</code> with the
     * supplied tile.
     *
     * @param tile a <code>Tile</code> value
     */
    public DebugForeignColonyMessage(Tile tile) {
        this.tileId = tile.getId();
    }

    /**
     * Create a new <code>DebugForeignColonyMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DebugForeignColonyMessage(Game game, Element element) {
        this.tileId = element.getAttribute("tile");
    }


    /**
     * Handle a "debugForeignColony"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling the message.
     * @param connection The <code>Connection</code> the message was received on.
     *
     * @return An <code>Element</code> containing a representation of
     *         the settlement to view any units at that position, or
     *         an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = serverPlayer.getGame();

        if (!FreeCol.isInDebugMode()) {
            return Message.clientError("Not in Debug Mode!");
        }
        Tile tile;
        if (game.getFreeColGameObjectSafely(tileId) instanceof Tile) {
            tile = (Tile) game.getFreeColGameObjectSafely(tileId);
        } else {
            return Message.clientError("Invalid tileId");
        }
        if (tile.getColony() == null) {
            return Message.clientError("There is no colony at: " + tileId);
        }

        // Two versions of the tile, one detailed, one not.
        // The client is trusted to pop the first off,
        // show it, then process the update as normal.
        Element reply = createNewRootElement("update");
        Document doc = reply.getOwnerDocument();
        reply.appendChild(tile.toXMLElement(serverPlayer, doc, true, false));
        reply.appendChild(tile.toXMLElement(serverPlayer, doc, false, false));
        return reply;
    }

    /**
     * Convert this DebugForeignColonyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("tile", tileId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "debugForeignColony".
     */
    public static String getXMLElementTagName() {
        return "debugForeignColony";
    }
}
