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
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when paying tax arrears.
 */
public class PayArrearsMessage extends AttributeMessage {

    public static final String TAG = "payArrears";
    private static final String GOODS_TYPE_TAG = "goodsType";


    /**
     * Create a new {@code PayArrearsMessage} with the
     * supplied goods type.
     *
     * @param type The {@code GoodsType} to pay arrears for.
     */
    public PayArrearsMessage(GoodsType type) {
        super(TAG, GOODS_TYPE_TAG, type.getId());
    }

    /**
     * Create a new {@code PayArrearsMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public PayArrearsMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, GOODS_TYPE_TAG);
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
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final GoodsType goodsType = freeColServer.getSpecification()
            .getGoodsType(getStringAttribute(GOODS_TYPE_TAG));

        // Proceed to pay.
        return igc(freeColServer)
            .payArrears(serverPlayer, goodsType);
    }
}
