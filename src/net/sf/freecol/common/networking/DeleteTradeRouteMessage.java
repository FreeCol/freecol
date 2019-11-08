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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when deleting a trade route.
 */
public class DeleteTradeRouteMessage extends AttributeMessage {

    public static final String TAG = "deleteTradeRoute";
    private static final String TRADE_ROUTE_TAG = "tradeRoute";


    /**
     * Create a new {@code DeleteTradeRouteMessage} with the
     * supplied unit and route.
     *
     * @param tradeRoute The {@code TradeRoute} to delete.
     */
    public DeleteTradeRouteMessage(TradeRoute tradeRoute) {
        super(TAG, TRADE_ROUTE_TAG, tradeRoute.getId());
    }

    /**
     * Create a new {@code DeleteTradeRouteMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public DeleteTradeRouteMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, TRADE_ROUTE_TAG);
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
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String tradeRouteId = getStringAttribute(TRADE_ROUTE_TAG);
        
        TradeRoute tradeRoute;
        try {
            tradeRoute = serverPlayer.getOurFreeColGameObject(tradeRouteId, 
                TradeRoute.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        // Proceed to delete.
        return igc(freeColServer)
            .deleteTradeRoute(serverPlayer, tradeRoute);
    }
}
