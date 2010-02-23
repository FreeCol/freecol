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
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when buying goods in Europe.
 */
public class BuyGoodsMessage extends Message {
    /**
     * The id of the carrier to load to goods onto.
     */
    private String carrierId;

    /**
     * The id of the type of goods to buy.
     */
    private String goodsTypeId;

    /**
     * The amount of goods to buy.
     */
    private int amount;

    /**
     * Create a new <code>BuyGoodsMessage</code>.
     *
     * @param carrier The <code>Unit</code> to load the goods onto.
     * @param type The type of goods to buy.
     * @param amount The amount of goods to buy.
     */
    public BuyGoodsMessage(Unit carrier, GoodsType type, int amount) {
        this.carrierId = carrier.getId();
        this.goodsTypeId = type.getId();
        this.amount = amount;
    }

    /**
     * Create a new <code>BuyGoodsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public BuyGoodsMessage(Game game, Element element) {
        this.carrierId = element.getAttribute("carrier");
        this.goodsTypeId = element.getAttribute("type");
        this.amount = Integer.parseInt(element.getAttribute("amount"));
    }

    /**
     * Handle a "buyGoods"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the carrier,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit carrier;
        try {
            carrier = server.getUnitSafely(carrierId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (!carrier.canCarryGoods()) {
            return Message.clientError("Not a carrier: " + carrierId);
        }
        if (!carrier.isInEurope()) {
            return Message.clientError("Not in Europe: " + carrierId);
        }
        GoodsType type = FreeCol.getSpecification().getGoodsType(goodsTypeId);
        if (type == null) {
            return Message.clientError("Not a goods type: " + goodsTypeId);
        }
        if (amount <= 0) {
            return Message.clientError("Amount must be positive: "
                                       + Integer.toString(amount));
        }

        // FIXME: market.buy() should be in the controller, but there
        // are two cases remaining that are hard to move still.
        //
        // 1. There is a shortcut buying of equipment in Europe in
        // Unit.equipWith().
        // 2. Also for the goods required for a building in
        // Colony.payForBuilding().  This breaks the pattern implemented
        // here as there is no unit involved.
        //
        // Try to buy.
        InGameController igc = server.getInGameController();
        ModelMessage message = null;
        Market market = player.getMarket();
        try {
            market.buy(type, amount, player);
            carrier.getGoodsContainer().addGoods(type, amount);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (market.hasPriceChanged(type)) {
            // This type of goods has changed price, so we will update
            // the market and send a message as well.
            message = market.makePriceChangeMessage(type);
            market.flushPriceChange(type);
        }
        igc.propagateToEuropeanMarkets(type, amount, serverPlayer);

        // Build reply.
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        reply.appendChild(update);
        update.appendChild(player.toXMLElementPartial(doc, "gold", "score"));
        update.appendChild(carrier.toXMLElement(player, doc));
        if (message != null) {
            update.appendChild(market.toXMLElement(player, doc));
            Element addMessages = doc.createElement("addMessages");
            reply.appendChild(addMessages);
            message.addToOwnedElement(addMessages, player);
        }
        return reply;
    }

    /**
     * Convert this BuyGoodsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("carrier", carrierId);
        result.setAttribute("type", goodsTypeId);
        result.setAttribute("amount", Integer.toString(amount));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "buyGoods".
     */
    public static String getXMLElementTagName() {
        return "buyGoods";
    }
}
//TODO: this and SellGoodsMessage are almost identical, collapse into
//a single TradeGoods?