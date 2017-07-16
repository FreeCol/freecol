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

    /** Maximum number of retries before closing the connection. */
    private static final int MAXIMUM_RETRIES = 5;

    /** A map of network ids to the corresponding waiting thread. */
    private final Map<Integer, NetworkReplyObject> waitingThreads
        = Collections.synchronizedMap(new HashMap<Integer, NetworkReplyObject>());

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
     *     {@code ReceivingThread} belongs to.
     * @param threadName The base name for the thread.
     */
    public ReceivingThread(Connection connection, String threadName) {
        super("ReceivingThread-" + threadName);

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
        this.waitingThreads.put(networkReplyId, nro);
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
     * Stop this thread.
     *
     * @return True if the thread was previously running and is now stopped.
     */
    private synchronized boolean stopThread() {
        boolean ret = this.shouldRun;
        if (this.shouldRun) {
            this.shouldRun = false;
            for (NetworkReplyObject o : this.waitingThreads.values()) {
                o.interrupt();
            }
        }
        return ret;
    }
        
    /**
     * Ask this thread to stop work.
     *
     * @param reason A brief description of why the thread should stop.
     */
    public void askToStop(String reason) {
        if (stopThread()) {
            logger.info(getName() + ": stopped receiving thread: " + reason);
        }
    }

    /**
     * Disconnects this thread.
     */
    private void disconnect() {
        askToStop("disconnect");
        this.connection.sendDisconnect();
    }

    /**
     * Create a thread to handle an incoming question message.
     *
     * @param qm The {@code QuestionMessage} to handle.
     * @param replyId The network reply.
     * @return A new {@code Thread} to do the work, or null if none required.
     */
    private Thread messageQuestion(final QuestionMessage qm,
                                   final int replyId) {
        final Connection conn = this.connection;
        final Message query = qm.getMessage();
        if (query == null) return null;
        final String tag = query.getType();
        final String name = getName() + "-question-" + replyId + "-" + tag;

        return new Thread(name) {
            @Override
            public void run() {
                Message reply;
                try {
                    reply = conn.handle(query);
                } catch (FreeColException fce) {
                    logger.log(Level.WARNING, name + ": handler fail", fce);
                    return;
                }

                final String replyTag = (reply == null) ? "null"
                    : reply.getType();
                try {
                    conn.sendMessage(new ReplyMessage(replyId, reply));
                    logger.log(Level.FINEST, name + " -> " + replyTag);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, name + ": response " + replyTag
                        + "fail", ex);
                }
            }
        };
    }

    /**
     * Create a thread to handle an incoming ordinary message.
     *
     * @param message The {@code Message} to handle.
     * @return A new {@code Thread} to do the work, or null if none required.
     */
    private Thread messageUpdate(final Message message) {
        if (message == null) return null;
        final String inTag = message.getType();
        final Connection conn = this.connection;
        final String name = getName() + "-update-" + inTag;
        
        return new Thread(name) {
            @Override
            public void run() {
                Message reply;
                try {
                    reply = conn.handle(message);
                } catch (FreeColException fce) {
                    logger.log(Level.WARNING, name + ": handler fail", fce);
                    return;
                }

                final String outTag = (reply == null) ? "null"
                    : reply.getType();
                try {
                    conn.sendMessage(reply);
                    logger.log(Level.FINEST, name + " -> " + outTag);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, name + ": send exception", ex);
                }
            }
        };
    }

    /**
     * Listens to the InputStream and calls the message handler for
     * each message received.
     * 
     * @exception IOException If thrown by the
     *     {@link FreeColNetworkInputStream}.
     * @exception SAXException if a problem occured during parsing.
     * @exception XMLStreamException if a problem occured during parsing.
     */
    private void listen() throws IOException, SAXException, XMLStreamException {
        String tag;
        int replyId = -1;
        try {
            tag = this.connection.startListen();
        } catch (XMLStreamException xse) {
            if (!shouldRun()) return; // Connection shutdown, fail expected
            logger.log(Level.WARNING, getName() + ": listen fail", xse);
            tag = DisconnectMessage.TAG;
        }

        // Read the message, optionally create a thread to handle it
        Thread t = null;
        switch (tag) {
        case DisconnectMessage.TAG:
            // Do not actually read the message, it might be a fake one
            // due to end-of-stream.
            askToStop("listen-disconnect");
            t = messageUpdate(TrivialMessage.disconnectMessage);
            break;

        case Connection.REPLY_TAG:
            // A reply.  Always respond, even when failing, so as to
            // unblock the waiting thread.

            replyId = this.connection.getReplyId();
            Message rm;
            try {
                rm = this.connection.reader();
            } catch (Exception ex) {
                rm = null;
                logger.log(Level.WARNING, getName() + ": reply fail", ex);
            }
            NetworkReplyObject nro = this.waitingThreads.remove(replyId);
            if (nro == null) {
                logger.warning(getName() + ": did not find reply " + replyId);
            } else {
                nro.setResponse(rm);
            }
            break;

        case Connection.QUESTION_TAG:
            // A question.  Build a thread to handle it and send a reply.

            replyId = this.connection.getReplyId();
            try {
                Message m = this.connection.reader();
                assert m instanceof QuestionMessage;
                t = messageQuestion((QuestionMessage)m, replyId);
            } catch (Exception ex) {
                logger.log(Level.WARNING, getName() + ": question fail", ex);
                askToStop("listen-question-fail");
            }
            break;
            
        default:
            // An ordinary update message.
            // Build a thread to handle it and possibly respond.

            try {
                t = messageUpdate(this.connection.reader());
            } catch (Exception ex) {
                logger.log(Level.FINEST, getName() + ": fail", ex);
                askToStop("listen-update-fail");
            }
            break;
        }

        // Start the thread
        if (t != null) t.start();

        this.connection.endListen(); // Clean up
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
                    logger.log(Level.WARNING, getName() + ": XML fail", ex);
                    if (++timesFailed > MAXIMUM_RETRIES) {
                        disconnect();
                    }
                } catch (IOException ioe) {
                    if (!shouldRun()) break;
                    logger.log(Level.WARNING, getName() + ": IO fail", ioe);
                    disconnect();
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, getName() + ": unexpected fail", ex);
        } finally {
            askToStop("run complete");
        }
        logger.info(getName() + ": finished");
    }
}
