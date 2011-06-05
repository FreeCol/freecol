/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import net.sf.freecol.common.model.UnitType;

import org.w3c.dom.Element;


/**
 * Represents the need for a worker within a <code>Colony</code>.
 */
public class WorkerWish extends Wish {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(WorkerWish.class.getName());

    private UnitType unitType;
    private boolean expertNeeded;


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
    *       completly.
    * @param expertNeeded Determines wether the <code>unitType</code> is
    *       required or not.
    */
    public WorkerWish(AIMain aiMain, Location destination, int value, UnitType unitType, boolean expertNeeded) {
        super(aiMain, getXMLElementTagName() + ":" + aiMain.getNextID());

        if (destination == null) {
            throw new NullPointerException("destination == null");
        }

        this.destination = destination;
        setValue(value);
        this.unitType = unitType;
        this.expertNeeded = expertNeeded;
    }


    /**
    * Creates a new <code>WorkerWish</code> from the given
    * XML-representation.
    *
    * @param aiMain The main AI-object.
    * @param element The root element for the XML-representation
    *       of a <code>WorkerWish</code>.
    */
    public WorkerWish(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>WorkerWish</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param id The unique ID of this object.
     */
    public WorkerWish(AIMain aiMain, String id) {
        super(aiMain, id);
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
        super(aiMain, in.getAttributeValue(null, "ID"));
        readFromXML(in);
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
    * Returns the type of unit needed for releasing this wish.
    * @return The type of unit.
    */
    public UnitType getUnitType() {
        return unitType;
    }

    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        if (destination == null) {
            // Avoid writing corrupt WorkerWish, mitigating #3084370.
            return;
        }

        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
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
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *      from the stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        destination = (Location) getAIMain().getFreeColGameObject(in.getAttributeValue(null, "destination"));

        final String transportableStr = in.getAttributeValue(null, "transportable");
        if (transportableStr != null) {
            transportable = (Transportable) getAIMain().getAIObject(transportableStr);
            if (transportable == null) {
                transportable = new AIUnit(getAIMain(), transportableStr);
            }
        } else {
            transportable = null;
        }
        setValue(Integer.parseInt(in.getAttributeValue(null, "value")));

        unitType = getAIMain().getGame().getSpecification().getUnitType(in.getAttributeValue(null, "unitType"));
        expertNeeded = Boolean.valueOf(in.getAttributeValue(null, "expertNeeded")).booleanValue();
        in.nextTag();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "workerWish"
    */
    public static String getXMLElementTagName() {
        return "workerWish";
    }

    public String toString() {
        return "WorkerWish: " + unitType.getNameKey()
            + " (" + getValue() + (expertNeeded ? ", expert)" : ")");
    }
}
