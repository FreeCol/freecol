/*
 *  MessageHandler.java - Handles complete incoming messages.
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
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
    * @param connection The connection the message came from.
    * @param element The element to handle.
    * @return The reply (if any) or <i>null</i>.
    * @throws FreeColException
    */
    public Element handle(Connection connection, Element element) throws FreeColException;

}
