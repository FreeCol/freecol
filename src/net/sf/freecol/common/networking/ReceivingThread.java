
package net.sf.freecol.common.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
* The thread that checks for incoming messages.
*/
final class ReceivingThread extends Thread {
    private static final Logger logger = Logger.getLogger(ReceivingThread.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final FreeColNetworkInputStream in;
    private boolean halt;

    private int nextNetworkReplyId = 0;

    private Vector threadsWaitingForNetworkReply = new Vector();
    private final Connection connection;





    /**
    * The constructor to use.
    *
    * @param connection The <code>Connection</code> this <code>ReceivingThread</code>
    *                   belongs to.
    * @param in The stream to read from.
    */
    ReceivingThread(Connection connection, InputStream in) {
        super("ReceivingThread");

        this.connection = connection;
        this.in = new FreeColNetworkInputStream(in);
        halt = false;
    }





    /**
    * Gets the next <code>networkReplyId</code> that will be used when identifing a network message.
    * @return The next available <code>networkReplyId</code>.
    */
    public int getNextNetworkReplyId() {
        nextNetworkReplyId++;

        return nextNetworkReplyId - 1;
    }


    /**
    * Creates and registers a new <code>NetworkReplyObject</code> with the
    * specified ID.
    *
    * @param networkReplyId The id of the message the calling thread should wait for.
    * @return The <code>NetworkReplyObject</code> containing the network message.
    */
    public NetworkReplyObject waitForNetworkReply(int networkReplyId) {
        NetworkReplyObject nro = new NetworkReplyObject(networkReplyId);

        threadsWaitingForNetworkReply.add(nro);

        return nro;
    }


    /**
    * The method that does it all. Listens to the inputstream and then tries to
    * recognize messages. Calls the messagehandler for each message received.
    */
    public void run() {
        Message msg = null;
        int retry = 0;

        while (!isHalted() && (msg == null || !msg.isType("disconnect"))) {
            msg = null;
            in.enable();

            try {
                msg = new Message(in);
            } catch (SAXException sxe) {
                if (!isHalted()) {
                    // Error generated during parsing
                    Exception  x = sxe;
                    if (sxe.getException() != null) {
                        x = sxe.getException();
                    }
                    StringWriter sw = new StringWriter();
                    x.printStackTrace(new PrintWriter(sw));
                    logger.warning(sw.toString());
                    retry++;
                    if (retry > 10) {
                        try {
                            Element disconnectElement = Message.createNewRootElement("disconnect");
                            disconnectElement.setAttribute("reason", "SAXException");
                            connection.getMessageHandler().handle(connection, disconnectElement);
                        } catch (FreeColException e) {}
                        break;
                    } else {
                        continue;
                    }
                } else {
                    return;
                }
            } catch (IOException ioe) {
                if (!isHalted()) {
                    // I/O error
                    StringWriter sw = new StringWriter();
                    ioe.printStackTrace(new PrintWriter(sw));
                    logger.warning(sw.toString());

                    try {
                        Element disconnectElement = Message.createNewRootElement("disconnect");
                        disconnectElement.setAttribute("reason", "IOException");
                        connection.getMessageHandler().handle(connection, disconnectElement);
                    } catch (FreeColException e) {}
                    break;
                } else {
                    return;
                }
            }

            retry = 0;
            
            if (isHalted()) {
                return;
            }

            if (msg.isType("invalid")) {
                logger.warning("--INVALID MESSAGE RECIEVED--");
            }

            // START DEBUG-LINES:
            if (FreeCol.isInDebugMode()) {
                System.out.println(connection.convertElementToString(msg.getDocument().getDocumentElement()));
                System.out.println();
                System.out.flush();
            }
            // END DEBUB

            if (msg.isType("reply")) { // == this is a reply-message:
                boolean foundNetworkReplyObject = false;

                for (int i=0; i<threadsWaitingForNetworkReply.size(); i++) {
                    NetworkReplyObject nro = (NetworkReplyObject) threadsWaitingForNetworkReply.get(i);

                    if (nro.getNetworkReplyId() == Integer.parseInt(msg.getAttribute("networkReplyId"))) {
                        nro.setResponse(msg);
                        threadsWaitingForNetworkReply.remove(i);
                        foundNetworkReplyObject = true;
                        break; // Should only be one 'NetworkReplyObject' for each 'networkReplyId'.
                    }
                }

                if (!foundNetworkReplyObject) {
                    logger.warning("Could not find networkReplyId=" + msg.getAttribute("networkReplyId"));
                }

            } else { // == this is not a reply-message:
                final Message theMsg  = msg;

                /*
                TODO: The tag "urgent" should be used to mark messages
                        that should be processed in a separate thread:
                */
                Thread t = new Thread() {
                    public void run() {
                        connection.handleAndSendReply(theMsg);
                    }
                };
                t.setName("MessageHandler:"+t.getName());
                t.start();
            }
        }
        
        stopWorking();
    }
    
    
    /**
    * Checks if this thread has been halted.
    */
    private synchronized boolean isHalted() {
        return halt;
    }


    /**
    * Tells this thread that it doesn't need to do any more work.
    */
    synchronized void stopWorking() {
        halt = true;
    }

    
    


    



    /**
    * Input stream for buffering the data from the network.
    *
    * <br><br>
    *
    * This is just a buffered input stream that signals end-of-stream when a
    * given token {@link #END_OF_STREAM} is encountered. In order to continue receiving data,
    * the method {@link #enable} has to be called. Calls to {@link #close} has no
    * effect, the underlying input stream has to be closed directly.
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
        * @param in The input stream in which this object should get
        *           the data from.
        */
        FreeColNetworkInputStream(InputStream in) {
            this.in = in;
        }



        /**
        * Fills the buffer with data.
        * @return <i>true</i> if the buffer has been filled with
        *         data, and <i>false</i> if an error occured.
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
                logger.info("Could not read data from stream.");
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
        * Prepares the input stream for a new message.
        * <br><br>
        * Makes the subsequent calls to <code>read</code> return
        * the data instead of <code>-1</code>.
        */
        void enable() {
            wait = false;
        }


        /**
        * Reads a single byte.
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
                    return buffer[BUFFER_SIZE-1];
                } else {
                    return buffer[bStart-1];
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
        *         message has ended, that is; if the token {@link #END_OF_STREAM} 
        *         is encountered.
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
            for (; r<len; r++) {
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

                b[r+off] = buffer[bStart];

                bStart++;
                if (bStart == bEnd || bEnd == 0 && bStart == BUFFER_SIZE) {
                    empty = true;
                    if (!fill()) {
                        wait = true;
                        return -1;
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
