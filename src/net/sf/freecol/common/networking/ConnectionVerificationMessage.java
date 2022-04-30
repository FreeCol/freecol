/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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
import net.sf.freecol.common.model.Game;


/**
 * A verification message sent from the meta server as a reply to {@link RegisterServerMessage}.
 */
public class ConnectionVerificationMessage extends AttributeMessage {

    public static final String TAG = "connectionVerification";

    private static final String CONNECTABLE_TAG = "connectable";
    

    /**
     * Create a new {@code ConnectionVerificationMessage}.
     *
     * @param connectable {@code true}, if the server could be connected to from the meta server.
     */
    public ConnectionVerificationMessage(boolean connectable) {
        super(TAG, CONNECTABLE_TAG, Boolean.toString(connectable));
    }

    /**
     * Create a new {@code ConnectionVerificationMessage} from a stream.
     *
     * @param game The {@code Game}, which is null and ignored.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ConnectionVerificationMessage(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(TAG, xr, CONNECTABLE_TAG);
    }
    
   
    public boolean isConnectable() {
        return super.getBooleanAttribute(CONNECTABLE_TAG, false);        
    }
}
