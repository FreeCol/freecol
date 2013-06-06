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
import java.util.Comparator;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class Role extends BuildableType {

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
            .applyModifier(1, "model.modifier.offence", null, null);
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
            .applyModifier(1, "model.modifier.defence", null, null);
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


    // Serialization

    private static final String DOWNGRADE_TAG = "downgrade";
    private static final String EXPERT_UNIT_TAG = "expertUnit";
    private static final String MAXIMUM_COUNT_TAG = "maximumCount";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        if (downgrade != null) {
            writeAttribute(out, DOWNGRADE_TAG, downgrade);
        }

        if (expertUnit != null) {
            writeAttribute(out, EXPERT_UNIT_TAG, expertUnit);
        }

        if (maximumCount > 1) {
            writeAttribute(out, MAXIMUM_COUNT_TAG, maximumCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        downgrade = spec.getType(in, DOWNGRADE_TAG,
                                 Role.class, (Role)null);

        expertUnit = spec.getType(in, EXPERT_UNIT_TAG,
                                  UnitType.class, (UnitType)null);

        maximumCount = getAttribute(in, MAXIMUM_COUNT_TAG, 1);
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
