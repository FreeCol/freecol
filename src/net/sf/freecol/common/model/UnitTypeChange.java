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

import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Simple container for individual type changes.
 */
public class UnitTypeChange extends FreeColSpecObjectType {

    public static final String TAG = "unit-type-change";
    
    /** The unit type to change from. */
    public UnitType from;

    /** The unit type to change to. */
    public UnitType to;

    /** The percentage chance of the change occurring. */
    public int probability;

    /** The number of turns for the change to take, if not immediate. */
    public int turns;

    // @compat 0.11.6
    private static AtomicInteger fakeIdIndex = new AtomicInteger(0);
    // end @compat 0.11.6


    /**
     * Trivial constructor.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to use.
     */
    public UnitTypeChange(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Read a unit change from a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param spec The {@code Specification} to read within.
     * @exception XMLStreamException if there is a problem reading
     *     the stream.
     */
    public UnitTypeChange(FreeColXMLReader xr, Specification spec)
        throws XMLStreamException {
        this(xr.readId(), spec);

        readFromXML(xr);
    }


    /**
     * Helper to check if a change is available to a player.
     * This is useful when the change involves a transfer of ownership.
     *
     * @param player The {@code Player} to test.
     * @return True if the player can use the to-unit-type.
     */
    public boolean isAvailableTo(Player player) {
        return this.to.isAvailableTo(player);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        UnitTypeChange o = copyInCast(other, UnitTypeChange.class);
        if (o == null || !super.copyIn(o)) return false;
        this.from = o.from;
        this.to = o.to;
        this.probability = o.probability;
        this.turns = o.turns;
        return true;
    }


    // Serialization

    private static final String FROM_TAG = "from";
    private static final String PROBABILITY_TAG = "probability";
    private static final String TO_TAG = "to";
    private static final String TURNS_TAG = "turns";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);
        
        xw.writeAttribute(FROM_TAG, this.from);

        xw.writeAttribute(TO_TAG, this.to);

        xw.writeAttribute(PROBABILITY_TAG, this.probability);

        if (this.turns > 0) xw.writeAttribute(TURNS_TAG, this.turns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        
        super.readAttributes(xr);
    
        this.from = xr.getType(spec, FROM_TAG, UnitType.class, (UnitType)null);

        this.to = xr.getType(spec, TO_TAG, UnitType.class, (UnitType)null);

        this.probability = xr.getAttribute(PROBABILITY_TAG, 0);

        this.turns = xr.getAttribute(TURNS_TAG, -1);

        // @compat 0.11.6

        // UnitTypeChange became a FreeColSpecObjectType in 0.11.6, and
        // thus gained an identifier.  Make sure we provide one,
        // even if is fake.  This is mostly fixed by the compatibility
        // fragment load in Specification.fixUnitChanges, but that does not
        // handle mods.
        String id = getId();
        if (id == null || "".equals(id)) {
            setId("model.unitChange.faked." + fakeIdIndex.incrementAndGet());
        }
        // end @compat 0.11.6
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(this.from.getSuffix())
            .append("->").append(this.to.getSuffix())
            .append('/').append(this.probability);
        if (this.turns > 0) sb.append('/').append(this.turns);
        return sb.toString();
    }            
}
