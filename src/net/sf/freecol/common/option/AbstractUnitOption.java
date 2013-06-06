/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

    /** The value of this option. */
    private AbstractUnit value = null;

    /** An Option to determine the UnitType of the AbstractUnit. */
    private UnitTypeOption unitType = null;

    /** An Option to determine the Role of the AbstractUnit. */
    private StringOption role = null;

    /** An Option to determine the number of the AbstractUnit. */
    private IntegerOption number = null;


    /**
     * Creates a new <code>AbstractUnitOption</code>.
     *
     * @param id The object identifier.
     */
    public AbstractUnitOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>AbstractUnitOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public AbstractUnitOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>AbstractUnitOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public AbstractUnitOption(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the unit type.
     *
     * @return The <code>UnitTypeOption</code> containing the unit type.
     */
    public final UnitTypeOption getUnitType() {
        return unitType;
    }

    /**
     * Get the role.
     *
     * @return The <code>StringOption</code> containing the role.
     */
    public final StringOption getRole() {
        return role;
    }

    /**
     * Get the number.
     *
     * @return The <code>IntegerOption</code> containing the number.
     */
    public final IntegerOption getNumber() {
        return number;
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    public AbstractUnitOption clone() {
        AbstractUnitOption result = new AbstractUnitOption(getId());
        result.setValues(this);
        if (value != null) result.setValue(value.clone());
        if (unitType != null) result.unitType = unitType.clone();
        if (role != null) result.role = role.clone();
        if (number != null) result.number = number.clone();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public AbstractUnit getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(AbstractUnit value) {
        final AbstractUnit oldValue = this.value;
        this.value = value;

        if (isDefined && (((value == null) != (oldValue == null))
                || (value != null && !value.equals(oldValue)))) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    public boolean isNullValueOK() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void generateChoices() {
        unitType.generateChoices();
        List<String> roles = new ArrayList<String>();
        for (Role r : Role.values()) {
            roles.add(r.getId());
        }
        role.setChoices(roles);
    }


    // Serialization

    private static final String NUMBER_TAG = "number";
    private static final String ROLE_TAG = "role";
    private static final String UNIT_TYPE_TAG = "unitType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        number.toXML(out, NUMBER_TAG);

        role.toXML(out, ROLE_TAG);

        unitType.toXML(out, UNIT_TYPE_TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        super.readChildren(in);

        AbstractUnit au = null;
        if (unitType != null && role != null && number != null) {
            au = new AbstractUnit(unitType.getValue(),
                                  Role.valueOf(role.getValue()),
                                  number.getValue());
        }
        setValue(au);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (NUMBER_TAG.equals(tag)) {
            number = new IntegerOption(getId() + ".number", spec);
            number.readFromXML(in);

        } else if (ROLE_TAG.equals(tag)) {
            role = new StringOption(getId() + ".role", spec);
            role.readFromXML(in);

        } else if (UNIT_TYPE_TAG.equals(tag)) {
            unitType = new UnitTypeOption(getId() + ".unitType", spec);
            unitType.readFromXML(in);

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[").append(getId())
            .append(" ").append(value).append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitOption".
     */
    public static String getXMLElementTagName() {
        return "unitOption";
    }
}
