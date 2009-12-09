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
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to update a player's market due to trading by other players.
 */
public class UpdateMarketMessage extends Message {
    /**
     * The id of the type of goods traded.
     */
    private String goodsTypeId;

    /**
     * The amount of goods traded.
     */
    private String amount;

    /**
     * Create a new <code>UpdateMarketMessage</code>.
     *
     * @param type The type of goods traded.
     * @param amount The amount of goods traded.
     */
    public UpdateMarketMessage(GoodsType type, int amount) {
        this.goodsTypeId = type.getId();
        this.amount = Integer.toString(amount);
    }

    /**
     * Create a new <code>UpdateMarketMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public UpdateMarketMessage(Game game, Element element) {
        this.goodsTypeId = element.getAttribute("type");
        this.amount = element.getAttribute("amount");
    }


    /**
     * Client-side handler for "updateMarket"-messages.
     *
     * @param player The <code>Player</code> that receives the message.
     * @return Null.
     */
    public Element clientHandler(Player player) {
        GoodsType type;
        int quantity;
        Market market;
        if ((type = FreeCol.getSpecification().getGoodsType(goodsTypeId)) != null
            && (quantity = Integer.parseInt(amount)) != 0
            && (market = player.getMarket()) != null) {
            if (market.addGoodsToMarket(type, quantity)) {
                player.addModelMessage(market.makePriceMessage(type));
            }
        }
        return null;
    }


    /**
     * Do not handle a "updateMarket"-message!
     * These messages are handled client-side.
     *
     * @param server The <code>FreeColServer</code> that handles the message.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return Null.
     */
    public Element handle(FreeColServer server, Connection connection) {
        throw new IllegalStateException("The server originates "
                                        + getXMLElementTagName() + " messages, "
                                        + "it does not handle them.");
    }

    /**
     * Convert this UpdateMarketMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("type", goodsTypeId);
        result.setAttribute("amount", amount);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "updateMarket".
     */
    public static String getXMLElementTagName() {
        return "updateMarket";
    }
}
