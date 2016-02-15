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
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to get a new trade route.
 */
public class NewTradeRouteMessage extends DOMMessage {

    public static final String TAG = "newTradeRoute";

    /** The new trade route. */
    private TradeRoute tradeRoute;


    /**
     * Create a new <code>NewTradeRouteMessage</code> with the given
     * message identifier and message.
     */
    public NewTradeRouteMessage() {
        super(getTagName());
    }

    /**
     * Create a new <code>NewTradeRouteMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NewTradeRouteMessage(Game game, Element element) {
        super(getTagName());

        this.tradeRoute = getChild(game, element, 0, true, TradeRoute.class);
    }


    // Public interface

    public TradeRoute getTradeRoute() {
        return this.tradeRoute;
    }


    /**
     * Handle a "newTradeRoute"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update setting the trade route.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        this.tradeRoute = server.getInGameController()
            .newTradeRoute(serverPlayer);
        return this.toXMLElement();
    }

    /**
     * Convert this NewTradeRouteMessage to XML.
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
     * @return "newTradeRoute".
     */
    public static String getTagName() {
        return TAG;
    }
}
