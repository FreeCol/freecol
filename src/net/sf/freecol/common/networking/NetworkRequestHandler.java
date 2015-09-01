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

import org.w3c.dom.Element;


/**
 * A network request handler knows how to handle in a given request type.
 */
public interface NetworkRequestHandler {

    /**
     * Handle a request represented by an {@link Element} and return another
     * {@link Element} or null as the answer.
     * 
     * @param connection The message's <code>Connection</code>.
     * @param element The root <code>Element</code> of the message.
     * @return The reply <code>Element</code>, which may be null.
     */
    Element handle(Connection connection, Element element);
}
