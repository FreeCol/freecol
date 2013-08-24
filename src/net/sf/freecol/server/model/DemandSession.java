/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * A type of session to handle demanding tribute from a colony.
 */
public class DemandSession extends TransactionSession {

    private static final Logger logger = Logger.getLogger(DemandSession.class.getName());

    /** The goods demanded. */
    private Goods goods;

    /** The gold demanded. */
    private int gold;

    /** The tension change if the demand fails. */
    private int tension;

    /** The native player. */
    private Player demander;

    /** The colony player. */
    private Player victim;


    public DemandSession(Unit unit, Colony colony) {
        super(makeSessionKey(DemandSession.class, unit, colony));
        goods = null;
        gold = -1;
        tension = -1;
        demander = unit.getOwner();
        victim = colony.getOwner();
    }

    public void complete(ChangeSet cs) {
        if (tension > 0) {
            ((ServerPlayer)demander).csModifyTension(victim, tension, cs);
        }
        super.complete(cs);
    }

    public Goods getGoods() {
        return goods;
    }

    public void setGoods(Goods goods) {
        this.goods = goods;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public int getTension() {
        return tension;
    }

    public void setTension(int tension) {
        this.tension = tension;
    }
}
