/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;


/**
 * Handles complete incoming messages.
 */
public interface MessageHandler {
    
    /**
     * Handles the main element of an XML message.
     *
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> to handle.
     * @return The reply (if any) or <i>null</i>.
     * @throws FreeColException
     */
    public Element handle(Connection connection, Element element)
        throws FreeColException;
}
