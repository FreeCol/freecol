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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.FreeColServer;


/**
 * The message sent when the client requests buying land.
 */
public class BuyLandMessage extends Message {
    /**
     * The tile to buy.
     */
    private Tile tile;

    /**
     * Create a new <code>BuyLandMessage</code> with the supplied tile.
     *
     * @param tile The <code>Tile</code> to buy.
     */
    public BuyLandMessage(Tile tile) {
        this.tile = tile;
    }

    /**
     * Create a new <code>BuyLandMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public BuyLandMessage(Game game, Element element) {
        this.tile = (Tile) game.getFreeColGameObject(element.getAttribute("tile"));
    }

    /**
     * Handle a "buyLand"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> the message was received on.
     *
     * @return Null or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        if (tile == null) {
            return Message.clientError("Tile must not be null.");
        } else if (tile.getOwner() == null) {
            tile.setOwner(player);
        } else if (tile.getOwner().isEuropean()) {
            return Message.createError("server.buyLand.europeans", null);
        } else {
            player.buyLand(tile);
        }
        return null;
    }

    /**
     * Convert this BuyLandMessage to XML.
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
     * @return "buyLand".
     */
    public static String getXMLElementTagName() {
        return "buyLand";
    }
}
