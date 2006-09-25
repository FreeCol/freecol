package net.sf.freecol.common.networking;


/**
* Class for storing a network response. If the response has not
* been set when {@link #getResponse} have been called, this method
* will block until {@link #setResponse} has been called.
*/
public class NetworkReplyObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private Object response = null;
    private int networkReplyId;
    private boolean streamed;
    
    
    
    /**
    * The constructor.
    *
    * @param networkReplyId The unique identifier for the network message
    *                       this object will store.
    * @param streamed Should be set to <code>true</code> if the
    *       incoming data should be handled as stream data.
    */
    public NetworkReplyObject(int networkReplyId, boolean streamed) {
        this.networkReplyId = networkReplyId;
        this.streamed = streamed;
    }
    

    /**
     * Checks if this <code>NetworkReplyObject</code> 
     * expects streamed data.
     * 
     * @return <code>true</code> if the incoming data
     *      should be handled as a stream.
     */
    public boolean isStreamed() {
        return streamed;
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
        notify();
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
    * Gets the response. If the response has not been set, this method
    * will block until {@link #setResponse} has been called.
    *
    * @return the response.
    */
    public synchronized Object getResponse() {
        if (response == null) {
            try {
                wait();
            } catch (InterruptedException ie) {}
        }

       return response;
    }
}
