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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.option.UnitTypeOption.TypeSelector;

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
    private UnitTypeOption unitType;

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
     * Creates a new <code>AbstractUnitOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public AbstractUnitOption(Specification specification) {
        super(specification);
    }

    public AbstractUnitOption(String id, Specification specification) {
        super(id, specification);
    }

    public AbstractUnitOption clone() {
        AbstractUnitOption result = new AbstractUnitOption(getId(), getSpecification());
        if (getValue() == null) {
            result.setValue(null);
        } else {
            result.setValue(getValue().clone());
        }
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

        if (!((value == null && oldValue == null)
              || value.equals(oldValue)) && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }

   /**
     * Get the <code>UnitType</code> value.
     *
     * @return a <code>UnitTypeOption</code> value
     */
    public final UnitTypeOption getUnitType() {
        return unitType;
    }

    /**
     * Set the <code>UnitType</code> value.
     *
     * @param newUnitType The new UnitType value.
     */
    public final void setUnitType(final UnitTypeOption newUnitType) {
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
     */
    public void generateChoices() {
        unitType.generateChoices();
        List<String> roles = new ArrayList<String>();
        for (Role r : Role.values()) {
            roles.add(r.getId());
        }
        role.setChoices(roles);
    }

    /**
     * Returns whether <code>null</code> is an acceptable value for
     * this Option. This method always returns <code>true</code>.
     *
     * @return true
     */
    public boolean isNullValueOK() {
        return true;
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

    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
        number = new IntegerOption(getId() + ".number", getSpecification());
        unitType = new UnitTypeOption(getId() + ".unitType", getSpecification());
        role = new StringOption(getId() + ".role", getSpecification());
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String tag = in.getLocalName();
            if ("number".equals(tag)) {
                number.readFromXMLImpl(in);
            } else if ("unitType".equals(tag)) {
                unitType.readFromXMLImpl(in);
            } else if ("role".equals(tag)) {
                role.readFromXMLImpl(in);
            }
        }

        if (unitType.getValue() != null && role.getValue() != null && number.getValue() != null) {
            setValue(new AbstractUnit(unitType.getValue(), Role.valueOf(role.getValue()), number.getValue()));
        } else {
            setValue(null);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitOption".
     */
    public static String getXMLElementTagName() {
        return "unitOption";
    }

    public String toString() {
        return getId() + " <" + value.toString() + ">";
    }

}
