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
     * Unit carrying it is defeated. Horses should be lost before
     * Muskets, for example.
     */
    private int combatLossPriority;

    /**
     * What this equipment type becomes if it is captured by Indians
     * (if captureEquipmentByIndians is true) or Europeans (otherwise).
     */
    private String captureEquipmentId = null;
    private boolean captureEquipmentByIndians = false;

    /**
     * The default Role of the Unit carrying this type of Equipment.
     */
    private Role role;

    /**
     * Describe militaryEquipment here.
     */
    private boolean militaryEquipment;

    /**
     * A List containing the IDs of equipment types compatible with this one.
     */
    private List<String> compatibleEquipment = new ArrayList<String>();


    public EquipmentType(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Get the <code>MaximumCount</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMaximumCount() {
        return maximumCount;
    }

    /**
     * Set the <code>MaximumCount</code> value.
     *
     * @param newMaximumCount The new MaximumCount value.
     */
    public final void setMaximumCount(final int newMaximumCount) {
        this.maximumCount = newMaximumCount;
    }

    /**
     * Get the <code>Role</code> value.
     *
     * @return a <code>Role</code> value
     */
    public final Role getRole() {
        return role;
    }

    /**
     * Set the <code>Role</code> value.
     *
     * @param newRole The new Role value.
     */
    public final void setRole(final Role newRole) {
        this.role = newRole;
    }

    /**
     * Get the <code>CombatLossPriority</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getCombatLossPriority() {
        return combatLossPriority;
    }

    /**
     * Set the <code>CombatLossPriority</code> value.
     *
     * @param newCombatLossPriority The new CombatLossPriority value.
     */
    public final void setCombatLossPriority(final int newCombatLossPriority) {
        this.combatLossPriority = newCombatLossPriority;
    }

    /**
     * Returns true if this EquipmentType can be captured in combat.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canBeCaptured() {
        return (combatLossPriority > 0);
    }

    /**
     * Get the type of equipment to capture, handling the case where
     * Europeans and Indians use different <code>EquipmentType</code>s
     * for the same underlying goods.
     *
     * @param byIndians is the capture by the Indians?
     *
     * @return an <code>EquipmentType</code> value
     */
    public EquipmentType getCaptureEquipment(boolean byIndians) {
        return (captureEquipmentId != null
                && byIndians == captureEquipmentByIndians)
            ? getSpecification().getEquipmentType(captureEquipmentId)
            : this;
    }

    /**
     * Returns the abilities required by this Type.
     *
     * @return the abilities required by this Type.
     */
    public Map<String, Boolean> getUnitAbilitiesRequired() {
        return getAbilitiesRequired();
    }

    /**
     * Returns true if this type of equipment is compatible with the
     * given type of equipment.
     *
     * @param otherType an <code>EquipmentType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isCompatibleWith(EquipmentType otherType) {
        if (this.getId().equals(otherType.getId())) {
            // model.equipment.tools for example
            return true;
        }
        return compatibleEquipment.contains(otherType.getId()) &&
            otherType.compatibleEquipment.contains(getId());
    }

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

    /**
     * Returns true if Equipment of this type grants an offence bonus
     * or a defence bonus.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isMilitaryEquipment() {
        return militaryEquipment;
    }

    /**
     * Set the <code>MilitaryEquipment</code> value.
     *
     * @param newMilitaryEquipment The new MilitaryEquipment value.
     */
    public final void setMilitaryEquipment(final boolean newMilitaryEquipment) {
        this.militaryEquipment = newMilitaryEquipment;
    }


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("maximum-count", Integer.toString(maximumCount));
        out.writeAttribute("combat-loss-priority",
            Integer.toString(combatLossPriority));
        out.writeAttribute("role", role.toString().toLowerCase(Locale.US));
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        for (String compatible : compatibleEquipment) {
            out.writeStartElement("compatible-equipment");
            out.writeAttribute(ID_ATTRIBUTE_TAG, compatible);
            out.writeEndElement();
        }

        if (captureEquipmentId != null) {
            out.writeStartElement("capture-equipment");
            out.writeAttribute(ID_ATTRIBUTE_TAG, captureEquipmentId);
            out.writeAttribute("by-indians",
                Boolean.toString(captureEquipmentByIndians));
            out.writeEndElement();
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        maximumCount = getAttribute(in, "maximum-count", 1);
        combatLossPriority = getAttribute(in, "combat-loss-priority", -1);
        String roleString = getAttribute(in, "role", "default");
        role = Enum.valueOf(Role.class, roleString.toUpperCase(Locale.US));
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        super.readChildren(in);

        for (Modifier modifier : getModifiers()) {
            if (modifier.getId().equals(Modifier.OFFENCE) ||
                modifier.getId().equals(Modifier.DEFENCE)) {
                militaryEquipment = true;
                for (AbstractGoods goods : getGoodsRequired()) {
                    goods.getType().setMilitaryGoods(true);
                }
                break;
            }
        }
    }

    /**
     * Reads a child object.
     *
     * @param in The XML stream to read.
     * @exception XMLStreamException if an error occurs
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        String nodeName = in.getLocalName();
        if ("required-location-ability".equals(nodeName)) {
            // @compat 0.10.0
            String abilityId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            boolean value = getAttribute(in, VALUE_TAG, true);
            getAbilitiesRequired().put(abilityId, value);
            getSpecification().addAbility(abilityId);
            in.nextTag(); // close this element
            // end compatibility code
        } else if ("compatible-equipment".equals(nodeName)) {
            String equipmentId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            compatibleEquipment.add(equipmentId);
            in.nextTag(); // close this element
        } else if ("capture-equipment".equals(nodeName)) {
            captureEquipmentId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            captureEquipmentByIndians = getAttribute(in, "by-indians", false);
            in.nextTag();
        } else {
            super.readChild(in);
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "equipment-type".
     */
    public static String getXMLElementTagName() {
        return "equipment-type";
    }
}
