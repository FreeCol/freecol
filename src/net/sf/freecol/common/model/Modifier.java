
package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Element;


/**
 * The <code>Modifier</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Modifier extends FreeColObject implements Cloneable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int ADDITIVE = 0;
    public static final int MULTIPLICATIVE = 1;
    public static final int PERCENTAGE = 2;
    public static final int COMBINED = 3;
    
    /**
     * The ID of the modifier, used to look up name, etc.
     */
    private String id;

    /**
     * The type of this modifier
     */
    private int type;

    /**
     * The value of this modifier
     */
    private float values[] = {0, 1, 0};



    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, float value, int type) {
        this.id = id;
        this.type = type;
        this.values[this.type] = value;
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, float value, String type) {
        this.id = id;
        this.type = getTypeFromString(type);
        this.values[this.type] = value;
    }
    
    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param element an <code>Element</code> value
     */
    public Modifier(Element element) {
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Modifier(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }
    
    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param modifier a <code>Modifier</code> value
     */
    public Modifier(Modifier modifier) {
        id = modifier.id;
        type = modifier.type;
        for (int i = 0; i < values.length; i++) {
            values[i] = modifier.values[i];
        }
    }
    
    private int getTypeFromString(String type) {
        if ("additive".equals(type)) {
            return ADDITIVE;
        } else if ("multiplicative".equals(type)) {
            return MULTIPLICATIVE;
        } else if ("percentage".equals(type)) {
            return PERCENTAGE;
        } else if ("combined".equals(type)) {
            return COMBINED;
        } else {
            throw new IllegalArgumentException("Unknown type");
        }
    }
    
    private String getTypeAsString() {
        switch(getType()) {
        case ADDITIVE:
            return "additive";
        case MULTIPLICATIVE:
            return "multiplicative";
        case PERCENTAGE:
            return "percentage";
        case COMBINED:
            return "combined";
        default:
            // It can't happen
            return null;
        }
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        id = in.getAttributeValue(null, "id");
        type = getTypeFromString(in.getAttributeValue(null, "type"));
        if (type == COMBINED) {
            values = readFromArrayElement("values", in, new float[0]);
        } else {
            values[type] = Float.parseFloat(in.getAttributeValue(null, "value"));
        }
        
        in.nextTag();
    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("id", id);
        out.writeAttribute("type", getTypeAsString());
        if (type == COMBINED) {
            toArrayElement("values", values, out);
        } else {
            out.writeAttribute("value", Float.toString(values[type]));
        }

        out.writeEndElement();
    }
    
    public static String getXMLElementTagName() {
        return "modifier";
    }

    /**
     * Get the <code>Id</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
    }

    /**
     * Set the <code>Id</code> value.
     *
     * @param newId The new Id value.
     */
    public void setId(final String newId) {
        this.id = newId;
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public void setType(final int newType) {
        this.type = newType;
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>float</code> value
     */
    public float getValue() {
        return values[type];
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final float newValue) {
        this.values[type] = newValue;
    }

    /**
     * Returns the inverse modifier of this one
     *
     * @return a inverse <code>Modifier</code>
     */
    public Modifier getInverse() {
        Modifier newModifier = new Modifier(this);

        if (type == COMBINED) {
            for (int i = 0; i < values.length; i++) {
                newModifier.values[i] = getInverse(i);
            }
        } else {
            newModifier.values[type] = getInverse(type);
        }
        return newModifier;
    }
    
    private float getInverse(int type) {
        switch(type) {
        case ADDITIVE:
        case PERCENTAGE:
            return -values[type];
        case MULTIPLICATIVE:
            return 1 / values[type];
        default:
            // It can't happen
            return values[type];
        }
    }
    
    /**
     * Combines this modifier with another.
     *
     * @param otherModifier a <code>Modifier</code> value
     */
    public void combine(Modifier otherModifier) {
        if (type != otherModifier.getType()) {
            type = COMBINED;
        }
        
        if (otherModifier.getType() != COMBINED) {
            combine(otherModifier, otherModifier.getType());
        } else {
            for (int i = 0; i < values.length; i++) {
                combine(otherModifier, i);
            }
        }
    }
    
    private void combine(Modifier otherModifier, int type) {
        switch(type) {
        case ADDITIVE:
        case PERCENTAGE:
            values[type] += otherModifier.values[type];
            return;
        case MULTIPLICATIVE:
            values[type] *= otherModifier.values[type];
            return;
        }
    }
 
    /**
     * Applies this modifier to a number.
     *
     * @param number a <code>float</code> value
     * @return a <code>float</code> value
     */
    public float applyTo(float number) {
        if (type == COMBINED) {
            for (int i = 0; i < values.length; i++) {
                number = applyTo(number, i);
            }
            return number;
        } else {
            return applyTo(number, type);
        }
    }
    
    private float applyTo(float number, int type) {
        switch(type) {
        case ADDITIVE:
            return number + values[type];
        case MULTIPLICATIVE:
            return number * values[type];
        case PERCENTAGE:
            return number + (number * values[type]) / 100;
        default:
            return number;
        }
    }
}
