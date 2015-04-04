/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.List;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when querying a settlement for what it has for sale.
 */
public class GoodsForSaleMessage extends DOMMessage {

    /** The identifier of the unit that is trading. */
    private final String unitId;

    /** The identifier of the settlement that is trading. */
    private final String settlementId;

    /** The list of goods for sale. */
    private final List<Goods> sellGoods;


    /**
     * Create a new <code>GoodsForSaleMessage</code> with the
     * supplied name.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param sellGoods A list of <code>Goods</code> to be sold.
     */
    public GoodsForSaleMessage(Unit unit, Settlement settlement,
                               List<Goods> sellGoods) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.sellGoods = sellGoods;
    }

    /**
     * Create a new <code>GoodsForSaleMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public GoodsForSaleMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
        this.sellGoods = new ArrayList<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            this.sellGoods.add(new Goods(game, (Element) children.item(i)));
        }
    }


    // Public interface

    public List<Goods> getGoods() {
        return this.sellGoods;
    }


    /**
     * Handle a "goodsForSale"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return This <code>GoodsForSaleMessage</code> with the goods
     *     for sale attached as children or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        IndianSettlement settlement;
        try {
            settlement = unit.getAdjacentIndianSettlementSafely(settlementId);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        // Try to collect the goods for sale.
        return server.getInGameController()
            .getGoodsForSale(serverPlayer, unit, settlement);
    }

    /**
     * Convert this GoodsForSaleMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "unit", unitId,
            "settlement", settlementId);
        if (sellGoods != null) {
            for (Goods goods : sellGoods) {
                result.appendChild(goods.toXMLElement(result.getOwnerDocument()));
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
