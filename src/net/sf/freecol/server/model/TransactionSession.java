/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColGameObject;


/**
 * Convenience wrapper class for the map that underlies
 * transaction sessions.
 * TODO: This is leaky, fix.
 * TODO: Relax server restriction on AI transactions?
 */
public class TransactionSession {

    private static final Logger logger = Logger.getLogger(TransactionSession.class.getName());

    private static Map<String, Map<String, TransactionSession>> allSessions
        = new HashMap<String, Map<String, TransactionSession>>();

    private Map<String, Object> session;


    private TransactionSession() {
        session = new HashMap<String, Object>();
    }

    public Object get(String id) {
        return session.get(id);
    }

    public void put(String id, Object val) {
        session.put(id, val);
    }

    /**
     * Looks up the TransactionSession unique to the specified fcgos in
     * allSessions.
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
     * Create and remember a new TransactionSession unique to the
     * specified ids.
     *
     * @param o1 First <code>FreeColGameObject</code>.
     * @param o2 Second <code>FreeColGameObject</code>.
     * @return The transaction session.
     */
    public static TransactionSession create(FreeColGameObject o1,
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

    /**
     * Find a TransactionSession unique to the specified ids, or
     * create and remember a new one.
     *
     * @param o1 First <code>FreeColGameObject</code>.
     * @param o2 Second <code>FreeColGameObject</code>.
     * @return The transaction session.
     */
    public static TransactionSession find(FreeColGameObject o1,
                                          FreeColGameObject o2) {
        Map<String, TransactionSession> base = allSessions.get(o1.getId());
        if (base == null) {
            base = new HashMap<String, TransactionSession>();
            allSessions.put(o1.getId(), base);
        }
        TransactionSession session = base.get(o2.getId());
        if (session == null) {
            session = new TransactionSession();
            base.put(o2.getId(), session);
        }
        return session;
    }

    /**
     * Remember a TransactionSession (save to allSessions).
     *
     * @param o1 First <code>FreeColGameObject</code>.
     * @param o2 Second <code>FreeColGameObject</code>.
     * @param session The <code>TransactionSession</code> to save.
     */
    public static void remember(FreeColGameObject o1,
                                FreeColGameObject o2,
                                TransactionSession session) {
        Map<String, TransactionSession> base = allSessions.get(o1.getId());
        if (base == null) {
            base = new HashMap<String, TransactionSession>();
            allSessions.put(o1.getId(), base);
        }
        base.put(o2.getId(), session);
    }

    /**
     * Forget a TransactionSession (remove from allSessions).
     *
     * @param o1 First <code>FreeColGameObject</code>.
     * @param o2 Second <code>FreeColGameObject</code>.
     */
    public static void forget(FreeColGameObject o1, FreeColGameObject o2) {
        TransactionSession.forget(o1.getId(), o2.getId());
    }

    /**
     * Forget a TransactionSession (remove from allSessions).
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
}
