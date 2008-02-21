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

    public static enum Type { ADDITIVE, MULTIPLICATIVE, PERCENTAGE, COMBINED }

    public static final float[] defaultValues = new float[] { 0f, 1f, 0f };

    private float[] values = new float[] { 0f, 1f, 0f };

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

    /**
     * A list of modifiers that contributed to this one, if it is a
     * composite Modifier. The elements of this list must not be
     * COMBINED Modifiers.
     */
    private List<Modifier> modifiers;

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
     * Creates a new <code>Modifier</code> instance.
     *
     * @param modifier a <code>Modifier</code> value
     */
    public Modifier(Modifier modifier) {
        copyValues(modifier);
    }
    
    // -- Methods --

    /**
     * Copies the fields of the given Modifier into this one.
     *
     * @param modifier a <code>Modifier</code> value
     */
    private void copyValues(Modifier modifier) {
        setId(modifier.getId());
        setType(modifier.getType());
        setSource(modifier.getSource());
        setFirstTurn(modifier.getFirstTurn());
        setLastTurn(modifier.getLastTurn());
        setScopes(new ArrayList<Scope>(modifier.getScopes()));
        if (modifier.getModifiers() != null) {
            modifiers = new ArrayList<Modifier>(modifier.getModifiers());
        }
        System.arraycopy(modifier.values, 0, values, 0, 3);
        increment = modifier.increment;
        incrementType = modifier.incrementType;
    }
    
    /**
     * Returns <code>true</code> if this Modifier is composed of other
     * Modifiers.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isComposite() {
        return !(getModifiers() == null || getModifiers().isEmpty());
    }

    /**
     * Get the <code>Modifiers</code> value.
     *
     * @return a <code>List<Modifier></code> value
     */
    public List<Modifier> getModifiers() {
        return modifiers;
    }

    /**
     * Get the <code>Modifiers</code> value.
     *
     * @return a <code>List<Modifier></code> value
     */
    public List<Feature> getFeatures() {
        return new ArrayList<Feature>(modifiers);
    }

    /**
     * Set the <code>Modifiers</code> value.
     *
     * @param newModifiers The new Modifiers value.
     */
    public void setModifiers(final List<Modifier> newModifiers) {
        if (newModifiers == null || newModifiers.isEmpty()) {
            modifiers = null;
            return;
        } else if (newModifiers.size() == 1) {
            copyValues(newModifiers.get(0));
        } else {
            modifiers = new ArrayList<Modifier>();
            modifiers.add(newModifiers.get(0));
            copyValues(newModifiers.get(0));
            for (Modifier modifier : newModifiers.subList(1, newModifiers.size())) {
                add(modifier);
            }
        }
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
     * Get the <code>defaultValue</code> value.
     *
     * @return a <code>float</code> value
     */
    public float getDefaultValue() {
        if (type == Type.COMBINED) {
            throw new IllegalArgumentException("Can not get the default value of a COMBINED Modifier.");
        } else {
            return defaultValues[type.ordinal()];
        }
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>float</code> value
     */
    public float getValue() {
        if (type == Type.COMBINED) {
            throw new IllegalArgumentException("Can not get the value of a COMBINED Modifier.");
        } else {
            return values[type.ordinal()];
        }
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final float newValue) {
        if (type == Type.COMBINED) {
            throw new IllegalArgumentException("Can not set the value of a COMBINED Modifier.");
        } else {
            values[type.ordinal()] = newValue;
        }
    }

    /**
     * Get the <code>Increment</code> increment.
     *
     * @return a <code>float</code> increment
     */
    public float getIncrement() {
        if (type == Type.COMBINED) {
            throw new IllegalArgumentException("Can not get the increment of a COMBINED Modifier.");
        } else {
            return increment;
        }
    }

    /**
     * Set the <code>Increment</code> increment.
     *
     * @param newIncrement The new Increment increment.
     */
    public void setIncrement(final float newIncrement, Type type, Turn firstTurn, Turn lastTurn) {
        if (this.type == Type.COMBINED) {
            throw new IllegalArgumentException("Can not set the increment of a COMBINED Modifier.");
        } else if (type == Type.COMBINED) {
            throw new IllegalArgumentException("Can not assign a COMBINED increment.");
        } else if (firstTurn == null) {
            throw new IllegalArgumentException("Parameter firstTurn must not be 'null'.");
        } else {
            increment = newIncrement;
            incrementType = type;
            setFirstTurn(firstTurn);
            setLastTurn(lastTurn);
        }
    }

    /**
     * Combines several Modifiers, which may be <code>null</code>. The
     * scopes of the Modifiers are not combined, so that the resulting
     * Modifier is always unscoped.
     *
     * @param modifiers some modifiers
     * @return a <code>Modifier</code> value, or <code>null</code> if
     * the arguments can not be combined
     */
    public static Modifier combine(Modifier... modifiers) {
        ArrayList<Modifier> newModifiers = new ArrayList<Modifier>();
        for (Modifier modifier : modifiers) {
            if (modifier == null) {
                continue;
            } else {
                newModifiers.add(modifier);
            }
        }
        if (newModifiers.isEmpty()) {
            return null;
        } else if (newModifiers.size() == 1) {
            return newModifiers.get(0);
        } else {
            Modifier result = new Modifier();
            result.setModifiers(newModifiers);
            return result;
        }
    }

    /**
     * Adds a Modifier to this one. The result is generally a
     * composite Modifier.
     *
     * @param modifier a <code>Modifier</code> value
     */
    public void add(Modifier modifier) {
        if (modifier == null) {
            return;
        } else if (!getId().equals(modifier.getId())){
            throw new IllegalArgumentException("Can not combine Modifiers with different IDs.");
        } else if (modifiers == null) {
            modifiers = new ArrayList<Modifier>();
            Modifier copy = new Modifier();
            copy.copyValues(this);
            modifiers.add(copy);
        }
        if (modifier.modifiers == null) {
            modifiers.add(modifier);
        } else {
            modifiers.addAll(modifier.modifiers);
        }
        if (type != modifier.type) {
            setType(Type.COMBINED);
        }
        values[Type.ADDITIVE.ordinal()] += modifier.values[Type.ADDITIVE.ordinal()];
        values[Type.PERCENTAGE.ordinal()] += modifier.values[Type.PERCENTAGE.ordinal()];
        values[Type.MULTIPLICATIVE.ordinal()] *= modifier.values[Type.MULTIPLICATIVE.ordinal()];

        if (modifier.hasScope()) {
            setScope(true);
        }
        if (modifier.getFirstTurn() != null &&
            (getFirstTurn() == null || 
             modifier.getFirstTurn().getNumber() < getFirstTurn().getNumber())) {
            setFirstTurn(modifier.getFirstTurn());
        } 
        if (modifier.getLastTurn() != null &&
            (getLastTurn() == null || 
             modifier.getLastTurn().getNumber() > getLastTurn().getNumber())) {
            setLastTurn(modifier.getLastTurn());
        }
    }

    /**
     * Removes another Modifier from this one and returns the result.
     * If this Modifier was the combination of two other Modifiers,
     * then removing one of the original Modifiers will return the
     * other original Modifier.
     *
     * @param newModifier a <code>Modifier</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier remove(Modifier newModifier) {
        if (getModifiers() != null && getModifiers().remove(newModifier)) {
            if (getModifiers().size() == 1) {
                return getModifiers().get(0);
            } else {
                setModifiers(getModifiers());
            }
        }
        return this;
    }

    /**
     * Returns a Modifier applicable to the argument, or null if there
     * is no such Modifier.
     *
     * @param object a <code>FreeColGameObjectType</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier getApplicableModifier(FreeColGameObjectType object) {
        return getApplicableModifier(object, null);
    }

    /**
     * Returns a Modifier applicable to the argument, or null if there
     * is no such Modifier. This method assumes that it is safe to
     * remove Modifiers that are out of date, because the Turn
     * increases monotonically.
     *
     * @param object a <code>FreeColGameObjectType</code> value
     * @param turn a <code>Turn</code> value
     * @return a <code>Modifier</code> value
     * @see Feature#isOutOfDate(Turn)
     */
    public Modifier getApplicableModifier(FreeColGameObjectType object, Turn turn) {
        if (getModifiers() == null) {
            if (appliesTo(object, turn)) {
                if (hasIncrement() && turn != null) {
                    int diff = turn.getNumber() - getFirstTurn().getNumber();
                    float newValue = getValue();
                    switch(incrementType) {
                    case ADDITIVE:
                        newValue += increment * diff;
                        break;
                    case MULTIPLICATIVE:
                        newValue *= increment * diff;
                        break;
                    case PERCENTAGE:
                        newValue += (newValue * increment * diff) / 100;
                        break;
                    }
                    Modifier newModifier = new Modifier(this);
                    newModifier.setValue(newValue);
                    return newModifier;
                } else {
                    return this;
                }
            } else {
                return null;
            }
        } else if (hasScope() || hasTimeLimit()) {
            List<Modifier> result = new ArrayList<Modifier>();
            Iterator<Modifier> iterator = getModifiers().iterator();
            while (iterator.hasNext()) {
                Modifier nextModifier = iterator.next();
                if (nextModifier.isOutOfDate(turn)) {
                    iterator.remove();
                } else {
                    result.add(nextModifier.getApplicableModifier(object, turn));
                }
            }
            return combine(result.toArray(new Modifier[result.size()]));
        } else {
            return this;
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
     * Applies this Modifier to a number.
     *
     * @param number a <code>float</code> value
     * @return a <code>float</code> value
     */
    public float applyTo(float number) {
        float result = number;
        result += values[Type.ADDITIVE.ordinal()];
        result *= values[Type.MULTIPLICATIVE.ordinal()];
        result += (result * values[Type.PERCENTAGE.ordinal()]) / 100;
        return result;
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
        setType(Enum.valueOf(Type.class, in.getAttributeValue(null, "type").toUpperCase()));
        String incrementString = in.getAttributeValue(null, "incrementType");
        if (incrementString != null) {
            setType(Enum.valueOf(Type.class, incrementString.toUpperCase()));
            increment = Float.parseFloat(in.getAttributeValue(null, "increment"));
        }
        setScope(Boolean.parseBoolean(in.getAttributeValue(null, "scope")));

        String firstTurn = in.getAttributeValue(null, "firstTurn");
        if (firstTurn != null) {
            setFirstTurn(new Turn(Integer.parseInt(firstTurn)));
        }

        String lastTurn = in.getAttributeValue(null, "lastTurn");
        if (lastTurn != null) {
            setLastTurn(new Turn(Integer.parseInt(lastTurn)));
        }
        if (type == Type.COMBINED) {
            values = readFromArrayElement("values", in, new float[0]);
        } else {
            values[type.ordinal()] = Float.parseFloat(in.getAttributeValue(null, "value"));
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("scope".equals(childName)) {
                Scope scope = new Scope(in);
                getScopes().add(scope);
                setScope(true);
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
        out.writeAttribute("type", type.toString());
        if (incrementType != null) {
            out.writeAttribute("incrementType", incrementType.toString());
            out.writeAttribute("increment", String.valueOf(increment));
        }

        out.writeAttribute("scope", String.valueOf(hasScope()));

        if (getFirstTurn() != null) {
            out.writeAttribute("firstTurn", String.valueOf(getFirstTurn().getNumber()));
        }
        if (getLastTurn() != null) {
            out.writeAttribute("lastTurn", String.valueOf(getLastTurn().getNumber()));
        }

        if (type != Type.COMBINED) {
            out.writeAttribute("value", Float.toString(values[type.ordinal()]));
        } else {
            toArrayElement("values", values, out);
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
