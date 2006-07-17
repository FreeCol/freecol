
package net.sf.freecol.metaserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.sf.freecol.common.networking.Connection;



/**
* The entry point and main controller object for the meta server.
*
* <br><br>
*
* When a new client connects to the meta server a new {@link Connection}
* is made, with {@link NetworkHandler} as the control object.
*
* @see net.sf.freecol.common.networking
*/
public final class MetaServer extends Thread {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(MetaServer.class.getName());

    private static final int REMOVE_DEAD_SERVERS_INTERVAL = 120000;
    public static final int REMOVE_OLDER_THAN = 90000;


    /** The public "well-known" socket to which clients may connect. */
    private ServerSocket serverSocket;

    /** A hash of Connection objects, keyed by the Socket they relate to. */
    private HashMap connections = new HashMap();

    /** Whether to keep running the main loop that is awaiting new client connections. */
    private boolean running = true;

    /** The TCP port that is beeing used for the public socket. */
    private int port;

    private NetworkHandler networkHandler;



    /**
    * Creates and starts a new <code>MetaServer</code>.
    * @param args The command-line options.
    */
    public static void main(String[] args) {
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: java net.sf.freecol.metaserver.MetaServer PORT_NUMBER");
            System.exit(-1);
        } catch (NumberFormatException e) {
            System.out.println("Usage: java net.sf.freecol.metaserver.MetaServer PORT_NUMBER");
            System.exit(-1);
        }

        MetaServer metaServer = null;
        try {
            metaServer = new MetaServer(port);
        } catch (IOException e) {
            logger.warning("Could not create MetaServer!");
            System.exit(-1);
        }

        metaServer.start();
    }




    /**
     * Creates a new network server. Use {@link #run metaServer.start()} to start
     * listening for new connections.
     *
     * @param port The TCP port to use for the public socket.
     * @throws IOException if the public socket cannot be created.
     */
    public MetaServer(int port) throws IOException {
        this.port = port;

        final MetaRegister mr = new MetaRegister();
        networkHandler = new NetworkHandler(this, mr);
        serverSocket = new ServerSocket(port);

        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    mr.removeDeadServers();
                } catch (Exception ex) {
                    logger.warning("Exception: " + ex.getMessage());
                }
            }
        }, REMOVE_DEAD_SERVERS_INTERVAL, REMOVE_DEAD_SERVERS_INTERVAL);
    }




    /**
    * Starts the thread's processing. Contains the loop that is waiting for new
    * connections to the public socket. When a new client connects to the server
    * a new {@link Connection} is made, with {@link NetworkHandler} as
    * the control object.
    */
    public void run() {
        while (running) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                logger.info("Got client connection from " + clientSocket.getInetAddress().toString());
                Connection connection = new Connection(clientSocket, getNetworkHandler());
                connections.put(clientSocket, connection);
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));

                logger.warning(sw.toString());
            }
        }
     }


     /**
     * Gets the control object that handles the network requests.
     * @return The <code>NetworkHandler</code>.
     */
     public NetworkHandler getNetworkHandler() {
        return networkHandler;
     }


    /**
    * Gets the TCP port that is beeing used for the public socket.
    * @return The TCP port.
    */
    public int getPort() {
        return port;
    }


    /**
    * Gets an iterator of every connection to this server.
    *
    * @return The <code>Iterator</code>.
    * @see Connection
    */
    public Iterator getConnectionIterator() {
        return connections.values().iterator();
    }


    /**
    * Shuts down the server thread.
    */
    public void shutdown() {
        running = false;

        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warning("Could not close the server socket!");
        }

        Iterator connectionsIterator = connections.values().iterator();
        while (connectionsIterator.hasNext()) {
            Connection c = (Connection) connectionsIterator.next();

            try {
                if (c != null) {
                    //c.reallyClose();
                    c.close();
                }
            } catch (IOException e) {
                logger.warning("Could not close the connection.");
            }
        }
        
        logger.info("Server shutdown.");
    }


    /**
    * Gets a <code>Connection</code> identified by a <code>Socket</code>.
    *
    * @param socket The <code>Socket</code> that identifies the
    *               <code>Connection</code>
    * @return The <code>Connection</code>.
    */
    public Connection getConnection(Socket socket) {
        return (Connection) connections.get(socket);
    }
    
    
    /**
    * Removes the given connection.
    * @param connection The connection that should be removed.
    */
    public void removeConnection(Connection connection) {
        connections.remove(connection.getSocket());
    }    
}
