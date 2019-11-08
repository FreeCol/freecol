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

package net.sf.freecol.common.networking;


/**
 * Class for storing a network response.  If the response has not been
 * set when {@link #getResponse} has been called, this method will
 * block until {@link #setResponse} is called.
 */
public class NetworkReplyObject {

    private static int ONE_SECOND = 1000; // 1000ms

    /** A unique identifier for the message to wait for. */
    private final int networkReplyId;

    /** The response from the network. */
    private Object response;

    /**
     * A gating flag that shows when the response is valid.  Starts false,
     * becomes true in {@link #setResponse}.
     */
    private volatile boolean responseGiven;


    /**
     * The constructor.
     *
     * @param networkReplyId The unique identifier for the network message
     *                       this object will store.
     */
    public NetworkReplyObject(int networkReplyId) {
        this.networkReplyId = networkReplyId;
        this.response = null;
        this.responseGiven = false;
    }

    /**
     * Gets the unique identifier for the network message this
     * object will store.
     *
     * @return the unique identifier.
     */
    public int getNetworkReplyId() {
        return this.networkReplyId;
    }

    /**
     * Gets the response. If the response has not been set, this method
     * will block until {@link #setResponse} has been called.
     *
     * @return the response.
     */
    public synchronized Object getResponse() {
        while (!this.responseGiven) {
            try {
                wait(ONE_SECOND);
            } catch (InterruptedException ie) {}
        }
        return this.response;
    }

    /**
     * Sets the response and continues {@code getResponse()}.
     *
     * @param response The response.
     * @see #getResponse
     */
    public synchronized void setResponse(Object response) {
        if (!this.responseGiven) {
            this.response = response;
            this.responseGiven = true;
            notifyAll();
        }
    }

    /**
     * Interrupt the wait for response.
     */
    public void interrupt() {
        setResponse(null);
    }
}
