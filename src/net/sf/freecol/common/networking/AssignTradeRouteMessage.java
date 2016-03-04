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
public class AssignTradeRouteMessage extends DOMMessage {

    public static final String TAG = "assignTradeRoute";
    private static final String TRADE_ROUTE_TAG = "tradeRoute";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the unit. */
    private final String unitId;

    /** The identifier of the trade route. */
    private final String tradeRouteId;


    /**
     * Create a new <code>AssignTradeRouteMessage</code> with the
     * supplied unit and route.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     */
    public AssignTradeRouteMessage(Unit unit, TradeRoute tradeRoute) {
        super(getTagName());

        this.unitId = unit.getId();
        this.tradeRouteId = (tradeRoute == null) ? null : tradeRoute.getId();
    }

    /**
     * Create a new <code>AssignTradeRouteMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public AssignTradeRouteMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.tradeRouteId = getStringAttribute(element, TRADE_ROUTE_TAG);
    }


    /**
     * Handle a "assignTradeRoute"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> that sent the message.
     * @param connection The <code>Connection</code> message was received on.
     * @return An <code>Element</code> to update the originating
     *     player with the result of the demand.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

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

    /**
     * Convert this AssignTradeRouteMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            "unit", unitId,
            "tradeRoute", tradeRouteId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "assignTradeRoute".
     */
    public static String getTagName() {
        return TAG;
    }
}
