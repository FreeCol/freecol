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
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.metaserver.MetaItem;
import net.sf.freecol.metaserver.MetaRegister;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;


/**
 * The message sent when to get a list of servers.
 */
public class ServerListMessage extends DOMMessage {

    public static final String TAG = "serverList";
    private static final String ADDRESS_TAG = "address";
    private static final String CURRENTLY_PLAYING_TAG = "currentlyPlaying";
    private static final String GAME_STATE_TAG = "gameState";
    private static final String IS_GAME_STARTED_TAG = "isGameStarted";
    private static final String NAME_TAG = "name";
    private static final String PORT_TAG = "port";
    private static final String SLOTS_AVAILABLE_TAG = "slotsAvailable";
    private static final String VERSION_TAG = "version";

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
        this.servers.addAll(mapChildren(element,
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
        mi.update(getStringAttribute(e, NAME_TAG),
            getStringAttribute(e, ADDRESS_TAG),
            getIntegerAttribute(e, PORT_TAG, -1),
            getIntegerAttribute(e, SLOTS_AVAILABLE_TAG, -1),
            getIntegerAttribute(e, CURRENTLY_PLAYING_TAG, -1),
            getBooleanAttribute(e, IS_GAME_STARTED_TAG, false),
            getStringAttribute(e, VERSION_TAG),
            getIntegerAttribute(e, GAME_STATE_TAG, -1));
        return mi;
    }

    /**
     * Convert a ServerInfo record to a message.
     *
     * @param si The <code>ServerInfo</code> to convert.
     * @return A new <code>DOMMessage</code>.
     */
    private static DOMMessage serverInfoToMessage(ServerInfo si) {
        return new DOMMessage(si.getTagName(),
            NAME_TAG, si.getName(),
            ADDRESS_TAG, si.getAddress(),
            PORT_TAG, Integer.toString(si.getPort()),
            SLOTS_AVAILABLE_TAG, Integer.toString(si.getSlotsAvailable()),
            CURRENTLY_PLAYING_TAG, Integer.toString(si.getCurrentlyPlaying()),
            IS_GAME_STARTED_TAG, Boolean.toString(si.getIsGameStarted()),
            VERSION_TAG, si.getVersion(),
            GAME_STATE_TAG, Integer.toString(si.getGameState()));
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
     * @param <T> The <code>ServerInfo</code> type to add.
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
        return new DOMMessage(getTagName())
            .addMessages(toList(map(this.servers,
                                    ServerListMessage::serverInfoToMessage)))
            .toXMLElement();
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
