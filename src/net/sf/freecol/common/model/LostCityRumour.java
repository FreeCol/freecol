/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.Specification;

/**
 * Represents a lost city rumour.
 */
public class LostCityRumour extends FreeColGameObjectType {

    /** Constants describing types of Lost City Rumours. */
    public static final int NO_SUCH_RUMOUR = -1,
        BURIAL_GROUND = 0,
        EXPEDITION_VANISHES = 1, 
        NOTHING = 2,
        LEARN = 3,
        TRIBAL_CHIEF = 4,
        COLONIST = 5,
        TREASURE = 6,
        FOUNTAIN_OF_YOUTH = 7;

    public static final int NUMBER_OF_RUMOURS = 8;

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        // this class is really just an enum, but needs to inherit from FreeColGameObjectType
    }

    protected void readFromXML(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        // this class is really just an enum, but needs to inherit from FreeColGameObjectType
    }

}


