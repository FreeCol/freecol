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
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;

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

    private static enum Change { NONE, UPDATE, REMOVE }

    public static enum Type { 
        ADDITIVE(0),
        MULTIPLICATIVE(1),
        PERCENTAGE(0),
        COMBINED(0);

        private float defaultValue;

        Type(float defaultValue) {
            this.defaultValue = defaultValue;
        }

        public float getDefaultValue() {
            return defaultValue;
        }
    }
    
    /**
     * The type of this Modifier
     */
    private Type type;

    /**
     * The float values of this modifier
     */
    private EnumMap<Type, Float> values = new EnumMap<Type, Float>(Type.class);

    /**
     * The value increments per turn (usually 'null'). This can be
     * used to create bonuses that increase or decrease over time.
     */
    private EnumMap<Type, Float> increments;

    /**
     * A list of modifiers that contributed to this one.
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
     * @param source a <code>String</code> value
     * @param type a <code>Type</code> value
     */
    private Modifier(String id, String source, Type type) {
        this(id, source, type.getDefaultValue(), type);
    }

    /**
     * Creates a new <code>Modifier</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value an <code>float</code> value
     * @param type the Type of the modifier
     */
    public Modifier(String id, float value, Type type) {
        this(id, null, value, type);
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
        values.put(type, value);
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
        values = new EnumMap<Type, Float>(Type.class);
        for (Entry<Type, Float> entry : modifier.values.entrySet()) {
            values.put(entry.getKey(), new Float(entry.getValue()));
        }
        if (modifier.increments != null) {
            increments = new EnumMap<Type, Float>(Type.class);
            for (Entry<Type, Float> entry : modifier.increments.entrySet()) {
                increments.put(entry.getKey(), new Float(entry.getValue()));
            }
        }
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
     * Set the <code>Modifiers</code> value.
     *
     * @param newModifiers The new Modifiers value.
     */
    public void setModifiers(final List<Modifier> newModifiers) {
        this.modifiers = newModifiers;
        if (newModifiers == null || newModifiers.isEmpty()) {
            return;
        } else if (newModifiers.size() == 1) {
            copyValues(newModifiers.get(0));
        } else {
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
        if (type == Type.COMBINED) {
            if (values.get(Type.ADDITIVE) == null) {
                values.put(Type.ADDITIVE, Type.ADDITIVE.getDefaultValue());
            }
            if (values.get(Type.PERCENTAGE) == null) {
                values.put(Type.PERCENTAGE, Type.PERCENTAGE.getDefaultValue());
            }
            if (values.get(Type.MULTIPLICATIVE) == null) {
                values.put(Type.MULTIPLICATIVE, Type.MULTIPLICATIVE.getDefaultValue());
            }
        }
    }


    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>float</code> value
     */
    public float getValue() {
        return values.get(type);
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final float newValue) {
        values.put(type, newValue);
    }

    /**
     * Returns true if this Modifier has value increments.
     *
     * @return a <code>boolean</code> value
     */
    public boolean hasIncrement() {
        return (increments != null);
    }

    /**
     * Get the <code>Increment</code> increment.
     *
     * @return a <code>float</code> increment
     */
    public float getIncrement() {
        return increments.get(type);
    }

    /**
     * Set the <code>Increment</code> increment.
     *
     * @param newIncrement The new Increment increment.
     */
    public void setIncrement(final float newIncrement) {
        if (increments == null) {
            increments = new EnumMap<Type, Float>(Type.class);
        }
        increments.put(type, newIncrement);
    }

    public boolean isDefaultValue() {
        if (type == Type.COMBINED) {
            return (values.get(Type.ADDITIVE).floatValue() ==
                    Type.ADDITIVE.getDefaultValue() &&
                    values.get(Type.PERCENTAGE).floatValue() ==
                    Type.PERCENTAGE.getDefaultValue() &&
                    values.get(Type.MULTIPLICATIVE).floatValue() ==
                    Type.MULTIPLICATIVE.getDefaultValue());
        } else {
            return (values.get(type).floatValue() == 
                    type.getDefaultValue());
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
        Modifier result = null;
        for (Modifier modifier : modifiers) {
            if (modifier == null) {
                continue;
            } else if (result == null) {
                result = new Modifier();
                result.copyValues(modifier);
                if (modifier.modifiers == null) {
                    result.modifiers = new ArrayList<Modifier>();
                    result.modifiers.add(modifier);
                } else {
                    // copyValues has already copied the modifiers
                }
            } else {
                result.add(modifier);
            }
        }
        if (result == null) {
            return null;
        } else {
            switch(result.modifiers.size()) {
            case 0:
                return null;
            case 1:
                return result.modifiers.get(0);
            default:
                return result;
            }
        }
    }

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
        if (modifier.values.get(Type.ADDITIVE) != null) {
            values.put(Type.ADDITIVE, 
                       values.get(Type.ADDITIVE) +
                       modifier.values.get(Type.ADDITIVE));
        }
        if (modifier.values.get(Type.PERCENTAGE) != null) {
            values.put(Type.PERCENTAGE, 
                       values.get(Type.PERCENTAGE) +
                       modifier.values.get(Type.PERCENTAGE));
        }
        if (modifier.values.get(Type.MULTIPLICATIVE) != null) {
            values.put(Type.MULTIPLICATIVE, 
                       values.get(Type.MULTIPLICATIVE) *
                       modifier.values.get(Type.MULTIPLICATIVE));
        }
        if (modifier.hasScope()) {
            setScope(true);
        }
        if (modifier.hasTimeLimit()) {
            setTimeLimit(true);
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
                if (newModifier.values.get(Type.ADDITIVE) != null) {
                    values.put(Type.ADDITIVE, 
                               values.get(Type.ADDITIVE) -
                               newModifier.values.get(Type.ADDITIVE));
                }
                if (newModifier.values.get(Type.PERCENTAGE) != null) {
                    values.put(Type.PERCENTAGE, 
                               values.get(Type.PERCENTAGE) -
                               newModifier.values.get(Type.PERCENTAGE));
                }
                if (newModifier.values.get(Type.MULTIPLICATIVE) != null) {
                    values.put(Type.MULTIPLICATIVE, 
                               values.get(Type.MULTIPLICATIVE) /
                               newModifier.values.get(Type.MULTIPLICATIVE));
                }
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
     * is no such Modifier.
     *
     * @param object a <code>FreeColGameObjectType</code> value
     * @param turn a <code>Turn</code> value
     * @return a <code>Modifier</code> value
     */
    public Modifier getApplicableModifier(FreeColGameObjectType object, Turn turn) {
        if (getModifiers() == null) {
            if (appliesTo(object, turn)) {
                return this;
            } else {
                return null;
            }
        } else if (hasScope() || hasTimeLimit()) {
            List<Modifier> result = new ArrayList<Modifier>();
            for (Modifier modifier : getModifiers()) {
                if (modifier.appliesTo(object, turn)) {
                    result.add(modifier);
                }
            }
            return combine(result.toArray(new Modifier[result.size()]));
        } else {
            return this;
        }
    }


    /**
     * Applies this Modifier to a number.
     *
     * @param number a <code>float</code> value
     * @return a <code>float</code> value
     */
    public float applyTo(float number) {
        float result = number;
        if (values.get(Type.ADDITIVE) != null) {
            result += values.get(Type.ADDITIVE);
        }
        if (values.get(Type.MULTIPLICATIVE) != null) {
            result *= values.get(Type.MULTIPLICATIVE);
        }
        if (values.get(Type.PERCENTAGE) != null) {
            result += (result * values.get(Type.PERCENTAGE)) / 100;
        }
        return result;
    }

    public Change newTurn(Turn turn) {
        if (getModifiers() == null) {
            if (isOutOfDate(turn)) {
                return Change.REMOVE;
            } else if (hasIncrement()) {
                float newValue = 0;
                switch(type) {
                case ADDITIVE:
                case PERCENTAGE:
                    newValue = getValue() + increments.get(type);
                    break;
                case MULTIPLICATIVE:
                    newValue = getValue() * increments.get(type);
                    break;
                case COMBINED:
                    throw new UnsupportedOperationException("Error calling newTurn on COMBINED Modifier.");
                }
                if (type.getDefaultValue() == newValue) {
                    return Change.REMOVE;
                } else {
                    setValue(newValue);
                    return Change.UPDATE;
                }
            } else {
                return Change.NONE;
            }
        } else if (hasTimeLimit() || hasIncrement()) {
            boolean recombine = false;
            List<Modifier> newModifiers = new ArrayList<Modifier>();
            for (Modifier modifier : getModifiers()) {
                switch(modifier.newTurn(turn)) {
                case NONE:
                    newModifiers.add(modifier);
                    break;
                case UPDATE:
                    newModifiers.add(modifier);
                    recombine = true;
                    break;
                case REMOVE:
                    recombine = true;
                    break;
                }
            }
            if (recombine) {
                setModifiers(newModifiers);
                return Change.UPDATE;
            } else {
                return Change.NONE;
            }
        } else {
            return Change.NONE;
        }
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
        setScope(Boolean.parseBoolean(in.getAttributeValue(null, "scope")));
        setTimeLimit(Boolean.parseBoolean(in.getAttributeValue(null, "timeLimit")));

        if (getType() == Type.COMBINED) {
            for (Type newType : Type.values()) {
                String value = in.getAttributeValue(null, type.toString());
                if (value != null) {
                    values.put(newType, Float.parseFloat(value));
                }
            }
        } else {
            String value = in.getAttributeValue(null, "value");
            if (value != null) {
                values.put(type, Float.parseFloat(value));
            }
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
        out.writeAttribute("scope", String.valueOf(hasScope()));
        out.writeAttribute("timeLimit", String.valueOf(hasTimeLimit()));

        /*
        if (firstTurn != null) {
            firstTurn.toXML();
        }
        if (lastTurn != null) {
            lastTurn.toXML();
        }
        */
        for (Scope scope : getScopes()) {
            scope.toXMLImpl(out);
        }
        out.writeAttribute("type", type.toString());
        if (type == Type.COMBINED) {
            for (Entry<Type, Float> entry : values.entrySet()) {
                out.writeAttribute(entry.getKey().toString(), entry.getValue().toString());
            }
        } else {
            out.writeAttribute("value", values.get(type).toString());
        }
        if (modifiers != null) {
            for (Modifier modifier : getModifiers()) {
                modifier.toXMLImpl(out);
            }
        }

        out.writeEndElement();
    }
    
}
