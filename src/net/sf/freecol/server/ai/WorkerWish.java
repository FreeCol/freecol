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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;


/**
 * Represents the need for a worker within a {@code Colony}.
 */
public final class WorkerWish extends Wish {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(WorkerWish.class.getName());

    public static final String TAG = "workerWish";

    /** The type of unit required. */
    private UnitType unitType;

    /** Whether the exact type is needed. */
    private boolean expertNeeded;


    /**
     * Creates a new uninitialized {@code WorkerWish} from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public WorkerWish(AIMain aiMain, String id) {
        super(aiMain, id);

        this.unitType = null;
        this.expertNeeded = false;
        this.initialized = false;
    }

    /**
     * Creates a new {@code WorkerWish}.
     *
     * @param aiMain The main AI-object.
     * @param destination The {@code Location} in which the
     *       {@link Wish#getTransportable transportable} assigned to
     *       this {@code WorkerWish} will have to reach.
     * @param value The value identifying the importance of
     *       this {@code Wish}.
     * @param unitType The type of unit needed for releasing this wish
     *       completely.
     * @param expertNeeded Determines wether the {@code unitType} is
     *       required or not.
     */
    public WorkerWish(AIMain aiMain, Location destination, int value,
                      UnitType unitType, boolean expertNeeded) {
        this(aiMain, TAG + ":" + aiMain.getNextId());

        if (destination == null) {
            throw new NullPointerException("destination == null: " + this);
        }

        this.destination = destination;
        setValue(value);
        this.unitType = unitType;
        this.expertNeeded = expertNeeded;
        setInitialized();
    }

    /**
     * Creates a new {@code WorkerWish} from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public WorkerWish(AIMain aiMain,
                      FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);

        setInitialized();
    }

    /**
     * {@inheritDoc}
     */
    public void setInitialized() {
        this.initialized = this.destination != null && this.unitType != null;
    }

    /**
     * Updates this {@code WorkerWish} with the given attributes.
     *
     * @param unitType The {@code UnitType} to wish for.
     * @param expertNeeded Is an expert unit required?
     * @param value The urgency of the wish.
     */
    public void update(UnitType unitType, boolean expertNeeded, int value) {
        setValue(value);
        this.unitType = unitType;
        this.expertNeeded = expertNeeded;
        if (transportable != null) transportable.setTransportPriority(value);
    }

    /**
     * Gets the type of unit needed for releasing this wish.
     *
     * @return The type of unit.
     */
    public UnitType getUnitType() {
        return unitType;
    }

    /**
     * Does a specified unit satisfy this wish?
     *
     * @param unit The {@code Unit} to test.
     * @return True if the unit either matches exactly if expertRequired,
     *     or at least matches in a land/naval sense if not.
     */
    public boolean satisfiedBy(Unit unit) {
        return (expertNeeded) 
            ? unit.getType() == unitType
            : unit.getType().isNaval() == unitType.isNaval();
    }

    /**
     * {@inheritDoc}
     */
    public <T extends AbstractGoods> boolean satisfiedBy(T goods) {
        return false;
    }


    // Override AIObject

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        if (unitType == null) {
            lb.add("\n  Wish unit without type: ", getId());
            result = result.fail();
        }
        return result;
    }


    // Serialization

    private static final String EXPERT_NEEDED_TAG = "expertNeeded";
    private static final String TRANSPORTABLE_TAG = "transportable";
    private static final String UNIT_TYPE_TAG = "unitType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(UNIT_TYPE_TAG, unitType);

        xw.writeAttribute(EXPERT_NEEDED_TAG, expertNeeded);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();
        final Specification spec = getSpecification();

        // Delegated from Wish
        transportable = (xr.hasAttribute(TRANSPORTABLE_TAG))
            ? xr.makeAIObject(aiMain, TRANSPORTABLE_TAG,
                              AIUnit.class, (AIUnit)null, true)
            : null;

        unitType = xr.getType(spec, UNIT_TYPE_TAG,
                              UnitType.class, (UnitType)null);
        
        expertNeeded = xr.getAttribute(EXPERT_NEEDED_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WorkerWish)) return false;
        WorkerWish other = (WorkerWish)o;
        return this.expertNeeded == other.expertNeeded
            && Utils.equals(this.unitType, other.unitType)
            && super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + Utils.hashCode(this.unitType);
        return 37 * hash + ((this.expertNeeded) ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("[", getId(),
            " ", ((unitType == null) ? "null" : unitType.getSuffix()),
            ((expertNeeded) ? "/expert" : ""),
            " -> ", destination, " (", getValue(), ")]");
        return lb.toString();
    }
}
