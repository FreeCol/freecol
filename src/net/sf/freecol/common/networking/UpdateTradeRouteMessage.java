/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
     * Create a new {@code UpdateTradeRouteMessage} with the
     * supplied trade route.
     *
     * @param tradeRoute The {@code TradeRoute} to update.
     */
    public UpdateTradeRouteMessage(TradeRoute tradeRoute) {
        super(TAG);

        this.tradeRoute = tradeRoute;
    }

    /**
     * Create a new {@code UpdateTradeRouteMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public UpdateTradeRouteMessage(Game game, Element element) {
        super(TAG);

        this.tradeRoute = getChild(game, element, 0, false, TradeRoute.class);
    }


    /**
     * {@inheritDoc}
     */
    public static MessagePriority getMessagePriority() {
        return MessagePriority.NORMAL;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return freeColServer.getInGameController()
            .updateTradeRoute(serverPlayer, this.tradeRoute);
    }

    /**
     * Convert this UpdateTradeRouteMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG)
            .add(this.tradeRoute).toXMLElement();
    }
}
