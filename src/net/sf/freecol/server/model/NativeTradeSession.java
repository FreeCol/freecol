/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.server.model;

import java.util.logging.Logger;

import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;


/**
 * A type of session to handle trading with a native settlement.
 */
public class NativeTradeSession extends Session {

    private static final Logger logger = Logger.getLogger(NativeTradeSession.class.getName());

    /** The moves the trading unit has left at start of session. */
    private final int movesLeft;

    /** The native trade information. */
    private NativeTrade nt;


    /**
     * Creates a new {@code NativeTradeSession}.
     *
     * @param nt The {@code NativeTrade}
     */
    private NativeTradeSession(NativeTrade nt) {
        super(makeSessionKey(NativeTradeSession.class, nt.getUnit(),
                             nt.getIndianSettlement()));

        this.movesLeft = nt.getUnit().getMovesLeft();
        this.nt = nt;
    }

    /**
     * Get the native trade underway.
     *
     * @return The {@code NativeTrade}.
     */
    public NativeTrade getNativeTrade() {
        return this.nt;
    }


    /**
     * Helper function to update a tentative native trade and open
     * a session for it.
     *
     * @param nt The proposed {@code NativeTrade}.
     * @return The updated {@code NativeTrade}.
     */
    public static NativeTrade openSession(NativeTrade nt) {
        nt = new NativeTrade(nt.getUnit(), nt.getIndianSettlement());
        new NativeTradeSession(nt).register();
        nt.initialize();
        return nt;
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean complete(ChangeSet cs) {
        boolean ret = super.complete(cs);

        if (this.nt.hasNotTraded()) { // Reset the moves if nothing happened
            Unit unit = this.nt.getUnit();
            unit.setMovesLeft(this.movesLeft);
            cs.addPartial(See.only(unit.getOwner()), unit,
                "movesLeft", String.valueOf(unit.getMovesLeft()));
        }
        this.nt.setDone();
        return ret;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(getClass())
            .append(" moves=").append(this.movesLeft)
            .append(' ').append(this.nt).append(']');
        return sb.toString();
    }
}
