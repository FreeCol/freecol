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
import net.sf.freecol.common.io.FreeColNetworkInputStream;

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
     *            {@code ReceivingThread} belongs to.
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
        connection.sendDisconnect();
    }


    // Individual parts of listen().  Work in progress

    private Thread domQuestion(final DOMMessage msg, final int replyId) {
        final String name = getName() + "-question-" + replyId
            + "-" + msg.getType();
        return new Thread(name) {
            @Override
            public void run() {
                final String tag = msg.getType();
                try {
                    ReceivingThread.this.connection.handleQuestion(msg, replyId);
                } catch (FreeColException fce) {
                    logger.log(Level.WARNING, name + ": handler failed", fce);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, name + ": send failed", ioe);
                }
            }
        };
    }

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
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, name + ": response " + replyTag
                        + "fail", ioe);
                    return;
                }
            }
        };
    }

    // Works for both Message and DOM code
    private void reply(Message message, int replyId) {
        NetworkReplyObject nro = waitingThreads.remove(replyId);
        if (nro == null) {
            logger.warning(getName() + ": not find reply " + replyId);
        } else {
            nro.setResponse(message);
        }
    }

    private Thread domUpdate(DOMMessage msg) {
        final String name = getName() + "-update-" + msg.getType();

        return new Thread(name) {
            @Override
            public void run() {
                final String tag = msg.getType();
                try {
                    ReceivingThread.this.connection.handleUpdate(msg);
                } catch (FreeColException fce) {
                    logger.log(Level.WARNING, name + ": handler fail", fce);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, name + ": send fail", ioe);
                }
            }
        };
    }

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
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, name + ": send fail", ioe);
                }
            }
        };
    }

    /**
     * Listens to the InputStream and calls the message handler for
     * each message received.
     * 
     * @throws IOException If thrown by the {@link FreeColNetworkInputStream}.
     * @throws SAXException if a problem occured during parsing.
     * @throws XMLStreamException if a problem occured during parsing.
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
        DOMMessage dm = null;
        switch (tag) {
        case DisconnectMessage.TAG:
            // Do not actually read the message, it might be a fake one
            // due to end-of-stream.
            dm = TrivialMessage.disconnectMessage;
            t = domUpdate(dm);
            askToStop("listen-disconnect");
            break;

        case Connection.REPLY_TAG:
            // A reply.  Always respond, even when failing, so as to
            // unblock the waiting thread.

            replyId = this.connection.getReplyId();
            if (false) { // DISABLED FOR NOW
                Message m = null;
                try {
                    m = this.connection.reader();
                } catch (FreeColException|XMLStreamException ex) {
                    // Just log for now, fall through to DOM-based code
                    logger.log(Level.FINEST, getName() + ": reply fail", ex);
                }
                if (m != null) {
                    assert m instanceof ReplyMessage;
                    reply(m, replyId);
                    break;
                }
            }

            try {
                dm = this.connection.domReader();
                reply(dm, replyId);
            } catch (IOException|SAXException ex) {
                reply(null, replyId);
                throw ex;
            }
            break;

        case Connection.QUESTION_TAG:
            // A question.  Build a thread to handle it and send a reply.

            replyId = this.connection.getReplyId();
            if (false) { // DISABLED FOR NOW
                Message m = null;
                try {
                    m = this.connection.reader();
                } catch (FreeColException|XMLStreamException ex) {
                    // Just log for now, fall through to DOM-based code
                    logger.log(Level.FINEST, getName() + ": question fail", ex);
                }
                if (m != null) {
                    assert m instanceof QuestionMessage;
                    t = messageQuestion((QuestionMessage)m, replyId);
                    break;
                }
            }

            dm = this.connection.domReader();
            t = domQuestion(dm, replyId);
            break;
            
        default:
            // An ordinary update message.
            // Build a thread to handle it and possibly respond.

            if (true) {
                Message m = null;
                try {
                    m = this.connection.reader();
                } catch (FreeColException|XMLStreamException ex) {
                    // Just log for now, fall through to DOM-based code
                    logger.log(Level.FINEST, getName() + ": fail", ex);
                }
                if (m != null) {
                    t = messageUpdate(m);
                    break;
                }
            }

            dm = this.connection.domReader();
            t = domUpdate(dm);
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
        connection.close();
        logger.info(getName() + ": finished");
    }
}
