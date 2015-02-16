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

package net.sf.freecol.common.networking;


/**
 * Class for storing a network response.  If the response has not been
 * set when {@link #getResponse} has been called, this method will
 * block until {@link #setResponse} is called.
 */
public class NetworkReplyObject {

    private Object response = null;
    private boolean responseGiven = false;
    private final int networkReplyId;


    /**
     * The constructor.
     *
     * @param networkReplyId The unique identifier for the network message
     *                       this object will store.
     */
    public NetworkReplyObject(int networkReplyId) {
        this.networkReplyId = networkReplyId;
    }

    /**
     * Sets the response and continues <code>getResponse()</code>.
     *
     * @param response The response.
     * @see #getResponse
     */
    public synchronized void setResponse(Object response) {
        if (response == null) {
            throw new NullPointerException();
        }
        this.response = response;
        this.responseGiven = true;
        notify();
    }

    /**
     * Gets the response. If the response has not been set, this method
     * will block until {@link #setResponse} has been called.
     *
     * @return the response.
     */
    public synchronized Object getResponse() {
        if (response == null) {
            try {
                while (!responseGiven) {
                    wait();
                }
            } catch (InterruptedException ie) {}
        }

        return response;
    }

    /**
     * Gets the unique identifier for the network message this
     * object will store.
     *
     * @return the unique identifier.
     */
    public int getNetworkReplyId() {
        return networkReplyId;
    }

    /**
     * Interrupts any thread waiting for a response.
     */
    public synchronized void interrupt() {
        responseGiven = true;
        notify();
    }
}
