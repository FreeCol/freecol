/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.Utils;


/**
 * The role of a unit.
 */
public class Role extends BuildableType {

    /** Container for valid role changes. */
    public static class RoleChange {

        public String from;
        public String capture;

        RoleChange(String from, String capture) {
            this.from = from;
            this.capture = capture;
        }

        public Role getFrom(Specification spec) {
            return spec.getRole(from);
        }

        public Role getCapture(Specification spec) {
            return spec.getRole(capture);
        }
    };

    /**
     * A comparator to order roles by descending total military
     * effectiveness.
     */
    public static final Comparator<Role> militaryComparator
        = new Comparator<Role>() {
            public int compare(Role role1, Role role2) {
                float amount1 = role1.getOffence() + role1.getDefence();
                float amount2 = role2.getOffence() + role2.getDefence();
                return (amount1 < amount2) ? 1
                    : (amount1 > amount2) ? -1
                    : 0;
            }
        };

    /** A comparator to sort roles by descending defensive power. */
    public static final Comparator<Role> defensiveComparator
        = new Comparator<Role>() {
            public int compare(Role role1, Role role2) {
                float defence1 = role1.getDefence();
                float defence2 = role2.getDefence();
                return (defence1 > defence2) ? 1
                    : (defence1 < defence2) ? -1
                    : 0;
            }
        };

    /** A comparator to sort roles by descending offensive power. */
    public static final Comparator<Role> offensiveComparator
        = new Comparator<Role>() {
            public int compare(Role role1, Role role2) {
                float offence1 = role1.getOffence();
                float offence2 = role2.getOffence();
                return (offence1 > offence2) ? 1
                    : (offence1 < offence2) ? -1
                    : 0;
            }
        };

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

