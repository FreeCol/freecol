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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;

import java.util.Objects;


/**
 * The message sent when selling at an IndianSettlement.
 */
public class SellMessage extends DOMMessage {

    public static final String TAG = "sell";
    private static final String GOLD_TAG = "gold";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";

    /** The object identifier of the unit that is selling. */
    private final String unitId;

    /** The object identifier of the settlement that is buying. */
    private final String settlementId;

    /** The sale price. */
    private final String goldString;

    /** The goods to be sold. */
    private final Goods goods;


    /**
     * Create a new {@code SellMessage}.
     *
     * @param unit The {@code Unit} that is trading.
     * @param is The {@code IndianSettlement} that is trading.
     * @param goods The {@code Goods} to sell.
     * @param gold The price of the goods.
     */
    public SellMessage(Unit unit, IndianSettlement is, Goods goods,
                       int gold) {
        super(TAG);

        this.unitId = unit.getId();
        this.settlementId = is.getId();
        this.goldString = Integer.toString(gold);
        this.goods = goods;
    }

    /**
     * Create a new {@code SellMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public SellMessage(Game game, Element element) {
        super(TAG);

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
     * Handle a "sell"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param player The {@code Player} the message applies to.
     * @param connection The {@code Connection} message was received on.
     * @return An update as a result of the sale, or an error
     *      {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        IndianSettlement is;
        try {
            is = unit.getAdjacentSettlement(this.settlementId,
                                            IndianSettlement.class);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        // Make sure we are trying to sell something that is there
        if (!Objects.equals(this.goods.getLocation(), unit)) {
            return serverPlayer.clientError("Goods " + this.goods.getId()
                + " is not with unit " + this.unitId)
                .build(serverPlayer);
        }

        int gold = getGold();
        if (gold < 0) {
            return serverPlayer.clientError("Bad gold: " + this.goldString)
                .build(serverPlayer);
        }

        // Proceed to sell
        return server.getInGameController()
            .sellToSettlement(serverPlayer, unit, is, this.goods, gold)
            .build(serverPlayer);
    }

    /**
     * Convert this SellMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            UNIT_TAG, this.unitId,
            SETTLEMENT_TAG, this.settlementId,
            GOLD_TAG, this.goldString)
            .add(goods).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "sell".
     */
    public static String getTagName() {
        return TAG;
    }
}
