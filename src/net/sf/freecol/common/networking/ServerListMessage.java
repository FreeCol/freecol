/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.metaserver.MetaItem;
import net.sf.freecol.metaserver.MetaRegister;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;


/**
 * The message sent when to get a list of servers.
 */
public class ServerListMessage extends DOMMessage {

    public static final String TAG = "serverList";

    /** The list of information about the available servers. */
    private List<ServerInfo> servers = new ArrayList<>();


    /**
     * Create a new <code>ServerListMessage</code>.  Used to generate
     * a request for servers.
     */
    public ServerListMessage() {
        super(getTagName());
    }

    /**
     * Create a new <code>ServerListMessage</code> from a
     * <code>MetaRegister</code>.  Used to generate the reply.
     *
     * @param mr The <code>MetaRegister</code> to query for servers.
     */
    public ServerListMessage(MetaRegister mr) {
        super(getTagName());

        this.servers.clear();
        this.servers.addAll(mr.getServers());
    }
        
    /**
     * Create a new <code>ServerListMessage</code> from a
     * supplied element.  Used to read the reply.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ServerListMessage(Game game, Element element) {
        super(getTagName());

        this.servers.clear();
        this.servers.addAll(mapChildren(game, element,
                                        ServerListMessage::elementToServer));
    }

    /**
     * Convert an element to a server.
     *
     * @param e The <code>Element</code> to examine.
     * @return A new <code>MetaItem</code> describing a server.
     */     
    private static MetaItem elementToServer(Element e) {
        MetaItem mi = new MetaItem();
        mi.update(e.getAttribute("name"),
            e.getAttribute("address"),
            Integer.parseInt(e.getAttribute("port")),
            Integer.parseInt(e.getAttribute("slotsAvailable")),
            Integer.parseInt(e.getAttribute("currentlyPlaying")),
            Boolean.parseBoolean(e.getAttribute("slotsAvailable")),
            e.getAttribute("version"),
            Integer.parseInt(e.getAttribute("gameState")));
        return mi;
    }

    // Public interface

    /**
     * Get the server information.
     *
     * @return The list of <code>ServerInfo</code>.
     */
    public List<ServerInfo> getServers() {
        return this.servers;
    }

    /**
     * Add information about a server.
     *
     * @param si The <code>ServerInfo</code> to add.
     */
    public <T extends ServerInfo> void addServer(T si) {
        this.servers.add(si);
    }

    /**
     * Handle a "serverList"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return Null.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        // Not needed, serverList messages are handled trivially in
        // metaregister.NetworkHandler.
        return null;
    }

    /**
     * Convert this ServerListMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        DOMMessage result = new DOMMessage(getTagName());
        for (ServerInfo si : this.servers) {
            result.add(new DOMMessage(si.getTagName(),
                    "name", si.getName(),
                    "address", si.getAddress(),
                    "port", Integer.toString(si.getPort()),
                    "slotsAvailable", Integer.toString(si.getSlotsAvailable()),
                    "currentlyPlaying", Integer.toString(si.getCurrentlyPlaying()),
                    "isGameStarted", Boolean.toString(si.getIsGameStarted()),
                    "version", si.getVersion(),
                    "gameState", Integer.toString(si.getGameState())));
        }
        return result.toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "serverList".
     */
    public static String getTagName() {
        return TAG;
    }
}
