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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to get a new trade route.
 */
public class NewTradeRouteMessage extends ObjectMessage {

    public static final String TAG = "newTradeRoute";

    /** The new trade route. */
    private TradeRoute tradeRoute;


    /**
     * Create a new {@code NewTradeRouteMessage} with the given
     * message identifier and message.
     */
    public NewTradeRouteMessage() {
        super(TAG);
    }

    /**
     * Create a new {@code NewTradeRouteMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public NewTradeRouteMessage(Game game, Element element) {
        this();

        this.tradeRoute = getChild(game, element, 0, true, TradeRoute.class);
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
        return freeColServer.getInGameController()
            .newTradeRoute(serverPlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        // Suppress toXML for now
        throw new XMLStreamException(getType() + ".toXML NYI");
    }

    /**
     * Convert this NewTradeRouteMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG)
            .add(this.tradeRoute).toXMLElement();
    }


    // Public interface

    /**
     * Get the new trade route.
     *
     * @return The {@code TradeRoute} attached to this message.
     */
    public TradeRoute getTradeRoute() {
        return this.tradeRoute;
    }

    /**
     * Set the new trade route.
     *
     * @param tradeRoute The {@code TradeRoute} to attach.
     * @return This message.
     */
    public NewTradeRouteMessage setTradeRoute(TradeRoute tradeRoute) {
        this.tradeRoute = tradeRoute;
        return this;
    }
}
