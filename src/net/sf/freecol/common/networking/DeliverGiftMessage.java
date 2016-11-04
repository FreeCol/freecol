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


/**
 * The message sent when delivering a gift to a Settlement.
 */
public class DeliverGiftMessage extends DOMMessage {

    public static final String TAG = "deliverGift";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";

    /** The object identifier of the unit that is delivering the gift. */
    private final String unitId;

    /** The object identifier of the settlement the gift is going to. */
    private final String settlementId;

    /** The goods to be delivered. */
    private final Goods goods;


    /**
     * Create a new {@code DeliverGiftMessage}.
     *
     * @param unit The {@code Unit} that is trading.
     * @param is The {@code IndianSettlement} that is trading.
     * @param goods The {@code Goods} to deliverGift.
     */
    public DeliverGiftMessage(Unit unit, IndianSettlement is, Goods goods) {
        super(TAG);

        this.unitId = unit.getId();
        this.settlementId = is.getId();
        this.goods = goods;
    }

    /**
     * Create a new {@code DeliverGiftMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public DeliverGiftMessage(Game game, Element element) {
        super(TAG);

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.settlementId = getStringAttribute(element, SETTLEMENT_TAG);
        this.goods = getChild(game, element, 0, Goods.class);
    }


    // Public interface

    /**
     * Get the {@code Unit} which is delivering the gift.  This
     * is a helper routine to be called in-client as it blindly trusts
     * its field.
     *
     * @return The {@code Unit}, or null if none.
     */
    public Unit getUnit() {
        return this.goods.getGame().getFreeColGameObject(this.unitId,
                                                         Unit.class);
    }

    /**
     * Get the {@code Settlement} which is receiving the gift.
     * This is a helper routine to be called in-client as it blindly trusts
     * its field.
     *
     * @return The {@code Settlement}, or null if none.
     */
    public IndianSettlement getSettlement() {
        return this.goods.getGame().getFreeColGameObject(this.settlementId,
            IndianSettlement.class);
    }

    /**
     * Get the {@code Goods} delivered as a gift.  This is a
     * helper routine to be called in-client as it blindly trusts its
     * field.
     *
     * @return The {@code Goods}, or null if none.
     */
    public Goods getGoods() {
        return this.goods;
    }


    /**
     * Handle a "deliverGift"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param serverPlayer The {@code ServerPlayer} the message applies to.
     * @return An update containing the unit and settlement, or an
     *     error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, ServerPlayer serverPlayer) {
        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        IndianSettlement is;
        try {
            is = unit.getAdjacentSettlement(this.settlementId,
                                            IndianSettlement.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        // Make sure we are trying to deliver something that is there
        if (this.goods.getLocation() != unit) {
            return serverPlayer.clientError("Gift " + this.goods.getId()
                + " is not with unit " + this.unitId)
                .build(serverPlayer);
        }

        // Proceed to deliver.
        return server.getInGameController()
            .deliverGiftToSettlement(serverPlayer, unit, is, goods)
            .build(serverPlayer);
    }

    /**
     * Convert this DeliverGiftMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            UNIT_TAG, this.unitId,
            SETTLEMENT_TAG, this.settlementId)
            .add(this.goods).toXMLElement();
    }
}
