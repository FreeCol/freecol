/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when loading goods.
 */
public class LoadGoodsMessage extends AttributeMessage {

    public static final String TAG = "loadGoods";
    private static final String AMOUNT_TAG = "amount";
    private static final String CARRIER_TAG = "carrier";
    private static final String LOCATION_TAG = "location";
    private static final String TYPE_TAG = "type";


    /**
     * Create a new {@code LoadGoodsMessage}.
     *
     * @param loc The {@code Location} of the goods.
     * @param type The {@code GoodsType} to load.
     * @param amount The amount of goods to load.
     * @param carrier The {@code Unit} to load the goods onto.
     */
    public LoadGoodsMessage(Location loc, GoodsType type, int amount,
                            Unit carrier) {
        super(TAG, LOCATION_TAG, loc.getId(),
              TYPE_TAG, type.getId(),
              AMOUNT_TAG, String.valueOf(amount),
              CARRIER_TAG, carrier.getId());
    }

    /**
     * Create a new {@code LoadGoodsMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public LoadGoodsMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, LOCATION_TAG, TYPE_TAG, AMOUNT_TAG, CARRIER_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final Specification spec = freeColServer.getSpecification();
        final String locationId = getStringAttribute(LOCATION_TAG);
        final String typeId = getStringAttribute(TYPE_TAG);
        final String carrierId = getStringAttribute(CARRIER_TAG);
        final String amountString = getStringAttribute(AMOUNT_TAG);

        FreeColGameObject fcgo = game.getFreeColGameObject(locationId);
        if (!(fcgo instanceof Location)) {
            return serverPlayer.clientError("Not a location: " + locationId);
        }

        Unit carrier;
        try {
            carrier = serverPlayer.getOurFreeColGameObject(carrierId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!carrier.canCarryGoods()) {
            return serverPlayer.clientError("Not a goods carrier: "
                + carrierId);
        } else if (carrier.getTradeLocation() == null) {
            return serverPlayer.clientError("Not at a trade location: "
                + carrierId);
        }

        GoodsType type = spec.getGoodsType(typeId);
        if (type == null) {
            return serverPlayer.clientError("Not a goods type: " + typeId);
        }

        int amount;
        try {
            amount = Integer.parseInt(amountString);
        } catch (NumberFormatException e) {
            return serverPlayer.clientError("Bad amount: " + amountString);
        }
        if (amount <= 0) {
            return serverPlayer.clientError("Amount must be positive: "
                + amountString);
        }

        // Load the goods
        return igc(freeColServer)
            .loadGoods(serverPlayer, (Location)fcgo, type, amount, carrier);
    }
}
