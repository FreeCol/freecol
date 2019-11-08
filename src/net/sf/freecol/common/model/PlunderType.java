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

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.RandomRange;


/**
 * The plunder available from a settlement.
 */
public class PlunderType extends FreeColSpecObjectType {

    public static final String TAG = "plunder";

    /** The range of plunder amounts. */
    private RandomRange plunder;
    
    /**
     * Creates a new plunder type.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public PlunderType(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Creates a new plunder type.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param specification The {@code Specification} to refer to.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    public PlunderType(FreeColXMLReader xr,
                       Specification specification) throws XMLStreamException {
        super(specification);

        readFromXML(xr);
    }


    /**
     * Gets the plunder range available for the supplied unit.
     *
     * @return The plunder range, or null if none applicable.
     */
    public final RandomRange getPlunder() {
        return this.plunder;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        PlunderType o = copyInCast(other, PlunderType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.plunder = o.getPlunder();
        return true;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (this.plunder != null) this.plunder.writeAttributes(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.plunder = new RandomRange(xr);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
