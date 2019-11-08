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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.AttributeMessage;


/**
 * The type of message that contains server information.
 */
public class ServerInfoMessage extends AttributeMessage {

    private static final String ADDRESS_TAG = "address";
    private static final String CURRENTLY_PLAYING_TAG = "currentlyPlaying";
    private static final String GAME_STATE_TAG = "gameState";
    private static final String IS_GAME_STARTED_TAG = "isGameStarted";
    private static final String NAME_TAG = "name";
    private static final String PORT_TAG = "port";
    private static final String SLOTS_AVAILABLE_TAG = "slotsAvailable";
    private static final String VERSION_TAG = "version";


    /**
     * Create a new {@code ServerInfoMessage}.
     *
     * @param tag The tag for this message.
     * @param si The {@code ServerInfo} to encapsulate.
     */
    protected ServerInfoMessage(String tag, ServerInfo si) {
        super(tag,
              NAME_TAG, si.getName(),
              ADDRESS_TAG, si.getAddress(),
              PORT_TAG, String.valueOf(si.getPort()),
              SLOTS_AVAILABLE_TAG, String.valueOf(si.getSlotsAvailable()),
              CURRENTLY_PLAYING_TAG, String.valueOf(si.getCurrentlyPlaying()),
              IS_GAME_STARTED_TAG, String.valueOf(si.getIsGameStarted()),
              VERSION_TAG, si.getVersion(),
              GAME_STATE_TAG, String.valueOf(si.getGameState()));
    }

    /**
     * Create a new {@code ServerInfoMessage} from a stream.
     *
     * @param tag The actual message tag.
     * @param game The {@code Game}, which is null and ignored.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ServerInfoMessage(String tag, @SuppressWarnings("unused") Game game,
                             FreeColXMLReader xr) throws XMLStreamException {
        super(tag, xr, NAME_TAG, ADDRESS_TAG, PORT_TAG, SLOTS_AVAILABLE_TAG,
              CURRENTLY_PLAYING_TAG, IS_GAME_STARTED_TAG, VERSION_TAG,
              GAME_STATE_TAG);
    }


    // Public interface

    /**
     * Get the server information encapsulated in this message.
     *
     * @return The {@code ServerInfo} found.
     */
    public ServerInfo getServerInfo() {
        return new ServerInfo(getStringAttribute(NAME_TAG),
                              getStringAttribute(ADDRESS_TAG),
                              getIntegerAttribute(PORT_TAG, -1),
                              getIntegerAttribute(SLOTS_AVAILABLE_TAG, -1),
                              getIntegerAttribute(CURRENTLY_PLAYING_TAG, -1),
                              getBooleanAttribute(IS_GAME_STARTED_TAG, Boolean.FALSE),
                              getStringAttribute(VERSION_TAG),
                              getIntegerAttribute(GAME_STATE_TAG, -1));
    }

    /**
     * Get the address.
     *
     * @return The address attribute.
     */
    public String getAddress() {
        return getStringAttribute(ADDRESS_TAG);
    }

    /**
     * Override the address attribute.
     *
     * @param address The new address.
     */
    public void setAddress(String address) {
        setStringAttribute(ADDRESS_TAG, address);
    }

    /**
     * Get the port.
     *
     * @return The port attribute.
     */
    public int getPort() {
        return getIntegerAttribute(PORT_TAG, -1);
    }
}
