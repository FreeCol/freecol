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

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when loading goods.
 */
public class LoadGoodsMessage extends DOMMessage {

    /** The identifier for the location of the goods. */
    private final String locationId;
    
    /** The identifier of the type of goods to load. */
    private final String goodsTypeId;

    /** The amount of goods to load. */
    private final String amountString;

    /** The identifier of the carrier to load to goods onto. */
    private final String carrierId;


    /**
     * Create a new <code>LoadGoodsMessage</code>.
     *
     * @param loc The <code>Location</code> of the goods.
     * @param type The <code>GoodsType</code> to load.
     * @param amount The amount of goods to load.
     * @param carrier The <code>Unit</code> to load the goods onto.
     */
    public LoadGoodsMessage(Location loc, GoodsType type, int amount,
                            Unit carrier) {
        super(getXMLElementTagName());

        this.locationId = loc.getId();
        this.goodsTypeId = type.getId();
        this.amountString = Integer.toString(amount);
        this.carrierId = carrier.getId();
    }

    /**
     * Create a new <code>LoadGoodsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public LoadGoodsMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.locationId = element.getAttribute("location");
        this.goodsTypeId = element.getAttribute("type");
        this.amountString = element.getAttribute("amount");
        this.carrierId = element.getAttribute("carrier");
    }


    /**
     * Handle a "loadGoods"-message.
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

        FreeColGameObject fcgo = player.getGame()
            .getFreeColGameObject(locationId);
        if (fcgo == null || !(fcgo instanceof Location)) {
            return DOMMessage.clientError("Not a location: " + locationId);
        }

        Unit carrier;
        try {
            carrier = player.getOurFreeColGameObject(carrierId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        if (!carrier.canCarryGoods()) {
            return DOMMessage.clientError("Not a goods carrier: " + carrierId);
        } else if (carrier.getTradeLocation() == null) {
            return DOMMessage.clientError("Not at a trade location: " + carrierId);
        }

        GoodsType type = server.getSpecification().getGoodsType(goodsTypeId);
        if (type == null) {
            return DOMMessage.clientError("Not a goods type: " + goodsTypeId);
        }

        int amount;
        try {
            amount = Integer.parseInt(amountString);
        } catch (NumberFormatException e) {
            return DOMMessage.clientError("Bad amount: " + amountString);
        }
        if (amount <= 0) {
            return DOMMessage.clientError("Amount must be positive: "
                + amountString);
        }

        // Load the goods
        return server.getInGameController()
            .loadGoods(serverPlayer, (Location)fcgo, type, amount, carrier);
    }

    /**
     * Convert this LoadGoodsMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "location", locationId,
            "type", goodsTypeId,
            "amount", amountString,
            "carrier", carrierId);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "loadGoods".
     */
    public static String getXMLElementTagName() {
        return "loadGoods";
    }
}
