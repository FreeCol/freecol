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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to get a new trade route.
 */
public class NewTradeRouteMessage extends ObjectMessage {

    public static final String TAG = "newTradeRoute";


    /**
     * Create a new {@code NewTradeRouteMessage} with the given
     * message identifier and message.
     *
     * @param tradeRoute An optional new {@code TradeRoute}.
     */
    public NewTradeRouteMessage(TradeRoute tradeRoute) {
        super(TAG);

        appendChild(tradeRoute);
    }

    /**
     * Create a new {@code NewTradeRouteMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public NewTradeRouteMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        this(null);

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        TradeRoute tradeRoute = null;
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (TradeRoute.TAG.equals(tag)) {
                    if (tradeRoute == null) {
                        tradeRoute = xr.readFreeColObject(game, TradeRoute.class);
                    } else {
                        expected(TAG, tag);
                    }
                } else {
                    expected(TradeRoute.TAG, tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChild(tradeRoute);
    }


    /**
     * Get the new trade route.
     *
     * @return The {@code TradeRoute} attached to this message.
     */
    private TradeRoute getTradeRoute() {
        return getChild(0, TradeRoute.class);
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
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final TradeRoute tr = getTradeRoute();
        if (tr == null) return;
        tr.intern();

        igc(freeColClient).newTradeRouteHandler(tr);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return igc(freeColServer)
            .newTradeRoute(serverPlayer);
    }
}
