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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
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
public final class Modifier extends Feature {

    public static final String OFFENCE = "model.modifier.offence";
    public static final String DEFENCE = "model.modifier.defence";

    public static final String COLONY_GOODS_PARTY = "model.monarch.colonyGoodsParty";

    public static final float UNKNOWN = Float.MIN_VALUE;

    public static enum Type { ADDITIVE, MULTIPLICATIVE, PERCENTAGE }

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
     * Describe incrementType here.
     */
    private Type incrementType;

    // -- Constructors --

    private Modifier() {
        // empty constructor
    };

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
     * @param source a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the Type of the modifier
     */
    public Modifier(String id, String source, float value, Type type) {
        setId(id);
        setSource(source);
        setType(type);
        setValue(value);
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

    // -- Factory methods --

    public static Modifier createTeaPartyModifier(Turn turn) {
        Modifier bellsBonus = new Modifier("model.goods.bells", COLONY_GOODS_PARTY,
                                           50, Type.PERCENTAGE);
        bellsBonus.setIncrement(-2, Type.ADDITIVE, turn, new Turn(turn.getNumber() + 25));
        return bellsBonus;
    }


    // -- Serialization --


    /**
     * Returns the XML tag name for this element.
     *
     * @return a <code>String</code> value
     */
    public static String getXMLElementTagName() {
        return "modifier";
    }

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        setType(Enum.valueOf(Type.class, in.getAttributeValue(null, "type").toUpperCase()));
        value = Float.parseFloat(in.getAttributeValue(null, "value"));
        String incrementString = in.getAttributeValue(null, "incrementType");
        if (incrementString != null) {
            setType(Enum.valueOf(Type.class, incrementString.toUpperCase()));
            increment = Float.parseFloat(in.getAttributeValue(null, "increment"));
        }
    }
    
    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
	super.writeAttributes(out);
	out.writeAttribute("value", String.valueOf(value));
	out.writeAttribute("type", type.toString());
        if (incrementType != null) {
            out.writeAttribute("incrementType", incrementType.toString());
            out.writeAttribute("increment", String.valueOf(increment));
        }
    }
    
    public String toString() {
        return getId() + " " + type + " " + value;
    }
    
}
