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
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when unloading goods.
 */
public class UnloadGoodsMessage extends AttributeMessage {

    public static final String TAG = "unloadGoods";
    private static final String AMOUNT_TAG = "amount";
    private static final String CARRIER_TAG = "carrier";
    private static final String TYPE_TAG = "type";


    /**
     * Create a new {@code UnloadGoodsMessage}.
     *
     * @param goodsType The {@code GoodsType} to unload.
     * @param amount The amount of goods to unload.
     * @param carrier The {@code Unit} carrying the goods.
     */
    public UnloadGoodsMessage(GoodsType goodsType, int amount, Unit carrier) {
        super(TAG, TYPE_TAG, goodsType.getId(),
              AMOUNT_TAG, String.valueOf(amount),
              CARRIER_TAG, carrier.getId());
    }

    /**
     * Create a new {@code UnloadGoodsMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public UnloadGoodsMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, TYPE_TAG, AMOUNT_TAG, CARRIER_TAG);
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
        final Specification spec = freeColServer.getSpecification();
        final String typeId = getStringAttribute(TYPE_TAG);
        final int amount = getIntegerAttribute(AMOUNT_TAG, -1);
        final String carrierId = getStringAttribute(CARRIER_TAG);
        
        Unit carrier;
        try {
            carrier = serverPlayer.getOurFreeColGameObject(carrierId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!carrier.canCarryGoods()) {
            return serverPlayer.clientError("Not a goods carrier: "
                + carrierId);
        }
        // Do not check location, carriers can dump goods anywhere

        GoodsType type = spec.getGoodsType(typeId);
        if (type == null) {
            return serverPlayer.clientError("Not a goods type: " + typeId);
        }

        if (amount <= 0) {
            return serverPlayer.clientError("Invalid amount: " + amount);
        }
        int present = carrier.getGoodsCount(type);
        if (present < amount) {
            return serverPlayer.clientError("Attempt to unload " + amount
                + " " + type.getId() + " but only " + present + " present");
        }

        // Try to unload.
        return igc(freeColServer)
            .unloadGoods(serverPlayer, type, amount, carrier);
    }
}
