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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when unloading cargo onto a carrier.
 */
public class UnloadCargoMessage extends Message {
    /**
     * The goods to be unloaded.
     */
    private Goods goods;

    /**
     * Create a new <code>UnloadCargoMessage</code> with the
     * supplied goods and carrier.
     *
     * @param goods The <code>Goods</code> to unload.
     */
    public UnloadCargoMessage(Goods goods) {
        this.goods = goods;
    }

    /**
     * Create a new <code>UnloadCargoMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public UnloadCargoMessage(Game game, Element element) {
        this.goods = new Goods(game, (Element) element.getChildNodes().item(0));
    }

    /**
     * Handle a "unloadCargo"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message received on.
     *
     * @return An update containing the containing location
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();

        // Sanity checks.
        Location loc = goods.getLocation();
        if (loc == null) {
            return Message.clientError("Goods in a null location.");
        } else if (!(loc instanceof Unit)) {
            return Message.clientError("Unload from non-unit.");
        }
        Unit carrier = (Unit) loc;
        if (carrier.getOwner() != player) {
            return Message.clientError("Unload from non-owned unit.");
        } else if (carrier.getTile() == null) {
            return Message.clientError("Unload from unit not on the map.");
        }

        // Perform the unload.
        Tile tile = carrier.getTile();
        Colony colony = (tile.getSettlement() instanceof Colony)
            ? (Colony) tile.getSettlement()
            : null;
        goods.adjustAmount();
        try {
            server.getInGameController().moveGoods(goods, colony);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (carrier.getInitialMovesLeft() != carrier.getMovesLeft()) {
            carrier.setMovesLeft(0);
        }

        // Build response.  Only have to update the carrier location,
        // as that *must* include the original location of the goods.
        Element reply = Message.createNewRootElement("update");
        Document doc = reply.getOwnerDocument();
        reply.appendChild(tile.toXMLElement(player, doc));
        return reply;
    }

    /**
     * Convert this UnloadCargoMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        Document doc = result.getOwnerDocument();
        result.appendChild(goods.toXMLElement(null, doc));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "unloadCargo".
     */
    public static String getXMLElementTagName() {
        return "unloadCargo";
    }
}
