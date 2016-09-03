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
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to handle native trade sessions.
 */
public class NativeTradeMessage extends DOMMessage {

    public static final String TAG = "nativeTrade";
    private static final String ACTION_TAG = "action";

    /** The action to perform. */
    private NativeTradeAction action;

    /** The native trade underway. */
    private NativeTrade nt = null;


    /**
     * Create a new {@code NativeTradeMessage} request with the
     * supplied unit and settlement.
     *
     * @param unit The {@code Unit} performing the trade.
     * @param is The {@code IndianSettlement} where the
     *     trade occurs.
     * @param action The {@code NativeTradeAction} to use.
     */
    public NativeTradeMessage(Unit unit, IndianSettlement is) {
        this(NativeTradeAction.OPEN, new NativeTrade(unit, is));
    }

    /**
     * Create a new {@code NativetradeMessage} with the
     * supplied unit and native settlement.
     *
     * @param unit The {@code Unit} performing the session.
     * @param is The {@code IndianSettlement} where the
     *     session occurs.
     */
    public NativeTradeMessage(NativeTradeAction action, NativeTrade nt) {
        super(getTagName());

        this.action = action;
        this.nt = nt;
    }

    /**
     * Create a new {@code NativeTradeMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public NativeTradeMessage(Game game, Element element) {
        super(getTagName());

        this.action = getEnumAttribute(element, ACTION_TAG,
            NativeTradeAction.class, (NativeTradeAction)null);
        this.nt = getChild(game, element, 0, false, NativeTrade.class);
    }


    // Public interface

    public NativeTradeAction getAction() {
        return this.action;
    }

    public NativeTrade getNativeTrade() {
        return this.nt;
    }

    
    /**
     * Handle a "nativeTrade"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param connection The {@code Connection} message was received on.
     * @return A reply encapsulating the possibilities for this
     *     trade, or an error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        return server.getInGameController()
            .nativeTrade(serverPlayer, getAction(), getNativeTrade())
            .build(serverPlayer);
    }

    /**
     * Convert this NativeTradeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            ACTION_TAG, getAction().toString())
            .add(this.nt).toXMLElement();
    }


    /**
     * The tag name of the root element representing this object.
     *
     * @return "nativeTrade".
     */
    public static String getTagName() {
        return TAG;
    }
}
