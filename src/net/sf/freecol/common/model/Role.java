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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony.NoBuildReason;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The role of a unit.
 */
public class Role extends BuildableType {

    public static final String TAG = "role";

    /** Container for valid role changes. */
    public static class RoleChange {

        public final String from;
        public final String capture;

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
        = Comparator.comparingDouble((Role r) ->
            r.getOffence() + r.getDefence()).reversed();

    /**
     * The Role to downgrade to after losing a battle. Defaults to
     * {@code null}. Note that some UnitTypes and Roles may be
     * disposed instead of downgraded when losing a battle.
     */
    private Role downgrade;

    /**
     * The maximum multiple of required goods this Role may
     * carry.  Defaults to {@code 1}.
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
     * Creates a new {@code Role} instance.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public Role(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Is this the default role?
     *
     * @param roleId The role identifier to test.
     * @return True if this is the default role.
     */
    public static boolean isDefaultRoleId(String roleId) {
        return Specification.DEFAULT_ROLE_ID.equals(roleId);
    }

    /**
     * Is this the default role?
     *
     * @return True if this is the default role.
     */
    public boolean isDefaultRole() {
        return isDefaultRoleId(getId());
    }

    /**
     * Get the last part of a role identifier.
     *
     * @return The role suffix.
     */
    public String getRoleSuffix() {
        return Role.getRoleIdSuffix(getId());
    }

