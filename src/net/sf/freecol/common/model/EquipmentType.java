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
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * This whole class is now @compat 0.10.x.  We no longer use equipment types.
 *
 * EquipmentTypes are now subsumed by roles.
 * Delete this whole file in due course.
 *
 * A type of equipment.  Equipment differs from goods (although it is often
 * derived from it) in that it must be attached to a unit.
 */
public class EquipmentType extends BuildableType {

    public static final EquipmentType[] NO_EQUIPMENT = new EquipmentType[0];

    /** The maximum number of equipment items that can be combined. */
    private int maximumCount = 1;

    /**
     * Determines which type of Equipment will be lost first if the
     * Unit carrying it is defeated.  Horses should be lost before
     * Muskets, for example.
     */
    private int combatLossPriority = -1;

    /**
     * What this equipment type becomes if it is captured by Indians
     * (if captureEquipmentByIndians is true) or Europeans (otherwise).
     */
    private String captureEquipmentId = null;
    private boolean captureEquipmentByIndians = false;

    /** The default Role of the Unit carrying this type of Equipment. */
    private Role role = null;

    /** Is this military equipment? */
    private boolean militaryEquipment = false;

    /**
     * A list containing the object identifiers of equipment types
     * compatible with this one.
     */
    private List<String> compatibleEquipment = null;


    /**
     * Simple constructor.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public EquipmentType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the maximum combinable amount of this equipment type.
     *
     * @return The maximum combinable count.
     */
    public final int getMaximumCount() {
        return maximumCount;
    }

    /**
     * Get the combat loss priority.
     *
     * @return The combat loss priority.
     */
    public final int getCombatLossPriority() {
        return combatLossPriority;
    }

    /**
     * Can this equipment type be captured in combat?
     *
     * @return True if this equipment can be captured.
     */
    public boolean canBeCaptured() {
        return combatLossPriority > 0;
    }

    /**
     * Get the type of equipment to capture, handling the case where
     * Europeans and Indians use different <code>EquipmentType</code>s
     * for the same underlying goods.
     *
     * @param byIndians Is the capture by the Indians?
     * @return The captured <code>EquipmentType</code>.
     */
    public EquipmentType getCaptureEquipment(boolean byIndians) {
        return (captureEquipmentId != null
                && byIndians == captureEquipmentByIndians)
            ? getSpecification().getEquipmentType(captureEquipmentId)
            : this;
    }

    /**
     * Is this type of equipment compatible with the given type of equipment?
     *
     * @param otherType The other <code>EquipmentType</code>.
     * @return True if the equipment is compatible.
     */
    public boolean isCompatibleWith(EquipmentType otherType) {
        if (this.getId().equals(otherType.getId())) {
            // model.equipment.tools for example
            return true;
        }
        return compatibleEquipment != null
            && compatibleEquipment.contains(otherType.getId())
            && otherType.compatibleEquipment.contains(getId());
    }

    /**
     * Add a compatible equipment identifier.
     *
     * @param equipmentId The equipment identifier.
     */
    private void addCompatibleEquipment(String equipmentId) {
        if (compatibleEquipment == null) compatibleEquipment = new ArrayList<>();
        compatibleEquipment.add(equipmentId);
    }

    /**
     * Get the role for this equipment type.
     *
     * @return The equipment related role.
     */
    public final Role getRole() {
        return role;
    }

    /**
     * Set the role for this equipment type.
     *
     * @param role The new equipment related <code>Role</code>.
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Is this military equiment?
     * (True if it grants an offensive or defensive bonus)
     *
     * @return True if this is military equipment.
     */
    public final boolean isMilitaryEquipment() {
        return militaryEquipment;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result
            + ((compatibleEquipment == null) ? 0 : compatibleEquipment
               .hashCode());
        return 37 * result + ((getId() == null) ? 0 : getId().hashCode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EquipmentType other = (EquipmentType) obj;
        if (compatibleEquipment == null) {
            if (other.compatibleEquipment != null)
                return false;
        } else if (!compatibleEquipment.equals(other.compatibleEquipment))
            return false;
        if (getId() == null) {
            if (other.getId() != null)
                return false;
        } else if (!getId().equals(other.getId()))
            return false;
        return true;
    }


    // Serialization

    private static final String BY_INDIANS_TAG = "by-indians";
    private static final String CAPTURE_EQUIPMENT_TAG = "capture-equipment";
    private static final String COMBAT_LOSS_PRIORITY_TAG = "combat-loss-priority";
    private static final String COMPATIBLE_EQUIPMENT_TAG = "compatible-equipment";
    private static final String MAXIMUM_COUNT_TAG = "maximum-count";
    private static final String ROLE_TAG = "role";
    // @compat 0.10.0
    private static final String REQUIRED_LOCATION_ABILITY_TAG = "required-location-ability";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(MAXIMUM_COUNT_TAG, maximumCount);

        xw.writeAttribute(COMBAT_LOSS_PRIORITY_TAG, combatLossPriority);
        
        xw.writeAttribute(ROLE_TAG, role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (captureEquipmentId != null) {
            xw.writeStartElement(CAPTURE_EQUIPMENT_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, captureEquipmentId);

            xw.writeAttribute(BY_INDIANS_TAG, captureEquipmentByIndians);

            xw.writeEndElement();
        }

        if (compatibleEquipment != null) {
            for (String compatible : compatibleEquipment) {
                xw.writeStartElement(COMPATIBLE_EQUIPMENT_TAG);
                
                xw.writeAttribute(ID_ATTRIBUTE_TAG, compatible);
                
                xw.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        maximumCount = xr.getAttribute(MAXIMUM_COUNT_TAG, 1);

        combatLossPriority = xr.getAttribute(COMBAT_LOSS_PRIORITY_TAG, -1);

        role = xr.getRole(getSpecification(), ROLE_TAG, Role.class,
                          getSpecification().getDefaultRole());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            captureEquipmentId = null;
            captureEquipmentByIndians = false;
            compatibleEquipment = null;
        }

        super.readChildren(xr);

        for (Modifier modifier : getModifiers()) {
            if (Modifier.OFFENCE.equals(modifier.getId())
                || Modifier.DEFENCE.equals(modifier.getId())) {
                militaryEquipment = true;
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (CAPTURE_EQUIPMENT_TAG.equals(tag)) {
            captureEquipmentId = xr.readId();
            captureEquipmentByIndians = xr.getAttribute(BY_INDIANS_TAG, false);
            xr.closeTag(CAPTURE_EQUIPMENT_TAG);

        } else if (COMPATIBLE_EQUIPMENT_TAG.equals(tag)) {
            addCompatibleEquipment(xr.readId());
            xr.closeTag(COMPATIBLE_EQUIPMENT_TAG);

        // @compat 0.10.0
        } else if (REQUIRED_LOCATION_ABILITY_TAG.equals(tag)) {
            String abilityId = xr.readId();
            Map<String, Boolean> required = getRequiredAbilities();
            required.put(abilityId, xr.getAttribute(VALUE_TAG, true));
            setRequiredAbilities(required);
            spec.addAbility(abilityId);
            xr.closeTag(REQUIRED_LOCATION_ABILITY_TAG);
        // end @compat

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "equipment-type".
     */
    public static String getXMLElementTagName() {
        return "equipment-type";
    }
}
