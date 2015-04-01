/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.server.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;


/**
 * The networking server in which new clients can connect and methods
 * like <code>sendToAll</code> are kept.
 *
 * <br><br>
 *
 * When a new client connects to the server a new {@link Connection}
 * is made, with {@link net.sf.freecol.server.control.UserConnectionHandler}
 * as the control object.
 *
 * @see net.sf.freecol.common.networking
 */
public final class Server extends Thread {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    /** Backlog for socket. */
    private static final int BACKLOG_DEFAULT = 10;

    /** The public "well-known" socket to which clients may connect. */
    private final ServerSocket serverSocket;

    /** A hash of Connection objects, keyed by the Socket they relate to. */
    private final HashMap<Socket, Connection> connections = new HashMap<>();

    /**
     * Whether to keep running the main loop that is awaiting new
     * client connections.
     */
    private boolean running = true;

    /** The owner of this <code>Server</code>. */
    private final FreeColServer freeColServer;

    /** The name of the host for the public socket. */
    private final String host;

    /** The TCP port that is beeing used for the public socket. */
    private final int port;

    /** For information about this variable see the run method. */
    private final Object shutdownLock = new Object();


    /**
     * Creates a new network server. Use {@link #run server.start()} to start
     * listening for new connections.
     *
     * @param freeColServer The owner of this <code>Server</code>.
     * @param host The name of the host for the public socket.
     * @param port The TCP port to use for the public socket.
     * @throws IOException if the public socket cannot be created.
     */
    public Server(FreeColServer freeColServer, String host,
                  int port) throws IOException {
        super(FreeCol.SERVER_THREAD + "Server");

        this.freeColServer = freeColServer;
        this.host = host;
        this.port = port;
        this.serverSocket = new ServerSocket(port, BACKLOG_DEFAULT,
                                             InetAddress.getByName(host));
        this.serverSocket.setReuseAddress(true);
    }


    /**
     * Gets the host that is being used for the public socket.
     *
     * @return The name of the host.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Gets the TCP port that is being used for the public socket.
     *
     * @return The TCP port.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Gets a <code>Connection</code> identified by a <code>Socket</code>.
     *
     * @param socket The <code>Socket</code> that identifies the
     *               <code>Connection</code>
     * @return The <code>Connection</code>.
     */
    public Connection getConnection(Socket socket) {
        return connections.get(socket);
    }

    /**
     * Adds a (usually Dummy)Connection into the hashmap.
     *
     * @param connection The connection to add.
     */
    public void addDummyConnection(Connection connection) {
        if (!running) return;
        connections.put(new Socket(), connection);
    }

    /**
     * Adds a Connection into the hashmap.
     *
     * @param connection The connection to add.
     */
    public void addConnection(Connection connection) {
        if (!running) return;
        connections.put(connection.getSocket(), connection);
    }

    /**
     * Removes the given connection.
     *
     * @param connection The connection that should be removed.
     */
    public void removeConnection(Connection connection) {
        connections.remove(connection.getSocket());
    }

    /**
     * Sets the specified <code>MessageHandler</code> to all connections.
     *
     * @param mh The <code>MessageHandler</code> to use.
     */
    public void setMessageHandlerToAllConnections(MessageHandler mh) {
        for (Connection c : connections.values()) {
            c.setMessageHandler(mh);
        }
    }

    /**
     * Sends a network message to all connections with an optional exception.
     *
     * @param element The root <code>Element</code> of the message to send.
     * @param exceptConnection An optional <code>Connection</code> not
     *     to send to.
     */
    public void sendToAll(Element element, Connection exceptConnection) {
        for (Connection c : new ArrayList<>(connections.values())) {
            if (c == exceptConnection) continue;
            if (c.isAlive()) {
                try {
                    c.sendAndWait(element);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Unable to send to: " + c, e);
                }
            } else {
                logger.log(Level.INFO, "Reap dead connection: " + c);
                removeConnection(c);
            }
        }
    }

    /**
     * Sends a network message to all connections.
     *
     * @param element The root element of the message to send.
     */
    public void sendToAll(Element element) {
        sendToAll(element, null);
    }

    /**
     * Start the thread processing.  Contains the loop that is waiting
     * for new connections to the public socket.  When a new client
     * connects to the server a new {@link Connection} is made, with
     * {@link net.sf.freecol.server.control.UserConnectionHandler} as
     * the control object.
     */
    @Override
    public void run() {
        // This method's entire body is synchronized to shutdownLock.
        // The reason why this is done is to prevent the shutdown
        // method from finishing before this thread is finished
        // working.  We have to do this because the
        // ServerSocket::close method keeps the server alive for
        // several milliseconds *even after the close method is
        // finished*.  Because of this a new server can't be created
        // on the same port as this server right after closing this
        // server.
        //
        // Now that the shutdown method 'hangs' until the entire
        // server thread is finished you can be certain that the
        // ServerSocket is REALLY closed after execution of shutdown.
        synchronized (shutdownLock) {
            while (running) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();

                    logger.info("Got client connection from "
                        + clientSocket.getInetAddress()
                        + ":" + clientSocket.getPort());
                    Connection connection =
                        new Connection(clientSocket,
                            freeColServer.getUserConnectionHandler(),
                            FreeCol.SERVER_THREAD);
                    addConnection(connection);
                } catch (IOException e) {
                    if (running) {
                        logger.log(Level.WARNING, "Connection failed: ", e);
                    }
                }
            }
        }
    }

    /**
     * Shuts down the server thread.
     */
    public void shutdown() {
        this.running = false;
 
        try {
            serverSocket.close();
            logger.fine("Closed server socket.");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not close the server socket!", e);
        }

        synchronized (shutdownLock) {
            // See run() above.
            logger.fine("Wait for Server.run to complete.");
        }

        for (Connection c : connections.values()) {
            if (c.isAlive()) c.close();
        }
        connections.clear();

        freeColServer.removeFromMetaServer();
        logger.fine("Server shutdown.");
    }
}
