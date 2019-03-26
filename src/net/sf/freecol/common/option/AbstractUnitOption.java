/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.option.UnitTypeOption.TypeSelector;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Represents an option where the valid choice is an AbstractUnit.
 */
public class AbstractUnitOption extends AbstractOption<AbstractUnit> {

    private static final Logger logger = Logger.getLogger(AbstractUnitOption.class.getName());

    public static final String TAG = "unitOption";

    private static final TypeSelector DEFAULT_SELECTOR = TypeSelector.UNITS;
    
    /** The value of this option. */
    private AbstractUnit value = null;

    /** An Option to determine the UnitType of the AbstractUnit. */
    private UnitTypeOption unitTypeOption = null;

    /** An Option to determine the Role of the AbstractUnit. */
    private StringOption roleOption = null;

    /** An Option to determine the number of the AbstractUnit. */
    private IntegerOption numberOption = null;

    /** An optional nation type for the unit. */
    private NationType nationType = null;


    /**
     * Creates a new {@code AbstractUnitOption}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public AbstractUnitOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new {@code AbstractUnitOption}.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public AbstractUnitOption(String id, Specification specification) {
        super(id, specification);
    }


    private void requireUnitTypeOption() {
        if (this.unitTypeOption == null) {
            this.unitTypeOption = new UnitTypeOption(getId() + ".unitType",
                                                     getSpecification());
        }
    }

    private void requireRoleOption() {
        if (this.roleOption == null) {
            this.roleOption = new StringOption(getId() + ".role",
                                               getSpecification());
        }
    }

    private void requireNumberOption() {
        if (this.numberOption == null) {
            this.numberOption = new IntegerOption(getId() + ".number",
                                                  getSpecification());
        }
    }

    /**
     * Get the unit type option.
     *
     * @return The {@code UnitTypeOption} containing the unit type.
     */
    public final UnitTypeOption getUnitType() {
        return this.unitTypeOption;
    }

    /**
     * Get the role option.
     *
     * @return The {@code StringOption} containing the role.
     */
    public final StringOption getRole() {
        return this.roleOption;
    }

    /**
     * Get the number option.
     *
     * @return The {@code IntegerOption} containing the number.
     */
    public final IntegerOption getNumber() {
        return this.numberOption;
    }

    /**
     * Get the nation type.
     *
     * @return The optional {@code NationType}.
     */
    public final NationType getNationType() {
        return this.nationType;
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractUnitOption cloneOption() {
        final Specification spec = getSpecification();
        AbstractUnitOption result = new AbstractUnitOption(getId(), spec);
        result.setValues(this);
        if (value != null) {
            AbstractUnit au = new AbstractUnit(value.getType(spec),
                value.getRoleId(), value.getNumber());
            result.setValue(au);
        }
        if (unitTypeOption != null) result.unitTypeOption = unitTypeOption.cloneOption();
        if (roleOption != null) result.roleOption = roleOption.cloneOption();
        if (numberOption != null) result.numberOption = numberOption.cloneOption();
        result.nationType = nationType;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractUnit getValue() {
        return this.value;
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
            this.unitTypeOption = null;
            this.roleOption = null;
            this.numberOption = null;
        } else {
            requireUnitTypeOption();
            this.unitTypeOption.setValue(value.getType(spec));
            requireRoleOption();
            this.roleOption.setValue(value.getRoleId());
            requireNumberOption();
            this.numberOption.setValue(value.getNumber());
        }

        if (isDefined && (((this.value == null) != (oldValue == null))
                || (this.value != null && !this.value.equals(oldValue)))) {
            firePropertyChange(VALUE_TAG, oldValue, this.value);
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
        this.unitTypeOption.generateChoices();
        if (this.roleOption.getChoices().isEmpty()
            && this.roleOption.getValue() != null) {
            List<String> op = Collections.singletonList(roleOption.getValue());
            this.roleOption.setChoices(op);
        }
    }


    // Serialization

    private static final String NATION_TYPE_TAG = "nationType";
    private static final String NUMBER_TAG = "number";
    private static final String ROLE_TAG = "role";
    private static final String UNIT_TYPE_TAG = "unitType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (nationType != null) xw.writeAttribute(NATION_TYPE_TAG, nationType);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        numberOption.toXML(xw, NUMBER_TAG);

        roleOption.toXML(xw, ROLE_TAG);

        unitTypeOption.toXML(xw, UNIT_TYPE_TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();
        nationType = xr.getType(spec, NATION_TYPE_TAG,
                                NationType.class, (NationType)null);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        AbstractUnit au = null;
        if (unitTypeOption != null
            && roleOption != null
            && numberOption != null) {
            au = new AbstractUnit(unitTypeOption.getValue(),
                                  roleOption.getValue(),
                                  numberOption.getValue());
        }
        setValue(au);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (tag != null) {
            switch (tag) {
            case NUMBER_TAG:
                requireNumberOption();
                numberOption.readFromXML(xr);
                break;
            case ROLE_TAG:
                requireRoleOption();
                roleOption.readFromXML(xr);
                break;
            case UNIT_TYPE_TAG:
                requireUnitTypeOption();
                unitTypeOption.readFromXML(xr);
                break;
            default:
                super.readChild(xr);
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append('[').append(getId())
            .append(' ').append(this.value);
        if (this.unitTypeOption != null) {
            sb.append(' ').append(this.unitTypeOption);
        }
        if (this.roleOption != null) {
            sb.append(' ').append(this.roleOption);
        }
        if (this.numberOption != null) {
            sb.append(' ').append(this.numberOption);
        }
        sb.append(']');
        return sb.toString();
    }
}
