/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.control.ChangeSet;


/**
 * A type of session to handle looting of cargo.
 */
public class LootSession extends TransactionSession {

    private static final Logger logger = Logger.getLogger(LootSession.class.getName());

    /** The goods that are available to be captured. */
    private final List<Goods> capture;


    public LootSession(Unit winner, Unit loser, List<Goods> capture) {
        super(makeSessionKey(LootSession.class, winner, loser));
        this.capture = capture;
    }


    @Override
    public void complete(ChangeSet cs) {
        super.complete(cs);
    }

    public List<Goods> getCapture() {
        return capture;
    }
}
