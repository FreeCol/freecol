/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when assigning a trade route to a unit.
 */
public class AssignTradeRouteMessage extends DOMMessage {

    /**
     * The id of the unit.
     */
    private String unitId;

    /**
     * The id of the trade route.
     */
    private String tradeRouteId;

    /**
     * Create a new <code>AssignTradeRouteMessage</code> with the
     * supplied unit and route.
     *
     * @param unit The <code>Unit</code> to assign a trade route to.
     * @param tradeRoute The <code>TradeRoute</code> to assign.
     */
    public AssignTradeRouteMessage(Unit unit, TradeRoute tradeRoute) {
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
        this.unitId = element.getAttribute("unit");
        this.tradeRouteId = (element.hasAttribute("tradeRoute"))
            ? element.getAttribute("tradeRoute")
            : null;
    }

    /**
     * Handle a "assignTradeRoute"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the assignTradeRouted unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();

        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        TradeRoute tradeRoute;
        if (tradeRouteId == null) {
            tradeRoute = null;
        } else if (game.getFreeColGameObjectSafely(tradeRouteId)
            instanceof TradeRoute) {
            tradeRoute
                = (TradeRoute) game.getFreeColGameObjectSafely(tradeRouteId);
        } else {
            return DOMMessage.clientError("Not a trade route: "
                + tradeRouteId);
        }

        // Proceed to assign.
        return server.getInGameController()
            .assignTradeRoute(serverPlayer, unit, tradeRoute);
    }

    /**
     * Convert this AssignTradeRouteMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        if (tradeRouteId != null) {
            result.setAttribute("tradeRoute", tradeRouteId);
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "assignTradeRoute".
     */
    public static String getXMLElementTagName() {
        return "assignTradeRoute";
    }
}
