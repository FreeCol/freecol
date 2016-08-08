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

    /** The native trade underway. */
    private NativeTrade nt = null;


    /**
     * Create a new <code>NativeTradeMessage</code> request with the
     * supplied unit and settlement.
     *
     * @param unit The <code>Unit</code> performing the trade.
     * @param is The <code>IndianSettlement</code> where the
     *     trade occurs.
     * @param action The <code>NativeTradeAction</code> to use.
     */
    public NativeTradeMessage(Unit unit, IndianSettlement is) {
        this(new NativeTrade(NativeTradeAction.OPEN, unit, is));
    }

    /**
     * Create a new <code>NativetradeMessage</code> with the
     * supplied unit and native settlement.
     *
     * @param unit The <code>Unit</code> performing the session.
     * @param is The <code>IndianSettlement</code> where the
     *     session occurs.
     */
    public NativeTradeMessage(NativeTrade nt) {
        super(getTagName());

        this.nt = nt;
    }

    /**
     * Create a new <code>NativeTradeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NativeTradeMessage(Game game, Element element) {
        super(getTagName());

        this.nt = getChild(game, element, 0, false, NativeTrade.class);
    }


    // Public interface

    public NativeTradeAction getAction() {
        return this.nt.getAction();
    }

    public NativeTrade getNativeTrade() {
        return this.nt;
    }

    
    /**
     * Handle a "nativeTrade"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     * @return A reply encapsulating the possibilities for this
     *     trade, or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        return server.getInGameController()
            .nativeTrade(serverPlayer, getNativeTrade())
            .build(serverPlayer);
    }

    /**
     * Convert this NativeTradeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName())
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
