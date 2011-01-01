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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;


/**
 * Convenience wrapper class for the map that underlies
 * transaction sessions.
 */
public class TransactionSession {

    private static final Logger logger = Logger.getLogger(TransactionSession.class.getName());

    private static final Map<String, Map<String, TransactionSession>> allSessions
        = new HashMap<String, Map<String, TransactionSession>>();

    private Map<String, Object> session;


    private TransactionSession() {
        session = new HashMap<String, Object>();
    }

    /**
     * Create and remember a new TransactionSession unique to the
     * specified ids.
     *
     * @param o1 First <code>FreeColGameObject</code>.
     * @param o2 Second <code>FreeColGameObject</code>.
     * @return The transaction session.
     */
    private static TransactionSession create(FreeColGameObject o1,
                                             FreeColGameObject o2) {
        Map<String, TransactionSession> base = allSessions.get(o1.getId());
        if (base == null) {
            base = new HashMap<String, TransactionSession>();
            allSessions.put(o1.getId(), base);
        } else {
            if (base.containsKey(o2.getId())) base.remove(o2.getId());
        }
        TransactionSession session = new TransactionSession();
        base.put(o2.getId(), session);
        return session;
    }


    // Public interface
    /**
     * Clear all transactions.  Useful at the start of turn.
     */
    public static void clearAll() {
        for (Map<String, TransactionSession> s : allSessions.values()) {
            s.clear();
        }
        allSessions.clear();
    }

    /**
     * Establish a "diplomacy" session.
     *
     * @param unit The <code>Unit</code> that is visiting a settlement.
     * @param settlement The <code>Settlement</code> to be visited.
     */
    public static TransactionSession establishDiplomacySession(Unit unit, Settlement settlement) {
        TransactionSession ts = TransactionSession.create(unit, settlement);
        return ts;
    }

    /**
     * Establish a "loot" session.
     *
     * @param winner The <code>Unit</code> that is looting.
     * @param loser The <code>Unit</code> to be looted.
     * @param capture The <code>Goods</code> to choose from.
     */
    public static TransactionSession establishLootSession(Unit winner,
                                                          Unit loser,
                                                          List<Goods> capture) {
        TransactionSession ts = TransactionSession.create(winner, loser);
        ts.put("lootCargo", capture);
        return ts;
    }

    /**
     * Establish a "trade" session.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> to trade with.
     */
    public static TransactionSession establishTradeSession(Unit unit, Settlement settlement) {
        TransactionSession ts = TransactionSession.create(unit, settlement);
        ts.put("actionTaken", false);
        ts.put("unitMoves", unit.getMovesLeft());
        boolean atWar = settlement.getOwner().atWarWith(unit.getOwner());
        ts.put("canBuy", !atWar);
        ts.put("canSell", !atWar && unit.getSpaceTaken() > 0);
        ts.put("canGift", true);
        return ts;
    }

    /**
     * Looks up the TransactionSession unique to the specified ids in
     * allSessions.
     *
     * @param id1 First id.
     * @param id2 Second id.
     * @return The transaction session, or null if not found.
     */
    public static TransactionSession lookup(String id1, String id2) {
        Map<String, TransactionSession> base = allSessions.get(id1);
        return (base == null) ? null : base.get(id2);
    }

    /**
     * Looks up the TransactionSession unique to the specified game objects.
     *
     * @param o1 First <code>FreeColGameObject</code>.
     * @param o2 Second <code>FreeColGameObject</code>.
     * @return The transaction session, or null if not found.
     */
    public static TransactionSession lookup(FreeColGameObject o1,
                                            FreeColGameObject o2) {
        return TransactionSession.lookup(o1.getId(), o2.getId());
    }

    /**
     * Forget a TransactionSession.
     *
     * @param id1 The first id.
     * @param id2 The second id.
     */
    public static void forget(String id1, String id2) {
        Map<String, TransactionSession> base = allSessions.get(id1);
        if (base != null) {
            base.remove(id2);
            if (base.isEmpty()) allSessions.remove(id1);
        }
    }

    /**
     * Forget a TransactionSession.
     *
     * @param o1 First <code>FreeColGameObject</code>.
     * @param o2 Second <code>FreeColGameObject</code>.
     */
    public static void forget(FreeColGameObject o1, FreeColGameObject o2) {
        TransactionSession.forget(o1.getId(), o2.getId());
    }

    /**
     * Gets a property from a TransactionSession.
     *
     * @param id The property id to query.
     * @return The requested property.
     */
    public Object get(String id) {
        return session.get(id);
    }

    /**
     * Puts a property into a TransactionSession.
     *
     * @param id The property id to set.
     * @param val The property to set.
     */
    public void put(String id, Object val) {
        session.put(id, val);
    }

}
