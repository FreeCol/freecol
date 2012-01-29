/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * The thread that checks for incoming messages.
 */
final class ReceivingThread extends Thread {

    private static final Logger logger = Logger.getLogger(ReceivingThread.class.getName());

    /** Maximum number og retries before closing the connection. */
    private static final int MAXIMUM_RETRIES = 5;

    private final FreeColNetworkInputStream in;

    private XMLStreamReader xmlIn = null;

    private boolean shouldRun;

    private int nextNetworkReplyId = 1;

    private final Map<Integer, NetworkReplyObject> threadsWaitingForNetworkReply;

    private final Connection connection;

    private boolean locked = false;


    /**
     * The constructor to use.
     * 
     * @param connection The <code>Connection</code> this
     *            <code>ReceivingThread</code> belongs to.
     * @param in The stream to read from.
     */
    ReceivingThread(Connection connection, InputStream in, String threadName) {
        super(threadName + "ReceivingThread - " + connection.toString());

        this.connection = connection;
        this.in = new FreeColNetworkInputStream(in);

        shouldRun = true;

        threadsWaitingForNetworkReply = Collections.synchronizedMap(
                new HashMap<Integer, NetworkReplyObject>());
    }

    /**
     * Gets the next <code>networkReplyId</code> that will be used when
     * identifing a network message.
     * 
     * @return The next available <code>networkReplyId</code>.
     */
    public synchronized int getNextNetworkReplyId() {
        return nextNetworkReplyId++;
    }

    /**
     * Creates and registers a new <code>NetworkReplyObject</code> with the
     * specified ID.
     * 
     * @param networkReplyId The id of the message the calling thread should
     *            wait for.
     * @return The <code>NetworkReplyObject</code> containing the network
     *         message.
     */
    public NetworkReplyObject waitForNetworkReply(int networkReplyId) {
        NetworkReplyObject nro = new NetworkReplyObject(networkReplyId, false);
        threadsWaitingForNetworkReply.put(networkReplyId, nro);
        return nro;
    }

    /**
     * Creates and registers a new <code>NetworkReplyObject</code> with the
     * specified ID.
     * 
     * @param networkReplyId The id of the message the calling thread should
     *            wait for.
     * @return The <code>NetworkReplyObject</code> containing the network
     *         message.
     */
    public NetworkReplyObject waitForStreamedNetworkReply(int networkReplyId) {
        NetworkReplyObject nro = new NetworkReplyObject(networkReplyId, true);
        threadsWaitingForNetworkReply.put(networkReplyId, nro);
        return nro;
    }

    /**
     * Receives messages from the network in a loop. This method is invoked when
     * the thread starts and the thread will stop when this method returns.
     */
    public void run() {
        int timesFailed = 0;

        try {
            while (shouldRun()) {
                try {
                    listen();
                    timesFailed = 0;
                } catch (XMLStreamException e) {
                    timesFailed++;
                    // warnOf(e);
                    if (shouldRun && timesFailed > MAXIMUM_RETRIES) {
                        disconnect();
                    }
                } catch (SAXException e) {
                    timesFailed++;
                    // warnOf(e);
                    if (shouldRun && timesFailed > MAXIMUM_RETRIES) {
                        disconnect();
                    }
                } catch (IOException e) {
                    // warnOf(e);
                    if (shouldRun) {
                        disconnect();
                    }
                }
            }
        } finally {
            askToStop();
        }
    }

    public void unlock() {
        locked = false;
    }


    /**
     * Listens to the inputstream and calls the messagehandler for each message
     * received.
     * 
     * @exception IOException If thrown by the {@link FreeColNetworkInputStream}.
     * @exception SAXException if a problem occured during parsing.
     * @exception XMLStreamException if a problem occured during parsing.
     */
    private void listen() throws IOException, SAXException, XMLStreamException {
        while (locked) {
            try {
                // TODO: Fix this sleep(1) is not a solution
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Do nothing here.
            }
        }

        final int LOOK_AHEAD = 500;
        BufferedInputStream bis = new BufferedInputStream(in, LOOK_AHEAD);
        in.enable();
        bis.mark(LOOK_AHEAD);
        if (!shouldRun()) return;

        XMLInputFactory xif = XMLInputFactory.newInstance();
        xmlIn = xif.createXMLStreamReader(bis);
        xmlIn.nextTag();

        boolean disconnectMessage =
                (xmlIn.getLocalName().equals("disconnect")) ? true : false;
        if (xmlIn.getLocalName().equals("reply")) {

            String networkReplyID =
                    xmlIn.getAttributeValue(null, "networkReplyId");

            NetworkReplyObject nro = threadsWaitingForNetworkReply.remove(
                    Integer.valueOf(networkReplyID));

            if (nro != null) {
                if (nro.isStreamed()) {
                    locked = true;
                    nro.setResponse(xmlIn);
                } else {
                    xmlIn.close();
                    xmlIn = null;
                    bis.reset();

                    final DOMMessage msg = new DOMMessage(bis);
                    nro.setResponse(msg);
                }
            } else {
                while (xmlIn.hasNext()) {
                    xmlIn.next();
                }
                xmlIn.close();
                xmlIn = null;
                logger.warning("Could not find networkReplyId="
                        + networkReplyID);
            }
        } else {
            xmlIn.close();
            xmlIn = null;
            bis.reset();
            connection.handleAndSendReply(bis);
        }

        if (disconnectMessage) {
            askToStop();
        }
    }

