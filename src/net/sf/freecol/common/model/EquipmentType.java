/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Unit.Role;

public class EquipmentType extends BuildableType {

    public static final EquipmentType NO_EQUIPMENT = new EquipmentType();

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
     * The default Role of the Unit carrying this type of Equipment.
     */
    private Role role;

    /**
     * Describe militaryEquipment here.
     */
    private boolean militaryEquipment;

    /**
     * Stores the abilities required of a unit to be equipped.
     */
    private HashMap<String, Boolean> requiredUnitAbilities = new HashMap<String, Boolean>();
    
    /**
     * Stores the abilities required of the location where the unit is
     * to be equipped.
     */
    private HashMap<String, Boolean> requiredLocationAbilities = new HashMap<String, Boolean>();
    
    /**
     * A List containing the IDs of equipment types compatible with this one.
     */
    private List<String> compatibleEquipment = new ArrayList<String>();


    public EquipmentType() {}

    public EquipmentType(int index) {
        setIndex(index);
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
     * Returns true if this EquipmentType can be captured in combat.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canBeCaptured() {
        return (combatLossPriority > 0);
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
     * Returns the abilities required by this Type.
     *
     * @return the abilities required by this Type.
     */
    public Map<String, Boolean> getUnitAbilitiesRequired() {
        return requiredUnitAbilities;
    }

    /**
     * Returns the abilities required by this Type.
     *
     * @return the abilities required by this Type.
     */
    public Map<String, Boolean> getLocationAbilitiesRequired() {
        return requiredLocationAbilities;
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


    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readFromXML(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        maximumCount = getAttribute(in, "maximum-count", 1);
        combatLossPriority = getAttribute(in, "combat-loss-priority", 0);
        String roleString = getAttribute(in, "role", "default");
        role = Enum.valueOf(Role.class, roleString.toUpperCase());

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String nodeName = in.getLocalName();
            if (Ability.getXMLElementTagName().equals(nodeName)) {
                Ability ability = new Ability(in);
                if (ability.getSource() == null) {
                    ability.setSource(getNameKey());
                }
                addAbility(ability);
                specification.getAbilityKeys().add(ability.getId());
            } else if ("required-ability".equals(nodeName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                getUnitAbilitiesRequired().put(abilityId, value);
                in.nextTag(); // close this element
                specification.getAbilityKeys().add(abilityId);
            } else if ("required-location-ability".equals(nodeName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                getLocationAbilitiesRequired().put(abilityId, value);
                in.nextTag(); // close this element
                specification.getAbilityKeys().add(abilityId);
            } else if ("required-goods".equals(nodeName)) {
                GoodsType type = specification.getGoodsType(in.getAttributeValue(null, "id"));
                int amount = getAttribute(in, "value", 0);
                AbstractGoods requiredGoods = new AbstractGoods(type, amount);
                if (getGoodsRequired() == null) {
                    setGoodsRequired(new ArrayList<AbstractGoods>());
                }
                getGoodsRequired().add(requiredGoods);
                in.nextTag(); // close this element
            } else if ("compatible-equipment".equals(nodeName)) {
                String equipmentId = in.getAttributeValue(null, "id");
                compatibleEquipment.add(equipmentId);
                in.nextTag(); // close this element
            } else if (Modifier.getXMLElementTagName().equals(nodeName)) {
                Modifier modifier = new Modifier(in); // Modifier close the element
                if (modifier.getSource() == null) {
                    modifier.setSource(getNameKey());
                }
                addModifier(modifier);
                specification.getModifierKeys().add(modifier.getId());
                if (modifier.getId().equals(Modifier.OFFENCE) ||
                    modifier.getId().equals(Modifier.DEFENCE)) {
                    militaryEquipment = true;
                }
            } else {
                logger.finest("Parsing of " + nodeName + " is not implemented yet");
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT ||
                        !in.getLocalName().equals(nodeName)) {
                    in.nextTag();
                }
            }
        }

        if (militaryEquipment) {
            for (AbstractGoods goods : getGoodsRequired()) {
                goods.getType().setMilitaryGoods(true);
            }
        }

    }
}
