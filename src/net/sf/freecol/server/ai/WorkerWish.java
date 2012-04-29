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

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

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
     * @param id The unique ID of this object.
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

        uninitialized = getDestination() == null;
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
    public WorkerWish(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in);

        uninitialized = getDestination() == null;
    }


    /**
     * Updates this <code>WorkerWish</code> with the
     * given attributes.
     *
     * @param value The value identifying the importance of
     *       this <code>Wish</code>.
     * @param unitType The type of unit needed for releasing this wish
     *       completly.
     * @param expertNeeded Determines wether the <code>unitType</code> is
     *       required or not.
     */
    public void update(int value, UnitType unitType, boolean expertNeeded) {
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

    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE, getId());

        out.writeAttribute("destination", destination.getId());

        if (transportable != null) {
            out.writeAttribute("transportable", transportable.getId());
        }

        out.writeAttribute("value", Integer.toString(getValue()));

        out.writeAttribute("unitType", unitType.getId());

        out.writeAttribute("expertNeeded", Boolean.toString(expertNeeded));

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *      from the stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        final AIMain aiMain = getAIMain();

        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        String str = in.getAttributeValue(null, "destination");
        destination = aiMain.getGame().getFreeColLocation(str);

        if ((str = in.getAttributeValue(null, "transportable")) != null) {
            if ((transportable = (AIUnit)aiMain.getAIObject(str)) == null) {
                transportable = new AIUnit(aiMain, str);
            }
        } else {
            transportable = null;
        }

        setValue(getAttribute(in, "value", -1));

        str = in.getAttributeValue(null, "unitType");

        unitType = getSpecification().getUnitType(str);

        expertNeeded = getAttribute(in, "expertNeeded", false);

        in.nextTag();
    }

    /**
     * {@inherit-doc}
     */
    @Override
    public String toString() {
        return "[" + getId() + " " + unitType.getNameKey()
            + " (" + getValue() + (expertNeeded ? ", expert" : "") + "]";
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "workerWish"
     */
    public static String getXMLElementTagName() {
        return "workerWish";
    }
}
