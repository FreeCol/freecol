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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.server.control.ChangeSet;


/**
 * Root class for sessions.
 */
public abstract class TransactionSession {

    private static final Logger logger = Logger.getLogger(TransactionSession.class.getName());

    /**
     * A map of all active sessions.
     */
    protected static final Map<String, TransactionSession> allSessions
        = new HashMap<>();

    /** Has this session been completed? */
    private boolean completed;


    /**
     * Protected constructor, we only really instantiate specific types
     * of transactions.
     *
     * @param key A unique key to lookup this transaction with.
     */
    protected TransactionSession(String key) {
        if (allSessions.get(key) != null) {
            throw new IllegalArgumentException("Duplicate session: " + key);
        }
        completed = false;
        allSessions.put(key, this);
        logger.finest("Created session: " + key);
    }

    /**
     * All transaction types must implement a completion action.  The
     * last thing they should do is call this to remove reference to
     * this transaction.
     *
     * @param cs A <code>ChangeSet</code> to update with changes that
     *     occur when completing this session.
     */
    public void complete(ChangeSet cs) {
        completed = true;
    }
    
    /**
     * Make a transaction session key.
     *
     * @param type An identifier for the type of transaction.
     * @param o1 A string to uniquely identify the transaction.
     * @param o2 Another string to uniquely identify the transaction.
     * @return A transaction session key.
     */
    protected static String makeSessionKey(Class type,
                                           String o1, String o2) {
        return type + "-" + o1 + "-" + o2;
    }

    /**
     * Make a transaction session key given two game objects.
     *
     * @param type An identifier for the type of transaction.
     * @param o1 A <code>FreeColGameObject</code> involved in the session.
     * @param o2 Another <code>FreeColGameObject</code> involved in the session.
     * @return A transaction session key.
     */
    protected static String makeSessionKey(Class type,
                                           FreeColGameObject o1,
                                           FreeColGameObject o2) {
        return makeSessionKey(type, o1.getId(), o2.getId());
    }


    // Public interface

    /**
     * Complete all transactions.  Useful at the end of turn.
     *
     * @param cs A <code>ChangeSet</code> to update.
     */
    public static void completeAll(ChangeSet cs) {
        for (TransactionSession ts : allSessions.values()) {
            if (!ts.completed) ts.complete(cs);
        }
        clearAll();
    }

    /**
     * Clear all transactions.
     */
    public static void clearAll() {
        allSessions.clear();
    }

    /**
     * Look up a session of specified type given the game objects involved.
     *
     * @param type The class of session.
     * @param o1 The first <code>FreeColGameObject</code> in the session.
     * @param o2 The second <code>FreeColGameObject</code> in the session.
     * @return A session of the specified type, or null if not found.
     */
    public static <T extends TransactionSession> T lookup(Class<T> type,
        FreeColGameObject o1, FreeColGameObject o2) {
        return lookup(type, o1.getId(), o2.getId());
    }

    /**
     * Look up a session of specified type given the IDs of the game objects
     * involved.  This version is needed for sessions where one of the objects
     * may have already been disposed of while the session is still valid.
     *
     * @param type The class of session.
     * @param s1 The identifier of the first object in the session.
     * @param s2 The identifier of the second object in the session.
     * @return A session of the specified type, or null if not found.
     */
    public static <T extends TransactionSession> T lookup(Class<T> type,
        String s1, String s2) {
    	String key = makeSessionKey(type, s1, s2);
        TransactionSession ts = allSessions.get(key);
        if (ts != null && ts.completed) {
            allSessions.remove(key);
            ts = null;
        }
        return (ts == null) ? null : type.cast(ts);
    }
}

