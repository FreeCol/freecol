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
 * The message sent when negotiating a sale at an IndianSettlement.
 */
public class SellPropositionMessage extends DOMMessage {

    /** The object identifier of the unit that is selling. */
    private final String unitId;

    /** The object identifier of the settlement that is buying. */
    private final String settlementId;

    /** The goods to be sold. */
    private final Goods goods;

    /** The price being negotiated. */
    private final String goldString;


    /**
     * Create a new <code>SellPropositionMessage</code>.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The price of the goods (negative if unknown).
     */
    public SellPropositionMessage(Unit unit, Settlement settlement,
                                   Goods goods, int gold) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.goods = goods;
        this.goldString = Integer.toString(gold);
    }

    /**
     * Create a new <code>SellPropositionMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SellPropositionMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
        this.goldString = element.getAttribute("gold");
        this.goods = new Goods(game,
            DOMMessage.getChildElement(element, Goods.getXMLElementTagName()));
    }


    // Public interface

    /**
     * What is the price currently negotiated for this transaction?
     *
     * @return The current price.
     */
    public int getGold() {
        try {
            return Integer.parseInt(goldString);
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    /**
     * Handle a "sellProposition"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return This message with updated gold value, or an error
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

        // Make sure we are trying to sell something that is there
        if (goods.getLocation() != unit) {
            return DOMMessage.clientError("Goods " + goods.getId()
                + " are not with unit " + unitId);
        }

        // Proceed to price.
        return server.getInGameController()
            .sellProposition(serverPlayer, unit, settlement, goods, getGold());
    }

    /**
     * Convert this SellPropositionMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "unit", unitId,
            "settlement", settlementId,
            "gold", goldString);
        result.appendChild(goods.toXMLElement(result.getOwnerDocument()));
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
