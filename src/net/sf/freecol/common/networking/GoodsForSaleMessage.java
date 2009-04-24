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
import java.util.ArrayList;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when renaming a FreeColGameObject.
 */
public class GoodsForSaleMessage extends Message {
    /**
     * The id of the unit that is trading.
     */
    private String unitId;

    /**
     * The id of the settlement that is trading.
     */
    private String settlementId;

    /**
     * The list of goods for sale.
     */
    private List<Goods> sellGoods;


    /**
     * Create a new <code>GoodsForSaleMessage</code> with the
     * supplied name.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     */
    public GoodsForSaleMessage(Unit unit, Settlement settlement) {
        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.sellGoods = new ArrayList<Goods>();
    }

    /**
     * Create a new <code>GoodsForSaleMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public GoodsForSaleMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
        this.sellGoods = new ArrayList<Goods>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            this.sellGoods.add(new Goods(game, (Element) children.item(i)));
        }
    }

    /**
     * Handle a "goodsForSale"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> the message was received on.
     *
     * @return This <code>GoodsForSaleMessage</code> with the goods for
     *         sale attached as children,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();
        Unit unit;
        IndianSettlement settlement;

        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
            settlement = server.getAdjacentIndianSettlementSafely(settlementId, unit);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        sellGoods = settlement.getSellGoods();
        if (!sellGoods.isEmpty()) {
            AIPlayer aiPlayer = (AIPlayer) server.getAIMain().getAIObject(settlement.getOwner());
            for (Goods goods : sellGoods) {
                aiPlayer.registerSellGoods(goods);
            }
        }
        return this.toXMLElement();
    }

    /**
     * Convert this GoodsForSaleMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("settlement", settlementId);
        if (!sellGoods.isEmpty()) {
            for (Goods goods : sellGoods) {
                result.appendChild(goods.toXMLElement(null, result.getOwnerDocument()));
            }
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "goodsForSale".
     */
    public static String getXMLElementTagName() {
        return "goodsForSale";
    }
}
