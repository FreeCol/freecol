/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Game;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;


/**
 * The message sent to query and list the available servers.
 */
public final class ServerListMessage extends ObjectMessage {

    public static final String TAG = "serverList";

    /** The list of information about the available servers. */
    private List<ServerInfo> servers = new ArrayList<>();


    /**
     * Create a new {@code ServerListMessage}.  Used to generate
     * a request for servers.
     */
    public ServerListMessage() {
        super(TAG);

        this.servers.clear();
    }

    /**
     * Create a new {@code ServerListMessage} from a stream.
     *
     * @param game The {@code Game}, which is null and ignored.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ServerListMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        this();

        List<ServerInfo> svs = new ArrayList<>();
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (ServerInfo.TAG.equals(tag)) {
                svs.add(new ServerInfoMessage(tag, game, xr).getServerInfo());
            } else {
                expected(ServerInfo.TAG, tag);
            }
        }
        xr.expectTag(TAG);
        addServers(svs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        for (ServerInfo si : servers) {
            new ServerInfoMessage(ServerInfo.TAG, si).toXML(xw);
        }
    }


    // Public interface

    /**
     * Get the server information.
     *
     * @return The list of {@code ServerInfo}.
     */
    public List<ServerInfo> getServers() {
        return this.servers;
    }

    /**
     * Add information about a server.
     *
     * @param si The {@code ServerInfo} to add.
     * @return This message.
     */
    public ServerListMessage addServer(ServerInfo si) {
        this.servers.add(si);
        return this;
    }

    /**
     * Add information about several servers.
     *
     * @param lsi The list of {@code ServerInfo} to add.
     * @return This message.
     */
    public ServerListMessage addServers(List<ServerInfo> lsi) {
        this.servers.addAll(lsi);
        return this;
    }
}
