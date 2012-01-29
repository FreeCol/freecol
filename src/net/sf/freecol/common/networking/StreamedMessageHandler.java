/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;



/**
* Handles streamed incoming messages.
*/
public interface StreamedMessageHandler {
    
    /**
     * Handles the main element of an XML message.
     *
     * @param connection The connection the message came from.
     * @param in The stream containing the message.
     * @param out The output stream for the reply.
     */
    public void handle(Connection connection, XMLStreamReader in, XMLStreamWriter out);
    
    /**
     * Checks if the message handler support the given message.
     * @param tagName The tag name of the message to check.
     * @return The result.
     */
    public boolean accepts(String tagName);

}
