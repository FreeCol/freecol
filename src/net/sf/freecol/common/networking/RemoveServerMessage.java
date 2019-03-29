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
import net.sf.freecol.common.networking.Connection;


/**
 * The message sent to remove a server from the meta-server.
 */
public class RemoveServerMessage extends ServerInfoMessage {

    public static final String TAG = "remove";


    /**
     * Create a new {@code RemoveServerMessage}.
     *
     * @param si The {@code ServerInfo} describing the server to remove.
     */
    public RemoveServerMessage(ServerInfo si) {
        super(TAG, si);
    }

    /**
     * Create a new {@code RemoveServerMessage} from a stream.
     *
     * @param game The {@code Game}, which is null and ignored.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public RemoveServerMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, game, xr);
    }
}
