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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Specification;

import org.w3c.dom.Element;


/**
 * Represents the need for a worker within a <code>Colony</code>.
 */
public class WorkerWish extends Wish {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(WorkerWish.class.getName());

    /** The type of unit required. */
    private UnitType unitType;

    /** Whether the exact type is needed. */
    private boolean expertNeeded;


    /**
     * Creates a new uninitialized <code>WorkerWish</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public WorkerWish(AIMain aiMain, String id) {
        super(aiMain, id);

        unitType = null;
        expertNeeded = false;
    }

    /**
     * Creates a new <code>WorkerWish</code>.
     *
     * @param aiMain The main AI-object.
     * @param destination The <code>Location</code> in which the
     *       {@link Wish#getTransportable transportable} assigned to
     *       this <code>WorkerWish</code> will have to reach.
     * @param value The value identifying the importance of
     *       this <code>Wish</code>.
     * @param unitType The type of unit needed for releasing this wish
     *       completely.
     * @param expertNeeded Determines wether the <code>unitType</code> is
     *       required or not.
     */
    public WorkerWish(AIMain aiMain, Location destination, int value,
                      UnitType unitType, boolean expertNeeded) {
        this(aiMain, getXMLElementTagName() + ":" + aiMain.getNextId());

        if (destination == null) {
            throw new NullPointerException("destination == null");
        }

        this.destination = destination;
        setValue(value);
        this.unitType = unitType;
        this.expertNeeded = expertNeeded;
        uninitialized = false;
    }

    /**
     * Creates a new <code>WorkerWish</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation 
     *       of a <code>Wish</code>.
     */
    public WorkerWish(AIMain aiMain, Element element) {
        super(aiMain, element);

        uninitialized = unitType == null;
    }
    
    /**
     * Creates a new <code>WorkerWish</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public WorkerWish(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in);

        uninitialized = unitType == null;
    }


    /**
     * Updates this <code>WorkerWish</code> with the given attributes.
     *
     * @param value The urgency of the wish.
     * @param unitType The <code>UnitType</code> to wish for.
     * @param expertNeeded Is an expert unit required?
     */
    public void update(UnitType unitType, boolean expertNeeded, int value) {
        setValue(value);
        this.unitType = unitType;
        this.expertNeeded = expertNeeded;
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
     * @param unit The <code>Unit</code> to test.
     * @return True if the unit either matches exactly if expertRequired,
     *     or at least matches in a land/naval sense if not.
     */
    public boolean satisfiedBy(Unit unit) {
        return (expertNeeded) 
            ? unit.getType() == unitType
            : unit.getType().isNaval() == unitType.isNaval();
    }


    // Override AIObject

    /**
     * Checks the integrity of this AI object.
     *
     * @return True if the <code>WorkerWish</code> is valid.
     */
    @Override
    public boolean checkIntegrity() {
        return super.checkIntegrity()
            && unitType != null;
    }


    // Serialization

    private static final String EXPERT_NEEDED_TAG = "expertNeeded";
    private static final String UNIT_TYPE_TAG = "unitType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, UNIT_TYPE_TAG, unitType);

        writeAttribute(out, EXPERT_NEEDED_TAG, expertNeeded);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        final Specification spec = getSpecification();

        unitType = spec.getType(in, UNIT_TYPE_TAG,
                                UnitType.class, (UnitType)null);
        
        expertNeeded = getAttribute(in, EXPERT_NEEDED_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        super.readChildren(in);

        if (unitType != null) uninitialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId())
            .append(" for ").append(destination)
            .append(" ").append((unitType == null) ? "null" : unitType.getNameKey())
            .append(" (").append(getValue())
            .append((expertNeeded) ? ", expert" : "")
            .append(")]");
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "workerWish"
     */
    public static String getXMLElementTagName() {
        return "workerWish";
    }
}
