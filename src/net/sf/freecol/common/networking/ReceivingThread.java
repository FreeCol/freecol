/*
 *  ReceivingThread.java - The thread that checks for incoming messages.
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

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
                    Thread t = new Thread() {
                        public void run() {
                            connection.handleAndSendReply(theMsg);
                        }
                    };
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
