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

package net.sf.freecol.metaserver;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DOMMessageHandler;
import net.sf.freecol.common.networking.ServerListMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;


/**
 * Wrapper class to contain utility functions used by the FreeColServer to
 * talk to the meta-server.
 */
public class MetaServerUtils {

    private static final Logger logger = Logger.getLogger(MetaServerUtils.class.getName());

    /**
     * Handle messages sent by the meta-server.
     * Currently, only "serverList".
     */
    public static class MetaInputHandler implements DOMMessageHandler {

        private static final Logger logger = Logger.getLogger(MetaInputHandler.class.getName());
        
        /** Callback to swallow server lists. */
        public final Consumer<List<ServerInfo>> consumer;


        /**
         * Create a new MetaInputHandler with a given consumer.
         *
         * @param consumer The {@code Consumer} to swallow the server list.
         */
        public MetaInputHandler(Consumer<List<ServerInfo>> consumer) {
            this.consumer = consumer;
        }

        /**
         * {@inheritDoc}
         */
        public Element handle(Connection connection, Element element)
            throws FreeColException {
            if (element == null) return null;
            final String tag = element.getTagName();
            switch (tag) {
            case TrivialMessage.DISCONNECT_TAG:
                disconnect();
                break;
            case ServerListMessage.TAG:
                serverList(new ServerListMessage(element));
                break;
            default:
                logger.warning("MetaInputHandler does not handle: " + tag);
                break;
            }
            return null;
        }

        /**
         * Handle a "disconnect"-message.
         */
        public void disconnect() {} // Do nothing
        
        /**
         * Handle a "serverList"-message.
         */
        public void serverList(ServerListMessage message) {
            this.consumer.accept(message.getServers());
        }            
    }

    /** Sentinel server info to allow check for activity. */
    private static final ServerInfo sentinel = new ServerInfo(null,
        null, -1, -1, -1, false, null, -1);

    /** Error message type. */
    public static final String NO_ROUTE_TO_SERVER = "noRouteToServer";
    
    /** Client timer update interval. */
    private static final int UPDATE_INTERVAL = 60000;


    /**
     * Utility to get a connection to the meta-server and handle the
     * server list it returns.
     *
     * @param si A list of {@code ServerInfo} records, to be filled in
     *     by the meta-server.     
     * @return A {@code Connection}, or null on failure.
     */
    public static Connection getMetaServerConnection(List<ServerInfo> si) {
        // Create a consumer for the response to meta-server
        // "serverList" messages.  Most of the time we do not care, so
        // the default is to do nothing, however if we have a non-null
        // server info list, arrange to fill it.
        Consumer<List<ServerInfo>> consumer = (List<ServerInfo> lsi) -> {};
        if (si != null) {
            si.clear();
            si.add(sentinel);
            consumer = (lsi) -> {
                si.clear();
                if (lsi != null) si.addAll(lsi);
            };
        }

        String host = FreeCol.getMetaServerAddress();
        int port = FreeCol.getMetaServerPort();
        try {
            return new Connection(host, port, new MetaInputHandler(consumer),
                                  FreeCol.SERVER_THREAD);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Could not connect to meta-server: "
                + host + ":" + port, ioe);
        }
        return null;
    }

    /**
     * Gets a list of servers from the meta server.
     *
     * @return A list of {@link ServerInfo} objects, or null on error.
     */
    public static List<ServerInfo> getServerList() {
        List<ServerInfo> si = new ArrayList<>();
        Connection mc = getMetaServerConnection(si);
        if (mc == null) return null;
        try {
            mc.ask(new ServerListMessage());
            return (si.size() == 1 && si.get(0) == sentinel) ? null : si;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Meta-server did not list servers: "
                + FreeCol.getMetaServerAddress()
                + ":" + FreeCol.getMetaServerPort(), ioe);
        } finally {
            mc.close();
        }
        return null;
    }

    /**
     * Utility to start an update timer for a given server.
     *
     * @param freeColServer The {@code FreeColServer} to send update messages
     *     to the meta-server.
     * @return The {@code Timer} that was started, so that it can be cancelled
     *     if no longer needed.
     */
    public static Timer startUpdateTimer(FreeColServer freeColServer) {
        // This update is really a "Hi! I am still here!"-message,
        // since an additional update should be sent when a new
        // player is added to/removed from this server etc.
        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!freeColServer.updateMetaServer(false)) cancel();
                }
            }, UPDATE_INTERVAL, UPDATE_INTERVAL);
        return t;
    }
}
