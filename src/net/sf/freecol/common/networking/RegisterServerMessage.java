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


/**
 * The message sent to register a server from the meta-server.
 */
public class RegisterServerMessage extends ServerInfoMessage {

    public static final String TAG = "register";


    /**
     * Create a new {@code RegisterServerMessage}.
     *
     * @param si The {@code ServerInfo} to encapsulate.
     */
    public RegisterServerMessage(ServerInfo si) {
        super(TAG, si);
    }

    /**
     * Create a new {@code RegisterServerMessage} from a stream.
     *
     * @param game The {@code Game}, which is null and ignored.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public RegisterServerMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, game, xr);
    }
}
