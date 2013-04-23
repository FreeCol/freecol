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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Unit.Role;


public class EquipmentType extends BuildableType {

    public static final EquipmentType[] NO_EQUIPMENT = new EquipmentType[0];

    /**
     * The maximum number of equipment items that can be combined.
     */
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

    /**
     * The default Role of the Unit carrying this type of Equipment.
     */
    private Role role = Role.DEFAULT;

    /**
     * Is this military equipment?
     */
    private boolean militaryEquipment = false;

    /**
     * A list containing the ids of equipment types compatible with this one.
     */
    private List<String> compatibleEquipment = null;


    /**
     * Simple constructor.
     *
     * @param id The object identifier.
     * @param specification The containing <code>Specification</code>.
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
     * Get the role for this equipment type.
     *
     * @return The equipment related role.
     */
    public final Role getRole() {
        return role;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
            * result
            + ((compatibleEquipment == null) ? 0 : compatibleEquipment
               .hashCode());
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        return result;
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
    private static final String REQUIRED_LOCATION_ABILITY_TAG = "required-location-ability";
    private static final String ROLE_TAG = "role";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, MAXIMUM_COUNT_TAG, maximumCount);

        writeAttribute(out, COMBAT_LOSS_PRIORITY_TAG, combatLossPriority);
        
        writeAttribute(out, ROLE_TAG, role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (captureEquipmentId != null) {
            out.writeStartElement(CAPTURE_EQUIPMENT_TAG);

            writeAttribute(out, ID_ATTRIBUTE_TAG, captureEquipmentId);

            writeAttribute(out, BY_INDIANS_TAG, captureEquipmentByIndians);

            out.writeEndElement();
        }

        if (compatibleEquipment != null) {
            for (String compatible : compatibleEquipment) {
                out.writeStartElement(COMPATIBLE_EQUIPMENT_TAG);
                
                writeAttribute(out, ID_ATTRIBUTE_TAG, compatible);
                
                out.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        maximumCount = getAttribute(in, MAXIMUM_COUNT_TAG, 1);

        combatLossPriority = getAttribute(in, COMBAT_LOSS_PRIORITY_TAG, -1);

        role = getAttribute(in, ROLE_TAG, Role.class, Role.DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            captureEquipmentId = null;
            captureEquipmentByIndians = false;
            compatibleEquipment = null;
        }

        super.readChildren(in);

        for (Modifier modifier : getModifiers()) {
            if (modifier.getId().equals(Modifier.OFFENCE)
                || modifier.getId().equals(Modifier.DEFENCE)) {
                militaryEquipment = true;
                for (AbstractGoods goods : getRequiredGoods()) {
                    goods.getType().setMilitaryGoods(true);
                }
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (CAPTURE_EQUIPMENT_TAG.equals(tag)) {
            captureEquipmentId = readId(in);
            captureEquipmentByIndians = getAttribute(in, BY_INDIANS_TAG, false);
            closeTag(in, CAPTURE_EQUIPMENT_TAG);

        } else if (COMPATIBLE_EQUIPMENT_TAG.equals(tag)) {
            String equipmentId = readId(in);
            if (equipmentId != null) {
                if (compatibleEquipment == null) {
                    compatibleEquipment = new ArrayList<String>();
                }
                compatibleEquipment.add(equipmentId);
            }
            closeTag(in, COMPATIBLE_EQUIPMENT_TAG);

        // @compat 0.10.0
        } else if (REQUIRED_LOCATION_ABILITY_TAG.equals(tag)) {
            String abilityId = readId(in);
            Map<String, Boolean> required = getRequiredAbilities();
            required.put(abilityId, getAttribute(in, VALUE_TAG, true));
            setRequiredAbilities(required);
            spec.addAbility(abilityId);
            closeTag(in, REQUIRED_LOCATION_ABILITY_TAG);
        // end @compat

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "equipment-type".
     */
    public static String getXMLElementTagName() {
        return "equipment-type";
    }
}
