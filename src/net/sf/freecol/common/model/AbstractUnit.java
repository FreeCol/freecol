/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * Contains the information necessary to create a new unit.
 */
public class AbstractUnit extends FreeColObject {

    /** The role identifier of this AbstractUnit. */
    private String roleId = "model.role.default";

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
     * Create a copy of this abstract unit.
     *
     * @return The copy.
     */
    public AbstractUnit clone() {
        return new AbstractUnit(getId(), getRoleId(), getNumber());
    }


    /**
     * Get the <code>UnitType</code> value.
     *
     * @param specification The <code>Specification</code> to refer to.
     * @return The unit type.
     */
    public final UnitType getUnitType(Specification specification) {
        return specification.getUnitType(getId());
    }

    /**
     * Get the <code>Role</code> value.
     *
     * @param specification The <code>Specification</code> to refer to.
     * @return The unit role.
     */
    public final Role getRole(Specification specification) {
        return specification.getRole(roleId);
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
     * Gets a description of this abstract unit.
     *
     * @return A <code>StringTemplate</code> describing the abstract unit.
     */
    public StringTemplate getLabel() {
        StringTemplate unitTemplate = Messages.getLabel(getId(), getRoleId(),
                                                        getNumber());
        return StringTemplate.template("abstractUnit")
                             .addAmount("%number%", getNumber())
                             .addStringTemplate("%unit%", unitTemplate);
    }

    /**
     * Gets the equipment necessary to create a Unit with the same
     * type and role as this AbstractUnit.
     *
     * @param spec A <code>Specification<code> to query.
     * @return An array of equipment types.
     */
    public EquipmentType[] getEquipment(Specification spec) {
        List<EquipmentType> equipment = new ArrayList<EquipmentType>();
        EquipmentType[] types = {
            ("model.role.armedBrave".equals(roleId))
            ? spec.getEquipmentType("model.equipment.indian.muskets")
            : ("model.role.mountedBrave".equals(roleId))
            ? spec.getEquipmentType("model.equipment.indian.horses")
            : ("model.role.nativeDragoon".equals(roleId))
            ? spec.getEquipmentType("model.equipment.indian.muskets")
            : ("model.role.pioneer".equals(roleId))
            ? spec.getEquipmentType("model.equipment.tools")
            : ("model.role.missionary".equals(roleId))
            ? spec.getEquipmentType("model.equipment.missionary")
            : ("model.role.scout".equals(roleId))
            ? spec.getEquipmentType("model.equipment.horses")
            : ("model.role.soldier".equals(roleId)
                || "model.role.infantry".equals(roleId))
            ? spec.getEquipmentType("model.equipment.muskets")
            : ("model.role.dragoon".equals(roleId)
                || "model.role.cavalry".equals(roleId))
            ? spec.getEquipmentType("model.equipment.muskets")
            : null,
            ("model.role.dragoon".equals(roleId)
                || "model.role.cavalry".equals(roleId))
            ? spec.getEquipmentType("model.equipment.horses")
            : ("model.role.nativeDragoon".equals(roleId))
            ? spec.getEquipmentType("model.equipment.indian.horses")
            : null
        };
        for (EquipmentType et : types) {
            if (et == null) continue;
            for (int count = 0; count < et.getMaximumCount(); count++) {
                equipment.add(et);
            }
        }
        return equipment.toArray(new EquipmentType[equipment.size()]);
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

        roleId = xr.getAttribute(ROLE_TAG, "model.role.default");

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
