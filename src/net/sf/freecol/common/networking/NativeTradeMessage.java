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

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to handle native trade sessions.
 */
public class NativeTradeMessage extends ObjectMessage {

    public static final String TAG = "nativeTrade";
    private static final String ACTION_TAG = "action";


    /**
     * Create a new {@code NativeTradeMessage} request with the
     * supplied unit and settlement.
     *
     * @param unit The {@code Unit} performing the trade.
     * @param is The {@code IndianSettlement} where the
     *     trade occurs.
     */
    public NativeTradeMessage(Unit unit, IndianSettlement is) {
        this(NativeTradeAction.OPEN, new NativeTrade(unit, is));
    }

    /**
     * Create a new {@code NativetradeMessage} with the
     * supplied unit and native settlement.
     *
     * @param action The {@code NativeTradeAction}
     * @param nt The {@code NativeTrade}
     */
    public NativeTradeMessage(NativeTradeAction action, NativeTrade nt) {
        super(TAG, ACTION_TAG, action.toString());

        appendChild(nt);
    }

    /**
     * Create a new {@code NativeTradeMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public NativeTradeMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, ACTION_TAG);

        NativeTrade nt = null;
        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (NativeTrade.TAG.equals(tag)) {
                    if (nt == null) {
                        nt = xr.readFreeColObject(game, NativeTrade.class);
                    } else {
                        expected(TAG, tag);
                    }
                } else {
                    expected(NativeTrade.TAG, tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChild(nt);
    }


    private NativeTradeAction getAction() {
        return getEnumAttribute(ACTION_TAG, NativeTradeAction.class,
                                (NativeTradeAction)null);
    }

    private NativeTrade getNativeTrade() {
        return getChild(0, NativeTrade.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.LATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final NativeTrade nt = getNativeTrade();
        final NativeTradeAction action = getAction();

        aiPlayer.nativeTradeHandler(action, nt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final NativeTradeAction action = getAction();
        final NativeTrade nt = getNativeTrade();

        igc(freeColClient).nativeTradeHandler(action, nt);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final NativeTrade nt = getNativeTrade();
        if (nt == null) {
            return serverPlayer.clientError("Null native trade");
        }
        
        final Unit unit = nt.getUnit();
        if (unit == null) {
            return serverPlayer.clientError("Null unit");
        }
        if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit not on the map: "
                + unit.getId());
        }

        final IndianSettlement is = nt.getIndianSettlement();
        if (is == null) {
            return serverPlayer.clientError("Null settlement");
        }

        if (!unit.getTile().isAdjacent(is.getTile())) {
            return serverPlayer.clientError("Unit not adjacent to settlement");
        }

        return igc(freeColServer)
            .nativeTrade(serverPlayer, getAction(), nt);
    }
}