    /**
     * Get the last part of a role identifier.
     *
     * @param roleId A role identifier.
     * @return The role suffix.
     */
    public static String getRoleIdSuffix(String roleId) {
        return lastPart(roleId, ".");
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
    public List<AbstractGoods> getRequiredGoodsList(int roleCount) {
        List<AbstractGoods> result = getRequiredGoodsList();
        if (roleCount > 1 && !result.isEmpty()) {
            for (AbstractGoods ag : result) {
                ag.setAmount(roleCount * ag.getAmount());
            }
        }
        return result;
    }

    /**
     * Get the required goods for this role, considering also the role count,
     * as a stream.
     *
     * @param roleCount The role count.
     * @return A stream of required goods.
     */
    public Stream<AbstractGoods> getRequiredGoods(int roleCount) {
        return getRequiredGoodsList(roleCount).stream();
    }

    /**
     * Get the price of the required goods in a given market.
     *
     * @param market The {@code Market} to evaluate in.
     * @return The price of the goods for this role.
     */
    public int getRequiredGoodsPrice(Market market) {
        return sum(getRequiredGoods(),
                   ag -> market.getBidPrice(ag.getType(),
                                            ag.getAmount() * getMaximumCount()));
    }
        
    /**
     * Get the role changes that can allow a unit to assume this role.
     *
     * @return A list of {@code RoleChange}s.
     */
    public final List<RoleChange> getRoleChanges() {
        if (this.roleChanges == null) this.roleChanges = new ArrayList<>();
        return this.roleChanges;
    }

    /**
     * Set the role change list.
     *
     * @param roleChanges The new list of {@code RoleChange}s.
     */
    protected final void setRoleChanges(List<RoleChange> roleChanges) {
        if (this.roleChanges == null) {
            this.roleChanges = new ArrayList<>();
        } else {
            this.roleChanges.clear();
        }
        this.roleChanges.addAll(roleChanges);
    }

    /**
     * Add a new role change.
     *
     * @param from The source role identifier.
     * @param capture The identifier for the role to capture.
     */
    private void addRoleChange(String from, String capture) {
        if (roleChanges == null) roleChanges = new ArrayList<>();
        roleChanges.add(new RoleChange(from, capture));
    }

    /**
     * Get the offense value for this role.
     *
     * @return The offense value.
     */
    public double getOffence() {
        return apply(0.0f, null, Modifier.OFFENCE);
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
    private double getDefence() {
        return apply(0.0f, null, Modifier.DEFENCE);
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
     * @param other The other {@code Role} to compare with.
     * @return True if the other role is compatible.
     */
    public boolean isCompatibleWith(Role other) {
        return Role.rolesCompatible(this, other);
    }

    /**
     * Are two roles compatible.
     *
     * @param role1 A {@code Role} to compare.
     * @param role2 The other {@code Role} to compare.
     * @return True if the roles are compatible.
     */
    public static boolean rolesCompatible(Role role1, Role role2) {
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
     * the second.  The first role may be {@code null} implying
     * the default role, the second must not.  Note that excess goods
     * that are left over after the change will appear on the list
     * with negative amounts.
     *
     * @param from The current {@code Role}.
     * @param fromCount The role count for the current role.
     * @param to The new {@code Role} to assume.
     * @param toCount The role count for the new role.
     * @return A list of {@code AbstractGoods} required to
     *     make the change.
     */
    public static List<AbstractGoods> getGoodsDifference(Role from,
        int fromCount, Role to, int toCount) {
        List<AbstractGoods> result = new ArrayList<>();
        if (from != to && !(from == null && to.isDefaultRole())) {
            List<AbstractGoods> fromGoods = (from == null)
                ? new ArrayList<AbstractGoods>()
                : from.getRequiredGoodsList(fromCount);
            List<AbstractGoods> toGoods = to.getRequiredGoodsList(toCount);
            for (AbstractGoods ag : toGoods) {
                int amount = ag.getAmount()
                    - AbstractGoods.getCount(ag.getType(), fromGoods);
                if (amount != 0) {
                    result.add(new AbstractGoods(ag.getType(), amount));
                }
            }
            result.addAll(transform(fromGoods,
                          ag -> !any(toGoods, AbstractGoods.matches(ag.getType())),
                          ag -> new AbstractGoods(ag.getType(), -ag.getAmount())));
        }
        return result;
    }

    /**
     * Establish a simple ordering.
     *
     * Normal roles, then REF-specific roles, then native-specific
     * roles.  Reduce by role-specific equipment amounts to
     * further separate the levels such that the heavier armed roles
     * sort first.
     *
     * @return A role index.
     */
    public int getRoleIndex() {
        int base = (requiresAbility(Ability.NATIVE)) ? 30
            : (requiresAbility(Ability.REF_UNIT)) ? 20
            : 10;
        return base - getRequiredGoodsList().size();
    }

    /**
     * Filter a list of proposed roles by availability.
     *
     * @param player The {@code Player} to own the unit.
     * @param type The {@code UnitType} to check.
     * @param roles A list of proposed {@code Role}s.
     * @return A list of available {@code Role}s.
     */
    public static List<Role> getAvailableRoles(Player player, UnitType type,
                                               List<Role> roles) {
        return transform(roles, r -> r.isAvailableTo(player, type));
    }

    /**
     * Longer format debug helper.
     *
     * @return A more detailed description of this role.
     */
    public String toFullString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[Role ").append(getSuffix());
        forEachMapEntry(getRequiredAbilities(), e ->
            sb.append(' ').append(e.getKey()).append('=').append(e.getValue()));
        sb.append(']');
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public NoBuildReason canBeBuiltInColony(Colony colony,
                                            List<BuildableType> assumeBuilt) {
        return Colony.NoBuildReason.NONE;
    }

        
    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Role o = copyInCast(other, Role.class);
        if (o == null || !super.copyIn(o)) return false;
        this.downgrade = o.getDowngrade();
        this.maximumCount = o.getMaximumCount();
        this.expertUnit = o.getExpertUnit();
        this.setRoleChanges(o.getRoleChanges());
        return true;
    }


    // Serialization

    private static final String CAPTURE_TAG = "capture";
    private static final String DOWNGRADE_TAG = "downgrade";
    private static final String FROM_TAG = "from";
    private static final String EXPERT_UNIT_TAG = "expert-unit";
    private static final String MAXIMUM_COUNT_TAG = "maximum-count";
    private static final String ROLE_CHANGE_TAG = "role-change";
    // @compat 0.11.3
    private static final String OLD_EXPERT_UNIT_TAG = "expertUnit";
    private static final String OLD_MAXIMUM_COUNT_TAG = "maximumCount";
    // end @compat 0.11.3


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

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_EXPERT_UNIT_TAG)) {
            expertUnit = xr.getType(spec, OLD_EXPERT_UNIT_TAG,
                                    UnitType.class, (UnitType)null);
        } else
        // end @compat 0.11.3
            expertUnit = xr.getType(spec, EXPERT_UNIT_TAG,
                                    UnitType.class, (UnitType)null);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MAXIMUM_COUNT_TAG)) {
            maximumCount = xr.getAttribute(OLD_MAXIMUM_COUNT_TAG, 1);
        } else
        // end @compat 0.11.3
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
    public String getXMLTagName() { return TAG; }
}
