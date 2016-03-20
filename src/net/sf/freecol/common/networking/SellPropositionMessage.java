/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

    public static final String TAG = "sellProposition";
    private static final String GOLD_TAG = "gold";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";

    /** The object identifier of the unit that is selling. */
    private final String unitId;

    /** The object identifier of the settlement that is buying. */
    private final String settlementId;

    /** The price being negotiated. */
    private final String goldString;

    /** The goods to be sold. */
    private final Goods goods;


    /**
     * Create a new <code>SellPropositionMessage</code>.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The price of the goods (negative if unknown).
     */
    public SellPropositionMessage(Unit unit, Settlement settlement,
                                  Goods goods, int gold) {
        super(getTagName());

        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.goldString = Integer.toString(gold);
        this.goods = goods;
    }

    /**
     * Create a new <code>SellPropositionMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SellPropositionMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.settlementId = getStringAttribute(element, SETTLEMENT_TAG);
        this.goldString = getStringAttribute(element, GOLD_TAG);
        this.goods = getChild(game, element, 0, Goods.class);
    }


    // Public interface

    /**
     * What is the price currently negotiated for this transaction?
     *
     * @return The current price.
     */
    public int getGold() {
        try {
            return Integer.parseInt(this.goldString);
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
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        IndianSettlement settlement;
        try {
            settlement = unit
                .getAdjacentIndianSettlementSafely(this.settlementId);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        // Make sure we are trying to sell something that is there
        if (this.goods.getLocation() != unit) {
            return serverPlayer.clientError("Goods " + this.goods.getId()
                + " are not with unit " + this.unitId)
                .build(serverPlayer);
        }

        // Proceed to price.
        return server.getInGameController()
            .sellProposition(serverPlayer, unit, settlement,
                             this.goods, getGold())
            .toXMLElement();
    }

    /**
     * Convert this SellPropositionMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            SETTLEMENT_TAG, this.settlementId,
            GOLD_TAG, this.goldString)
            .add(this.goods).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "sellProposition".
     */
    public static String getTagName() {
        return TAG;
    }
}
