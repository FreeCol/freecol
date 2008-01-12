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
 * The <code>Ability</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Ability extends Feature {


    private boolean value = true;

    /**
     * A list of abilities that contributed to this one.
     */
    private List<Ability> abilities;

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id a <code>String</code> value
     */
    public Ability(String id) {
        this(id, null, true);
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id a <code>String</code> value
     * @param value a <code>boolean</code> value
     */
    public Ability(String id, boolean value) {
        this(id, null, value);
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param id a <code>String</code> value
     * @param source a <code>String</code> value
     * @param value a <code>boolean</code> value
     */
    public Ability(String id, String source, boolean value) {
        setId(id);
        setSource(source);
        this.value = value;
    }
    
    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param element an <code>Element</code> value
     */
    public Ability(Element element) {
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Ability(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }
    
    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public void setValue(final boolean newValue) {
        this.value = newValue;
    }

    /**
     * Get the <code>Abilities</code> value.
     *
     * @return a <code>List<Ability></code> value
     */
    public List<Ability> getAbilities() {
        return abilities;
    }

    /**
     * Set the <code>Abilities</code> value.
     *
     * @param newAbilities The new Abilities value.
     */
    public void setAbilities(final List<Ability> newAbilities) {
        this.abilities = newAbilities;
    }

    /**
     * Combines several Abilities, which may be <code>null</code>. The
     * scopes of the Abilities are not combined, so that the resulting
     * Ability is always unscoped.
     *
     * @param abilities some abilities
     * @return an <code>Ability</code> value, or <code>null</code> if
     * the arguments can not be combined
     */
    public static Ability combine(Ability... abilities) {
        String resultId = null;
        Ability result = new Ability(resultId, "result", true);
        result.abilities = new ArrayList<Ability>();

        for (Ability ability : abilities) {
            if (ability == null) {
                continue;
            } else if (resultId == null) {
                // this is the first new ability
                resultId = ability.getId();
                result.setId(resultId);
            } else if (!resultId.equals(ability.getId())) {
                return null;
            }
            if (ability.abilities == null) {
                result.abilities.add(ability);
            } else {
                result.abilities.addAll(ability.abilities);
            }
            result.value = result.value && ability.value;
        }
        switch(result.abilities.size()) {
        case 0:
            return null;
        case 1:
            return result.abilities.get(0);
        default:
            return result;
        }
    }

    /**
     * Removes another Ability from this one and returns the result.
     * If this Ability was the combination of two other Abilities,
     * then removing one of the original Abilities will return the
     * other original Ability.
     *
     * @param newAbility a <code>Ability</code> value
     * @return a <code>Ability</code> value
     */
    public Ability remove(Ability newAbility) {
        if (abilities != null && abilities.remove(newAbility)) {
            if (abilities.size() == 1) {
                return abilities.get(0);
            } else if (!newAbility.value) {
                // only removing a false value might change the result
                // value
                for (Ability ability : abilities) {
                    if (!ability.value) {
                        return this;
                    }
                }
                value = true;
            }
        }
        return this;
    }

    /**
     * Returns a Ability applicable to the argument, or null if there
     * is no such Ability.
     *
     * @param object a <code>FreeColGameObjectType</code> value
     * @return a <code>Ability</code> value
     */
    public Ability getApplicableAbility(FreeColGameObjectType object) {
        if (abilities == null) {
            if (appliesTo(object)) {
                return this;
            } else {
                return null;
            }
        } else {
            List<Ability> result = new ArrayList<Ability>();
            for (Ability ability : abilities) {
                if (ability.appliesTo(object)) {
                    result.add(ability);
                }
            }
            return combine(result.toArray(new Ability[result.size()]));
        }
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
        value = getAttribute(in, "value", true);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("scope".equals(childName)) {
                Scope scope = new Scope(in);
                getScopes().add(scope);
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
        out.writeAttribute("value", String.valueOf(value));

        super.toXMLImpl(out);
        
        out.writeEndElement();
    }

    /**
     * Returns the XML tag name for this element.
     *
     * @return a <code>String</code> value
     */
    public static String getXMLElementTagName() {
        return "ability";
    }

}
