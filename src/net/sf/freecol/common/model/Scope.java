/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;


/**
 * The <code>Scope</code> class determines whether a given
 * <code>FreeColGameObjectType</code> fulfills certain requirements.
 */
public class Scope extends FreeColObject {

    /** 
     * The identifier of a <code>FreeColGameObjectType</code>, or
     * <code>Option</code>.
     */
    private String type = null;

    /** The object identifier of an <code>Ability</code>. */
    private String abilityId = null;

    /** The value of an <code>Ability</code>. */
    private boolean abilityValue = true;

    /** The name of an <code>Method</code>. */
    private String methodName = null;

    /**
     * The <code>String</code> representation of the value of an
     * <code>Method</code>.
     */
    private String methodValue = null;

    /** True if the scope applies to a null object. */
    private boolean matchesNull = true;

    /** Whether the match is negated. */
    private boolean matchNegated = false;


    /**
     * Deliberately empty constructor.
     */
    public Scope() {}

    /**
     * Creates a new <code>Scope</code> instance from a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    public Scope(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }


    /**
     * Get a key to display this scope with.
     *
     * @return A suitable key, or null if none found.
     */
    public String getKey() {
        return (getType() != null) ? getType()
            : (getAbilityId() != null) ? getAbilityId()
            : (getMethodName() != null) ? "scope.method." + getMethodName()
            : null;
    }

    /**
     * Does this scope match null?
     *
     * @return True if this scope matches null.
     */
    public boolean isMatchesNull() {
        return matchesNull;
    }

    public void setMatchesNull(final boolean newMatchesNull) {
        this.matchesNull = newMatchesNull;
    }

    /**
     * Is the match negated for this scope?
     *
     * @return True if this match is negated.
     */
    public boolean isMatchNegated() {
        return matchNegated;
    }

    public void setMatchNegated(final boolean newMatchNegated) {
        this.matchNegated = newMatchNegated;
    }

    public String getType() {
        return type;
    }

    public void setType(final String newType) {
        this.type = newType;
    }

    /**
     * Gets the ability identifier.
     *
     * @return The ability id.
     */
    public String getAbilityId() {
        return abilityId;
    }

    /**
     * Sets the ability identifier.
     *
     * @param newAbilityId The new ability id.
     */
    public void setAbilityId(final String newAbilityId) {
        this.abilityId = newAbilityId;
    }

    public boolean getAbilityValue() {
        return abilityValue;
    }

    public void setAbilityValue(final boolean newAbilityValue) {
        this.abilityValue = newAbilityValue;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(final String newMethodName) {
        this.methodName = newMethodName;
    }

    public String getMethodValue() {
        return methodValue;
    }

    public void setMethodValue(final String newMethodValue) {
        this.methodValue = newMethodValue;
    }

    /**
     * Does this scope apply to a given object?
     *
     * @param object The <code>FreeColGameObjectType</code> to test.
     * @return True if the scope is applicable.
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
            } else if (object instanceof FreeColObject) {
                FreeColGameObjectType fcgot = object.invokeMethod("getType",
                    FreeColGameObjectType.class, (FreeColGameObjectType)null);
                if (fcgot == null || !type.equals(fcgot.getId())) {
                    return matchNegated;
                }
            } else {
                return matchNegated;
            }
        }
        if (abilityId != null && object.hasAbility(abilityId) != abilityValue) {
            return matchNegated;
        }
        if (methodName != null) {
            Object ret = object.invokeMethod(methodName, Object.class, null);
            if (!String.valueOf(ret).equals(methodValue)) return matchNegated;
        }
        return !matchNegated;
    }


    // @compat 0.10.7
    /**
     * Helper for scope fixups.
     *
     * @return A new scope to negatively match on persons.
     */
    public static Scope makeNegatedPersonScope() {
        Scope scope = new Scope();
        scope.setAbilityId("model.ability.person");
        scope.setMatchNegated(true);
        return scope;
    }
    // end @compat 0.10.7


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof Scope) {
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
            if (abilityId == null) {
                if (!Utils.equals(otherScope.getAbilityId(), abilityId)) {
                    return false;
                }
            } else if (!abilityId.equals(otherScope.getAbilityId())) {
                return false;
            }
            if (abilityValue != otherScope.getAbilityValue()) {
                return false;
            }
            if (methodName == null) {
                if (!Utils.equals(otherScope.getMethodName(), methodName)) {
                    return false;
                }
            } else if (!methodName.equals(otherScope.getMethodName())) {
                return false;
            }
            if (methodValue == null) {
                if (!Utils.equals(otherScope.getMethodValue(), methodValue)) {
                    return false;
                }
            } else if (!methodValue.equals(otherScope.getMethodValue())) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + (type == null ? 0 : type.hashCode());
        hash = 31 * hash + (abilityId == null ? 0 : abilityId.hashCode());
        hash = 31 * hash + (abilityValue ? 1 : 0);
        hash = 31 * hash + (methodName == null ? 0 : methodName.hashCode());
        hash = 31 * hash + (methodValue == null ? 0 : methodValue.hashCode());
        hash = 31 * hash + (matchesNull ? 1 : 0);
        return 31 * hash + (matchNegated ? 1 : 0);
    }


