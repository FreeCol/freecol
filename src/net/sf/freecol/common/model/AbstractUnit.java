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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * Contains the information necessary to create a new unit.
 */
public class AbstractUnit extends FreeColObject {

    /** The role identifier of this AbstractUnit. */
    private String roleId = Specification.DEFAULT_ROLE_ID;

    /** The number of units. */
    private int number = 1;


    /**
     * Deliberately empty constructor.
     */
    public AbstractUnit() {}

    /**
     * Create a new AbstractUnit.
     *
     * @param id The object identifier.
     * @param roleId The unit role identifier.
     * @param number A number of units.
     */
    public AbstractUnit(String id, String roleId, int number) {
        setId(id);
        this.roleId = roleId;
        this.number = number;
    }

    /**
     * Create a new AbstractUnit.
     *
     * @param unitType The type of unit to create.
     * @param roleId The unit role identifier.
     * @param number The number of units.
     */
    public AbstractUnit(UnitType unitType, String roleId, int number) {
        this(unitType.getId(), roleId, number);
    }

    /**
     * Creates a new <code>AbstractUnit</code> instance.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if an error occurs
     */
    public AbstractUnit(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }


    /**
     * Get the role identifier.
     *
     * @return The role identifier.
     */
    public final String getRoleId() {
        return roleId;
    }

    /**
     * Set the role identifier.
     *
     * @param roleId The new role identifier.
     */
    public final void setRoleId(final String roleId) {
        this.roleId = roleId;
    }

    /**
     * Get the number of units.
     *
     * @return The number of units.
     */
    public final int getNumber() {
        return number;
    }

    /**
     * Set the number of units.
     *
     * @param newNumber The new number of units.
     */
    public final void setNumber(final int newNumber) {
        this.number = newNumber;
    }

    /**
     * Gets a template describing this abstract unit.
     *
     * @return A <code>StringTemplate</code> describing the abstract unit.
     */
    public StringTemplate getLabel() {
        StringTemplate tmpl = Messages.getUnitLabel(null, getId(), getNumber(),
                                                    null, getRoleId(), null);
        return StringTemplate.template("model.abstractUnit.label")
                             .addAmount("%number%", getNumber())
                             .addStringTemplate("%unit%", tmpl);
    }

    /**
     * Get a description of this abstract unit.
     *
     * @return A <code>String</code> describing this abstract unit.
     */
    public String getDescription() {
        return Messages.message(getLabel());
    }

    /**
     * Convenience accessor for the role.
     *
     * @param spec A <code>Specification</code> to look up the role in.
     * @return The <code>Role</code> of this abstract unit.
     */
    public Role getRole(Specification spec) {
        return spec.getRole(getRoleId());
    }

    /**
     * Convenience accessor for the unit type.
     *
     * @param spec A <code>Specification</code> to look up the type in.
     * @return The <code>UnitType</code> of this abstract unit.
     */
    public UnitType getType(Specification spec) {
        return spec.getUnitType(getId());
    }

    /**
     * Get the approximate offence power that an instantiated unit
     * corresponding to this abstract form would have.
     *
     * @param spec A <code>Specification</code> to look up.
     * @return The approximate offence power.
     */
    public double getOffence(Specification spec) {
        int n = getNumber();
        Role role = getRole(spec);
        UnitType type = spec.getUnitType(getId());
        return n * (type.getOffence() + role.getOffence());
    }

    /**
     * Calculate the approximate offence power of a list of units.
     *
     * @param spec A <code>Specification</code> to look up the type in.
     * @param units A list of <code>AbstractUnit</code>s.
     * @return The approximate offence power.
     */
    public static double calculateStrength(Specification spec,
                                           List<AbstractUnit> units) { 
        return units.stream().mapToDouble(au -> au.getOffence(spec)).sum();
    }

    /**
     * Get a deep copy of a list of abstract units.
     *
     * @param units The list of <code>AbstractUnit</code>s to copy.
     * @return A list of <code>AbstractUnit</code>s.
     */
    public static List<AbstractUnit> deepCopy(List<AbstractUnit> units) {
        List<AbstractUnit> result = new ArrayList<>();
        for (AbstractUnit au : units) {
            result.add(new AbstractUnit(au.getId(), au.getRoleId(),
                                        au.getNumber()));
        }
        return result;
    }

    /**
     * Get a template for a list of abstract units.
     *
     * @param base The label template base.
     * @param units The list of <code>AbstractUnit</code>s to use.
     * @return A suitable <code>StringTemplate</code>.
     */
    public static StringTemplate getListLabel(String base,
                                              List<AbstractUnit> units) {
        StringTemplate template = StringTemplate.label(base);
        for (AbstractUnit au : units) {
            template.addStringTemplate(au.getLabel());
        }
        return template;
    }


    // Serialization

    private static final String ROLE_TAG = "role";
    private static final String NUMBER_TAG = "number";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(ROLE_TAG, roleId);

        xw.writeAttribute(NUMBER_TAG, number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        roleId = xr.getAttribute(ROLE_TAG, Specification.DEFAULT_ROLE_ID);
        // @compat 0.10.7
        roleId = Role.fixRoleId(roleId);
        // end @compat

        number = xr.getAttribute(NUMBER_TAG, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append(number).append(" ").append(getId())
            .append(" (").append(roleId).append(")");
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
     * @return "abstractUnit".
     */
    public static String getXMLElementTagName() {
        return "abstractUnit";
    }
}
