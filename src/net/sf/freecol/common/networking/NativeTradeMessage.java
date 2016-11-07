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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
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
     */
    public NativeTradeMessage(Unit unit, IndianSettlement is) {
        this(NativeTradeAction.OPEN, new NativeTrade(unit, is));
    }

    /**
     * Create a new {@code NativetradeMessage} with the
     * supplied unit and native settlement.
     *
     * @param action The {@code NativeTradeAction}
     * @param nt The {@code NativeTrade}
     */
    public NativeTradeMessage(NativeTradeAction action, NativeTrade nt) {
        super(TAG);

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
        super(TAG);

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
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final NativeTrade nt = getNativeTrade();
        if (nt == null) {
            return serverPlayer.clientError("Null native trade");
        }
        
        final Unit unit = nt.getUnit();
        if (unit == null) {
            return serverPlayer.clientError("Null unit");
        }
        if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit not on the map: "
                + unit.getId());
        }

        final IndianSettlement is = nt.getIndianSettlement();
        if (is == null) {
            return serverPlayer.clientError("Null settlement");
        }

        if (!unit.getTile().isAdjacent(is.getTile())) {
            return serverPlayer.clientError("Unit not adjacent to settlement");
        }

        return freeColServer.getInGameController()
            .nativeTrade(serverPlayer, getAction(), nt);
    }

    /**
     * Convert this NativeTradeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            ACTION_TAG, getAction().toString())
            .add(this.nt).toXMLElement();
    }
}
