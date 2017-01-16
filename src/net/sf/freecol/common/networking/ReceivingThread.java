/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
     * has to be called.  Calls to {@code close()} have no
     * effect, the underlying input stream has to be closed directly.
     */
    private static class FreeColNetworkInputStream extends InputStream {

        private static final int EOS_RESULT = -1;

        private final InputStream in;

        private final byte[] buffer = new byte[Connection.BUFFER_SIZE];

        private final byte[] bb = new byte[1];
        
        private int bStart = 0;

        private int bSize = 0;

        private boolean wait = false;


        /**
         * Creates a new {@code FreeColNetworkInputStream}.
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
         * Makes the subsequent calls to {@code read} return the data
         * instead of {@code EOS_RESULT}.
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

            int r = this.in.read(buffer, 0, buffer.length);
            if (r <= 0) return false;

            this.bStart = 0;
            this.bSize = r;
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
                if (value == (byte)Connection.END_OF_STREAM) {
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
     * @param connection The {@code Connection} this
     *            {@code ReceivingThread} belongs to.
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
     * Creates and registers a new {@code NetworkReplyObject} with the
     * specified object identifier.
     * 
     * @param networkReplyId The identifier of the message the calling
     *     thread should wait for.
     * @return The {@code NetworkReplyObject} containing the network
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
     */
    private void disconnect() {
        askToStop();
        connection.sendDisconnect();
    }

    // Individual parts of listen().  Work in progress
    private String peek(FreeColXMLReader xr) {
        try {
            xr.nextTag();
            return xr.getLocalName();
        } catch (XMLStreamException xse) {
            // Expected to fail at EOS
        }
        return TrivialMessage.DISCONNECT_TAG;
    }

    private DOMMessage reader(BufferedInputStream bis)
        throws IOException, SAXException {
        bis.reset();
        return new DOMMessage(bis);
    }

    private void reply(DOMMessage msg, int replyId) {
        NetworkReplyObject nro = waitingThreads.remove(replyId);
        if (nro == null) {
            logger.warning("Could not find reply: " + replyId);
        } else {
            nro.setResponse(msg);
        }
    }

    private Thread query(DOMMessage msg, final int replyId) {
        return new Thread(this.connection.getName() + "-query-" + replyId + "-"
                          + msg.getType()) {
            @Override
            public void run() {
                final String tag = msg.getType();
                try {
                    ReceivingThread.this.connection.handleQuery(msg, replyId);
                } catch (FreeColException fce) {
                    logger.log(Level.WARNING, "Query " + replyId
                        + " handler for " + tag + " failed", fce);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Query " + replyId
                        + " response send for " + tag + " failed", ioe);
                }
            }
        };
    }

    private Thread update(DOMMessage msg) {
        return new Thread(this.connection.getName() + "-update-"
                          + msg.getType()) {
            @Override
            public void run() {
                final String tag = msg.getType();
                try {
                    ReceivingThread.this.connection.handleUpdate(msg);
                } catch (FreeColException fce) {
                    logger.log(Level.WARNING, "Update handler for " + tag
                        + " failed", fce);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Update send for " + tag
                        + " failed", ioe);
                }
            }
        };
    }

    /**
     * Respond to an incoming message according to it tag
     * optionally starting a handler thread.
     *
     * @param bis The {@code BufferedInputStream} to read the message from.
     * @param tag The message tag.
     * @param replyId The replyId, if any.
     */
    private void handle(BufferedInputStream bis, String tag, int replyId)
        throws IOException, SAXException {
        Thread t = null;
        DOMMessage msg;
        switch (tag) {

        case Connection.REPLY_TAG:
            // A reply.  Always respond, even when failing, so as to
            // unblock the waiting thread.
            try {
                msg = reader(bis);
                reply(msg, replyId);
            } catch (IOException|SAXException ex) {
                reply(null, replyId);
                throw ex;
            }
            return;

        case Connection.QUESTION_TAG:
            // A query.  Build a thread to handle it and send a reply.
            msg = reader(bis);
            t = query(msg, replyId);
            break;
            
        default:
            // An ordinary update message.  Build a thread to handle
            // it and possibly respond.
            msg = reader(bis);
            t = update(msg);
            break;
        }

        // Start the thread
        t.start();
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
        final int LOOK_AHEAD = Connection.BUFFER_SIZE;
        BufferedInputStream bis = new BufferedInputStream(in, LOOK_AHEAD);

        // Start using FreeColXMLReader.  This must grow.
        FreeColXMLReader xr = null;
        bis.mark(LOOK_AHEAD);
        try {
            xr = new FreeColXMLReader(bis); //.setTracing(true);
            String tag = peek(xr);
            int replyId = -1;
            if (TrivialMessage.DISCONNECT_TAG.equals(tag)) {
                // Could not read tag, or real disconnect.
                // Signal stop, and do *not* try to read reply id, it will fail
                askToStop();
            } else {
                replyId = xr.getAttribute(Connection.NETWORK_REPLY_ID_TAG, -1);
            }
            handle(bis, tag, replyId);
        } finally {
            if (xr != null) xr.close();
        }
    }


    // Override Thread

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // Receive messages from the network in a loop. This method is
        // invoked when the thread starts and the thread will stop when
        // this method returns.
        int timesFailed = 0;
        try {
            while (shouldRun()) {
                try {
                    listen();
                    timesFailed = 0;
                } catch (SAXException|XMLStreamException ex) {
                    if (!shouldRun()) break;
                    logger.log(Level.WARNING, "XML fail", ex);
                    if (++timesFailed > MAXIMUM_RETRIES) {
                        disconnect();
                    }
                } catch (IOException ioe) {
                    if (!shouldRun()) break;
                    logger.log(Level.WARNING, "IO fail", ioe);
                    disconnect();
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected exception.", e);
        } finally {
            askToStop();
        }
        connection.close();
        logger.info("Finished: " + getName());
    }
}
