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

    public static final String ADD_TAX_TO_BELLS = "model.ability.addTaxToBells";

    /**
     * The ability of some buildings (e.g. the schoolhouse) to teach
     * skills.
     */
    public static final String CAN_TEACH = "model.ability.teach";

    /**
     * The somewhat controversial ability of expert units in factory
     * level buildings to produce a certain amount of goods even when
     * no raw materials are available. Allegedly, this is a feature of
     * the original game.
     */
    public static final String EXPERTS_USE_CONNECTIONS =
        "model.ability.expertsUseConnections";

    /**
     * The ability of certain buildings (e.g. the stables) to produce
     * goods even if no units are present.
     */
    public static final String AUTO_PRODUCTION =
        "model.ability.autoProduction";

    /**
     * The ability of certain buildings (e.g. the stables) to avoid
     * producing more goods than the colony can store, which would
     * normally go to waste.
     */
    public static final String AVOID_EXCESS_PRODUCTION =
        "model.ability.avoidExcessProduction";

    /**
     * The ability of certain consumers (e.g. BuildQueues) to consume
     * a large amount of goods at once instead of turn by turn.
     */
    public static final String CONSUME_ALL_OR_NOTHING =
        "model.ability.consumeAllOrNothing";


    private boolean value = true;

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
     * @param source a <code>FreeColGameObjectType</code> value
     * @param value a <code>boolean</code> value
     */
    public Ability(String id, FreeColGameObjectType source, boolean value) {
        setId(id);
        setSource(source);
        this.value = value;
    }

    /**
     * Creates a new <code>Ability</code> instance.
     *
     * @param template an <code>Ability</code> value
     */
    public Ability(Ability template) {
        super.copy(template);
        this.value = template.value;
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
     * @param specification a <code>Specification</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Ability(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        readFromXMLImpl(in, specification);
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


    public int hashCode() {
        int hash = super.hashCode();
        hash += (value ? 1 : 0);
        return hash;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Ability) {
            return super.equals(o) && (value == ((Ability) o).value);
        } else {
            return false;
        }
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
        super.toXML(out, "ability");
    }

    public void readAttributes(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        super.readAttributes(in, specification);
        value = getAttribute(in, VALUE_TAG, true);
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute(VALUE_TAG, String.valueOf(value));
    }

    /**
     * Returns the XML tag name for this element.
     *
     * @return a <code>String</code> value
     */
    public static String getXMLElementTagName() {
        return "ability";
    }

    public String toString() {
        return getId() + (getSource() == null ? " " : " (" + getSource().getId() + ") ") +
            " " + value;
    }

}
