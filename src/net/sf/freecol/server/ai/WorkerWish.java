
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Represents the need for a worker within a <code>Colony</code>.
*/
public class WorkerWish extends Wish {
    private static final Logger logger = Logger.getLogger(WorkerWish.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private int unitType;
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
    public WorkerWish(AIMain aiMain, Location destination, int value, int unitType, boolean expertNeeded) {
        super(aiMain, getXMLElementTagName() + ":" + aiMain.getNextID());

        if (destination == null) {
            throw new NullPointerException("destination == null");
        }
        
        this.destination = destination;
        this.value = value;
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
    public void update(int value, int unitType, boolean expertNeeded) {
        this.value = value;
        this.unitType = unitType;
        this.expertNeeded = expertNeeded;
    }

    /**
    * Returns the type of unit needed for releasing this wish.
    * @return The {@link Unit#getType type of unit}.
    */
    public int getUnitType() {
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
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getID());
        out.writeAttribute("destination", destination.getID());
        if (transportable != null) {
            out.writeAttribute("transportable", transportable.getID());
        }
        out.writeAttribute("value", Integer.toString(value));

        out.writeAttribute("unitType", Integer.toString(unitType));
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
        id = in.getAttributeValue(null, "ID");
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
        value = Integer.parseInt(in.getAttributeValue(null, "value"));

        unitType = Integer.parseInt(in.getAttributeValue(null, "unitType"));
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
        return "WorkerWish: " + Unit.getName(unitType) + " (" + expertNeeded + ")";
    }
}
