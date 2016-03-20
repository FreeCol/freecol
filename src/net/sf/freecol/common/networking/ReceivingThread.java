/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;

import org.xml.sax.SAXException;


/**
 * The thread that checks for incoming messages.
 */
final class ReceivingThread extends Thread {

    private static final Logger logger = Logger.getLogger(ReceivingThread.class.getName());

    /**
     * Input stream for buffering the data from the network.
     * 
     * This is just an input stream that signals end-of-stream when a
     * given token {@link Connection#END_OF_STREAM} is encountered.
     * In order to continue receiving data, the method {@link #enable}
     * has to be called.  Calls to <code>close()</code> have no
     * effect, the underlying input stream has to be closed directly.
     */
    private static class FreeColNetworkInputStream extends InputStream {

        public static final int BUFFER_SIZE = 16384;

        private static final int EOS_RESULT = -1;

        private final InputStream in;

        private final byte[] buffer = new byte[BUFFER_SIZE];

        private final byte[] bb = new byte[1];
        
        private int bStart = 0;

        private int bEnd = 0;

        private int bSize = 0;

        private boolean wait = false;


        /**
         * Creates a new <code>FreeColNetworkInputStream</code>.
         * 
         * @param in The input stream in which this object should get the data
         *            from.
         */
        public FreeColNetworkInputStream(InputStream in) {
            this.in = in;
        }

        /**
         * Prepares the input stream for a new message.
         *
         * Makes the subsequent calls to <code>read</code> return the data
         * instead of <code>EOS_RESULT</code>.
         */
        public void enable() {
            this.wait = false;
        }

        /**
         * Fills the buffer with data.
         * 
         * @return True if a non-zero amount of data was read into the buffer.
         * @exception IOException is thrown by the underlying read.
         * @exception IllegalStateException if the buffer is not empty.
         */
        private boolean fill() throws IOException {
            if (this.bSize != 0) throw new IllegalStateException("Not empty.");

            int r = this.in.read(buffer, 0, BUFFER_SIZE);
            if (r <= 0) return false;

            this.bStart = 0;
            this.bEnd = this.bSize = r;
            return true;
        }

        /**
         * Reads a single byte.
         * 
         * @return The byte read, or EOS_RESULT on error or "end" of stream.
         * @see #read(byte[], int, int)
         * @exception IOException is thrown by the underlying read.
         */
        @Override
        public int read() throws IOException {
            return (read(bb, 0, 1) == 1) ? bb[0] : EOS_RESULT;
        }

        /**
         * Reads from the buffer and returns the data.
         * 
         * @param b The buffer to put the data in.
         * @param off The offset to use when writing the data.
         * @param len The maximum number of bytes to read.
         * @return The actual number of bytes read, or EOS_RESULT if
         *     the message has ended
         *     ({@link Connection#END_OF_STREAM} was encountered).
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (this.wait) return EOS_RESULT;

            int n = 0;
            for (; n < len; n++) {
                if (this.bSize == 0 && !fill()) {
                    this.wait = true;
                    break;
                }

                byte value = buffer[this.bStart];
                this.bStart++;
                this.bSize--;
                if (value == Connection.END_OF_STREAM) {
                    this.wait = true;
                    break;
                }
                b[n + off] = value;
            }

            return (n > 0 || !this.wait) ? n : EOS_RESULT;
        }
    }

    /** Maximum number of retries before closing the connection. */
    private static final int MAXIMUM_RETRIES = 5;

    /** A map of network ids to the corresponding waiting thread. */
    private final Map<Integer, NetworkReplyObject> waitingThreads
        = Collections.synchronizedMap(new HashMap<Integer, NetworkReplyObject>());

    /** The wrapped version of the input stream. */
    private final FreeColNetworkInputStream in;

    /** The connection to receive on. */
    private final Connection connection;

    /** Whether the thread should run. */
    private boolean shouldRun;

    /** A counter for reply ids. */
    private int nextNetworkReplyId;


    /**
     * The constructor to use.
     * 
     * @param connection The <code>Connection</code> this
     *            <code>ReceivingThread</code> belongs to.
     * @param in The stream to read from.
     * @param threadName The base name for the thread.
     */
    public ReceivingThread(Connection connection, InputStream in,
                           String threadName) {
        super(threadName + "-ReceivingThread-" + connection);

        this.in = new FreeColNetworkInputStream(in);
        this.connection = connection;
        this.shouldRun = true;
        this.nextNetworkReplyId = 1;
    }

    /**
     * Gets the next network reply identifier that will be used when
     * identifing a network message.
     * 
     * @return The next available network reply identifier.
     */
    public synchronized int getNextNetworkReplyId() {
        return nextNetworkReplyId++;
    }

    /**
     * Creates and registers a new <code>NetworkReplyObject</code> with the
     * specified object identifier.
     * 
     * @param networkReplyId The identifier of the message the calling
     *     thread should wait for.
     * @return The <code>NetworkReplyObject</code> containing the network
     *     message.
     */
    public NetworkReplyObject waitForNetworkReply(int networkReplyId) {
        NetworkReplyObject nro = new NetworkReplyObject(networkReplyId);
        waitingThreads.put(networkReplyId, nro);
        return nro;
    }

    /**
     * Checks if this thread should run.
     *
     * @return True if the thread should run.
     */
    private synchronized boolean shouldRun() {
        return this.shouldRun;
    }

