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

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when negotiating a sale at an IndianSettlement.
 */
public class SellPropositionMessage extends Message {
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
     * The price being negotiated.
     */
    private int gold;

    /**
     * Create a new <code>SellPropositionMessage</code>.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The price of the goods (negative if unknown).
     */
    public SellPropositionMessage(Unit unit, Settlement settlement,
                                   Goods goods, int gold) {
        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.goods = goods;
        this.gold = gold;
    }

    /**
     * Create a new <code>SellPropositionMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SellPropositionMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
        this.gold = Integer.parseInt(element.getAttribute("gold"));
        this.goods = new Goods(game, Message.getChildElement(element, Goods.getXMLElementTagName()));
    }

    /**
     * What is the price currently negotiated for this transaction?
     *
     * @return The current price.
     */
    public int getGold() {
        return gold;
    }

    /**
     * Handle a "sellProposition"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return This message with updated gold value.
     * @throws IllegalStateException if there is problem with the arguments.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();
        Unit unit = server.getUnitSafely(unitId, serverPlayer);
        IndianSettlement settlement = server.getAdjacentIndianSettlementSafely(settlementId, unit);

        // Make sure we are trying to sell something that is there
        Boolean ok = false;
        for (Goods sell : unit.getGoodsList()) {
            if (sell.getId() == goods.getId()) ok = true;
        }
        if (!ok) {
            throw new IllegalStateException("sellProposition for non-existant goods");
        }

        InGameController controller = (InGameController) server.getController();
        if (!controller.isTransactionSessionOpen(unit, settlement)) {
            throw new IllegalStateException("Trying to sell without opening a transaction session");
        }
        java.util.Map<String,Object> session = controller.getTransactionSession(unit, settlement);
        if (!(Boolean) session.get("canSell")) {
            throw new IllegalStateException("Trying to sell in a session where selling is not allowed.");
        }

        // AI considers the proposition, return with a gold value
        AIPlayer ai = (AIPlayer) server.getAIMain().getAIObject(settlement.getOwner());
        gold = ai.tradeProposition(unit, settlement, goods, gold);
        return this.toXMLElement();
    }

    /**
     * Convert this SellPropositionMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("settlement", settlementId);
        result.setAttribute("gold", Integer.toString(gold));
        result.appendChild(goods.toXMLElement(null, result.getOwnerDocument()));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "sellProposition".
     */
    public static String getXMLElementTagName() {
        return "sellProposition";
    }
}
