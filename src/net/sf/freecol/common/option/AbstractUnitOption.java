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

package net.sf.freecol.common.option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.option.UnitTypeOption.TypeSelector;


/**
 * Represents an option where the valid choice is an AbstractUnit.
 */
public class AbstractUnitOption extends AbstractOption<AbstractUnit> {

    private static final Logger logger = Logger.getLogger(AbstractUnitOption.class.getName());

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


    private void requireUnitType() {
        this.unitType = new UnitTypeOption(getId() + ".unitType",
                                           getSpecification());
    }

    private void requireRole() {
        this.role = new StringOption(getId() + ".role",
                                     getSpecification());
    }

    private void requireNumber() {
        this.number = new IntegerOption(getId() + ".number",
                                        getSpecification());
    }

    /**
     * Get the unit type option.
     *
     * @return The <code>UnitTypeOption</code> containing the unit type.
     */
    public final UnitTypeOption getUnitType() {
        return unitType;
    }

    /**
     * Get the role option.
     *
     * @return The <code>StringOption</code> containing the role.
     */
    public final StringOption getRole() {
        return role;
    }

    /**
     * Get the number option.
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
    @Override
    public AbstractUnitOption clone() {
        final Specification spec = getSpecification();
        AbstractUnitOption result = new AbstractUnitOption(getId(), spec);
        result.setValues(this);
        if (value != null) {
            AbstractUnit au = new AbstractUnit(value.getType(spec),
                value.getRoleId(), value.getNumber());
            result.setValue(au);
        }
        if (unitType != null) result.unitType = unitType;
        if (role != null) result.role = role;
        if (number != null) result.number = number;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractUnit getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(AbstractUnit value) {
        final Specification spec = getSpecification();
        final AbstractUnit oldValue = this.value;
        this.value = value;
        if (value == null) {
            this.unitType = null;
            this.role = null;
            this.number = null;
        } else {
            requireUnitType();
            this.unitType.setValue(value.getType(spec));
            requireRole();
            this.role.setValue(value.getRoleId());
            requireNumber();
            this.number.setValue(value.getNumber());
        }

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
    @Override
    public boolean isNullValueOK() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateChoices() {
        unitType.generateChoices();
        List<String> roles = new ArrayList<>();
        for (Role r : getSpecification().getRoles()) {
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
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        number.toXML(xw, NUMBER_TAG);

        role.toXML(xw, ROLE_TAG);

        unitType.toXML(xw, UNIT_TYPE_TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        AbstractUnit au = null;
        if (unitType != null && role != null && number != null) {
            au = new AbstractUnit(unitType.getValue(),
                                  role.getValue(),
                                  number.getValue());
        }
        setValue(au);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (null != tag) switch (tag) {
            case NUMBER_TAG:
                requireNumber();
                number.readFromXML(xr);
                break;
            case ROLE_TAG:
                requireRole();
                role.readFromXML(xr);
                break;
            case UNIT_TYPE_TAG:
                requireUnitType();
                unitType.readFromXML(xr);
                break;
            default:
                super.readChild(xr);
                break;
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
    @Override
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
