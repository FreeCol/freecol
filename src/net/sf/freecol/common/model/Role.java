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
     * carry. Defaults to <code>1</code>.
     */
    private int maximumCount = 1;

    /**
     * The expert unit for this Role, e.g. a hardy pioneer is an
     * expert for the pioneer role.
     */
    private UnitType expertUnit;

    /**
     * Sorts roles by defensive power, descendingly.
     */
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

    /**
     * Sorts roles by offensive power, descendingly.
     */
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
     * @param id a <code>String</code> value
     * @param specification a <code>Specification</code> value
     */
    public Role(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Get the <code>Downgrade</code> value.
     *
     * @return a <code>Role</code> value
     */
    public final Role getDowngrade() {
        return downgrade;
    }

    /**
     * Set the <code>Downgrade</code> value.
     *
     * @param newDowngrade The new Downgrade value.
     */
    public final void setDowngrade(final Role newDowngrade) {
        this.downgrade = newDowngrade;
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
     * Get the <code>ExpertUnit</code> value.
     *
     * @return an <code>UnitType</code> value
     */
    public final UnitType getExpertUnit() {
        return expertUnit;
    }

    /**
     * Set the <code>ExpertUnit</code> value.
     *
     * @param newExpertUnit The new ExpertUnit value.
     */
    public final void setExpertUnit(final UnitType newExpertUnit) {
        this.expertUnit = newExpertUnit;
    }

    private float getOffence() {
        return getFeatureContainer().applyModifier(1, "model.modifier.offence", null, null);
    }

    public boolean isOffensive() {
        return getOffence() > 1;
    }

    private float getDefence() {
        return getFeatureContainer().applyModifier(1, "model.modifier.defence", null, null);
    }

    public boolean isDefensive() {
        return getDefence() > 1;
    }

    public boolean isCompatibleWith(Role other) {
        return isCompatibleWith(this, other);
    }

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
            for (AbstractGoods goods : getGoodsRequired()) {
                int amount = goods.getAmount() - downgrade.getAmountRequiredOf(goods.getType());
                if (amount > 0) {
                    result.add(new AbstractGoods(goods.getType(), amount));
                }
            }
        }
        return result;
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

        downgrade = getSpecification().getType(in, "downgrade", Role.class, null);
        expertUnit = getSpecification().getType(in, "expertUnit", UnitType.class, null);
        maximumCount = getAttribute(in, "maximumCount", 1);
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

        if (downgrade != null) {
            out.writeAttribute("downgrade", downgrade.getId());
        }

        if (expertUnit != null) {
            out.writeAttribute("expertUnit", expertUnit.getId());
        }

        if (maximumCount > 1) {
            out.writeAttribute("maximumCount", Integer.toString(maximumCount));
        }
    }


    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "role"
     */
    public static String getXMLElementTagName() {
        return "role";
    }
}