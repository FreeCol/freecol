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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when assigning a trade route to a unit.
 */
public class AssignTradeRouteMessage extends AttributeMessage {

    public static final String TAG = "assignTradeRoute";
    private static final String TRADE_ROUTE_TAG = "tradeRoute";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code AssignTradeRouteMessage} with the
     * supplied unit and route.
     *
     * @param unit The {@code Unit} to assign a trade route to.
     * @param tradeRoute The {@code TradeRoute} to assign.
     */
    public AssignTradeRouteMessage(Unit unit, TradeRoute tradeRoute) {
        super(TAG, UNIT_TAG, unit.getId(),
              TRADE_ROUTE_TAG, ((tradeRoute == null) ? null : tradeRoute.getId()));
    }
    /**
     * Create a new {@code AssignTradeRouteMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public AssignTradeRouteMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, TRADE_ROUTE_TAG);
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
        final String unitId = getStringAttribute(UNIT_TAG);
        final String tradeRouteId = getStringAttribute(TRADE_ROUTE_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        TradeRoute tradeRoute;
        if (tradeRouteId == null) {
            tradeRoute = null;
        } else {
            try {
                tradeRoute = serverPlayer.getOurFreeColGameObject(tradeRouteId, 
                    TradeRoute.class);
            } catch (Exception e) {
                return serverPlayer.clientError(e.getMessage());
            }
        }

        // Proceed to assign.
        return igc(freeColServer)
            .assignTradeRoute(serverPlayer, unit, tradeRoute);
    }
}
