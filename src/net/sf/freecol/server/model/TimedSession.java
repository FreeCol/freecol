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

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.sf.freecol.common.networking.ChangeSet;


/**
 * Root class for timed sessions.
 */
public abstract class TimedSession extends Session {

    private static final Logger logger = Logger.getLogger(TimedSession.class.getName());

    /** The timer that controls the session duration. */
    private Timer timer;


    /**
     * Protected constructor, we only really instantiate specific types
     * of transactions.
     *
     * @param key A unique key to lookup this transaction with.
     * @param timeout The timeout for the session.
     */
    protected TimedSession(String key, long timeout) {
        super(key);

        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    complete(false);
                }
            }, timeout * 1000 /* ms, scale to seconds */);
    }


    /**
     * Complete this task with the given result.
     *
     * By default this will be called (with value == false) when the
     * timer expires.
     *
     * @param result The result to complete the session with.
     * @return The result of the session.
     */
    protected abstract boolean complete(boolean result);

    /**
     * Cancel the timer task.
     */
    protected synchronized void cancel() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean complete(ChangeSet cs) {
        boolean ret = super.complete(cs);
        cancel();
        return ret;
    }
}
