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

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when loading cargo onto a carrier.
 */
public class LoadCargoMessage extends Message {
    /**
     * The goods to be loaded.
     */
    private Goods goods;

    /**
     * The id of the carrier.
     */
    private String carrierId;

    /**
     * Create a new <code>LoadCargoMessage</code> with the
     * supplied goods and carrier.
     *
     * @param goods The <code>Goods</code> to load.
     * @param carrier The <code>Unit</code> to load onto.
     */
    public LoadCargoMessage(Goods goods, Unit carrier) {
        this.goods = goods;
        this.carrierId = carrier.getId();
    }

    /**
     * Create a new <code>LoadCargoMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public LoadCargoMessage(Game game, Element element) {
        this.carrierId = element.getAttribute("carrier");
        this.goods = new Goods(game, (Element) element.getChildNodes().item(0));
    }

    /**
     * Handle a "loadCargo"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message received on.
     *
     * @return An update containing the carrier,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();

        Unit carrier;
        try {
            carrier = server.getUnitSafely(carrierId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }

        // Perform the load.
        goods.adjustAmount();
        try {
            server.getInGameController().moveGoods(goods, carrier);
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
        Location loc = carrier.getLocation();
        if (loc instanceof Europe) {
            reply.appendChild(((Europe) loc).toXMLElement(player, doc));
        } else if (loc instanceof Tile) {
            reply.appendChild(((Tile) loc).toXMLElement(player, doc));
        } else { // ``Can not happen''
            throw new IllegalStateException("Carrier not in Europe or Tile.");
        }
        return reply;
    }

    /**
     * Convert this LoadCargoMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        Document doc = result.getOwnerDocument();
        result.setAttribute("carrier", carrierId);
        result.appendChild(goods.toXMLElement(null, doc));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "loadCargo".
     */
    public static String getXMLElementTagName() {
        return "loadCargo";
    }
}
