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

package net.sf.freecol.common.model;

import java.lang.reflect.Method;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * The <code>Scope</code> class determines whether a given
 * <code>FreeColGameObjectType</code> fulfills certain requirements.
 */
public class Scope extends FreeColObject implements Cloneable {


    /**
     * The ID of a <code>FreeColGameObjectType</code>.
     */
    private String type;

    /**
     * The ID of an <code>Ability</code>.
     */
    private String abilityID;

    /**
     * The value of an <code>Ability</code>.
     */
    private boolean abilityValue = true;

    /**
     * The name of an <code>Method</code>.
     */
    private String methodName;

    /**
     * The <code>String</code> representation of the value of an
     * <code>Method</code>.
     */
    private String methodValue;

    /**
     * True if the scope applies to a null object.
     */
    private boolean matchesNull = true;

    /**
     * Whether the match is negated.
     */
    private boolean matchNegated = false;


    /**
     * Creates a new <code>Scope</code> instance.
     *
     */
    public Scope() {}

    /**
     * Creates a new <code>Scope</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Scope(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }

    /**
     * Get the <code>MatchesNull</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isMatchesNull() {
        return matchesNull;
    }

    /**
     * Set the <code>MatchesNull</code> value.
     *
     * @param newMatchesNull The new MatchesNull value.
     */
    public void setMatchesNull(final boolean newMatchesNull) {
        this.matchesNull = newMatchesNull;
    }

    /**
     * Get the <code>MatchNegated</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isMatchNegated() {
        return matchNegated;
    }

    /**
     * Set the <code>MatchNegated</code> value.
     *
     * @param newMatchNegated The new MatchNegated value.
     */
    public void setMatchNegated(final boolean newMatchNegated) {
        this.matchNegated = newMatchNegated;
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public void setType(final String newType) {
        this.type = newType;
    }

    /**
     * Get the <code>AbilityID</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getAbilityID() {
        return abilityID;
    }

    /**
     * Set the <code>AbilityID</code> value.
     *
     * @param newAbilityID The new AbilityID value.
     */
    public void setAbilityID(final String newAbilityID) {
        this.abilityID = newAbilityID;
    }

    /**
     * Get the <code>AbilityValue</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isAbilityValue() {
        return abilityValue;
    }

    /**
     * Set the <code>AbilityValue</code> value.
     *
     * @param newAbilityValue The new AbilityValue value.
     */
    public void setAbilityValue(final boolean newAbilityValue) {
        this.abilityValue = newAbilityValue;
    }

    /**
     * Get the <code>MethodName</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Set the <code>MethodName</code> value.
     *
     * @param newMethodName The new MethodName value.
     */
    public void setMethodName(final String newMethodName) {
        this.methodName = newMethodName;
    }

    /**
     * Get the <code>MethodValue</code> value.
     *
     * @return an <code>String</code> value
     */
    public String getMethodValue() {
        return methodValue;
    }

    /**
     * Set the <code>MethodValue</code> value.
     *
     * @param newMethodValue The new MethodValue value.
     */
    public void setMethodValue(final String newMethodValue) {
        this.methodValue = newMethodValue;
    }


    /**
     * Describe <code>appliesTo</code> method here.
     *
     * @param object a <code>FreeColGameObjectType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean appliesTo(FreeColObject object) {
        if (object == null) {
            return matchesNull;
        }
        if (type != null) {
            if (object instanceof FreeColGameObjectType) {
                if (!type.equals(object.getId())) {
                    return matchNegated;
                }
            } else if (object instanceof FreeColGameObject) {
                try {
                    Method method = object.getClass().getMethod("getType");
                    if (method != null
                        && FreeColGameObjectType.class.isAssignableFrom(method.getReturnType())) {
                        FreeColGameObjectType objectType =
                            (FreeColGameObjectType) method.invoke(object);
                        if (!type.equals(objectType.getId())) {
                            return matchNegated;
                        }
                    } else {
                        return matchNegated;
                    }
                } catch(Exception e) {
                    return matchNegated;
                }
            } else {
                
                return matchNegated;
            }
        }
        if (abilityID != null && object.hasAbility(abilityID) != abilityValue) {
            return matchNegated;
        }
        if (methodName != null) {
            try {
                Method method = object.getClass().getMethod(methodName);
                if (method != null 
                    && !String.valueOf(method.invoke(object)).equals(methodValue)) {
                    return matchNegated;
                }
            } catch(Exception e) {
                return matchNegated;
            }
        }
        return !matchNegated;
    }

    public int hashCode() {
        int hash = 7;
        hash += 31 * hash + (type == null ? 0 : type.hashCode());
        hash += 31 * hash + (abilityID == null ? 0 : abilityID.hashCode());
        hash += 31 * hash + (abilityValue ? 1 : 0);
        hash += 31 * hash + (methodName == null ? 0 : methodName.hashCode());
        hash += 31 * hash + (methodValue == null ? 0 : methodValue.hashCode());
        hash += 31 * hash + (matchesNull ? 1 : 0);
        hash += 31 * hash + (matchNegated ? 1 : 0);
        return hash;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Scope) {
            Scope otherScope = (Scope) o;
            if (matchNegated != otherScope.matchNegated) {
                return false;
            }
            if (matchesNull != otherScope.matchesNull) {
                return false;
            }
            if (type == null) {
                if (otherScope.getType() != type) {
                    return false;
                }
            } else if (!type.equals(otherScope.getType())) {
                return false;
            }
            if (abilityID == null) {
                if (otherScope.getAbilityID() != abilityID) {
                    return false;
                }
            } else if (!abilityID.equals(otherScope.getAbilityID())) {
                return false;
            }
            if (abilityValue != otherScope.isAbilityValue()) {
                return false;
            }
            if (methodName == null) {
                if (otherScope.getMethodName() != methodName) {
                    return false;
                }
            } else if (!methodName.equals(otherScope.getMethodName())) {
                return false;
            }
            if (methodValue == null) {
                if (otherScope.getMethodValue() != methodValue) {
                    return false;
                }
            } else if (!methodValue.equals(otherScope.getMethodValue())) {
                return false;
            }
            return true;
        } else {
            return false;
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
        out.writeStartElement(getXMLElementTagName());
        writeAttributes(out);
        out.writeEndElement();
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        out.writeAttribute("matchNegated", Boolean.toString(matchNegated));
        out.writeAttribute("matchesNull", Boolean.toString(matchesNull));
        if (type != null) {
            out.writeAttribute("type", type);
        }
        if (abilityID != null) {
            out.writeAttribute("ability-id", abilityID);
            out.writeAttribute("ability-value", Boolean.toString(abilityValue));
        }
        if (methodName != null) {
            out.writeAttribute("method-name", methodName);
            // method value may be null in the Operand sub-class
            if (methodValue != null) {
                out.writeAttribute("method-value", methodValue);
            }
        }
    }
    
    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @param specification A <code>Specification</code> to use.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        matchNegated = getAttribute(in, "matchNegated", false);
        matchesNull = getAttribute(in, "matchesNull", true);
        type = in.getAttributeValue(null, "type");
        abilityID = in.getAttributeValue(null, "ability-id");
        abilityValue = getAttribute(in, "ability-value", true);
        methodName = in.getAttributeValue(null, "method-name");
        methodValue = in.getAttributeValue(null, "method-value");
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "scope".
     */
    public static String getXMLElementTagName() {
        return "scope";
    }
}
