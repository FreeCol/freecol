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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.networking.ChangeSet;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Root class for sessions.
 */
public abstract class Session {

    private static final Logger logger = Logger.getLogger(Session.class.getName());

    /** A map of all active sessions. */
    private static final Map<String, Session> allSessions = new HashMap<>();

    /** The key to this session. */
    private String key;

    /** Has this session been completed? */
    private boolean completed = false;


    /**
     * Protected constructor, we only really instantiate specific types
     * of transactions.
     *
     * @param key A unique key to lookup this transaction with.
     */
    protected Session(String key) {
        Session s = getSession(key);
        if (s != null) {
            throw new IllegalArgumentException("Duplicate session: " + key
                                               + " -> " + s);
        }
        this.key = key;
        this.completed = false;
        logger.finest("Created session: " + key);
    }


    /**
     * Get the session key.
     *
     * @return The key for this session.
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Register a new session.
     */
    public void register() {
        synchronized (allSessions) {
            allSessions.put(this.getKey(), this);
        }
    }

    /**
     * Get the session with a given key.
     *
     * @param key The session key.
     * @return The {@code session} found.
     */
    private static Session getSession(String key) {
        synchronized (allSessions) {
            return allSessions.get(key);
        }
    }
    
    /**
     * Is this session complete?
     *
     * @return True if the session is complete.
     */
    private synchronized boolean isComplete() {
        return this.completed;
    }
    
    /**
     * All transaction types must implement a completion action.
     *
     * This is called by the controller at the end of turn to complete
     * any sessions that have not yet completed, or if the controller
     * gets the required response to complete the session.
     *
     * @param cs A {@code ChangeSet} to update with changes that
     *     occur when completing this session.
     * @return True if the session was already complete.
     */
    protected synchronized boolean complete(ChangeSet cs) {
        boolean ret = this.completed;
        this.completed = true;
        return ret;
    }

    /**
     * Make a transaction session key.
     *
     * Note: sort the keys to make session key independent of argument order.
     *
     * @param type An identifier for the type of transaction.
     * @param o1 A string to uniquely identify the transaction.
     * @param o2 Another string to uniquely identify the transaction.
     * @return A transaction session key.
     */
    protected static String makeSessionKey(Class type,
                                           String o1, String o2) {
        return (o1.compareTo(o2) < 0)
            ? type + "-" + o1 + "-" + o2
            : type + "-" + o2 + "-" + o1;
    }

    /**
     * Make a transaction session key given two game objects.
     *
     * @param type An identifier for the type of transaction.
     * @param o1 A {@code FreeColGameObject} involved in the session.
     * @param o2 Another {@code FreeColGameObject} involved in the session.
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
     * @param cs A {@code ChangeSet} to update.
     */
    public static void completeAll(ChangeSet cs) {
        List<Session> sessions;
        synchronized (allSessions) {
            sessions = transform(allSessions.values(), s -> !s.isComplete());
            allSessions.clear();
        }
        for (Session ts : sessions) ts.complete(cs);
    }

    /**
     * Clear all sessions.
     */
    public static void clearAll() {
        synchronized (allSessions) {
            allSessions.clear();
        }
    }

    /**
     * Find a session matching a predicate.
     *
     * @param pred The {@code Predicate} to match.
     * @return The {@code Session} found if any.
     */
    public static Session findSession(Predicate<Session> pred) {
        synchronized (allSessions) {
            return find(allSessions.values(), pred);
        }
    }

    /**
     * Look up a session of specified type given the game objects involved.
     *
     * @param <T> The actual session class found.
     * @param type The class of session.
     * @param o1 The first {@code FreeColGameObject} in the session.
     * @param o2 The second {@code FreeColGameObject} in the session.
     * @return A session of the specified type, or null if not found.
     */
    public static <T extends Session> T lookup(Class<T> type,
        FreeColGameObject o1, FreeColGameObject o2) {
        return lookup(type, o1.getId(), o2.getId());
    }

    /**
     * Look up a session of specified type given the IDs of the game objects
     * involved.  This version is needed for sessions where one of the objects
     * may have already been disposed of while the session is still valid.
     *
     * @param <T> The actual session class found.
     * @param type The class of session.
     * @param s1 The identifier of the first object in the session.
     * @param s2 The identifier of the second object in the session.
     * @return A session of the specified type, or null if not found.
     */
    public static <T extends Session> T lookup(Class<T> type,
                                               String s1, String s2) {
        String key = makeSessionKey(type, s1, s2);
        return lookup(type, key);
    }

    /**
     * Look up a session given its key.
     *
     * @param <T> The actual session class found.
     * @param type The class of session.
     * @param key The session key.
     * @return A session of the specified type, or null if not found.
     */
    public static <T extends Session> T lookup(Class<T> type, String key) {
        Session ts = getSession(key);
        if (ts != null && ts.isComplete()) {
            synchronized (allSessions) {
                allSessions.remove(key);
            }
            ts = null;
        }
        return (ts == null) ? null : type.cast(ts);
    }
}

