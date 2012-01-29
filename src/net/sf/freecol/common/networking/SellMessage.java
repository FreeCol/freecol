/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when selling at an IndianSettlement.
 */
public class SellMessage extends DOMMessage {

    /**
     * The ID of the unit that is selling.
     */
    private String unitId;

    /**
     * The ID of the settlement that is buying.
     */
    private String settlementId;

    /**
     * The goods to be sold.
     */
    private Goods goods;

    /**
     * The sale price.
     */
    private String goldString;

    /**
     * Create a new <code>SellMessage</code>.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The price of the goods.
     */
    public SellMessage(Unit unit, Settlement settlement, Goods goods,
                       int gold) {
        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.goods = goods;
        this.goldString = Integer.toString(gold);
    }

    /**
     * Create a new <code>SellMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SellMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
        this.goods = new Goods(game,
            DOMMessage.getChildElement(element, Goods.getXMLElementTagName()));
        this.goldString = element.getAttribute("gold");
    }

    /**
     * Handle a "sell"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return Null.
     * @throws IllegalStateException if there is problem with the arguments.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Unit unit;
        IndianSettlement settlement;

        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
            settlement = server.getAdjacentIndianSettlementSafely(settlementId, unit);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        // Make sure we are trying to sell something that is there
        if (goods.getLocation() != unit) {
            return DOMMessage.createError("server.trade.noGoods", "Goods "
                + goods.getId() + " is not with unit " + unitId);
        }
        int gold;
        try {
            gold = Integer.parseInt(goldString);
        } catch (NumberFormatException e) {
            return DOMMessage.clientError("Bad gold: " + goldString);
        }

        // Proceed to sell
        return server.getInGameController()
            .sellToSettlement(serverPlayer, unit, settlement, goods, gold);
    }

    /**
     * Convert this SellMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("settlement", settlementId);
        result.appendChild(goods.toXMLElement(null, result.getOwnerDocument()));
        result.setAttribute("gold", goldString);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "sell".
     */
    public static String getXMLElementTagName() {
        return "sell";
    }
}
