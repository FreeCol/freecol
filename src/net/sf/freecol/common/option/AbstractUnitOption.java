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

package net.sf.freecol.common.option;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Specification.TypeSelector;
import net.sf.freecol.common.model.Unit.Role;

/**
 * Represents an option where the valid choice is an AbstractUnit.
 */
public class AbstractUnitOption extends AbstractOption<AbstractUnit> {

    private static Logger logger = Logger.getLogger(AbstractUnitOption.class.getName());

    private static final TypeSelector DEFAULT_SELECTOR = TypeSelector.UNITS;

    private AbstractUnit value;

    /**
     * An Option to determine the UnitType of the AbstractUnit.
     */
    private StringOption unitType;

    /**
     * An Option to determine the Role of the AbstractUnit.
     */
    private StringOption role;

    /**
     * An Option to determine the number of the AbstractUnit.
     */
    private IntegerOption number;

    /**
     * Creates a new <code>AbstractUnitOption</code>.
     * Get the <code>UnitType</code> value.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     * @return a <code>StringOption</code> value
     */
    public AbstractUnitOption(String id) {
        super(id);
    }


    /**
     * Creates a new  <code>AbstractUnitOption</code>.
     *
     * @param in The <code>XMLStreamReader</code> containing the data.
     * @exception XMLStreamException if an error occurs
     */
    public AbstractUnitOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXMLImpl(in);
    }

    public AbstractUnitOption clone() {
        AbstractUnitOption result = new AbstractUnitOption(getId());
        result.setValue(getValue().clone());
        result.unitType = unitType.clone();
        result.role = role.clone();
        result.number = number.clone();
        return result;
    }


    /**
     * Gets the current value of this <code>AbstractUnitOption</code>.
     * @return The value.
     */
    public AbstractUnit getValue() {
        return value;
    }


    /**
     * Sets the value of this <code>AbstractUnitOption</code>.
     * @param value The value to be set.
     */
    public void setValue(AbstractUnit value) {
        final AbstractUnit oldValue = this.value;
        this.value = value;

        if (!value.equals(oldValue) && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }

   /**
     * Get the <code>UnitType</code> value.
     *
     * @return a <code>StringOption</code> value
     */
    public final StringOption getUnitType() {
        return unitType;
    }

    /**
     * Set the <code>UnitType</code> value.
     *
     * @param newUnitType The new UnitType value.
     */
    public final void setUnitType(final StringOption newUnitType) {
        this.unitType = newUnitType;
    }

    /**
     * Get the <code>Role</code> value.
     *
     * @return a <code>StringOption</code> value
     */
    public final StringOption getRole() {
        return role;
    }

    /**
     * Set the <code>Role</code> value.
     *
     * @param newRole The new Role value.
     */
    public final void setRole(final StringOption newRole) {
        this.role = newRole;
    }

    /**
     * Get the <code>Number</code> value.
     *
     * @return an <code>IntegerOption</code> value
     */
    public final IntegerOption getNumber() {
        return number;
    }

    /**
     * Set the <code>Number</code> value.
     *
     * @param newNumber The new Number value.
     */
    public final void setNumber(final IntegerOption newNumber) {
        this.number = newNumber;
    }

    /**
     * Generate the choices to provide to the UI based on the
     * generateChoices value.
     *
     * @param specification the Specification that defines the game
     * objects whose IDs will be generated
     */
    public void generateChoices(Specification specification) {
        unitType.generateChoices(specification);
        role.generateChoices(specification);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());

        // TODO: we REALLY, REALLY need to clean up serialization!
        unitType.toXML(out, "unitType");
        role.toXML(out, "role");
        number.toXMLImpl(out, "number");

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);

        if (id == null && getId().equals(NO_ID)){
            throw new XMLStreamException("invalid <" + getXMLElementTagName()
                                         + "> tag : no id attribute found.");
        }

        if (getId() == NO_ID) {
            setId(id);
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String tag = in.getLocalName();
            if ("number".equals(tag)) {
                number = new IntegerOption(id + ".number");
                number.readFromXMLImpl(in);
            } else if ("unitType".equals(tag)) {
                unitType = new StringOption(id + ".unitType");
                unitType.readFromXMLImpl(in);
            } else if ("role".equals(tag)) {
                role = new StringOption(id + ".role");
                role.readFromXMLImpl(in);
            }
        }
        setValue(new AbstractUnit(unitType.getValue(), Role.valueOf(role.getValue()), number.getValue()));
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitOption".
     */
    public static String getXMLElementTagName() {
        return "unitOption";
    }
}
