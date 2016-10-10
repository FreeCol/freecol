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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.metaserver.ServerInfo;

import org.w3c.dom.Element;


/**
 * The message sent to remove a server from the meta-server.
 */
public class RemoveServerMessage extends ServerInfoMessage {

    public static final String TAG = "remove";


    /**
     * Create a new {@code RemoveServerMessage}.
     *
     * @param conn A {@code Connection} to the meta-server.
     */
    public RemoveServerMessage(Connection conn) {
        super(TAG, new ServerInfo(null,
                                  conn.getHostAddress(),
                                  conn.getSocket().getPort(),
                                  0, 0, false, null, 0));
    }

    /**
     * Create a new {@code RemoveServerMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public RemoveServerMessage(@SuppressWarnings("unused") Game game,
                               Element element) {
        super(TAG, element);
    }
}