    /** The role changes by capture available for this role. */
    private List<RoleChange> roleChanges = null;


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
     * Is this the default role?
     *
     * @return True if this is the default role.
     */
    public boolean isDefaultRole() {
        return Specification.DEFAULT_ROLE_ID.equals(getId());
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
     * @return The role suffix.
     */
    public String getRoleSuffix() {
        return Role.getRoleSuffix(getId());
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
        return (Specification.DEFAULT_ROLE_ID.equals(roleId)) ? null
            : "model.unit.role." + getRoleSuffix(roleId);
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
     * Get the required goods for this role, considering also the role count.
     *
     * @param roleCount The role count.
     * @return A list of required goods.
     */
    public List<AbstractGoods> getRequiredGoods(int roleCount) {
        List<AbstractGoods> result = getRequiredGoods();
        if (roleCount > 1 && !result.isEmpty()) {
            for (AbstractGoods ag : result) {
                ag.setAmount(roleCount * ag.getAmount());
            }
        }
        return result;
    }

    /**
     * Get the role changes that can allow a unit to assume this role.
     *
     * @return A list of <code>RoleChange</code>s.
     */
    public final List<RoleChange> getRoleChanges() {
        return (roleChanges == null) ? Collections.<RoleChange>emptyList()
            : this.roleChanges;
    }

    /**
     * Add a new role change.
     *
     * @param from The source role identifier.
     * @param capture The identifier for the role to capture.
     */
    public void addRoleChange(String from, String capture) {
        if (roleChanges == null) roleChanges = new ArrayList<RoleChange>();
        roleChanges.add(new RoleChange(from, capture));
    }

    /**
     * Get the offense value for this role.
     *
     * @return The offense value.
     */
    public float getOffence() {
        return applyModifiers(0f, null, Modifier.OFFENCE);
    }

    /**
     * Is this an offensive role?
     *
     * @return True if this is an offensive role.
     */
    public boolean isOffensive() {
        return hasModifier(Modifier.OFFENCE);
    }

    /**
     * Get the defence value for this role.
     *
     * @return The defence value.
     */
    private float getDefence() {
        return applyModifiers(0f, null, Modifier.DEFENCE);
    }

    /**
     * Is this an defensive role?
     *
     * @return True if this is an defensive role.
     */
    public boolean isDefensive() {
        return hasModifier(Modifier.DEFENCE);
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

    /**
     * Gets a list of goods required to change from the first role to
     * the second.  The first role may be <code>null</code> implying
     * the default role, the second must not.  Note that excess goods
     * that are left over after the change will appear on the list
     * with negative amounts.
     *
     * @param from The current <code>Role</code>.
     * @param fromCount The role count for the current role.
     * @param to The new <code>Role</code> to assume.
     * @param toCount The role count for the new role.
     * @return A list of <code>AbstractGoods</code> required to
     *     make the change.
     */
    public static List<AbstractGoods> getGoodsDifference(Role from,
        int fromCount, Role to, int toCount) {
        List<AbstractGoods> result = new ArrayList<AbstractGoods>();
        if (from != to && !(from == null && to.isDefaultRole())) {
            List<AbstractGoods> fromGoods = (from == null)
                ? new ArrayList<AbstractGoods>()
                : from.getRequiredGoods(fromCount);
            List<AbstractGoods> toGoods = to.getRequiredGoods(toCount);
            for (AbstractGoods ag : toGoods) {
                int amount = ag.getAmount()
                    - AbstractGoods.getCount(ag.getType(), fromGoods);
                if (amount != 0) {
                    result.add(new AbstractGoods(ag.getType(), amount));
                }
            }
            for (AbstractGoods ag : fromGoods) {
                if (AbstractGoods.findByType(ag.getType(), toGoods) == null) {
                    result.add(new AbstractGoods(ag.getType(), -ag.getAmount()));
                }
            }
        }
        return result;
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

    /**
     * Filter a list of proposed roles by availability.
     *
     * @param player The <code>Player</code> to own the unit.
     * @param type The <code>UnitType</code> to check.
     * @param roles A list of proposed <code>Role</code>s.
     * @return A list of available <code>Role</code>s.
     */
    public static List<Role> getAvailableRoles(Player player, UnitType type,
                                               List<Role> roles) {
        List<Role> result = new ArrayList<Role>();
        for (Role r : roles) {
            if (r.isAvailableTo(player, type)) result.add(r);
        }
        return result;
    }

    /**
     * Longer format debug helper.
     *
     * @return A more detailed description of this role.
     */
    public String toFullString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[Role ").append(getSuffix());
        for (Entry<String, Boolean> entry : getRequiredAbilities().entrySet()) {
            sb.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("]");
        return sb.toString();
    }

        
    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FreeColObject other) {
        int cmp = 0;
        if (other instanceof Role) {
            Role role = (Role)other;
            cmp = role.getAbilityIndex() - this.getAbilityIndex();
            if (cmp == 0) {
                cmp = role.getRequiredGoods().size()
                    - this.getRequiredGoods().size();
            }
        }
        if (cmp == 0) cmp = super.compareTo(other);
        return cmp;
    }


    // Serialization

    private static final String CAPTURE_TAG = "capture";
    private static final String DOWNGRADE_TAG = "downgrade";
    private static final String FROM_TAG = "from";
    private static final String EXPERT_UNIT_TAG = "expertUnit";
    private static final String MAXIMUM_COUNT_TAG = "maximumCount";
    private static final String ROLE_CHANGE_TAG = "role-change";


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
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (RoleChange rc : getRoleChanges()) {
            xw.writeStartElement(ROLE_CHANGE_TAG);

            xw.writeAttribute(FROM_TAG, rc.from);

            xw.writeAttribute(CAPTURE_TAG, rc.capture);

            xw.writeEndElement();
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
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            roleChanges = null;
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (ROLE_CHANGE_TAG.equals(tag)) {
            String from = xr.getAttribute(FROM_TAG, (String)null);
            String capture = xr.getAttribute(CAPTURE_TAG, (String)null);
            addRoleChange(from, capture);
            xr.closeTag(ROLE_CHANGE_TAG);

        } else {
            super.readChild(xr);
        }
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
