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
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when unloading goods.
 */
public class UnloadGoodsMessage extends DOMMessage {

    public static final String TAG = "unloadGoods";
    private static final String AMOUNT_TAG = "amount";
    private static final String CARRIER_TAG = "carrier";
    private static final String TYPE_TAG = "type";

    /** The identifier of the type of goods to unload.  */
    private final String goodsTypeId;

    /** The amount of goods to unload. */
    private final String amountString;

    /** The identifier of the carrier to unload to goods from. */
    private final String carrierId;


    /**
     * Create a new <code>UnloadGoodsMessage</code>.
     *
     * @param goodsType The <code>GoodsType</code> to unload.
     * @param amount The amount of goods to unload.
     * @param carrier The <code>Unit</code> carrying the goods.
     */
    public UnloadGoodsMessage(GoodsType goodsType, int amount, Unit carrier) {
        super(getTagName());

        this.goodsTypeId = goodsType.getId();
        this.amountString = Integer.toString(amount);
        this.carrierId = carrier.getId();
    }

    /**
     * Create a new <code>UnloadGoodsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public UnloadGoodsMessage(Game game, Element element) {
        super(getTagName());

        this.goodsTypeId = getStringAttribute(element, TYPE_TAG);
        this.amountString = getStringAttribute(element, AMOUNT_TAG);
        this.carrierId = getStringAttribute(element, CARRIER_TAG);
    }


    /**
     * Handle a "unloadGoods"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the carrier, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Specification spec = server.getSpecification();

        Unit carrier;
        try {
            carrier = player.getOurFreeColGameObject(this.carrierId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (!carrier.canCarryGoods()) {
            return serverPlayer.clientError("Not a goods carrier: "
                + this.carrierId)
                .build(serverPlayer);
        }
        // Do not check location, carriers can dump goods anywhere

        GoodsType type = spec.getGoodsType(this.goodsTypeId);
        if (type == null) {
            return serverPlayer.clientError("Not a goods type: "
                + this.goodsTypeId)
                .build(serverPlayer);
        }

        int amount;
        try {
            amount = Integer.parseInt(this.amountString);
        } catch (NumberFormatException e) {
            return serverPlayer.clientError("Bad amount: " + this.amountString)
                .build(serverPlayer);
        }
        if (amount <= 0) {
            return serverPlayer.clientError("Amount must be positive: "
                + this.amountString)
                .build(serverPlayer);
        }
        int present = carrier.getGoodsCount(type);
        if (present < amount) {
            return serverPlayer.clientError("Attempt to unload " + amount
                + " " + type.getId() + " but only " + present + " present.")
                .build(serverPlayer);
        }

        // Try to unload.
        return server.getInGameController()
            .unloadGoods(serverPlayer, type, amount, carrier)
            .build(serverPlayer);
    }

    /**
     * Convert this UnloadGoodsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            TYPE_TAG, this.goodsTypeId,
            AMOUNT_TAG, this.amountString,
            CARRIER_TAG, this.carrierId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "unloadGoods".
     */
    public static String getTagName() {
        return TAG;
    }
}
