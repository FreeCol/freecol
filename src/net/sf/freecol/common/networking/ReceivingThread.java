
package net.sf.freecol.common.networking;

import net.sf.freecol.FreeCol;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Logger;

import java.util.Vector;



/**
* The thread that checks for incoming messages.
*/
final class ReceivingThread extends Thread {
    private static final Logger logger = Logger.getLogger(ReceivingThread.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final int BUFFER_SIZE = 4096;

    private final BufferedReader reader;
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
        this.reader = new BufferedReader(new InputStreamReader(in));
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
        boolean adios = false;
        Message msg = null;

        StringBuffer buffer = new StringBuffer();

        while (!adios && !halt) {
            try {
                buffer.setLength(0);
                
                int i = reader.read();
                while (!halt && i != -1 && ((char) i) != '\0') {
                    buffer.append(((char) i));
                    i = reader.read();
                }
            } catch (SocketException e) {
                if (halt) {
                    break;
                } else {
                    // Just close the connection for now.
                    halt = true;
                    break;
                }
            } catch(IOException e) {
                if (halt) {
                    break;
                } else {
                    logger.warning("Could not read from socket.");
                    e.printStackTrace();
                    halt = true;
                    break;
                }
            }

            if (!halt && buffer.length() != 0) {
                String textMessage = buffer.toString();

                msg = new Message(textMessage);

                // TODO: Add some kind of ServerShutdown message and change
                // the following test to check for that message.
                if (msg.getType().equals("???")) {
                    adios = true;
                }

                if (msg.getType().equals("invalid")) {
                  logger.warning("--INVALID MESSAGE RECIEVED--");
                }
                
                // START DEBUG-LINES:
                if (FreeCol.isInDebugMode()) {
                    System.out.println(msg);
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

                } else if (!msg.getType().equals("invalid")) { // == this is not a reply-message:
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

            msg = null;
        }
    }
    

    /**
    * Tells this thread that it doesn't need to do any more work.
    */
    void stopWorking() {
        halt = true;

        try {
            reader.close();
        } catch (IOException e) {
            logger.warning("Could not close input stream!");
        }
    }
}
