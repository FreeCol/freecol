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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when spying on a settlement.
 */
public class DebugForeignColonyMessage extends Message {

    Tile tile;

    /**
     * An Element describing the settlement spied upon.
     */
    Element tileElement;

    /**
     * Create a new <code>DebugForeignColonyMessage</code> with the
     * supplied tile.
     *
     * @param tile a <code>Tile</code> value
     */
    public DebugForeignColonyMessage(Tile tile) {
        this.tile = tile;
        this.tileElement = null;
    }

    /**
     * Create a new <code>DebugForeignColonyMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DebugForeignColonyMessage(Game game, Element element) {
        this.tile = (Tile) game.getFreeColGameObject(element.getAttribute("tile"));
        this.tileElement = (element.getChildNodes().getLength() != 1) ? null
            : (Element) element.getChildNodes().item(0);
    }

    public Element getTileElement() {
        return tileElement;
    }


    /**
     * Handle a "spySettlement"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling the message.
     * @param connection The <code>Connection</code> the message was received on.
     *
     * @return An <code>Element</code> containing a representation of the
     *         settlement being spied upon and any units at that position,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        if (!FreeCol.isInDebugMode()) {
            return Message.clientError("Not in Debug Mode!");
        } else if (tile == null) {
            return Message.clientError("Could not find tile");
        }
        Settlement settlement = tile.getSettlement();
        if (settlement == null) {
            return Message.clientError("There is no settlement at: " + tile.getId());
        }

        // Two versions of the tile, one detailed, one not.
        // The client is trusted (gritch gritch) to pop the first off,
        // show it, then process the update as normal.
        // Given we are *correctly* revealing information the client
        // would not normally have, there is not much to be done about
        // the trust issue.
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
        result.setAttribute("tile", tile.getId());
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
