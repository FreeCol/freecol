/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.control.ChangeSet;


/**
 * A type of session to handle trading.
 */
public class MercenariesSession extends TransactionSession {

    private static final Logger logger = Logger.getLogger(MercenariesSession.class.getName());

    /** The mercenaries on offer. */
    private List<AbstractUnit> mercenaries;

    /** The price the mercenaries are on offer for. */
    private int price;


    public MercenariesSession(Monarch monarch, Player player) {
        super(makeSessionKey(MercenariesSession.class, monarch, player));
        this.mercenaries = null;
        this.price = -1;
    }

    public void complete(ChangeSet cs) {
        super.complete(cs);
    }

    public List<AbstractUnit> getMercenaries() {
        return mercenaries;
    }

    public void setMercenaries(List<AbstractUnit> mercenaries) {
        this.mercenaries = mercenaries;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
