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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Element;


/**
 * The <code>Modifier</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Modifier extends Feature implements Cloneable {



    public static final int ADDITIVE = 0;
    public static final int MULTIPLICATIVE = 1;
    public static final int PERCENTAGE = 2;
    public static final int COMBINED = 3;
    
    /**
     * The type of this Modifier
     */
    private int type;

    /**
     * The float values of this modifier
     */
    private float values[] = {0, 1, 0};

    /**
     * A list of modifiers that contributed to this one.
     */
    private List<Modifier> modifiers;

    // -- Constructors --


    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param type an <code>int</code> value
     */
    private Modifier(String id, int type) {
        setId(id);
        this.type = type;
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, float value, int type) {
        this(id, null, value, type);
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, String source, float value, int type) {
        setId(id);
        setSource(source);
        setType(type);
        this.values[type] = value;
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, float value, String type) {
        this(id, null, value, type);
    }


    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the type of the modifier
     */
    public Modifier(String id, String source, float value, String type) {
        setId(id);
        setSource(source);
        setType(getTypeFromString(type));
        this.values[getType()] = value;
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
        setId(modifier.getId());
        setType(modifier.getType());
        setSource(modifier.getSource());
        setScopes(new ArrayList(modifier.getScopes()));
        if (modifier.getModifiers() != null) {
            modifiers = new ArrayList(modifier.getModifiers());
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = modifier.values[i];
        }
    }
    
    // -- Methods --

    /**
     * Get the <code>Modifiers</code> value.
     *
     * @return a <code>List<Modifier></code> value
     */
    public List<Modifier> getModifiers() {
        return modifiers;
    }

    /**
     * Set the <code>Modifiers</code> value.
     *
     * @param newModifiers The new Modifiers value.
     */
    public void setModifiers(final List<Modifier> newModifiers) {
        this.modifiers = newModifiers;
    }


    /**
     * Describe <code>getTypeFromString</code> method here.
     *
     * @param type a <code>String</code> value
     * @return an <code>int</code> value
     */
    protected int getTypeFromString(String type) {
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
    
    /**
     * Describe <code>getTypeAsString</code> method here.
     *
     * @return a <code>String</code> value
     */
    protected String getTypeAsString() {
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
     * Returns the XML tag name for this element.
     *
     * @return a <code>String</code> value
     */
    public static String getXMLElementTagName() {
        return "modifier";
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>float</code> value
     */
    public float getValue() {
        return values[getType()];
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final float newValue) {
        this.values[getType()] = newValue;
    }

    /**
     * Combines this modifier with another.
     *
     * @param otherModifier a <code>Modifier</code> value
     */
    public static Modifier combine(Modifier... modifiers) {
        switch(modifiers.length) {
        case 0:
            return null;
        case 1:
            return modifiers[0];
        default:
            String id = modifiers[0].getId();
            Modifier result = new Modifier(id, modifiers[0].type);
            result.modifiers = new ArrayList<Modifier>();

            for (Modifier modifier : modifiers) {
                if (!id.equals(modifier.getId())) {
                    return null;
                }
                if (result.type != modifier.type) {
                    result.setType(COMBINED);
                }
                if (modifier.modifiers == null) {
                    result.modifiers.add(modifier);
                } else {
                    result.modifiers.addAll(modifier.modifiers);
                }
                result.values[ADDITIVE] += modifier.values[ADDITIVE];
                result.values[PERCENTAGE] += modifier.values[PERCENTAGE];
                result.values[MULTIPLICATIVE] *= modifier.values[MULTIPLICATIVE];
            }
            return result;
        }
    }

    /**
     * Remove values of other Modifier from this one.
     *
     * @param modifier a <code>Modifier</code> value
     */
    public void removeValues(Modifier modifier) {
        values[ADDITIVE] -= modifier.values[ADDITIVE];
        values[PERCENTAGE] -= modifier.values[PERCENTAGE];
        values[MULTIPLICATIVE] /= modifier.values[MULTIPLICATIVE];
    }


    /**
     * Applies this Modifier to a number.
     *
     * @param number a <code>float</code> value
     * @return a <code>float</code> value
     */
    public float applyTo(float number) {
        float result = number + values[ADDITIVE];
        result *= values[MULTIPLICATIVE];
        result += (result * values[PERCENTAGE]) / 100;
        return result;
    }

    // -- Serialization --


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        setSource(in.getAttributeValue(null, "source"));
        setType(getTypeFromString(in.getAttributeValue(null, "type")));

        if (type == COMBINED) {
            values = readFromArrayElement("values", in, new float[0]);
        } else {
            values[getType()] = Float.parseFloat(in.getAttributeValue(null, "value"));
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("scope".equals(childName)) {
                Scope scope = new Scope(in);
                getScopes().add(scope);
            } else if ("modifier".equals(childName)) {
                Modifier modifier = new Modifier(in);
                if (modifiers == null) {
                    modifiers = new ArrayList<Modifier>();
                }
                modifiers.add(modifier);
            } else {
                logger.finest("Parsing of " + childName + " is not implemented yet");
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT ||
                       !in.getLocalName().equals(childName)) {
                    in.nextTag();
                }
            }
        }        

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

        out.writeAttribute("id", getId());
        if (getSource() != null) {
            out.writeAttribute("source", getSource());
        }
        out.writeAttribute("type", getTypeAsString());
        if (type == COMBINED) {
            toArrayElement("values", values, out);
        } else {
            out.writeAttribute("value", Float.toString(values[getType()]));
        }
        for (Scope scope : getScopes()) {
            scope.toXMLImpl(out);
        }
        if (modifiers != null) {
            for (Modifier modifier : getModifiers()) {
                modifier.toXMLImpl(out);
            }
        }

        out.writeEndElement();
    }
    
}
