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


import org.w3c.dom.Element;


/**
 * The <code>Modifier</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat. The Modifier may be applicable only to certain Objects
 * specified by means of <code>Scope</code> objects.
 */
public final class Modifier extends Feature implements Comparable<Modifier> {

    public static final String OFFENCE = "model.modifier.offence";
    public static final String DEFENCE = "model.modifier.defence";
    public static final String CAPITAL_DEFENCE = "model.modifier.capitalDefence";
    public static final String SETTLEMENT_DEFENCE = "model.modifier.settlementDefence";

    public static final float UNKNOWN = Float.MIN_VALUE;

    public static enum Type { ADDITIVE, MULTIPLICATIVE, PERCENTAGE }

    // index values for common modifier types
    public static int BASIC_PRODUCTION_INDEX = 0;
    public static int COLONY_PRODUCTION_INDEX = 10;
    public static int EXPERT_PRODUCTION_INDEX = 20;
    public static int FATHER_PRODUCTION_INDEX = 30;
    public static int IMPROVEMENT_PRODUCTION_INDEX = 40;
    public static int AUTO_PRODUCTION_INDEX = 50;
    public static int BUILDING_PRODUCTION_INDEX = 60;
    public static int NATION_PRODUCTION_INDEX = 70;

    private float value;

    /**
     * The value increments per turn. This can be used to create
     * Modifiers whose values increase or decrease over time.
     */
    private float increment;
    
    /**
     * The type of this Modifier
     */
    private Type type;

    /**
     * The type of increment.
     */
    private Type incrementType;

    /**
     * The sorting index.
     */
    private int index = -1;


    // -- Constructors --

