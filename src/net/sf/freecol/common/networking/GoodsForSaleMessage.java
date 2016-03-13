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


/**
 * The message sent when querying a settlement for what it has for sale.
 */
public class GoodsForSaleMessage extends DOMMessage {

    public static final String TAG = "goodsForSale";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the unit that is trading. */
    private final String unitId;

    /** The identifier of the settlement that is trading. */
    private final String settlementId;

    /** The list of goods for sale. */
    private final List<Goods> sellGoods = new ArrayList<>();


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
        super(getTagName());

        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.sellGoods.clear();
        if (sellGoods != null) this.sellGoods.addAll(sellGoods);
    }

    /**
     * Create a new <code>GoodsForSaleMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public GoodsForSaleMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.settlementId = getStringAttribute(element, SETTLEMENT_TAG);
        this.sellGoods.clear();
        this.sellGoods.addAll(getChildren(game, element, Goods.class));
    }


    // Public interface

    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(this.unitId, Unit.class);
    }

    public IndianSettlement getSettlement(Unit unit) {
        return unit.getAdjacentIndianSettlementSafely(this.settlementId);
    }

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
            unit = getUnit(player);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        IndianSettlement settlement;
        try {
            settlement = getSettlement(unit);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        // Try to collect the goods for sale.
        return server.getInGameController()
            .getGoodsForSale(serverPlayer, unit, settlement)
            .toXMLElement();
    }

    /**
     * Convert this GoodsForSaleMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            SETTLEMENT_TAG, this.settlementId)
            .add(this.sellGoods).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "goodsForSale".
     */
    public static String getTagName() {
        return TAG;
    }
}
