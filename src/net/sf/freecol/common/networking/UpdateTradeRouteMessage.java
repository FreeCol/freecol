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
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when updating a trade route.
 */
public class UpdateTradeRouteMessage extends DOMMessage {

    public static final String TAG = "updateTradeRoute";

    /** The trade route to update. */
    private final TradeRoute tradeRoute;


    /**
     * Create a new <code>UpdateTradeRouteMessage</code> with the
     * supplied trade route.
     *
     * @param tradeRoute The <code>TradeRoute</code> to update.
     */
    public UpdateTradeRouteMessage(TradeRoute tradeRoute) {
        super(getTagName());

        this.tradeRoute = tradeRoute;
    }

    /**
     * Create a new <code>UpdateTradeRouteMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public UpdateTradeRouteMessage(Game game, Element element) {
        super(getTagName());

        this.tradeRoute = getChild(game, element, 0, false, TradeRoute.class);
    }


    /**
     * Handle a "updateTradeRoute"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     * @return Null, or an error message on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        return server.getInGameController()
            .updateTradeRoute(serverPlayer, this.tradeRoute)
            .build(serverPlayer);
    }

    /**
     * Convert this UpdateTradeRouteMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName())
            .add(this.tradeRoute).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "updateTradeRoute".
     */
    public static String getTagName() {
        return TAG;
    }
}