    // Serialization

    private static final String ABILITY_ID_TAG = "ability-id";
    private static final String ABILITY_VALUE_TAG = "ability-value";
    private static final String MATCH_NEGATED_TAG = "match-negated";
    private static final String MATCHES_NULL_TAG = "matches-null";
    private static final String METHOD_NAME_TAG = "method-name";
    private static final String METHOD_VALUE_TAG = "method-value";
    private static final String TYPE_TAG = "type";
    // @compat 0.11.3
    private static final String OLD_MATCH_NEGATED_TAG = "matchNegated";
    private static final String OLD_MATCHES_NULL_TAG = "matchesNull";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        // Scopes do not have ids, no super.writeAttributes().
        // However, they might in future.

        xw.writeAttribute(MATCH_NEGATED_TAG, matchNegated);

        xw.writeAttribute(MATCHES_NULL_TAG, matchesNull);

        if (type != null) {
            xw.writeAttribute(TYPE_TAG, type);
        }

        if (abilityId != null) {
            xw.writeAttribute(ABILITY_ID_TAG, abilityId);

            xw.writeAttribute(ABILITY_VALUE_TAG, abilityValue);
        }

        if (methodName != null) {
            xw.writeAttribute(METHOD_NAME_TAG, methodName);

            if (methodValue != null) {
                // methodValue may be null in the Operand sub-class
                xw.writeAttribute(METHOD_VALUE_TAG, methodValue);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        // Scopes do not have ids, no super.readAttributes().
        // However, they might in future.

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MATCH_NEGATED_TAG)) {
            matchNegated = xr.getAttribute(OLD_MATCH_NEGATED_TAG, false);
        } else
        // end @compat 0.11.3
            matchNegated = xr.getAttribute(MATCH_NEGATED_TAG, false);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MATCHES_NULL_TAG)) {
            matchesNull = xr.getAttribute(OLD_MATCHES_NULL_TAG, true);
        } else
        // end @compat 0.11.3
            matchesNull = xr.getAttribute(MATCHES_NULL_TAG, true);

        type = xr.getAttribute(TYPE_TAG, (String)null);
        // @compat 0.10.x
        if ("model.equipment.muskets".equals(type)) {
            type = "model.role.soldier";
        } else if ("model.equipment.indian.horses".equals(type)) {
            type = "model.role.mountedBrave";
        } else if ("model.equipment.indian.muskets".equals(type)) {
            type = "model.role.armedBrave";
        }
        // end @compat 0.10.x

        abilityId = xr.getAttribute(ABILITY_ID_TAG, (String)null);

        abilityValue = xr.getAttribute(ABILITY_VALUE_TAG, true);

        methodName = xr.getAttribute(METHOD_NAME_TAG, (String)null);

        methodValue = xr.getAttribute(METHOD_VALUE_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[Scope ").append(type);
        if (abilityId != null) {
            sb.append(" ").append(abilityId).append("=").append(abilityValue);
        }
        if (methodName != null) {
            sb.append(" ").append(methodName).append("=").append(methodValue);
        }
        if (matchesNull) sb.append(" matches-null");
        if (matchNegated) sb.append(" match-negated");
        sb.append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "scope".
     */
    public static String getXMLElementTagName() {
        return "scope";
    }
}