    /**
     * Checks if this thread has been halted.
     */
    private synchronized boolean shouldRun() {
        return shouldRun;
    }

    /**
     * Tells this thread that it doesn't need to do any more work.
     */
    synchronized void askToStop() {
        shouldRun = false;
        for (NetworkReplyObject o : threadsWaitingForNetworkReply.values()) {
            o.interrupt();
        }
    }

    private void disconnect() {
        if (connection.getMessageHandler() != null) {
            try {
                Element disconnectElement =
                        DOMMessage.createNewRootElement("disconnect");
                disconnectElement.setAttribute("reason", "reception exception");
                connection.getMessageHandler().handle(connection,
                        disconnectElement);
            } catch (FreeColException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Input stream for buffering the data from the network.
     * 
     * <br>
     * <br>
     * 
     * This is just a buffered input stream that signals end-of-stream when a
     * given token {@link #END_OF_STREAM} is encountered. In order to continue
     * receiving data, the method {@link #enable} has to be called. Calls to
     * <code>close()</code> has no effect, the underlying input stream has to
     * be closed directly.
     */
    class FreeColNetworkInputStream extends InputStream {
        private static final int BUFFER_SIZE = 8192;

        private static final char END_OF_STREAM = '\n';

        private final InputStream in;

        private byte[] buffer = new byte[BUFFER_SIZE];

        private int bStart = 0;

        private int bEnd = 0;

        private boolean empty = true;

        private boolean wait = false;


        /**
         * Creates a new <code>FreeColNetworkInputStream</code>.
         * 
         * @param in The input stream in which this object should get the data
         *            from.
         */
        FreeColNetworkInputStream(InputStream in) {
            this.in = in;
        }

        /**
         * Fills the buffer with data.
         * 
         * @return <i>true</i> if the buffer has been filled with data, and
         *         <i>false</i> if an error occured.
         * @exception IOException if thrown by the underlying stream.
         */
        private boolean fill() throws IOException {
            int r;
            if (bStart < bEnd || empty && bStart == bEnd) {
                if (empty) {
                    bStart = 0;
                    bEnd = 0;
                }
                r = in.read(buffer, bEnd, BUFFER_SIZE - bEnd);
            } else if (bStart == bEnd) {
                throw new IllegalStateException();
            } else {
                r = in.read(buffer, bEnd, bStart - bEnd);
            }

            if (r <= 0) {
                logger.fine("Could not read data from stream.");
                return false;
            }

            empty = false;

            bEnd += r;
            if (bEnd == BUFFER_SIZE) {
                bEnd = 0;
            }
            return true;
        }

        /**
         * Prepares the input stream for a new message. <br>
         * <br>
         * Makes the subsequent calls to <code>read</code> return the data
         * instead of <code>-1</code>.
         */
        void enable() {
            wait = false;
        }

        /**
         * Reads a single byte.
         * 
         * @see #read(byte[], int, int)
         */
        public int read() throws IOException {
            if (wait) {
                return -1;
            }

            if (empty) {
                if (!fill()) {
                    wait = true;
                    return -1;
                }
            }

            if (buffer[bStart] == END_OF_STREAM) {
                bStart++;
                if (bStart == BUFFER_SIZE) {
                    bStart = 0;
                }
                if (bStart == bEnd) {
                    empty = true;
                }
                wait = true;
                return -1;
            } else {
                bStart++;
                if (bStart == bEnd || bEnd == 0 && bStart == BUFFER_SIZE) {
                    empty = true;
                }
                if (bStart == BUFFER_SIZE) {
                    bStart = 0;
                    return buffer[BUFFER_SIZE - 1];
                } else {
                    return buffer[bStart - 1];
                }
            }
        }

        /**
         * Reads from the buffer and returns the data.
         * 
         * @param b The place where the data will be put.
         * @param off The offset to use when writing the data to <code>b</code>.
         * @param len Number of bytes to read.
         * @return The actual number of bytes read and <code>-1</code> if the
         *         message has ended, that is; if the token
         *         {@link #END_OF_STREAM} is encountered.
         * 
         */
        public int read(byte[] b, int off, int len) throws IOException {
            if (wait) {
                return -1;
            }

            if (empty) {
                if (!fill()) {
                    wait = true;
                    return -1;
                }
            }

            int r = 0;
            for (; r < len; r++) {
                if (buffer[bStart] == END_OF_STREAM) {
                    bStart++;
                    if (bStart == BUFFER_SIZE) {
                        bStart = 0;
                    }
                    if (bStart == bEnd) {
                        empty = true;
                    }
                    wait = true;
                    return r;
                }

                b[r + off] = buffer[bStart];

                bStart++;
                if (bStart == bEnd || bEnd == 0 && bStart == BUFFER_SIZE) {
                    empty = true;
                    if (!fill()) {
                        wait = true;
                        return r + 1;
                    }
                }
                if (bStart == BUFFER_SIZE) {
                    bStart = 0;
                }
            }

            return len;
        }
    }
}
