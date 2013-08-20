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
import java.util.Comparator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.util.Utils;


/**
 * The role of a unit.
 */
public class Role extends BuildableType implements Comparable<Role> {

    /**
     * The Role to downgrade to after losing a battle. Defaults to
     * <code>null</code>. Note that some UnitTypes and Roles may be
     * disposed instead of downgraded when losing a battle.
     */
    private Role downgrade;

    /**
     * The maximum multiple of required goods this Role may
     * carry.  Defaults to <code>1</code>.
     */
    private int maximumCount = 1;

    /**
     * The expert unit for this Role, e.g. a hardy pioneer is an
     * expert for the pioneer role.
     */
    private UnitType expertUnit = null;

    /** Sorts roles by defensive power, descendingly. */
    public static final Comparator<Role> defensiveComparator = new Comparator<Role>() {
        public int compare(Role role1, Role role2) {
            float defence1 = role1.getDefence();
            float defence2 = role2.getDefence();
            if (defence1 > defence2) {
                return -1;
            } else if (defence1 < defence2) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    /** Sorts roles by offensive power, descendingly. */
    public static final Comparator<Role> offensiveComparator = new Comparator<Role>() {
        public int compare(Role role1, Role role2) {
            float offence1 = role1.getOffence();
            float offence2 = role2.getOffence();
            if (offence1 > offence2) {
                return -1;
            } else if (offence1 < offence2) {
                return 1;
            } else {
                return 0;
            }
        }
    };


    // and this, too
    public List<EquipmentType> getRoleEquipment() {
        Specification spec = getSpecification();
        List<EquipmentType> result = new ArrayList<EquipmentType>();
        if (getRequiredGoods().isEmpty()) {
            result.add(spec.getEquipmentType("model.equipment.missionary"));
        } else {
            for (AbstractGoods goods : getRequiredGoods()) {
                if ("model.goods.horses".equals(goods.getType().getId())) {
                    if (requiresAbility(Ability.NATIVE)) {
                        result.add(spec.getEquipmentType("model.equipment.indian.horses"));
                    } else {
                        result.add(spec.getEquipmentType("model.equipment.horses"));
                    }
                } else if ("model.goods.muskets".equals(goods.getType().getId())) {
                    if (requiresAbility(Ability.NATIVE)) {
                        result.add(spec.getEquipmentType("model.equipment.indian.muskets"));
                    } else {
                        result.add(spec.getEquipmentType("model.equipment.muskets"));
                    }
                } else if ("model.goods.tools".equals(goods.getType().getId())) {
                    result.add(spec.getEquipmentType("model.equipment.tools"));
                }
            }
        }
        return result;
    }

    /**
     * Creates a new <code>Role</code> instance.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public Role(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get a message key for this role.
     *
     * @return A message key, which is null for the default role.
     */
    public String getRoleKey() {
        return getRoleKey(getId());
    }

    /**
     * Get the last part of a role identifier.
     *
     * @param roleId A role identifier.
     * @return The role suffix.
     */
    public static String getRoleSuffix(String roleId) {
        return Utils.lastPart(roleId, ".");
    }

    /**
     * Get a message key for a given role identifier.
     *
     * @param roleId The role identifier to query.
     * @return A message key, which is null for the default role.
     */
    public static String getRoleKey(String roleId) {
        return ("model.role.default".equals(roleId)) ? null
            : getRoleSuffix(roleId);
    }

    /**
     * Get the downgraded role from this one.
     *
     * @return The downgraded role.
     */
    public final Role getDowngrade() {
        return downgrade;
    }

    /**
     * Sets the downgraded role.
     *
     * @param newDowngrade The new downgraded role.
     */
    public final void setDowngrade(final Role newDowngrade) {
        this.downgrade = newDowngrade;
    }

    /**
     * Gets the maximum goods count for the role.
     *
     * @return The maximum goods count.
     */
    public final int getMaximumCount() {
        return maximumCount;
    }

    /**
     * Sets the maximum goods count for this role.
     *
     * @param newMaximumCount The new maximum goods count.
     */
    public final void setMaximumCount(final int newMaximumCount) {
        this.maximumCount = newMaximumCount;
    }

    /**
     * Gets the expert profession for this role.
     *
     * @return The expert type.
     */
    public final UnitType getExpertUnit() {
        return expertUnit;
    }

    /**
     * Sets the expert profession for this role.
     *
     * @param newExpertUnit The new expert type.
     */
    public final void setExpertUnit(final UnitType newExpertUnit) {
        this.expertUnit = newExpertUnit;
    }

    /**
     * Get the offense value for this role.
     *
     * @return The offense value.
     */
    private float getOffence() {
        return getFeatureContainer()
            .applyModifier(1, Modifier.OFFENCE, null, null);
    }

    /**
     * Is this an offensive role?
     *
     * @return True if this is an offensive role.
     */
    public boolean isOffensive() {
        return getOffence() > 1;
    }

    /**
     * Get the defence value for this role.
     *
     * @return The defence value.
     */
    private float getDefence() {
        return getFeatureContainer()
            .applyModifier(1, Modifier.DEFENCE, null, null);
    }

    /**
     * Is this an defensive role?
     *
     * @return True if this is an defensive role.
     */
    public boolean isDefensive() {
        return getDefence() > 1;
    }

    /**
     * Is this role compatible with another?
     *
     * @param other The other <code>Role</code> to compare with.
     * @return True if the other role is compatible.
     */
    public boolean isCompatibleWith(Role other) {
        return isCompatibleWith(this, other);
    }

    /**
     * Are two roles compatible.
     *
     * @param role1 A <code>Role</code> to compare.
     * @param role2 The other <code>Role</code> to compare.
     * @return True if the roles are compatible.
     */
    public static boolean isCompatibleWith(Role role1, Role role2) {
        if (role1 == null) {
            return role2 == null;
        } else if (role2 == null) {
            return false;
        } else {
            return role1 == role2
                || role1.getDowngrade() == role2
                || role2.getDowngrade() == role1;
        }
    }


    public List<AbstractGoods> getDowngradeGoods() {
        List<AbstractGoods> result = new ArrayList<AbstractGoods>();
        if (downgrade != null) {
            for (AbstractGoods ag : getRequiredGoods()) {
                int amount = ag.getAmount()
                    - downgrade.getRequiredAmountOf(ag.getType());
                if (amount > 0) {
                    result.add(new AbstractGoods(ag.getType(), amount));
                }
            }
        }
        return result;
    }

    public int compareTo(Role other) {
        int diff = other.getAbilityIndex() - this.getAbilityIndex();
        if (diff == 0) {
            diff = other.getRequiredGoods().size()
                - this.getRequiredGoods().size();
        }
        if (diff == 0) {
            getId().compareTo(other.getId());
        }
        return diff;
    }

    private int getAbilityIndex() {
        if (requiresAbility(Ability.NATIVE)) {
            return 10;
        } else if (requiresAbility(Ability.REF_UNIT)) {
            return 5;
        } else {
            return 0;
        }
    }


    // Serialization

    private static final String DOWNGRADE_TAG = "downgrade";
    private static final String EXPERT_UNIT_TAG = "expertUnit";
    private static final String MAXIMUM_COUNT_TAG = "maximumCount";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (downgrade != null) {
            xw.writeAttribute(DOWNGRADE_TAG, downgrade);
        }

        if (expertUnit != null) {
            xw.writeAttribute(EXPERT_UNIT_TAG, expertUnit);
        }

        if (maximumCount > 1) {
            xw.writeAttribute(MAXIMUM_COUNT_TAG, maximumCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        downgrade = xr.getType(spec, DOWNGRADE_TAG,
                               Role.class, (Role)null);

        expertUnit = xr.getType(spec, EXPERT_UNIT_TAG,
                                UnitType.class, (UnitType)null);

        maximumCount = xr.getAttribute(MAXIMUM_COUNT_TAG, 1);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "role"
     */
    public static String getXMLElementTagName() {
        return "role";
    }
}