    @SuppressWarnings("unused")
    private Modifier() {
        // empty constructor
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the Type of the modifier
     */
    public Modifier(String id, float value, Type type) {
        setId(id);
        setType(type);
        setValue(value);
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>FreeColGameObjectType</code> value
     * @param value an <code>float</code> value
     * @param type the Type of the modifier
     */
    public Modifier(String id, FreeColGameObjectType source, float value, Type type) {
        setId(id);
        setSource(source);
        setType(type);
        setValue(value);
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param template a <code>Modifier</code> value
     */
    public Modifier(Modifier template) {
        setId(template.getId());
        setSource(template.getSource());
        setType(template.getType());
        setValue(template.getValue());
        if (template.hasIncrement()) {
            setIncrement(template.getIncrement(), template.getIncrementType(),
                         template.getFirstTurn(), template.getLastTurn());
        }
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
     * @param specification a <code>Specification</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Modifier(XMLStreamReader in, Specification specification) throws XMLStreamException {
        readFromXMLImpl(in, specification);
    }
    
    /**
     * Get the <code>Type</code> value.
     *
     * @return an <code>Type</code> value
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public void setType(final Type newType) {
        this.type = newType;
    }

    /**
     * Get the <code>IncrementType</code> value.
     *
     * @return a <code>Type</code> value
     */
    public Type getIncrementType() {
        return incrementType;
    }

    /**
     * Set the <code>IncrementType</code> value.
     *
     * @param newIncrementType The new IncrementType value.
     */
    public void setIncrementType(final Type newIncrementType) {
        this.incrementType = newIncrementType;
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>float</code> value
     */
    public float getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final float newValue) {
        value = newValue;
    }

    /**
     * Get the <code>Increment</code> increment.
     *
     * @return a <code>float</code> increment
     */
    public float getIncrement() {
        return increment;
    }

    /**
     * Set the <code>Increment</code> increment.
     *
     * @param newIncrement The new Increment increment.
     */
    public void setIncrement(final float newIncrement, Type type, Turn firstTurn, Turn lastTurn) {
        if (firstTurn == null) {
            throw new IllegalArgumentException("Parameter firstTurn must not be 'null'.");
        } else {
            increment = newIncrement;
            incrementType = type;
            setFirstTurn(firstTurn);
            setLastTurn(lastTurn);
        }
    }

    /**
     * Returns <code>true</code> if this Modifier has an increment.
     *
     * @return a <code>boolean</code> value
     */
    public boolean hasIncrement() {
        return incrementType != null;
    }

    /**
     * Get the <code>Index</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getIndex() {
        return index;
    }

    /**
     * Set the <code>Index</code> value.
     *
     * @param newIndex The new Index value.
     */
    public void setIndex(final int newIndex) {
        this.index = newIndex;
    }

    /**
     * Applies this Modifier to a number. This method does not take
     * scopes, increments or time limits into account.
     *
     * @param number a <code>float</code> value
     * @return a <code>float</code> value
     */
    public float applyTo(float number) {
        switch(type) {
        case ADDITIVE:
            return number + value;
        case MULTIPLICATIVE:
            return number * value;
        case PERCENTAGE:
            return number + (number * value) / 100;
        default:
            return number;
        }
    }

    public int hashCode() {
        int hash = super.hashCode();
        hash = hash + 31 * Float.floatToIntBits(value);
        hash = hash + 31 * Float.floatToIntBits(increment);
        hash = hash + 31 * (type == null ? 0 : type.hashCode());
        hash = hash + 31 * (incrementType == null ? 0 : incrementType.hashCode());
        hash = hash + 31 * index;
        return hash;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Modifier) {
            Modifier modifier = (Modifier) o;
            if (!super.equals(o)) {
                return false;
            }
            if (value != modifier.value) {
                return false;
            }
            if (increment != modifier.increment) {
                return false;
            }
            if (type == null) {
                if (modifier.type != null) {
                    return false;
                }
            } else if (modifier.type == null) {
                return false;
            } else if (!type.equals(modifier.type)) {
                return false;
            }
            if (incrementType == null) {
                if (modifier.incrementType != null) {
                    return false;
                }
            } else if (modifier.incrementType == null) {
                return false;
            } else if (!incrementType.equals(modifier.incrementType)) {
                return false;
            }
            return (index == modifier.index);
        } else {
            return false;
        }
    }

    /**
     * Compares this object with the specified object for
     * order. Returns a negative integer, zero, or a positive integer
     * as this object is less than, equal to, or greater than the
     * specified object.
     *
     * @param modifier a <code>Modifier</code> value
     * @return an <code>int</code> value
     */
    public int compareTo(Modifier modifier) {
        if (index == modifier.index) {
            return type.compareTo(modifier.type);
        } else {
            return (index - modifier.index);
        }
    }


    // -- Factory methods --

    public static Modifier createTeaPartyModifier(Turn turn) {
        Modifier bellsBonus = new Modifier("model.goods.bells", Specification.COLONY_GOODS_PARTY,
                                           50, Type.PERCENTAGE);
        bellsBonus.setIncrement(-2, Type.ADDITIVE, turn, new Turn(turn.getNumber() + 25));
        return bellsBonus;
    }


    // -- Serialization --


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        writeAttributes(out);
        out.writeEndElement();
    }

    /**
     * Returns the XML tag name for this element.
     *
     * @return a <code>String</code> value
     */
    public static String getXMLElementTagName() {
        return "modifier";
    }

    public void readAttributes(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        super.readAttributes(in, specification);
        setType(Enum.valueOf(Type.class, in.getAttributeValue(null, "type").toUpperCase()));
        value = Float.parseFloat(in.getAttributeValue(null, VALUE_TAG));
        String incrementString = in.getAttributeValue(null, "incrementType");
        if (incrementString != null) {
            setType(Enum.valueOf(Type.class, incrementString.toUpperCase()));
            increment = Float.parseFloat(in.getAttributeValue(null, "increment"));
        }
        index = getAttribute(in, "index", -1);
    }
    
    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
	super.writeAttributes(out);
	out.writeAttribute(VALUE_TAG, String.valueOf(value));
	out.writeAttribute("type", type.toString().toLowerCase());
        if (incrementType != null) {
            out.writeAttribute("incrementType", incrementType.toString());
            out.writeAttribute("increment", String.valueOf(increment));
        }
        if (index >= 0) {
            out.writeAttribute("index", Integer.toString(index));
        }
    }
    
    public String toString() {
        return getId() + (getSource() == null ? " " : " (" + getSource().getId() + ") ") +
            type + " " + value;
    }
    
}
