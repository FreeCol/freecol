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
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when to get a list of servers.
 */
public class ServerListMessage extends DOMMessage {

    public static final String TAG = "serverList";
    
    /** The list of information about the available servers. */
    private List<ServerInfo> serverInfo = new ArrayList<>();


    /**
     * Create a new <code>ServerListMessage</code>.
     */
    public ServerListMessage() {
        super(getTagName());
    }

    /**
     * Create a new <code>ServerListMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ServerListMessage(Game game, Element element) {
        super(getTagName());

        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element)nl.item(i);
            ServerInfo si = new ServerInfo(element.getAttribute("name"),
                element.getAttribute("address"),
                Integer.parseInt(element.getAttribute("port")),
                Integer.parseInt(element.getAttribute("slotsAvailable")),
                Integer.parseInt(element.getAttribute("currentlyPlaying")),
                Boolean.parseBoolean(element.getAttribute("slotsAvailable")),
                element.getAttribute("version"),
                Integer.parseInt(element.getAttribute("gameState")));
            serverInfo.add(si);
        }
    }


    // Public interface

    /**
     * Get the server information.
     *
     * @return The list of <code>ServerInfo</code>.
     */
    public List<ServerInfo> getServers() {
        return this.serverInfo;
    }

    /**
     * Add information about a server.
     *
     * @param si The <code>ServerInfo</code> to add.
     */
    public <T extends ServerInfo> void addServer(T si) {
        this.serverInfo.add(si);
    }


    /**
     * Handle a "serverList"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the server info.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        // Not needed, serverList messages are only sent by the server
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
        for (ServerInfo si : this.serverInfo) {
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
