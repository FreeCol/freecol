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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


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
     * Create a new {@code AssignTradeRouteMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AssignTradeRouteMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              TRADE_ROUTE_TAG, getStringAttribute(element, TRADE_ROUTE_TAG));
    }


    /**
     * Handle a "assignTradeRoute"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param serverPlayer The {@code ServerPlayer} that sent the message.
     * @return An {@code Element} to update the originating
     *     player with the result of the demand.
     */
    public Element handle(FreeColServer server, ServerPlayer serverPlayer) {
        final String unitId = getAttribute(UNIT_TAG);
        final String tradeRouteId = getAttribute(TRADE_ROUTE_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        TradeRoute tradeRoute;
        if (tradeRouteId == null) {
            tradeRoute = null;
        } else {
            try {
                tradeRoute = serverPlayer.getOurFreeColGameObject(tradeRouteId, 
                    TradeRoute.class);
            } catch (Exception e) {
                return serverPlayer.clientError(e.getMessage())
                    .build(serverPlayer);
            }
        }

        // Proceed to assign.
        return server.getInGameController()
            .assignTradeRoute(serverPlayer, unit, tradeRoute)
            .build(serverPlayer);
    }
}