    /**
     * Tells this thread that it does not need to do any more work.
     */
    public synchronized void askToStop() {
        if (this.shouldRun) {
            this.shouldRun = false;
            for (NetworkReplyObject o : this.waitingThreads.values()) {
                o.interrupt();
            }
        }
    }

    /**
     * Disconnects this thread.
     *
     * @param reason The reason to disconnect.
     */
    private void disconnect(String reason) {
        askToStop();
        if (connection.getMessageHandler() != null) {
            try {
                connection.getMessageHandler().handle(connection,
                    new DOMMessage(Connection.DISCONNECT_TAG,
                        "reason", reason).toXMLElement());
            } catch (FreeColException e) {
                logger.log(Level.WARNING, "Rx disconnect", e);
            }
        }
    }

    /**
     * Listens to the InputStream and calls the MessageHandler for
     * each message received.
     * 
     * @throws IOException If thrown by the {@link FreeColNetworkInputStream}.
     * @throws SAXException if a problem occured during parsing.
     * @throws XMLStreamException if a problem occured during parsing.
     */
    private void listen() throws IOException, SAXException, XMLStreamException {
        in.enable();

        // Open a rewindable stream
        final int LOOK_AHEAD = FreeColNetworkInputStream.BUFFER_SIZE;
        BufferedInputStream bis
            = new BufferedInputStream(in, LOOK_AHEAD);
        bis.mark(LOOK_AHEAD);
        FreeColXMLReader xr = new FreeColXMLReader(bis);

        // Peek at the tag of the first item in the stream.
        try {
            String tag;
            int replyId;
            try {
                xr.nextTag();
                tag = xr.getLocalName();
                replyId = xr.getAttribute(Connection.NETWORK_REPLY_ID_TAG, -1);
            } catch (XMLStreamException xse) {
                // EOS can occur when the other end disconnects
                tag = Connection.DISCONNECT_TAG;
                replyId = -1;
            }

            // Respond to message according to tag, optionally defining a
            // thread to start.
            Thread t = null;
            DOMMessage msg;
            switch (tag) {

            case Connection.DISCONNECT_TAG:
                // Disconnect at once if needed.
                askToStop();
                return;

            case Connection.REPLY_TAG:
                // A reply.  Look up its waiting thread and set a response.
                NetworkReplyObject nro = waitingThreads.remove(replyId);
                if (nro == null) {
                    logger.warning("Could not find replyId: " + replyId);
                    return;
                }
                try {
                    bis.reset();
                    msg = new DOMMessage(bis);
                    nro.setResponse(msg);
                } catch (IOException|SAXException ex) {
                    // Always respond, even when failed, so as to unblock the
                    // waiting thread.
                    nro.setResponse(null);
                    throw ex;
                }
                return;

            case Connection.QUESTION_TAG:
                // A query.  Build a thread to handle it and send a reply.
                bis.reset();
                msg = new DOMMessage(bis);
                final int finalReplyId = replyId;
                t = new Thread(msg.getType()) {
                        @Override
                        public void run() {
                            String tag = msg.getType();
                            try {
                                ReceivingThread.this.connection
                                    .handleQuery(msg, finalReplyId);
                            } catch (FreeColException fce) {
                                logger.log(Level.WARNING, "Query "
                                    + finalReplyId
                                    + " handler for " + tag + " failed", fce);
                            } catch (IOException ioe) {
                                logger.log(Level.WARNING, "Query "
                                    + finalReplyId
                                    + " response send for " + tag + " failed",
                                    ioe);
                            }
                        }
                    };
                break;

            default:
                // An ordinary update message.  Build a thread to handle
                // it and possibly respond.
                bis.reset();
                msg = new DOMMessage(bis);
                t = new Thread(msg.getType()) {
                        @Override
                        public void run() {
                            String tag = msg.getType();
                            try {
                                ReceivingThread.this.connection.handleUpdate(msg);
                            } catch (FreeColException fce) {
                                logger.log(Level.WARNING, "Update handler for "
                                    + tag + " failed", fce);
                            } catch (IOException ioe) {
                                logger.log(Level.WARNING, "Update send for "
                                    + tag + " failed", ioe);
                            }
                        }
                    };
                break;
            }

            // Start the optional thread
            if (t != null) {
                t.setName(this.connection.getName() + "-MessageHandler-"
                    + t.getName());
                t.start();
            }
        } finally {
            xr.close();
        }
    }

    /**
     * Receives messages from the network in a loop. This method is
     * invoked when the thread starts and the thread will stop when
     * this method returns.
     */
    @Override
    public void run() {
        int timesFailed = 0;

        try {
            while (shouldRun()) {
                try {
                    listen();
                    timesFailed = 0;
                } catch (SAXException|XMLStreamException e) {
                    if (!shouldRun()) break;
                    logger.log(Level.WARNING, "XML fail", e);
                    if (++timesFailed > MAXIMUM_RETRIES) {
                        disconnect("Too many failures (XML)");
                    }
                } catch (IOException e) {
                    if (!shouldRun()) break;
                    logger.log(Level.WARNING, "IO fail", e);
                    disconnect("Unexpected IO failure");
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected exception.", e);
        } finally {
            askToStop();
        }
        // Do not send disconnect again
        connection.reallyClose();
        logger.info("Finished: " + getName());
    }
}
